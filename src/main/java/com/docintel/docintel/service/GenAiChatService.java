package com.docintel.docintel.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.evaluation.EvaluationResponse;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import static org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID;

@Service
public class GenAiChatService {
    private static final Logger logger =
            LoggerFactory.getLogger(GenAiChatService.class);

    private final ResponseHelper responseHelper;
    private final GoogleGenAiChatModel chatModel;
    private final ChatClient chatClient;
    private final QuestionAnswerAdvisor qaAdvisor;

    public GenAiChatService(GoogleGenAiChatModel chatModel, ChatMemory chatMemory,
                            String systemPrompt, ResponseHelper responseHelper, VectorStore vectorStore) {
        this.chatModel = chatModel;
        logger.info("Chat initialized with model: {}, prompt: {}",
                chatModel.getDefaultOptions().getModel(), systemPrompt);
        this.responseHelper = responseHelper;
        this.chatClient = ChatClient.builder(chatModel)
                .defaultSystem(systemPrompt)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build()
                )
                .build();

        this.qaAdvisor = QuestionAnswerAdvisor
                .builder(vectorStore)
                .searchRequest(
                        SearchRequest.builder()
                                .topK(3)
                                .similarityThreshold(0.35)
                                .build()
                ).order(5) // among others advisor when it will act
                .build();
    }

    public String getRelevantInfoFromRag(String message, String convId) {
        ChatClient.CallResponseSpec responseHolder = chatClient.prompt()
                .user(message + "\nProvide references wherever available.")
                .advisors(this.qaAdvisor)
                .advisors(a -> a.param(CONVERSATION_ID, convId))
                .call();

        ChatResponse chatResponse = responseHolder.chatResponse();
        EvaluationResponse evaluationResponse =
                this.responseHelper.getEvaluationResponse(message, chatResponse, this.chatModel);
        var text = this.responseHelper.getResponse(chatResponse);
        logger.info("conversation[{}] {}", convId, text);
        this.responseHelper.getUsageData(chatResponse);
        return evaluationResponse != null ? text + "\n\nEvaluation: " + evaluationResponse : text;
    }
}
