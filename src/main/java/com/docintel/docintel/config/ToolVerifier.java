package com.docintel.docintel.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
@ConditionalOnProperty(
        name = "tool.verifier.enabled",
        havingValue = "true",
        matchIfMissing = false
)
public class ToolVerifier {
    private static final Logger logger = LoggerFactory.getLogger(ToolVerifier.class);
    private final ToolCallbackProvider toolProvider;

    public ToolVerifier(ToolCallbackProvider toolProvider) {

        this.toolProvider = toolProvider;
    }

    @PostConstruct
    public void checkTools() {

        ToolCallback[] callbacks = toolProvider.getToolCallbacks();

        String[] toolNames = Arrays.stream(callbacks).map(ToolCallback::getToolDefinition)
                .map(ToolDefinition::name)
                .toArray(String[]::new);


        logger.info("MCP Tools discovered after startup: {}", Arrays.toString(toolNames));
    }
}