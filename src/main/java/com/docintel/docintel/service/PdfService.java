package com.docintel.docintel.service;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class PdfService {
    private static final Logger logger = LoggerFactory.getLogger(PdfService.class);

    public File saveToTemp(MultipartFile file, String fileName) throws IOException {
        Path tmpDir = Files.createTempDirectory("pdf_ingest_");
        Path fullPath = tmpDir.resolve(fileName);
        Files.write(fullPath, file.getBytes());
        return fullPath.toFile();
    }
}

