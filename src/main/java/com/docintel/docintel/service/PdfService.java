package com.docintel.docintel.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service
public class PdfService {
    private static final Logger logger = LoggerFactory.getLogger(PdfService.class);

    public File saveToTemp(MultipartFile file, String fileName) throws IOException {
        Path tmpDir = Files.createTempDirectory("pdf_ingest_");
        Path fullPath = tmpDir.resolve(fileName);
        Files.write(fullPath, file.getBytes());
        return fullPath.toFile();
    }

    public List<byte[]> pdfToPngs(File pdf) throws Exception {
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            PDFRenderer renderer = new PDFRenderer(doc);
            List<byte[]> pages = new ArrayList<>();
            for (int i = 0; i < doc.getNumberOfPages(); i++) {
                BufferedImage img = renderer.renderImageWithDPI(i, 200);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(img, "png", baos);
                pages.add(baos.toByteArray());
            }
            return pages;
        }
    }
}

