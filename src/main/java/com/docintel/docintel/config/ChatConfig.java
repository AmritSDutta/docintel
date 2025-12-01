package com.docintel.docintel.config;

import com.google.genai.Client;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.TokenCountBatchingStrategy;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.ai.google.genai.GoogleGenAiEmbeddingConnectionDetails;
import org.springframework.ai.google.genai.common.GoogleGenAiSafetySetting;
import org.springframework.ai.google.genai.text.GoogleGenAiTextEmbeddingModel;
import org.springframework.ai.google.genai.text.GoogleGenAiTextEmbeddingModelName;
import org.springframework.ai.google.genai.text.GoogleGenAiTextEmbeddingOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.qdrant.QdrantVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.retry.support.RetryTemplateBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Function;

@Configuration
public class ChatConfig {


    @Bean
    public String systemPrompt() throws IOException {
        Resource res = new ClassPathResource("prompts/system.md");
        return res.getContentAsString(StandardCharsets.UTF_8);
    }

    @Bean
    public RetryTemplate retryTemplate() {
        return new RetryTemplateBuilder()
                .maxAttempts(3)
                .exponentialBackoff(300, 2.0, 3000)
                .retryOn(Exception.class)
                .build();
    }


    @Bean
    public GoogleGenAiChatModel googleGenAiChatModel(Client genAiClient, RetryTemplate retryTemplate) {
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
                .retryTemplate(retryTemplate)
                .build();
    }

    @Bean
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder().maxMessages(60).build();
    }

    @Bean
    public GoogleGenAiEmbeddingConnectionDetails googleGenAiEmbeddingConnectionDetails(@Value("${GEMINI_API_KEY}") String apiKey) {
        return GoogleGenAiEmbeddingConnectionDetails.builder().apiKey(apiKey).build();
    }

    @Bean
    public GoogleGenAiTextEmbeddingOptions googleGenAiTextEmbeddingOptions() {
        return GoogleGenAiTextEmbeddingOptions.builder()
                .model(GoogleGenAiTextEmbeddingModelName.TEXT_EMBEDDING_004.getName())
                .taskType(GoogleGenAiTextEmbeddingOptions.TaskType.RETRIEVAL_DOCUMENT)
                .dimensions(768)
                .build();
    }

    @Bean
    public EmbeddingModel embeddingModel(GoogleGenAiEmbeddingConnectionDetails googleGenAiEmbeddingConnectionDetails,
                                         GoogleGenAiTextEmbeddingOptions googleGenAiTextEmbeddingOptions) {
        return new GoogleGenAiTextEmbeddingModel(googleGenAiEmbeddingConnectionDetails, googleGenAiTextEmbeddingOptions);
    }

    @Bean
    public QdrantClient qdrantClient(
            @Value("${spring.ai.vectorstore.qdrant.host}") String host,
            @Value("${spring.ai.vectorstore.qdrant.port}") int port,
            @Value("${spring.ai.vectorstore.qdrant.api-key}") String apiKey
    ) {
        QdrantGrpcClient.Builder grpcClientBuilder =
                QdrantGrpcClient.newBuilder(
                        host, port, true, false);
        grpcClientBuilder.withApiKey(apiKey);

        return new QdrantClient(grpcClientBuilder.build());
    }

    @Bean
    public VectorStore vectorStore(QdrantClient qdrantClient, EmbeddingModel embeddingModel) {
        return QdrantVectorStore.builder(qdrantClient, embeddingModel)
                .initializeSchema(true)
                .batchingStrategy(new TokenCountBatchingStrategy())
                .build();
    }

    @Bean
    public OpenAiChatModel openAiChatModel(OpenAiApi openAiApi, RetryTemplate retryTemplate) {
        return OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(
                        OpenAiChatOptions.builder()
                                .model(OpenAiApi.ChatModel.GPT_5_NANO)
                                .build())
                .retryTemplate(retryTemplate)
                .build();
    }

    @Bean
    public SimpleLoggerAdvisor chatLoggerAdvisor() {
        Function<ChatClientRequest, String> requestToString = req -> {
            assert req.prompt().getOptions() != null;
            var model = req.prompt().getOptions().getModel();
            return "modelOptions=" + (model != null ? model : "null");
        };

        Function<ChatResponse, String> responseToString = resp -> {
            var meta = resp.getMetadata();
            return "metadata=" + meta.getUsage().toString();
        };

        return new SimpleLoggerAdvisor(
                requestToString,
                responseToString, 0);
    }

    @Bean
    public ChatClient.Builder openAiChatClientBuilder(
            OpenAiChatModel openAiModel,
            SimpleLoggerAdvisor chatLoggerAdvisor) {
        return ChatClient.builder(openAiModel).defaultAdvisors(chatLoggerAdvisor);
    }


}

