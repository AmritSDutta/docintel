package com.docintel.docintel.evaluator;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.evaluation.RelevancyEvaluator;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.evaluation.EvaluationRequest;
import org.springframework.ai.evaluation.EvaluationResponse;
import org.springframework.util.Assert;

import java.util.Collections;
import java.util.List;

/**
 * Validates each LLM response for relevancy and retries with an augmented prompt if needed.
 * Used to enforce grounded RAG answers by recursively refining the model output.
 * Stops when a response passes validation or attempts are exhausted.
 */
public class EvaluationRecursiveAdvisor implements CallAdvisor {
    private static final Logger logger = LoggerFactory.getLogger(EvaluationRecursiveAdvisor.class);

    /**
     * Maximum total attempts allowed for this request (initial call + retries).
     * A value of 0 disables recursion and uses only the first model response.
     * Retries stop once relevancy passes or the limit is reached.
     */
    private final int maxAttempt;
    private final RelevancyEvaluator relevancyEvaluator;


    public EvaluationRecursiveAdvisor(RelevancyEvaluator relevancyEvaluator, int maxRepeatAttempts) {
        Assert.notNull(relevancyEvaluator, "relevancyEvaluator must not be null");
        Assert.isTrue(maxRepeatAttempts >= 0, "repeatAttempts must be greater than or equal to 0");
        this.relevancyEvaluator = relevancyEvaluator;
        this.maxAttempt = maxRepeatAttempts;
    }

    @SuppressWarnings("null")
    @Override
    public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
        Assert.notNull(callAdvisorChain, "callAdvisorChain must not be null");
        Assert.notNull(chatClientRequest, "chatClientRequest must not be null");
        ChatClientResponse chatClientResponse = null;
        var repeatCounter = 0;
        boolean isValidResponse = true;
        var processedChatClientRequest = chatClientRequest;

        QuestionAnswerAdvisor questionAnswerAdvisor = callAdvisorChain.getCallAdvisors()
                .stream()
                .filter(it -> it instanceof QuestionAnswerAdvisor)
                .map(it -> (QuestionAnswerAdvisor) it)
                .findFirst()
                .orElse(null);

        if (questionAnswerAdvisor == null) {
            logger.info("QuestionAnswerAdvisor not found in advisor chain, hence this advisor is ineffective");
            return callAdvisorChain.copy(this).nextCall(processedChatClientRequest);
        }

        do {
            repeatCounter++;
            chatClientResponse = callAdvisorChain.copy(this).nextCall(processedChatClientRequest);


            // We should not validate tool call requests, only the content of the final response.
            if (chatClientResponse.chatResponse() == null || !chatClientResponse.chatResponse().hasToolCalls()) {

                logger.info("Relevancy evaluation is in progress");
                EvaluationResponse validationResponse = this.evaluateAnswerRelevance(
                        chatClientRequest, chatClientResponse);
                isValidResponse = validationResponse.isPass();

                if (!isValidResponse) {
                    logger.warn("Relevance evaluation failed: {}, Attempts left: {}",
                            isValidResponse, this.maxAttempt - repeatCounter);

                    Prompt augmentedPrompt = chatClientRequest.prompt()
                            .augmentUserMessage(userMessage -> userMessage.mutate()
                                    .text(userMessage.getText() + System.lineSeparator()
                                            + """
                                            Your previous answer was not grounded in the provided documents.
                                            Relevance to the given context has failed.
                                            Please stick to the given context for response.
                                            Please recheck the context and regenerate response with references.
                                            """
                                    )
                                    .build());

                    processedChatClientRequest = chatClientRequest.mutate().prompt(augmentedPrompt).build();
                }
            }
        }
        while (!isValidResponse && repeatCounter < this.maxAttempt);
        return chatClientResponse;
    }


    private EvaluationResponse evaluateAnswerRelevance(ChatClientRequest chatClientRequest,
                                                       ChatClientResponse chatClientResponse) {
        var message = chatClientRequest.prompt().getUserMessage().getText();

        if (chatClientResponse.chatResponse() == null ||
                chatClientResponse.chatResponse().getResult().getOutput().getText() == null) {

            logger.warn("ChatClientResponse is missing, required for evaluation validation.");
            return new EvaluationResponse(false, 0.0f, "", Collections.emptyMap());
        }
        var chatResponse = chatClientResponse.chatResponse();

        List<Document> retrievedDocs = chatClientResponse.chatResponse().getMetadata()
                .get(QuestionAnswerAdvisor.RETRIEVED_DOCUMENTS);
        EvaluationRequest evaluationRequest = new EvaluationRequest(
                message,
                retrievedDocs,
                chatResponse.getResult().getOutput().getText()
        );
        try {
            var evaluationResponse = this.relevancyEvaluator.evaluate(evaluationRequest);
            logger.info("EvaluationResponse: {}", evaluationResponse);
            return evaluationResponse;
        } catch (Exception ex) {
            logger.error("RelevancyEvaluator threw exception", ex);
            return new EvaluationResponse(false, 0.0f, "evaluator-exception", Collections.emptyMap());
        }


    }

    @NotNull
    @Override
    public String getName() {
        return "Evaluator Output Validation Advisor";
    }

    @Override
    public int getOrder() {
        return 1;
    }
}
