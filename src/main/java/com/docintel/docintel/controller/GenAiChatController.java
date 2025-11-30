package com.docintel.docintel.controller;

import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.logging.Logger;

@RestController
public class GenAiChatController {
    Logger logger = Logger.getLogger(GenAiChatController.class.getName());

    private final GoogleGenAiChatModel chatClient;

    public GenAiChatController(GoogleGenAiChatModel chatClient) {
        this.chatClient =  chatClient;
    }

    @GetMapping("/ai/chat")
    String generation(@RequestParam(value = "message",
                              defaultValue = "Hello LLM") String userInput) {
        ChatOptions chatOptions = ChatOptions.builder().model("gemini-2.5-flash-lite").build();
        Prompt prompt = new Prompt(userInput, chatOptions);
        ChatResponse chatResponse =  this.chatClient.call(prompt);
        ChatResponseMetadata chatResponseMetadata = chatResponse.getMetadata();
        logger.info("Model usage:"+ chatResponseMetadata.getModel());
        Usage usage = chatResponseMetadata.getUsage();
        logger.info("Token usage:"+ usage.getTotalTokens());
        return chatResponse.getResult().toString();
    }
}
