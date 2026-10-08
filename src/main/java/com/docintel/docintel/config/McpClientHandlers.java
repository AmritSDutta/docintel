package com.docintel.docintel.config;

import io.modelcontextprotocol.spec.McpSchema;
import org.springaicommunity.mcp.annotation.McpLogging;
import org.springaicommunity.mcp.annotation.McpProgress;
import org.springframework.stereotype.Component;

@Component
public class McpClientHandlers {

    @McpLogging(clients = "mcp_server")
    public void handleLoggingMessage(McpSchema.LoggingMessageNotification notification) {
        System.out.print("Received MCP log: " + notification.level() + " - " + notification.data());
    }

    /*@McpLogging(clients = "mcp_server")
    public void handleLoggingWithParams(McpSchema.LoggingLevel level, String logger, String data) {
        System.out.println(String.format("[%s] %s: %s", level, logger, data));
    }*/


    @McpProgress(clients = "mcp_server")
    public void handleProgressNotification(McpSchema.ProgressNotification notification) {
        double percentage = notification.progress() * 100;
        System.out.printf("Progress: %.2f%% - %s%n", percentage, notification.message());
    }


}
