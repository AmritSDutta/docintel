package com.docintel.docintel.controller;

import com.docintel.docintel.service.GenAiChatService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = GenAiChatController.class)
@Import(GenAiChatControllerTest.MockConfig.class)
class GenAiChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private GenAiChatService chatService;

    @Autowired
    private ChatMemory chatMemory;

    @TestConfiguration
    static class MockConfig {

        @Bean
        GenAiChatService genAiChatService() {
            return Mockito.mock(GenAiChatService.class);
        }

        @Bean
        ChatMemory chatMemory() {
            return Mockito.mock(ChatMemory.class);
        }
    }

    @Test
    void testChatEndpoint_ReturnsResponse() throws Exception {
        Mockito.when(chatService.getRelevantInfoFromRag("hello", "5z65c1d8"))
                .thenReturn("Mock response");

        mockMvc.perform(get("/ai/chat")
                        .param("message", "hello")
                        .param("conversationId", "5z65c1d8"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Response:Mock response")))
                .andExpect(content().string(containsString("[Conversation Id]: 5z65c1d8")));
    }
}
