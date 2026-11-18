package com.example.ordermcp;

import com.example.ordermcp.tool.OrderTools;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class OrderMcpApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderMcpApplication.class, args);
    }

    @Bean
    ToolCallbackProvider orderToolCallbacks(OrderTools orderTools) {
        return MethodToolCallbackProvider.builder().toolObjects(orderTools).build();
    }
}
