package com.docintel.docintel.controller;

import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GenAiChatController {

    private final GoogleGenAiChatModel chatClient;

    public GenAiChatController(GoogleGenAiChatModel chatClient) {
        this.chatClient =  chatClient;
    }

    @GetMapping("/ai/chat")
    String generation(@RequestParam(value = "message",
                              defaultValue = "Hello LLM") String userInput) {
        return this.chatClient.call("provide succinct answers."+ userInput);


    }
}
