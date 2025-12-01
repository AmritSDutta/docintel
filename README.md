# 📄 DocIntel-Lite for Spring AI (PDF-only edition)

A compact **Spring Boot + Spring AI 1.1** microservice that ingests PDFs, extracts multimodal content with Gemini, embeds text, stores vectors in **Qdrant**, retrieves context, generates LLM answers, and auto-evaluates them with an LLM-as-judge.  
*(Small, focused, and surprisingly sassy.)* 🤖✨

---

## 🌟 Highlights
- **PDF ingestion only** (tested)
- Multimodal extraction via `gemini-2.5-flash-lite`
- Embeddings with `text-embedding-004` (256-dim)
- Vector store: **Qdrant** (via Spring AI auto-config)
- Retrieval → Generation → Evaluation (Relevancy + Fact-checking)
- Grounded answers with page references

---

## 🏗️ Architecture (Spring-themed)
### DocIntel-Lite Architecture

Components (with Spring vibes):
- **PDF Upload** → page-wise vision extraction (Gemini)
- **Embedding** (text-embedding-004) → vectors stored in **Qdrant**
- **Retrieval** via Spring AI VectorStore abstraction
- **Generation** with Gemini; **Evaluation** with GPT-5-nano (LLM-as-Judge)
- Telemetry: token usage, latency, evaluation scores

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
- `POST /ingest/pdf` — multipart form upload: file parameter (PDF). Ingests, extracts, chunks, embeds, upserts to Qdrant.
- `POST /chat` — body: `{ "conversationId": "<id>", "query": "<your question>" }`. Returns grounded answer + evaluation object.

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