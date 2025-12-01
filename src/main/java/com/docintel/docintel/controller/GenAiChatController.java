package com.docintel.docintel.controller;

import com.docintel.docintel.service.GenAiChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;
import java.util.UUID;

@RestController
public class GenAiChatController {
    private static final Logger logger =
            LoggerFactory.getLogger(GenAiChatController.class);

    private final GenAiChatService genAiChatService;
    private final ChatMemory chatMemory;

    public GenAiChatController(
            GenAiChatService chatService,
            ChatMemory chatMemory) {
        this.genAiChatService = chatService;
        this.chatMemory = chatMemory;
    }

    private static String getOrCreateConversationId(String conversationId) {
        return Optional.ofNullable(conversationId)
                .filter(id -> !id.isBlank())
                .orElse(UUID.randomUUID().toString().split("-")[0]);
    }

    @GetMapping("/ai/chat")
    String generation(
            @RequestParam String message,
            @RequestParam(required = false, defaultValue = "5z65c1d8") String conversationId) {

        logger.info("received request conversation: {}", conversationId);
        var convId = getOrCreateConversationId(conversationId);
        logger.info("received request effective conversation: {}, user query: {}", convId, message);

        var response = this.genAiChatService.getRelevantInfoFromRag(message, convId);

        var ls = System.lineSeparator();
        logger.info("evaluated response[ {} ]:{} {}", convId, ls, response);
        return "Response:" + response + ls.repeat(2) + "[Conversation Id]: " + convId;
    }

    @GetMapping("/ai/history")
    public Object history(@RequestParam String conversationId) {
        logger.info("fetching conversation history : {}", conversationId);
        return chatMemory.get(conversationId);
    }

    @DeleteMapping("/ai/cleanse")
    public void deleteConversation(@RequestParam String conversationId) {
        chatMemory.clear(conversationId);
        logger.info("Cleared conversation history: {}", conversationId);
    }
}
