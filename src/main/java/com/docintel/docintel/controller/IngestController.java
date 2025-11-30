package com.docintel.docintel.controller;

import com.docintel.docintel.service.GenAiService;
import com.docintel.docintel.service.PdfService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

@RestController
@RequestMapping("/ingest/pdf")
public class IngestController {
    private static final Logger logger = LoggerFactory.getLogger(IngestController.class);

    private final PdfService pdfService;
    private final GenAiService genAiService;

    public IngestController(PdfService pdfService,
                               GenAiService genAiService) {
        this.pdfService = pdfService;
        this.genAiService = genAiService;
    }

    @PostMapping(value = "/genai", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> ingestDirectToGenAi(@RequestPart("file") MultipartFile file) throws Exception {
        logger.info("received file: {}", file.getName());
        File tmp = pdfService.saveToTemp(file);
        String json = genAiService.extractFromPdf(tmp);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(json);
    }

}
