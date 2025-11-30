package com.docintel.docintel.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

@Service
public class PdfService {

    public File saveToTemp(MultipartFile file) throws IOException {
        File tmp = Files.createTempFile("upload-", ".pdf").toFile();
        try (OutputStream os = new FileOutputStream(tmp)) {
            os.write(file.getBytes());
        }
        return tmp;
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

