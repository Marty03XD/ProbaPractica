package ro.autobrand.proba.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.opencsv.CSVWriter;
import ro.autobrand.proba.dto.InvoiceLineDto;

@Service
public class InvoicePdfService {

    private static final Pattern LINE_ITEM_PATTERN = Pattern.compile(
            "(\\d+\\.\\d{2})\\s+(RON|USD|EUR)\\s+(-?\\d+(?:\\.\\d+)?)\\s+(-?\\d+(?:\\.\\d+)?)\\s+\\w+\\s+\\d+\\s+(-?\\d+\\.\\d{2})([A-Z0-9]+)\\s+([A-Z][A-Z0-9 ]+)\\d*",
            Pattern.MULTILINE);

    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile(
            "Identificator vanzator articol pentru linia \\d+\\s*:\\s*([A-Z0-9]+)",
            Pattern.CASE_INSENSITIVE);

    public List<InvoiceLineDto> extractLines(MultipartFile file) throws IOException {
        String text = extractText(file.getInputStream());
        List<InvoiceLineDto> lines = parseLineItems(text);
        if (lines.isEmpty()) {
            lines = parseByIdentifiers(text);
        }
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("Nu s-au putut extrage linii de produs din PDF");
        }
        return lines;
    }

    public byte[] toCsv(List<InvoiceLineDto> lines) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (CSVWriter writer = new CSVWriter(new OutputStreamWriter(output, StandardCharsets.UTF_8))) {
            writer.writeNext(new String[] {
                    "cod_produs", "denumire_produs", "pret_unitar", "moneda", "cantitate"
            });
            for (InvoiceLineDto line : lines) {
                writer.writeNext(new String[] {
                        line.productCode(),
                        line.productName(),
                        line.unitPrice().toPlainString(),
                        line.currency(),
                        line.quantity().toPlainString()
                });
            }
        }
        return output.toByteArray();
    }

    private String extractText(InputStream inputStream) throws IOException {
        try (PDDocument document = Loader.loadPDF(inputStream.readAllBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    private List<InvoiceLineDto> parseLineItems(String text) {
        List<InvoiceLineDto> lines = new ArrayList<>();
        Matcher matcher = LINE_ITEM_PATTERN.matcher(text);
        while (matcher.find()) {
            lines.add(new InvoiceLineDto(
                    matcher.group(6).trim(),
                    normalizeProductName(matcher.group(7)),
                    new BigDecimal(matcher.group(1)),
                    matcher.group(2),
                    new BigDecimal(matcher.group(3))));
        }
        return lines;
    }

    private List<InvoiceLineDto> parseByIdentifiers(String text) {
        List<InvoiceLineDto> lines = new ArrayList<>();
        Matcher idMatcher = IDENTIFIER_PATTERN.matcher(text);
        while (idMatcher.find()) {
            String code = idMatcher.group(1);
            int start = Math.max(0, idMatcher.start() - 250);
            String window = text.substring(start, idMatcher.start());
            Matcher priceMatcher = Pattern.compile(
                            "(\\d+\\.\\d{2})\\s+(RON|USD|EUR)\\s+(-?\\d+(?:\\.\\d+)?)\\s+(-?\\d+(?:\\.\\d+)?)\\s+\\w+\\s+\\d+\\s+(-?\\d+\\.\\d{2})"
                                    + Pattern.quote(code)
                                    + "\\s+([A-Z][A-Z0-9 ]+)\\d*")
                    .matcher(window);
            if (priceMatcher.find()) {
                lines.add(new InvoiceLineDto(
                        code,
                        normalizeProductName(priceMatcher.group(6)),
                        new BigDecimal(priceMatcher.group(1)),
                        priceMatcher.group(2),
                        new BigDecimal(priceMatcher.group(3))));
            }
        }
        return lines;
    }

    private static String normalizeProductName(String raw) {
        return raw.trim().replaceAll("\\d+$", "").trim();
    }
}
