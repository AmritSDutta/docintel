package com.docintel.docintel.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.evaluation.RelevancyEvaluator;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.evaluation.EvaluationRequest;
import org.springframework.ai.evaluation.EvaluationResponse;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import static org.springframework.ai.model.ModelOptionsUtils.OBJECT_MAPPER;

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

    /**
     * Implements RelevancyEvaluator
     */
    protected EvaluationResponse getEvaluationResponse(
            final String message,
            final ChatResponse chatResponse,
            final GoogleGenAiChatModel chatModel) {
        EvaluationResponse evaluationResponse = null;
        if(chatResponse != null){
            var retrievedDocs = chatResponse.getMetadata().get(QuestionAnswerAdvisor.RETRIEVED_DOCUMENTS);
            try {
                logger.info("retrieved documents for the user query: {}",
                        OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(retrievedDocs));
            } catch (JsonProcessingException e) {
                logger.info("documents for the user query: {}", retrievedDocs);
                logger.error("JsonProcessingException", e);
            }
            EvaluationRequest evaluationRequest = new EvaluationRequest(
                    message,
                    chatResponse.getMetadata().get(QuestionAnswerAdvisor.RETRIEVED_DOCUMENTS),
                    chatResponse.getResult().getOutput().getText()
            );
            RelevancyEvaluator evaluator = new RelevancyEvaluator(ChatClient.builder(chatModel));
            evaluationResponse = evaluator.evaluate(evaluationRequest);
            logger.info("evaluation data: {}", evaluationResponse);
        }
        return evaluationResponse;
    }
}
