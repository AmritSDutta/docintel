package com.docintel.docintel.config;

import com.google.genai.Client;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.ai.google.genai.common.GoogleGenAiSafetySetting;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Configuration
public class ChatConfig {


    @Bean
    public String systemPrompt() throws IOException {
        Resource res = new ClassPathResource("prompts/system.md");
        return res.getContentAsString(StandardCharsets.UTF_8);
    }

    @Bean
    public GoogleGenAiChatModel googleGenAiChatModel(Client genAiClient) {
        var safety = List.of(
                new GoogleGenAiSafetySetting(
                        GoogleGenAiSafetySetting.HarmCategory.HARM_CATEGORY_HATE_SPEECH,
                        GoogleGenAiSafetySetting.HarmBlockThreshold.BLOCK_ONLY_HIGH,
                        GoogleGenAiSafetySetting.HarmBlockMethod.PROBABILITY
                ),
                new GoogleGenAiSafetySetting(
                        GoogleGenAiSafetySetting.HarmCategory.HARM_CATEGORY_DANGEROUS_CONTENT,
                        GoogleGenAiSafetySetting.HarmBlockThreshold.BLOCK_ONLY_HIGH,
                        GoogleGenAiSafetySetting.HarmBlockMethod.PROBABILITY
                ),
                new GoogleGenAiSafetySetting(
                        GoogleGenAiSafetySetting.HarmCategory.HARM_CATEGORY_HARASSMENT,
                        GoogleGenAiSafetySetting.HarmBlockThreshold.BLOCK_ONLY_HIGH,
                        GoogleGenAiSafetySetting.HarmBlockMethod.PROBABILITY
                ),
                new GoogleGenAiSafetySetting(
                        GoogleGenAiSafetySetting.HarmCategory.HARM_CATEGORY_SEXUALLY_EXPLICIT,
                        GoogleGenAiSafetySetting.HarmBlockThreshold.BLOCK_ONLY_HIGH,
                        GoogleGenAiSafetySetting.HarmBlockMethod.PROBABILITY
                )
        );


        return GoogleGenAiChatModel.builder()
                .genAiClient(genAiClient)
                .defaultOptions(
                        GoogleGenAiChatOptions
                                .builder()
                                .safetySettings(safety)
                                .model(GoogleGenAiChatModel.ChatModel.GEMINI_2_5_FLASH_LIGHT)
                                .build()
                )
                .build();
    }

    @Bean
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder().maxMessages(60).build();
    }

}

