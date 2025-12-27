package com.example.demo.controllers;

import com.example.demo.dao.OrderDAO;
import com.example.demo.dao.ProductDAO;
import com.example.demo.models.Order;
import com.example.demo.models.OrderItem;
import com.example.demo.models.Product;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Controller for the Reports UI.
 * Generates charts and reports based on product sales, time periods, and revenue.
 * 
 * @author Group07
 * @version 1.0
 */
public class ReportsController {
    @FXML private VBox reportsContainer;
    @FXML private Button closeButton;
    
    private OrderDAO orderDAO;
    private ProductDAO productDAO;
    
    public void setOrderDAO(OrderDAO orderDAO) {
        this.orderDAO = orderDAO;
    }
    
    public void setProductDAO(ProductDAO productDAO) {
        this.productDAO = productDAO;
    }
    
    @FXML
    public void initialize() {
        // Initialization happens when loadReports() is called
    }
    
    public void loadReports() {
        reportsContainer.getChildren().clear();
        
        System.out.println("DEBUG ReportsController: Loading reports...");
        List<Order> allOrders = orderDAO.getAllOrders();
        System.out.println("DEBUG ReportsController: Received " + allOrders.size() + " total orders from OrderDAO");
        
        // Filter only delivered orders for revenue calculations
        List<Order> deliveredOrders = allOrders.stream()
                .filter(o -> {
                    boolean isDelivered = o.getStatus() == Order.OrderStatus.DELIVERED;
                    if (isDelivered) {
                        System.out.println("DEBUG ReportsController: Found DELIVERED order #" + o.getId());
                    }
                    return isDelivered;
                })
                .collect(Collectors.toList());
        
        System.out.println("DEBUG ReportsController: Filtered to " + deliveredOrders.size() + " DELIVERED orders");
        
        // Log all order statuses for debugging
        Map<Order.OrderStatus, Long> statusCounts = allOrders.stream()
                .collect(Collectors.groupingBy(Order::getStatus, Collectors.counting()));
        System.out.println("DEBUG ReportsController: Order status breakdown: " + statusCounts);
        
        if (deliveredOrders.isEmpty()) {
            Label noDataLabel = new Label("No completed orders available for reports.\n" +
                    "Total orders in system: " + allOrders.size() + "\n" +
                    "Orders by status: " + statusCounts);
            noDataLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: gray;");
            reportsContainer.getChildren().add(noDataLabel);
            System.out.println("DEBUG ReportsController: No delivered orders found, showing empty message");
            return;
        }
        
        // 1. Revenue by Product Chart
        reportsContainer.getChildren().add(createRevenueByProductChart(deliveredOrders));
        reportsContainer.getChildren().add(new Separator());
        
        // 2. Revenue by Time Chart (Daily)
        reportsContainer.getChildren().add(createRevenueByTimeChart(deliveredOrders));
        reportsContainer.getChildren().add(new Separator());
        
        // 3. Product Sales Quantity Chart
        reportsContainer.getChildren().add(createProductSalesQuantityChart(deliveredOrders));
        reportsContainer.getChildren().add(new Separator());
        
        // 4. Summary Statistics
        reportsContainer.getChildren().add(createSummaryStatistics(deliveredOrders));
    }
    
    private VBox createRevenueByProductChart(List<Order> orders) {
        VBox chartBox = new VBox(10);
        chartBox.setStyle("-fx-padding: 15;");
        
        Label title = new Label("Revenue by Product");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        
        // Calculate revenue per product
        Map<String, BigDecimal> productRevenue = new HashMap<>();
        Map<Integer, String> productNames = new HashMap<>();
        
        List<Product> allProducts = productDAO.getAllProducts(null);
        for (Product p : allProducts) {
            productNames.put(p.getId(), p.getName());
        }
        
        for (Order order : orders) {
            for (OrderItem item : order.getItems()) {
                String productName = productNames.getOrDefault(item.getProductId(), "Product #" + item.getProductId());
                BigDecimal revenue = item.getLineTotal();
                productRevenue.merge(productName, revenue, BigDecimal::add);
            }
        }
        
        if (productRevenue.isEmpty()) {
            Label noData = new Label("No product revenue data available.");
            chartBox.getChildren().addAll(title, noData);
            return chartBox;
        }
        
        // Create bar chart
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Revenue (TL)");
        
        BarChart<String, Number> barChart = new BarChart<>(xAxis, yAxis);
        barChart.setTitle("Revenue by Product");
        barChart.setPrefHeight(400);
        
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Revenue");
        
        // Sort by revenue descending and take top 10
        productRevenue.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .limit(10)
                .forEach(entry -> {
                    series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue().doubleValue()));
                });
        
        barChart.getData().add(series);
        chartBox.getChildren().addAll(title, barChart);
        
        return chartBox;
    }
    
    private VBox createRevenueByTimeChart(List<Order> orders) {
        VBox chartBox = new VBox(10);
        chartBox.setStyle("-fx-padding: 15;");
        
        Label title = new Label("Revenue by Time (Last 30 Days)");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        
        // Calculate revenue per day for last 30 days
        Map<LocalDate, BigDecimal> dailyRevenue = new TreeMap<>();
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(30);
        
        // Initialize all dates with zero
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            dailyRevenue.put(date, BigDecimal.ZERO);
        }
        
        // Sum revenue by date
        for (Order order : orders) {
            LocalDate orderDate = order.getOrderTime().toLocalDate();
            if (!orderDate.isBefore(startDate) && !orderDate.isAfter(endDate)) {
                dailyRevenue.merge(orderDate, order.getTotalAfterTax(), BigDecimal::add);
            }
        }
        
        // Create line chart
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Revenue (TL)");
        
        LineChart<String, Number> lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setTitle("Daily Revenue (Last 30 Days)");
        lineChart.setPrefHeight(400);
        
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Daily Revenue");
        
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MM/dd");
        dailyRevenue.forEach((date, revenue) -> {
            series.getData().add(new XYChart.Data<>(date.format(dateFormatter), revenue.doubleValue()));
        });
        
        lineChart.getData().add(series);
        chartBox.getChildren().addAll(title, lineChart);
        
        return chartBox;
    }
    
    private VBox createProductSalesQuantityChart(List<Order> orders) {
        VBox chartBox = new VBox(10);
        chartBox.setStyle("-fx-padding: 15;");
        
        Label title = new Label("Product Sales Quantity");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        
        // Calculate quantity sold per product
        Map<String, BigDecimal> productQuantity = new HashMap<>();
        Map<Integer, String> productNames = new HashMap<>();
        
        List<Product> allProducts = productDAO.getAllProducts(null);
        for (Product p : allProducts) {
            productNames.put(p.getId(), p.getName());
        }
        
        for (Order order : orders) {
            for (OrderItem item : order.getItems()) {
                String productName = productNames.getOrDefault(item.getProductId(), "Product #" + item.getProductId());
                BigDecimal quantity = item.getQuantityKg();
                productQuantity.merge(productName, quantity, BigDecimal::add);
            }
        }
        
        if (productQuantity.isEmpty()) {
            Label noData = new Label("No product sales data available.");
            chartBox.getChildren().addAll(title, noData);
            return chartBox;
        }
        
        // Create pie chart
        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();
        
        // Sort by quantity descending and take top 8
        productQuantity.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .limit(8)
                .forEach(entry -> {
                    pieChartData.add(new PieChart.Data(entry.getKey(), entry.getValue().doubleValue()));
                });
        
        PieChart pieChart = new PieChart(pieChartData);
        pieChart.setTitle("Top Products by Sales Quantity (kg)");
        pieChart.setPrefHeight(400);
        
        chartBox.getChildren().addAll(title, pieChart);
        
        return chartBox;
    }
    
    private VBox createSummaryStatistics(List<Order> orders) {
        VBox statsBox = new VBox(10);
        statsBox.setStyle("-fx-padding: 15; -fx-background-color: #f0f0f0; -fx-border-radius: 5;");
        
        Label title = new Label("Summary Statistics");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        
        BigDecimal totalRevenue = orders.stream()
                .map(Order::getTotalAfterTax)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        int totalOrders = orders.size();
        
        BigDecimal averageOrderValue = totalOrders > 0 
                ? totalRevenue.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        
        // Calculate total items sold
        BigDecimal totalItemsSold = orders.stream()
                .flatMap(order -> order.getItems().stream())
                .map(OrderItem::getQuantityKg)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate earliestDate = orders.stream()
                .map(o -> o.getOrderTime().toLocalDate())
                .min(LocalDate::compareTo)
                .orElse(LocalDate.now());
        LocalDate latestDate = orders.stream()
                .map(o -> o.getOrderTime().toLocalDate())
                .max(LocalDate::compareTo)
                .orElse(LocalDate.now());
        
        VBox statsContent = new VBox(5);
        statsContent.getChildren().addAll(
                new Label("Total Revenue: " + formatPrice(totalRevenue)),
                new Label("Total Orders: " + totalOrders),
                new Label("Average Order Value: " + formatPrice(averageOrderValue)),
                new Label("Total Items Sold: " + String.format("%.2f kg", totalItemsSold.doubleValue())),
                new Label("Period: " + earliestDate.format(formatter) + " to " + latestDate.format(formatter))
        );
        
        statsBox.getChildren().addAll(title, statsContent);
        
        return statsBox;
    }
    
    @FXML
    private void handleClose() {
        ((javafx.stage.Stage) closeButton.getScene().getWindow()).close();
    }
    
    private String formatPrice(BigDecimal price) {
        return String.format("%.2f TL", price.doubleValue());
    }
}

