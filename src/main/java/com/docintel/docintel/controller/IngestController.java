package com.docintel.docintel.controller;

import com.docintel.docintel.service.GenAiIMultiModalIngestionService;
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
import java.util.Optional;

@RestController
@RequestMapping("/ingest/pdf")
public class IngestController {
    private static final Logger logger = LoggerFactory.getLogger(IngestController.class);

    private final PdfService pdfService;
    private final GenAiIMultiModalIngestionService genAiIMultiModalIngestionService;

    public IngestController(PdfService pdfService,
                               GenAiIMultiModalIngestionService genAiIMultiModalIngestionService) {
        this.pdfService = pdfService;
        this.genAiIMultiModalIngestionService = genAiIMultiModalIngestionService;
    }

    @PostMapping(value = "/genai", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> ingestDirectToGenAi(@RequestPart("file") MultipartFile file) throws Exception {
        var fileName = Optional.ofNullable(file.getOriginalFilename())
                .orElse("uploaded.pdf");
        logger.info("received file: {}", fileName);
        File tmp = pdfService.saveToTemp(file, fileName);
        String json = genAiIMultiModalIngestionService.extractFromPdf(tmp);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(json);
    }

}
