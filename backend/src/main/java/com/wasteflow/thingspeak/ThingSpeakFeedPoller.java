package com.wasteflow.thingspeak;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.wasteflow.bin.WasteBinService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Component
public class ThingSpeakFeedPoller {

    private static final Logger LOGGER = LoggerFactory.getLogger(ThingSpeakFeedPoller.class);

    private final RestClient restClient = RestClient.create();
    private final WasteBinService binService;
    private final long channelId;
    private final String readApiKey;

    public ThingSpeakFeedPoller(
            WasteBinService binService,
            @Value("${app.thingspeak.channel-id}") long channelId,
            @Value("${app.thingspeak.read-api-key}") String readApiKey
    ) {
        this.binService = binService;
        this.channelId = channelId;
        this.readApiKey = readApiKey;
    }

    @Scheduled(
            initialDelayString = "${app.thingspeak.initial-delay-ms:20000}",
            fixedDelayString = "${app.thingspeak.poll-interval-ms:20000}"
    )
    public void pollLatestReading() {
        try {
            ThingSpeakFeedResponse response = restClient.get()
                    .uri(buildFeedUri())
                    .retrieve()
                    .body(ThingSpeakFeedResponse.class);

            if (response == null || response.feeds() == null || response.feeds().isEmpty()) {
                throw new IllegalStateException("ThingSpeak returned no sensor readings");
            }

            ThingSpeakFeed feed = response.feeds().getFirst();
            Instant polledAt = Instant.now();
            Map<Long, Double> binFillPercentages =
                    ThingSpeakFillLevelConverter.sensorReadingsToBinFillPercentages(
                            feed.field1(),
                            feed.field2(),
                            feed.field3()
                    );
            binService.updateFillPercentages(
                    binFillPercentages,
                    parseTimestamp(feed.createdAt()),
                    polledAt
            );
        } catch (RestClientException | IllegalArgumentException | IllegalStateException exception) {
            LOGGER.error("Could not update sensor bins from ThingSpeak: {}", exception.getMessage(), exception);
        }
    }

    private URI buildFeedUri() {
        return UriComponentsBuilder.fromUriString("https://api.thingspeak.com/channels/{channelId}/feeds.json")
                .queryParam("api_key", readApiKey)
                .queryParam("results", 1)
                .encode()
                .buildAndExpand(channelId)
                .toUri();
    }

    private Instant parseTimestamp(String createdAt) {
        if (createdAt == null || createdAt.isBlank()) {
            throw new IllegalArgumentException("ThingSpeak reading timestamp is missing");
        }
        try {
            return Instant.parse(createdAt);
        } catch (java.time.format.DateTimeParseException exception) {
            throw new IllegalArgumentException("ThingSpeak reading timestamp is invalid", exception);
        }
    }

    private record ThingSpeakFeedResponse(List<ThingSpeakFeed> feeds) {
    }

    private record ThingSpeakFeed(
            @JsonProperty("created_at") String createdAt,
            String field1,
            String field2,
            String field3
    ) {
    }
}
