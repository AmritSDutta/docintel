package com.docintel.docintel.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Component
public class ResponseHelper {

    private static final Logger logger = LoggerFactory.getLogger(ResponseHelper.class);

    /**
     * Safely extract the assistant text from a ChatResponse.
     *
     * @param chatResponse response returned by the model (may be null)
     * @return assistant text or the literal "No response" when not available
     */
    protected String getResponse(ChatResponse chatResponse) {
        if (chatResponse == null) {
            return "No response";
        }

        var results = chatResponse.getResults();
        if (CollectionUtils.isEmpty(results)) {
            return "No response";
        }

        Generation last = results.getLast();
        if (last == null) {
            return "No response";
        }

        AssistantMessage output = last.getOutput();
        return (output.getText() != null) ? output.getText() : "No response";
    }

    /**
     * Read token-usage from the ChatResponse metadata, log it and return a formatted summary.
     *
     * @param chatResponse response returned by the model (may be null)
     * @return formatted usage summary (never null)
     */
    protected String getUsageData(ChatResponse chatResponse) {
        final var noUsage = "No usage metadata found.";
        if (chatResponse == null) {
            logger.info(noUsage);
            return noUsage;
        }

        ChatResponseMetadata meta = chatResponse.getMetadata();
        if (meta.getUsage() == null) {
            logger.info(noUsage);
            return noUsage;
        }

        Usage usage = meta.getUsage();
        try {
            // account for possible null Integer fields
            int total = usage.getTotalTokens() != null ? usage.getTotalTokens() : 0;
            int prompt = usage.getPromptTokens() != null ? usage.getPromptTokens() : 0;
            int completion = usage.getCompletionTokens() != null ? usage.getCompletionTokens() : 0;

            var summary = "Tokens — total: %d, prompt: %d, completion: %d"
                    .formatted(total, prompt, completion);

            logger.info(summary);
            return summary;
        } catch (Exception ex) {
            logger.error("Error parsing usage metadata", ex);
            String raw = "Raw usage: " + usage;
            logger.info(raw);
            return raw;
        }
    }

}
