package com.docintel.docintel.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.evaluation.RelevancyEvaluator;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.evaluation.EvaluationRequest;
import org.springframework.ai.evaluation.EvaluationResponse;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import static org.springframework.ai.model.ModelOptionsUtils.OBJECT_MAPPER;

@Component
public class ResponseHelper {

    private static final Logger logger = LoggerFactory.getLogger(ResponseHelper.class);
    @Autowired
    @Qualifier("openAiChatClientBuilder")
    private ChatClient.Builder openAiChatClientBuilder;

    private static RelevancyEvaluator evaluator;

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
     * Implements RelevancyEvaluator
     */
    protected EvaluationResponse getEvaluationResponse(
            final String message,
            final ChatResponse chatResponse,
            final GoogleGenAiChatModel chatModel) {
        EvaluationResponse evaluationResponse = null;
        if (chatResponse != null) {
            var retrievedDocs = chatResponse.getMetadata().get(QuestionAnswerAdvisor.RETRIEVED_DOCUMENTS);
            try {
                logger.trace("retrieved documents for the user query: {}",
                        OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(retrievedDocs));
            } catch (JsonProcessingException e) {
                logger.trace("documents for the user query: {}", retrievedDocs);
                logger.error("JsonProcessingException", e);
            }
            EvaluationRequest evaluationRequest = new EvaluationRequest(
                    message,
                    chatResponse.getMetadata().get(QuestionAnswerAdvisor.RETRIEVED_DOCUMENTS),
                    chatResponse.getResult().getOutput().getText()
            );
            RelevancyEvaluator evaluator = new RelevancyEvaluator(openAiChatClientBuilder);
            evaluationResponse = evaluator.evaluate(evaluationRequest);
            logger.info("evaluation data: {}", evaluationResponse);
        }
        return evaluationResponse;
    }
}
