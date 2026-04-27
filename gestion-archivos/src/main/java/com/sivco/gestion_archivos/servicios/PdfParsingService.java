package com.sivco.gestion_archivos.servicios;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PdfParsingService {

    private static final Logger logger = LoggerFactory.getLogger(PdfParsingService.class);


    // More permissive: matches 'T1', 't1', 'T 1', 't 01', case-insensitive
    private static final Pattern SENSOR_PATTERN = Pattern.compile("(?i)t\\s*(\\d+)");

    public String extractText(MultipartFile archivo) throws IOException {
        try (InputStream in = archivo.getInputStream(); PDDocument document = PDDocument.load(in)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            stripper.setLineSeparator("\n");
            String text = stripper.getText(document);
            logger.debug("Extracted {} characters from PDF {}", text != null ? text.length() : 0, archivo.getOriginalFilename());

            if (text == null || text.trim().isEmpty()) {
                logger.info("PDF extraction returned no text, attempting OCR fallback using Tesseract");
                try {
                    String ocr = performOcrWithTesseract(document);
                    if (ocr != null && !ocr.trim().isEmpty()) {
                        logger.info("OCR produced {} characters for PDF {}", ocr.length(), archivo.getOriginalFilename());
                        return ocr;
                    }
                } catch (Exception ocrex) {
                    logger.warn("OCR fallback failed: {}", ocrex.getMessage());
                }
                throw new IllegalArgumentException("No se pudo extraer texto válido del PDF. Verifique que el archivo contenga texto seleccionable o instale Tesseract para OCR si es un PDF escaneado.");
            }

            return text == null ? "" : text;
        } catch (IOException e) {
            logger.error("Error extracting text from PDF: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Render pages to images and run Tesseract CLI on each page. Requires `tesseract` binary available in PATH.
     */
    private String performOcrWithTesseract(PDDocument document) throws IOException, InterruptedException {
        // Ensure tesseract binary exists to avoid confusing errors
        if (!isTesseractAvailable()) {
            throw new IOException("Tesseract OCR not found in PATH. Install Tesseract and ensure 'tesseract' is available in the system PATH. Windows: https://github.com/UB-Mannheim/tesseract/wiki.");
        }

        PDFRenderer renderer = new PDFRenderer(document);
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < document.getNumberOfPages(); i++) {
            java.awt.image.BufferedImage image = renderer.renderImageWithDPI(i, 200);
            java.nio.file.Path tmpFile = null;
            try {
                tmpFile = java.nio.file.Files.createTempFile("pdfpage_", ".png");
                javax.imageio.ImageIO.write(image, "png", tmpFile.toFile());

                ProcessBuilder pb = new ProcessBuilder("tesseract", tmpFile.toAbsolutePath().toString(), "stdout");
                pb.redirectErrorStream(true);
                Process p = pb.start();

                try (java.io.InputStream is = p.getInputStream(); java.util.Scanner s = new java.util.Scanner(is, java.nio.charset.StandardCharsets.UTF_8.name())) {
                    s.useDelimiter("\\A");
                    String out = s.hasNext() ? s.next() : "";
                    sb.append(out).append('\n');
                }

                int exit = p.waitFor();
                if (exit != 0) {
                    logger.warn("Tesseract exited with code {} for page {}", exit, i);
                }
            } finally {
                if (tmpFile != null) {
                    try { java.nio.file.Files.deleteIfExists(tmpFile); } catch (Exception ignore) {}
                }
            }
        }

        return sb.toString();
    }

    private boolean isTesseractAvailable() {
        try {
            ProcessBuilder pb = new ProcessBuilder("tesseract", "-v");
            pb.redirectErrorStream(true);
            Process p = pb.start();
            boolean finished = p.waitFor(3, java.util.concurrent.TimeUnit.SECONDS);
            if (!finished) {
                p.destroy();
                return false;
            }
            int exit = p.exitValue();
            return exit == 0 || exit == 1; // some tesseract versions return 1 for -v on some platforms
        } catch (Exception e) {
            logger.debug("Tesseract availability check failed: {}", e.getMessage());
            return false;
        }
    }

    public Set<String> extractSensorCodes(String text) {
        Set<String> result = new HashSet<>();
        if (text == null || text.isEmpty()) return result;
        Matcher m = SENSOR_PATTERN.matcher(text);
        while (m.find()) {
            String digits = m.group(1).replaceFirst("^0+", "");
            if (digits.isEmpty()) continue;
            String tcode = "t" + digits;
            result.add(tcode);
            // Also add normalized sensor_N form used in CSV mapping
            result.add("sensor_" + digits);
        }
        return result;
    }
}
