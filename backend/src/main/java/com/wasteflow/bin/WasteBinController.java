package com.wasteflow.bin;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/bins")
public class WasteBinController {

    private final WasteBinService binService;

    public WasteBinController(WasteBinService binService) {
        this.binService = binService;
    }

    @GetMapping
    public List<WasteBinResponse> findAll() {
        return binService.findAll();
    }

    @GetMapping("/{id}")
    public WasteBinResponse findById(@PathVariable Long id) {
        return binService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WasteBinResponse create(@Valid @RequestBody CreateBinRequest request) {
        return binService.create(request);
    }

    @PutMapping("/{id}")
    public WasteBinResponse update(@PathVariable Long id, @Valid @RequestBody CreateBinRequest request) {
        return binService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        binService.delete(id);
    }
}