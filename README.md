# docintel
RAG evaluation in a Spring Boot Microservice


“DocIntel-Lite for Spring AI”
(Full RAG + Evaluation in a Spring Boot Microservice)

Flow

Ingest: Upload PDF/HTML → extract plain text via a simple parser.

Embed: Use Spring AI embeddings (OpenAI / Google / Ollama).

Retrieve: Vector-store abstraction (PgVector / Elastic / in-memory).

Generate: LLM answers via Spring AI ChatClient.

Evaluate:

RelevancyEvaluator (Spring AI built-in)

FactCheckingEvaluator

Add LLM-as-Judge using Recursive Advisors → regenerate answer if evaluation < threshold.

Why showcase-worthy

End-to-end architecture

Tight integration with Spring AI 1.1

Demonstrates automated quality control

Perfect portfolio piece for GenAI Architect roles.