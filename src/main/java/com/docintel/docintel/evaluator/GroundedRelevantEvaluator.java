package com.docintel.docintel.evaluator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.evaluation.EvaluationRequest;
import org.springframework.ai.evaluation.EvaluationResponse;
import org.springframework.ai.evaluation.Evaluator;

import java.util.Collections;
import java.util.Map;

/**
 * Evaluates model responses for both contextual groundedness and query relevance.
 * Ensures the answer is supported by the provided context while directly addressing the user's query.
 * Produces a pass or fail verdict, a weighted score, and concise diagnostic feedback.
 */
public class GroundedRelevantEvaluator implements Evaluator {

    private static final Logger logger = LoggerFactory.getLogger(GroundedRelevantEvaluator.class);

    private static final PromptTemplate CUSTOM_PROMPT_TEMPLATE = new PromptTemplate("""
            You are an automatic evaluator. Judge the Response on two dimensions:
        
            1) Groundedness — are factual claims in the Response directly supported by the provided Context?
            2) Relevance — does the Response meaningfully and directly answer the Query?
        
            Return ONLY a JSON object matching CustomEvaluationResponse:
            - pass: true iff the Response is BOTH sufficiently grounded and relevant.
            - score: overall float in [0.0,1.0] computed as 0.50*grounded_score + 0.50*relevance_score.
            - feedback: one short sentence summarizing the decision (mention main failure if any).
        
            Assessment rules:
            - grounded_score = 1.0 if every factual claim is supported by context; reduce for unsupported/contradicted claims.
            - relevance_score = 1.0 if the Response answers the Query directly and usefully;
              reduce for off-topic, partial, or missing answers.
            - Any PII/PCI -> set grounded_score *= 0.5 and add "PII" to feedback.
            - pass=true only if score >= 0.7 and relevance_score >= 0.6.
        
            Query:
            {query}
        
            Response:
            {response}
        
            Context:
            {context}
        """);

    private final ChatClient.Builder chatClientBuilder;

    public GroundedRelevantEvaluator(ChatClient.Builder chatClientBuilder) {
        this.chatClientBuilder = chatClientBuilder;
    }

    @Override
    public EvaluationResponse evaluate(EvaluationRequest evaluationRequest) {

        String response = evaluationRequest.getResponseContent();
        String context = doGetSupportingData(evaluationRequest);

        EvaluationResponse fallback = new EvaluationResponse(
                false,
                0.0f,
                "Model returned null/invalid evaluation.",
                Collections.emptyMap()
        );

        String userMessage = CUSTOM_PROMPT_TEMPLATE.render(
                Map.of(
                        "query", evaluationRequest.getUserText(),
                        "response", response,
                        "context", context
                )
        );

        CustomEvaluationResponse evaluation = null;
        try {
            evaluation = this.chatClientBuilder
                    .build()
                    .prompt()
                    .user(userMessage)
                    .call()
                    .entity(CustomEvaluationResponse.class);
        } catch (Exception ex) {
            logger.error("Evaluation LLM call failed", ex);
            return fallback;
        }

        if (evaluation == null) {
            logger.warn("LLM returned null evaluation object");
            return fallback;
        }

        logger.info("Evaluator result: {}", evaluation);

        return new EvaluationResponse(
                evaluation.pass(),
                evaluation.score(),
                evaluation.feedback(),
                Collections.emptyMap()
        );
    }

    record CustomEvaluationResponse(boolean pass, float score, String feedback) {}
}
