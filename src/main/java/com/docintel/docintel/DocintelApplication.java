package com.docintel.docintel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(
        exclude = {
                org.springframework.ai.model.google.genai.autoconfigure.chat.GoogleGenAiChatAutoConfiguration.class,
                org.springframework.ai.model.google.genai.autoconfigure.embedding.GoogleGenAiEmbeddingConnectionAutoConfiguration.class
        }
)
public class DocintelApplication {

    public static void main(String[] args) {
        SpringApplication.run(DocintelApplication.class, args);
    }

}
