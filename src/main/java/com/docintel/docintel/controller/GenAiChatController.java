package com.docintel.docintel.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.evaluation.EvaluationResponse;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;
import java.util.UUID;

import static org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID;

@RestController
public class GenAiChatController {
    private static final Logger logger = LoggerFactory.getLogger(GenAiChatController.class);

    private final ResponseHelper responseHelper;
    private final GoogleGenAiChatModel chatModel;
    private final ChatClient chatClient;
    private final ChatMemory chatMemory;
    private final VectorStore qdRantVectorStore;

    public GenAiChatController(GoogleGenAiChatModel chatModel, ChatMemory chatMemory,
                               String systemPrompt, ResponseHelper responseHelper, VectorStore vectorStore)
                                {
        this.qdRantVectorStore = vectorStore;
        this.chatModel =chatModel;
        logger.info("Chat initialized with model: {}, prompt: {}",
                chatModel.getDefaultOptions().getModel(), systemPrompt);
        this.responseHelper = responseHelper;
        this.chatMemory = chatMemory;
        this.chatClient = ChatClient.builder(chatModel)
                .defaultSystem(systemPrompt)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(this.chatMemory).build()
                )
                .build();
    }

    @GetMapping("/ai/chat")
    String generation(@RequestParam String message, @RequestParam(required = false) String conversationId) {

        var convId = Optional.ofNullable(conversationId)
                .filter(id -> !id.isBlank())
                .orElse(UUID.randomUUID().toString().split("-")[0]);

        ChatClient.CallResponseSpec responseHolder = chatClient.prompt()
                .user(message)
                .advisors(QuestionAnswerAdvisor.builder(this.qdRantVectorStore).build())
                .advisors(a -> a.param(CONVERSATION_ID, convId))
                .call();

        ChatResponse chatResponse = responseHolder.chatResponse();
        EvaluationResponse evaluationResponse = this.responseHelper.getEvaluationResponse(message, chatResponse, this.chatModel);
        var text = this.responseHelper.getResponse(chatResponse);
        logger.info("conversation[{}] {}", convId, text);
        this.responseHelper.getUsageData(chatResponse);
        return evaluationResponse != null ? text + "\nEvaluation: "+ evaluationResponse.toString() : text;
    }


    @GetMapping("/ai/history")
    public Object history(@RequestParam String conversationId) {
        logger.info("fetching conversation {}", conversationId);
        return chatMemory.get(conversationId);
    }


    @DeleteMapping("/ai/cleanse")
    public void deleteConversation(@RequestParam String conversationId) {
        chatMemory.clear(conversationId);
        logger.info("Cleared conversation {}", conversationId);
    }
}
