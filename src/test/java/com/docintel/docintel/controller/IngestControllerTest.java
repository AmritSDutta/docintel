package com.docintel.docintel.controller;

import com.docintel.docintel.service.GenAiIMultiModalIngestionService;
import com.docintel.docintel.service.PdfService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.context.TestConfiguration;

import java.io.File;
import java.nio.file.Files;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = IngestController.class)
@Import(IngestControllerTest.MockConfig.class)
class IngestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PdfService pdfService;

    @Autowired
    private GenAiIMultiModalIngestionService ingestService;

    @TestConfiguration
    static class MockConfig {
        @Bean
        PdfService pdfService() {
            return mock(PdfService.class);
        }
        @Bean
        GenAiIMultiModalIngestionService genAiIMultiModalIngestionService() {
            return mock(GenAiIMultiModalIngestionService.class);
        }
    }

    @Test
    void ingestDirectToGenAi_returnsJsonBody() throws Exception {
        MockMultipartFile multipartFile = new MockMultipartFile(
                "file",
                "test.pdf",
                "application/pdf",
                "dummy-pdf-bytes".getBytes()
        );

        File tmp = Files.createTempFile("test-upload-", ".pdf").toFile();
        tmp.deleteOnExit();

        when(pdfService.saveToTemp(any(), anyString())).thenReturn(tmp);
        when(ingestService.extractFromPdf(tmp)).thenReturn("{\"status\":\"ok\",\"pages\":5}");

        // Act & Assert
        mockMvc.perform(multipart("/ingest/pdf/genai")
                        .file(multipartFile)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().string("{\"status\":\"ok\",\"pages\":5}"));

        // Verify interactions
        verify(pdfService, times(1)).saveToTemp(any(), anyString());
        verify(ingestService, times(1)).extractFromPdf(tmp);
    }
}
