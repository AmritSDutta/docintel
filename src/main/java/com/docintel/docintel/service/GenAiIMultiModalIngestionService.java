package com.docintel.docintel.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import com.google.genai.types.GenerateContentResponseUsageMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GenAiIMultiModalIngestionService {
    private static final Logger logger = LoggerFactory.getLogger(GenAiIMultiModalIngestionService.class);

    private final Client genai;
    private final VectorStore qdRantVectorStore;

    public GenAiIMultiModalIngestionService(Client genAiClient, VectorStore vectorStore) {
        this.genai = genAiClient;
        this.qdRantVectorStore = vectorStore;
    }

    public String extractFromPdf(File pdfFile) throws Exception {
        logger.info("starting extraction {}", pdfFile.getName());
        byte[] pdfBytes = Files.readAllBytes(pdfFile.toPath());

        // Build a multimodal Content object (prompt + PDF)

        Content request = Content.fromParts(
                Part.fromText("""
                        Process this PDF page by page.
                        For each page:
                        1. Extract all tables and output them in valid Markdown.
                        2. Detect images/charts/diagrams and produce detailed captions.
                        3. Extract any JSON blocks and output valid JSON.
                        4. extract all texts and output a valid json.
                        5. Identify and extract any OCR-like or machine-read text blocks verbatim and output a valid json.
                        Return strict JSON matching the provided schema.
                        """),
                Part.fromBytes(pdfBytes, "application/pdf")
        );

        GenerateContentConfig config =
                GenerateContentConfig.builder()
                        .responseMimeType("application/json")
                        .responseSchema(SchemaBuildingHelper.getGenAiSchemaForExtractedPage())
                        .build();

        GenerateContentResponse response =
                this.genai.models.generateContent("gemini-2.5-flash", request, config);

        List<Document> docs = transformIntoDocument(response.text(), pdfFile);
        logger.info("Number of docs extracted: {}", docs.size());
        reportUsage(response);

        logger.info(this.qdRantVectorStore.getName());
        this.qdRantVectorStore.add(docs);
        logger.info("inserted into qdrant");
        return response.text();
    }

    private static void reportUsage(GenerateContentResponse response) {
        response.usageMetadata().flatMap(GenerateContentResponseUsageMetadata::totalTokenCount)
                .ifPresent(t -> logger.info("total token used in extraction: {}", t));
    }

    private List<Document> transformIntoDocument(String json, File pdfFile) throws JsonProcessingException {
        logger.info("starting transform into document {}", pdfFile.getName());
        final ObjectMapper mapper = new ObjectMapper();

        List<PageExtraction> pages = mapper.readValue(json, new TypeReference<>() {
        });

        /* Convert each page into a Document  */
        List<Document> docs = new ArrayList<>();

        for (PageExtraction p : pages) {
            Map<String, Object> metadata = new HashMap<>();

            // convert each page into canonical text that embedding models can understand
            StringBuilder sb = new StringBuilder();
            sb.append("Page ").append(p.pageNumber).append("\n");

            if (p.extracted_tables != null && !p.extracted_tables.isEmpty()) {
                metadata.put("tables", p.extracted_tables);
                sb.append("Tables:\n");
                p.extracted_tables.forEach(t -> sb.append(t).append("\n"));
            }

            if (p.images != null && !p.images.isEmpty()) {
                metadata.put("images", p.images);

                sb.append("Images:\n");
                for (Map<String, Object> img : p.images) {
                    Object caption = img.get("caption");
                    if (caption != null) sb.append("Caption: ").append(caption).append("\n");
                }
            }

            if (p.jsonBlocks != null && !p.jsonBlocks.isEmpty()) {
                metadata.put("json_blocks", p.jsonBlocks);
                sb.append("JSON Blocks:\n");
                for (Object jb : p.jsonBlocks) {
                    sb.append(mapper.writeValueAsString(jb)).append("\n");
                }
            }

            String content = sb.toString().trim();
            if (content.isEmpty()) {
                content = "Page " + p.pageNumber + " (empty)";
            }

            metadata.put("page_number", p.pageNumber);
            metadata.put("file_name", pdfFile.getName());

            Document doc = new Document(content, metadata);
            docs.add(doc);
        }
        return docs;
    }
}


