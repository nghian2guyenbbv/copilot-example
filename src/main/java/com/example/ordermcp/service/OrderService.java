package com.example.ordermcp.service;

import com.example.ordermcp.model.CreateOrderRequest;
import com.example.ordermcp.model.Order;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Service;

@Service
public class OrderService {

    private final Map<Long, Order> orders = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(123);

    public OrderService() {
        orders.put(123L, new Order(123L, "Alice", "Laptop", 1, "CREATED"));
    }

    public Optional<Order> findById(long id) {
        return Optional.ofNullable(orders.get(id));
    }

    public Order create(CreateOrderRequest request) {
        long id = sequence.incrementAndGet();
        Order order = new Order(id, request.customerName(), request.product(), request.quantity(), "CREATED");
        orders.put(id, order);
        return order;
    }
}
