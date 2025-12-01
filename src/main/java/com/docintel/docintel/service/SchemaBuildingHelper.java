package com.docintel.docintel.service;

import com.google.genai.types.Schema;

import java.util.List;
import java.util.Map;

public class SchemaBuildingHelper {

    public static Schema getGenAiSchemaForExtractedPage(){

        // PageExtraction object schema
        Schema pageSchema = Schema.builder()
                .type("object")
                .properties(Map.of(
                        "page_number", Schema.builder().type("integer").build(),
                        "extracted_tables", Schema.builder()
                                .type("array")
                                .nullable(true)
                                .items(Schema.builder().type("string").nullable(true).build())
                                .build(),
                        "images", Schema.builder()
                                .type("array")
                                .nullable(true)
                                .items(
                                        Schema.builder()
                                                .type("object")
                                                .properties(Map.of(
                                                        "caption", Schema.builder().type("string").nullable(true).build(),
                                                        "source", Schema.builder().type("string").nullable(true).build()
                                                ))
                                                .build()
                                ).build(),
                        "json_blocks", Schema.builder()
                                .type("array")
                                .nullable(true)
                                .items(
                                        Schema.builder()
                                                .type("object")
                                                .properties(Map.of(
                                                        "content", Schema.builder()
                                                                .type("object")
                                                                .properties(Map.of("raw", Schema.builder().type("string").nullable(true).build()))
                                                                .build()
                                                ))
                                                .build()
                                ).build(),
                        "text_content", Schema.builder().type("string").nullable(true).build()
                ))
                .required(List.of("page_number"))
                .build();

        // Array schema (list of PageExtraction)
        return Schema.builder().type("array").items(pageSchema).build();
    }
}
