# 📄 DocIntel-Lite for Spring AI (PDF-only edition)

## 🚀 Project Overview
This project demonstrates an end-to-end **multimodal Retrieval-Augmented Generation (RAG)** pipeline built using **Spring AI 1.1**, **Google Gemini**, and **Qdrant**. It ingests PDFs, extracts structured multimodal content, embeds it, stores it in a vector database, retrieves relevant context, synthesizes answers, and performs groundedness evaluation.

## 🔧 Key Features
1. **Multimodal RAG Pipeline** — PDF ingest → Gemini extraction → embeddings → Qdrant indexing → retrieval → synthesis → evaluation.
2. **Structured Extraction** — Gemini 2.5-Flash converts pages, tables, charts, and OCR blocks into strict JSON.
3. **Vector Storage** — Google GenAI embeddings stored in Qdrant with metadata and HNSW indexing.
4. **Spring AI Integration** — Chat models, advisors, evaluators, and vector store wired via Spring AI.
5. **Configurable Retrieval** — Top-K similarity search feeding retrieved chunks into synthesis.
6. **Custom Evaluation Layer** — GroundedRelevantEvaluator scores groundedness + relevance.
7. **Deterministic Rules** — Threshold scoring, hallucination checks, provenance-aware evaluation.
8. **Pipeline Transparency** — Full logging from ingest to evaluation for debugging and audits.
9. **Further Exploration** — Vector quality monitoring, schema hardening, multi-PDF index, caching, cost-profiling.
10. **High Showcase Value** — A compact blueprint for enterprise-grade multimodal RAG systems.

## 🏗️ Architecture (Placeholder)
```
PDF → Gemini Extraction → Embeddings → Qdrant → Retrieval → Synthesis → Evaluation → Response
```

## 📦 Setup
```bash
git clone <repo-url>
cd project
mvn clean install
```

## ▶️ Run
```bash
mvn spring-boot:run
```

## 📁 Project Structure
```
src/main/java/...    # Spring AI config, advisors, evaluators
src/main/resources/   # application properties
data/                 # embeddings, indexed chunks
```

---

## 🚀 Quick Start (Docker)

Build the image:
```bash
docker build -t docintel .
```

Run the container (example):
```bash
docker run -p 8080:8080   -e GEMINI_API_KEY=<KEY>   -e OPENAI_API_KEY=<KEY>   -e QDRANT_HOST=<HOST>   -e QDRANT_API_KEY=<KEY>   docintel
```

You should see startup logs including `DocIntel Multi-Modal RAG, by Amrit` and Tomcat on port 8080.

---

## 📥 API (short)
- `POST /ingest/pdf/genai` — multipart form upload: file parameter (PDF). Ingests, extracts, chunks, embeds, upserts to Qdrant.
- `POST /ai/chat` — body: `{ "conversationId": "<id>", "query": "<your question>" }`. Returns grounded answer + evaluation object.

---
## 🤖 Sample chat response

- 🔍 Query: what does Sample Chart Figure suggest ?


- 💬 Response:
  - The Sample Chart Figure suggests a data series that begins around x=1.00,
      decreases to a minimum around x=2.00, and then increases by x=3.00.
      The specific values mentioned are:

      *   Starting around 2.8 to 3.25 at x=1.00.
      *   Decreasing to a minimum of approximately 1.2 to 1.5 at x=2.00.
      *   Increasing to approximately 3.7 to 3.8 at x=3.00.

    References: Page 3

    Evaluation:
    - EvaluationResponse{pass=true, score=1.0, feedback='', metadata={}}

[Conversation Id]: 5z65c1d8
---
## ✅ What was tested
- PDF ingestion → extraction → embedding → qdrant upsert
- Retrieval + two-stage model generation (Gemini → GPT-5-nano)
- Evaluators returning `pass=true` with references (Page X)
- Token telemetry logged

> Note: Only PDFs have been validated. HTML/DOCX support is not included in this release.

---

## 🔧 Recommendations (quick)
- Tune Qdrant collection (dim=256, distance metric) and batching (100–500)
- Add evaluator negative test cases and configurable score threshold (e.g., 0.8)
- Consider conditional model orchestration (vision model for extraction; single synth model where possible)
- Add cost telemetry and per-conversation token caps

---

## 📚 References
- Spring AI 1.1 docs
- Qdrant best practices
- Google Generative AI model docs

---

## 🧾 License
MIT — do what you want, but please credit the intern (that’s the code) who never asks for coffee. ☕

---