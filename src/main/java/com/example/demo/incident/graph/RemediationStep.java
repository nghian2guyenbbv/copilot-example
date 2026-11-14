package com.example.demo.incident.graph;

import java.io.Serializable;

public record RemediationStep(String command, boolean dangerous, String reason) implements Serializable {
}
