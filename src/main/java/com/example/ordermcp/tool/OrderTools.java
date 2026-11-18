package com.example.ordermcp.tool;

import com.example.ordermcp.model.CreateOrderRequest;
import com.example.ordermcp.model.Order;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class OrderTools {

    private final RestClient restClient;

    public OrderTools(@Value("${order.api.base-url:http://localhost:${server.port:8080}}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    @Tool(name = "searchOrder",
            description = "Find, look up or get an EXISTING order by its numeric order ID. "
                    + "Use for requests like 'find the order 123' or 'show order 123'.")
    public OrderSearchResult searchOrder(@ToolParam(description = "Numeric order ID, e.g. 123") long orderId) {
        return restClient.get()
                .uri("/api/orders/{id}", orderId)
                .exchange((req, res) -> res.getStatusCode().is2xxSuccessful()
                        ? new OrderSearchResult(true, res.bodyTo(Order.class), "Order found")
                        : new OrderSearchResult(false, null, "Order " + orderId + " not found"));
    }

    public record OrderSearchResult(boolean found, Order order, String message) {
    }

    @Tool(name = "createOrder",
            description = "Create a NEW order. Use for requests like 'create an order' or 'place an order'. "
                    + "Returns the created order including its generated ID.")
    public Order createOrder(
            @ToolParam(description = "Name of the customer") String customerName,
            @ToolParam(description = "Product being ordered") String product,
            @ToolParam(description = "Quantity, positive integer") int quantity) {
        return restClient.post()
                .uri("/api/orders")
                .body(new CreateOrderRequest(customerName, product, quantity))
                .retrieve()
                .body(Order.class);
    }
}
