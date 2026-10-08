package com.docintel.docintel.controller;

import com.docintel.docintel.service.GenAiChatService;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
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

    @GetMapping("/ai/reactive_chat")
    Mono<String> generationReactive(
            @RequestParam String message,
            @RequestParam(required = false, defaultValue = "5z65c1d8") String conversationId) {

        logger.info("Received request conversationId={}, query={}", conversationId, message);

        return Mono.just(getOrCreateConversationId(conversationId))
                .doOnNext(cid -> logger.info("Effective conversationId={}", cid))
                .flatMap(cid ->
                        getSynthesisFromLLM(message, cid)
                )
                .timeout(Duration.ofSeconds(30));



    }

    @NotNull
    private Mono<String> getSynthesisFromLLM(String message, String cid) {
        return genAiChatService.getRelevantInfoFromRagReactive(message, cid)
                .doOnSubscribe(s -> logger.info("Starting RAG stream convId={}, query: {}", cid, message))
                .doOnNext(r -> logger.info("Received ChatResponse with {} results",
                        r.getResults().size()))
                .flatMapIterable(ChatResponse::getResults)
                .map(res -> {
                    AssistantMessage replies = res.getOutput();
                    return replies.getText() != null ? replies.getText() : "";
                })
                .filter(text -> !text.isBlank())
                .collectList()
                .doOnNext(list -> logger.info("Collected {} text fragments convId={}", list.size(), cid))
                .map(list -> String.join("", list))
                .doOnSuccess(finalText -> logger.info("Final response for convId={}, {}", cid,finalText));
    }
}
