package com.wasteflow.desktop;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.concurrent.ExecutionException;

public final class WasteFlowDesktop {
    private static final Color INK = new Color(28, 43, 38);
    private static final Color GREEN = new Color(34, 113, 77);
    private static final Color PALE = new Color(241, 246, 242);
    private static final Color MUTED = new Color(103, 117, 109);

    private final WasteFlowApiClient api;
    private final JFrame frame = new JFrame("WasteFlow · Operations");
    private final DefaultTableModel requestModel = tableModel("Priority", "Request", "Bin", "Urgency");
    private final DefaultTableModel binModel = tableModel("Bin", "Location", "Fill", "Capacity", "Status");
    private final DefaultTableModel stopModel = tableModel("Stop", "Bin ID", "Status", "Collected");
    private final JTable requestsTable = table(requestModel, ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    private final JTable binsTable = table(binModel, ListSelectionModel.SINGLE_SELECTION);
    private final JTable stopsTable = table(stopModel, ListSelectionModel.SINGLE_SELECTION);
    private final JComboBox<WasteFlowApiClient.Truck> truckPicker = new JComboBox<>();
    private final JComboBox<String> algorithmPicker = new JComboBox<>(new String[]{"DIJKSTRA", "NEAREST_STOP"});
    private final JButton refreshButton = new JButton("Refresh");
    private final JButton tickButton = new JButton("Advance simulation");
    private final JButton optimizeButton = new JButton("Plan selected requests");
    private final JButton undoRouteButton = new JButton("Undo latest plan");
    private final JButton startButton = new JButton("Start route");
    private final JButton completeStopButton = new JButton("Complete selected stop");
    private final JButton completeRouteButton = new JButton("Finish route");
    private final JLabel statusLabel = new JLabel("Connect to the WasteFlow API to load operations.");
    private final JLabel routeSummary = new JLabel("No active route");
    private List<WasteFlowApiClient.CollectionRequest> pendingRequests = List.of();
    private List<WasteFlowApiClient.Truck> trucks = List.of();
    private WasteFlowApiClient.Route activeRoute;

    private WasteFlowDesktop(String baseUrl) {
        api = new WasteFlowApiClient(baseUrl);
        buildWindow(baseUrl);
    }

    public static void main(String[] args) {
        String baseUrl = args.length > 0 ? args[0] : System.getenv().getOrDefault("WASTEFLOW_API_URL", "http://localhost:8080");
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException
                     | UnsupportedLookAndFeelException exception) {
                System.err.println("Could not load the system look and feel: " + exception.getMessage());
            }
            new WasteFlowDesktop(baseUrl).frame.setVisible(true);
        });
    }

    private void buildWindow(String baseUrl) {
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.setMinimumSize(new Dimension(1020, 680));
        frame.setSize(1240, 780);
        frame.setLocationByPlatform(true);

        JPanel root = new JPanel(new BorderLayout(0, 18));
        root.setBorder(BorderFactory.createEmptyBorder(20, 22, 16, 22));
        root.setBackground(PALE);
        root.add(buildHeader(baseUrl), BorderLayout.NORTH);
        root.add(buildWorkspace(), BorderLayout.CENTER);
        root.add(buildFooter(), BorderLayout.SOUTH);
        frame.setContentPane(root);

        refreshButton.addActionListener(event -> refresh());
        tickButton.addActionListener(event -> runAction("Advancing simulation", () -> {
            WasteFlowApiClient.SimulationTick result = api.tick();
            return operationResult("Simulation updated " + result.changedBinCount() + " bins and created "
                    + result.createdRequestCount() + " requests.");
        }));
        optimizeButton.addActionListener(event -> optimizeRoute());
        undoRouteButton.addActionListener(event -> runAction("Undoing latest route plan", () -> {
            WasteFlowApiClient.Route cancelledRoute = api.undoLatestPlannedRoute();
            return operationResult("Route #" + cancelledRoute.id()
                    + " cancelled; its requests are pending again.");
        }));
        startButton.addActionListener(event -> mutateRoute("Starting route", api::startRoute, "Route started."));
        completeStopButton.addActionListener(event -> completeSelectedStop());
        completeRouteButton.addActionListener(event -> mutateRoute("Finishing route", api::completeRoute, "Route completed; the truck is available again."));
        stopsTable.getSelectionModel().addListSelectionListener(event -> updateActionAvailability());

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                frame.dispose();
            }
        });
        updateActionAvailability();
        refresh();
    }

    private JPanel buildHeader(String baseUrl) {
        JPanel header = new JPanel(new BorderLayout(12, 8));
        header.setOpaque(false);
        JPanel title = new JPanel(new GridLayout(2, 1, 0, 4));
        title.setOpaque(false);
        JLabel heading = new JLabel("WasteFlow Operations");
        heading.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 25));
        heading.setForeground(INK);
        JLabel subtitle = new JLabel("Desktop control panel  ·  " + baseUrl);
        subtitle.setForeground(MUTED);
        title.add(heading);
        title.add(subtitle);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 2));
        controls.setOpaque(false);
        stylePrimary(refreshButton);
        styleSecondary(tickButton);
        controls.add(tickButton);
        controls.add(refreshButton);
        header.add(title, BorderLayout.CENTER);
        header.add(controls, BorderLayout.EAST);
        return header;
    }

    private JSplitPane buildWorkspace() {
        JPanel left = new JPanel(new BorderLayout(0, 12));
        left.setOpaque(false);
        left.add(section("Pending collection requests", requestsTable), BorderLayout.CENTER);
        JPanel planPanel = new JPanel(new BorderLayout(8, 8));
        planPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(218, 228, 220)),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)));
        planPanel.setBackground(Color.WHITE);
        JPanel selection = new JPanel(new GridLayout(2, 2, 8, 8));
        selection.setOpaque(false);
        selection.add(new JLabel("Available truck"));
        selection.add(new JLabel("Route ordering"));
        selection.add(truckPicker);
        selection.add(algorithmPicker);
        planPanel.add(selection, BorderLayout.CENTER);
        stylePrimary(optimizeButton);
        planPanel.add(optimizeButton, BorderLayout.SOUTH);
        left.add(planPanel, BorderLayout.SOUTH);

        JPanel right = new JPanel(new GridLayout(2, 1, 0, 12));
        right.setOpaque(false);
        right.add(section("Bin network", binsTable));
        JPanel routePanel = new JPanel(new BorderLayout(0, 8));
        routePanel.setOpaque(false);
        routePanel.add(section("Active route", stopsTable), BorderLayout.CENTER);
        routePanel.add(buildRouteActions(), BorderLayout.SOUTH);
        right.add(routePanel);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
        split.setResizeWeight(0.52);
        split.setDividerLocation(630);
        split.setBorder(null);
        split.setOpaque(false);
        return split;
    }

    private JPanel buildRouteActions() {
        JPanel panel = new JPanel(new BorderLayout(6, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        panel.setOpaque(false);
        routeSummary.setForeground(INK);
        routeSummary.setFont(routeSummary.getFont().deriveFont(Font.BOLD));
        panel.add(routeSummary, BorderLayout.NORTH);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        buttons.setOpaque(false);
        styleSecondary(startButton);
        styleSecondary(undoRouteButton);
        styleSecondary(completeStopButton);
        styleSecondary(completeRouteButton);
        buttons.add(startButton);
        buttons.add(undoRouteButton);
        buttons.add(completeStopButton);
        buttons.add(completeRouteButton);
        panel.add(buttons, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        statusLabel.setForeground(MUTED);
        footer.add(statusLabel, BorderLayout.CENTER);
        return footer;
    }

    private static JPanel section(String title, JTable table) {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(218, 228, 220)),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)));
        panel.setBackground(Color.WHITE);
        JLabel label = new JLabel(title);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 15f));
        label.setForeground(INK);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(400, 220));
        panel.add(label, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private void refresh() {
        runAction("Loading WasteFlow data", () -> {
            return operationResult("Data refreshed.");
        });
    }

    private OperationResult operationResult(String message) throws Exception {
        return new OperationResult(message, fetchData());
    }

    private DashboardData fetchData() throws Exception {
        List<WasteFlowApiClient.Bin> bins = api.getBins();
        List<WasteFlowApiClient.Truck> nextTrucks = api.getTrucks();
        List<WasteFlowApiClient.CollectionRequest> requests = api.getPendingRequests();
        WasteFlowApiClient.Route route = api.getActiveRoute();
        return new DashboardData(bins, nextTrucks, requests, route);
    }

    private void applyData(DashboardData data) {
        pendingRequests = data.requests();
        trucks = data.trucks();
        activeRoute = data.route();
        binModel.setRowCount(0);
        for (WasteFlowApiClient.Bin bin : data.bins()) {
            binModel.addRow(new Object[]{bin.binCode(), bin.locationName(),
                    format(bin.currentFillKg()) + " kg", format(bin.capacityKg()) + " kg", bin.status()});
        }

        requestModel.setRowCount(0);
        for (WasteFlowApiClient.CollectionRequest request : data.requests()) {
            requestModel.addRow(new Object[]{format(request.priorityScore()), request.id(),
                    request.binCode(), request.urgency()});
        }

        truckPicker.removeAllItems();
        for (WasteFlowApiClient.Truck truck : data.trucks()) {
            if ("AVAILABLE".equals(truck.status())) {
                truckPicker.addItem(truck);
            }
        }
        fillRouteTable(data.route());
        updateActionAvailability();
    }

    private void fillRouteTable(WasteFlowApiClient.Route route) {
        stopModel.setRowCount(0);
        if (route == null) {
            routeSummary.setText("No active route");
            return;
        }
        routeSummary.setText("Route #" + route.id() + " · " + route.status() + " · "
                + format(route.totalDistance()) + " km · " + format(route.totalTime()) + " min");
        for (WasteFlowApiClient.RouteStop stop : route.stops()) {
            stopModel.addRow(new Object[]{stop.sequenceNumber(), stop.binId(), stop.status(),
                    stop.collectedKg() == null ? "—" : format(stop.collectedKg()) + " kg"});
        }
    }

    private void optimizeRoute() {
        int[] selectedRows = requestsTable.getSelectedRows();
        WasteFlowApiClient.Truck truck = (WasteFlowApiClient.Truck) truckPicker.getSelectedItem();
        if (selectedRows.length == 0 || truck == null) {
            showError("Select one or more pending requests and an available truck.");
            return;
        }
        List<Long> ids = java.util.Arrays.stream(selectedRows)
                .map(requestsTable::convertRowIndexToModel)
                .mapToObj(index -> pendingRequests.get(index).id())
                .toList();
        String algorithm = (String) algorithmPicker.getSelectedItem();
        runAction("Planning route", () -> {
            WasteFlowApiClient.Route route = api.optimize(truck.id(), ids, algorithm);
            return operationResult("Route planned with " + route.stops().size() + " stops.");
        });
    }

    private void mutateRoute(String progress, RouteOperation operation, String successMessage) {
        if (activeRoute == null) {
            showError("There is no active route.");
            return;
        }
        long routeId = activeRoute.id();
        runAction(progress, () -> {
            operation.apply(routeId);
            return operationResult(successMessage);
        });
    }

    private void completeSelectedStop() {
        if (activeRoute == null) {
            showError("There is no active route.");
            return;
        }
        int selectedRow = stopsTable.getSelectedRow();
        if (selectedRow < 0) {
            showError("Select a route stop to complete.");
            return;
        }
        int modelRow = stopsTable.convertRowIndexToModel(selectedRow);
        WasteFlowApiClient.RouteStop stop = activeRoute.stops().get(modelRow);
        if (!"PENDING".equals(stop.status())) {
            showError("Only pending stops can be completed.");
            return;
        }
        long routeId = activeRoute.id();
        runAction("Completing stop", () -> {
            api.completeStop(routeId, stop.id());
            return operationResult("Stop " + stop.sequenceNumber() + " completed.");
        });
    }

    private void runAction(String progress, SupplierWithException<OperationResult> action) {
        setBusy(true);
        statusLabel.setText(progress + "...");
        new SwingWorker<OperationResult, Void>() {
            @Override
            protected OperationResult doInBackground() throws Exception {
                return action.get();
            }

            @Override
            protected void done() {
                setBusy(false);
                try {
                    OperationResult result = get();
                    applyData(result.data());
                    statusLabel.setText(result.message());
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showError("The operation was interrupted.");
                } catch (ExecutionException exception) {
                    Throwable cause = exception.getCause();
                    statusLabel.setText("Operation failed.");
                    showError(cause.getMessage() == null ? "The WasteFlow API request failed." : cause.getMessage());
                }
                updateActionAvailability();
            }
        }.execute();
    }

    private void setBusy(boolean busy) {
        refreshButton.setEnabled(!busy);
        tickButton.setEnabled(!busy);
        optimizeButton.setEnabled(!busy && !pendingRequests.isEmpty() && truckPicker.getSelectedItem() != null
                && activeRoute == null);
        undoRouteButton.setEnabled(!busy && isAssigned());
        startButton.setEnabled(!busy && isAssigned());
        completeStopButton.setEnabled(!busy && isCollecting() && stopsTable.getSelectedRow() >= 0);
        completeRouteButton.setEnabled(!busy && isCollecting() && allStopsComplete());
        requestsTable.setEnabled(!busy);
        binsTable.setEnabled(!busy);
        stopsTable.setEnabled(!busy);
    }

    private void updateActionAvailability() {
        boolean busy = !refreshButton.isEnabled();
        optimizeButton.setEnabled(!busy && !pendingRequests.isEmpty() && truckPicker.getSelectedItem() != null
                && activeRoute == null);
        undoRouteButton.setEnabled(!busy && isAssigned());
        startButton.setEnabled(!busy && isAssigned());
        completeStopButton.setEnabled(!busy && isCollecting() && stopsTable.getSelectedRow() >= 0);
        completeRouteButton.setEnabled(!busy && isCollecting() && allStopsComplete());
    }

    private boolean isAssigned() {
        return activeRoute != null && "ASSIGNED".equals(activeRoute.status());
    }

    private boolean isCollecting() {
        return activeRoute != null && "COLLECTING".equals(activeRoute.status());
    }

    private boolean allStopsComplete() {
        return activeRoute != null && !activeRoute.stops().isEmpty()
                && activeRoute.stops().stream().allMatch(stop -> "COMPLETED".equals(stop.status()));
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(frame, message, "WasteFlow operation failed", JOptionPane.ERROR_MESSAGE);
    }

    private static String format(double value) {
        return String.format(java.util.Locale.ROOT, "%.1f", value);
    }

    private static DefaultTableModel tableModel(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    private static JTable table(DefaultTableModel model, int selectionMode) {
        JTable table = new JTable(model);
        table.setSelectionMode(selectionMode);
        table.setFillsViewportHeight(true);
        table.setRowHeight(25);
        table.getTableHeader().setReorderingAllowed(false);
        table.setSelectionBackground(new Color(218, 237, 224));
        table.setSelectionForeground(INK);
        return table;
    }

    private static void stylePrimary(JButton button) {
        button.setBackground(GREEN);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
    }

    private static void styleSecondary(JButton button) {
        button.setBackground(Color.WHITE);
        button.setForeground(INK);
        button.setFocusPainted(false);
    }

    @FunctionalInterface
    private interface SupplierWithException<T> {
        T get() throws Exception;
    }

    @FunctionalInterface
    private interface RouteOperation {
        WasteFlowApiClient.Route apply(long routeId) throws Exception;
    }

    private record DashboardData(List<WasteFlowApiClient.Bin> bins, List<WasteFlowApiClient.Truck> trucks,
                                 List<WasteFlowApiClient.CollectionRequest> requests,
                                 WasteFlowApiClient.Route route) {}

    private record OperationResult(String message, DashboardData data) {}
}
