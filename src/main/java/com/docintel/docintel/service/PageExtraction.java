package com.docintel.docintel.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

public class PageExtraction {
    @JsonProperty("page_number")
    public int pageNumber;

    @JsonProperty("extracted_tables")
    public List<String> extracted_tables;

    @JsonProperty("images")
    public List<Map<String, Object>> images;

    @JsonProperty("json_blocks")
    public List<Object> jsonBlocks;

    @JsonProperty("text_content")
    private String textContent;

    public int getPageNumber() {
        return pageNumber;
    }

    public List<String> getExtracted_tables() {
        return extracted_tables;
    }

    public List<Map<String, Object>> getImages() {
        return images;
    }

    public List<Object> getJsonBlocks() {
        return jsonBlocks;
    }

    public String getTextContent() {
        return textContent;
    }
}
