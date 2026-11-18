package com.example.ordermcp.model;

public record CreateOrderRequest(String customerName, String product, int quantity) {
}
