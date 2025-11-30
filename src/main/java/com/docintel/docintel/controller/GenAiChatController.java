package com.docintel.docintel.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID;

@RestController
public class GenAiChatController {
    private static final Logger logger = LoggerFactory.getLogger(GenAiChatController.class);

    private final ChatClient chatClient;
    private final ChatMemory chatMemory;

    public GenAiChatController(GoogleGenAiChatModel chatModel, ChatMemory chatMemory, String systemPrompt) {
        logger.info("Chat initialized with model: {}, prompt: {}",chatModel.getDefaultOptions().getModel(),systemPrompt);

        this.chatMemory = chatMemory;
        this.chatClient = ChatClient.builder(chatModel)
                .defaultSystem(systemPrompt)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor
                                .builder(this.chatMemory)
                                .build()
                )
                .build();
    }

    @GetMapping("/ai/chat")
    String generation(@RequestParam String conversationId, @RequestParam String message) {
        ChatClient.CallResponseSpec responseHolder = chatClient.prompt()
                .user(message)
                .advisors(a -> a.param(CONVERSATION_ID, conversationId))
                .call();

        ChatResponse chatResponse = responseHolder.chatResponse();
        String text = getResponse(chatResponse);
        logger.info("conv={} -> {}", conversationId, text);
        getUsageData(chatResponse);
        return text;
    }


    @GetMapping("/ai/history")
    public Object history(@RequestParam String conversationId) {
        logger.info("fetching conversation {}", conversationId);
        return chatMemory.get(conversationId);
    }


    @DeleteMapping("/ai/conversation")
    public void deleteConversation(@RequestParam String conversationId) {
        chatMemory.clear(conversationId);
        logger.info("Cleared conversation {}", conversationId);
    }

    private String getResponse(ChatResponse chatResponse) {
        var finalResponse = "No response";
        if (chatResponse == null)
            return finalResponse;
        if (chatResponse.getResults().isEmpty())
            return finalResponse;

        Generation last_resp = chatResponse.getResults().getLast();
        AssistantMessage output = last_resp.getOutput();
        finalResponse = output.getText();
        return finalResponse;
    }

    private void getUsageData(ChatResponse chatResponse) {
        var usageSummary = "No usage metadata found.";
        if (chatResponse != null) {
            ChatResponseMetadata meta = chatResponse.getMetadata();
            if (meta.getUsage() != null) {
                Usage usage = meta.getUsage();
                try {
                    usageSummary =
                            "Tokens — total: %d, prompt: %d, completion: %d"
                                    .formatted(
                                            usage.getTotalTokens(),
                                            usage.getPromptTokens(),
                                            usage.getCompletionTokens()
                                    );

                } catch (Exception e) {
                    logger.error("error parsing metadata usage", e);
                    usageSummary = "Raw usage: " + usage.toString();
                }
            }
        }
        logger.info(usageSummary);
    }
}
