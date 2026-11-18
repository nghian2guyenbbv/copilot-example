package com.example.ordermcp.model;

public record Order(long id, String customerName, String product, int quantity, String status) {
}
