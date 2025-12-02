package com.docintel.docintel.service;

import com.docintel.docintel.evaluator.EvaluationRecursiveAdvisor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
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
                            String systemPrompt, ResponseHelper responseHelper, VectorStore vectorStore,
                            SimpleLoggerAdvisor chatLoggerAdvisor) {
        this.chatModel = chatModel;
        logger.info("Chat initialized with model: {}, prompt: {}",
                chatModel.getDefaultOptions().getModel(), systemPrompt);
        this.responseHelper = responseHelper;
        this.chatClient = ChatClient.builder(chatModel)
                .defaultSystem(systemPrompt)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build()
                )
                .defaultAdvisors(chatLoggerAdvisor)
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
                .advisors(new EvaluationRecursiveAdvisor(
                        this.responseHelper.getRelevancyEvaluator(),
                        2
                ))
                .call();

        ChatResponse chatResponse = responseHolder.chatResponse();
        EvaluationResponse customEvaluation =
                this.responseHelper.getCustomEvaluation(message, chatResponse);

        var text = this.responseHelper.getResponse(chatResponse);
        logger.info("{}{}conversation Id:[{}]", text, System.lineSeparator(), convId);
        var ls = System.lineSeparator();
        return text + ls.repeat(2) + "Evaluation:" + ls + customEvaluation;
    }
}
