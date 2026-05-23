package ro.autobrand.proba.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import ro.autobrand.proba.dto.InvoiceLineDto;

class InvoicePdfServiceTest {

    private final InvoicePdfService service = new InvoicePdfService();

    @Test
    void extractsLineFromSampleInvoice() throws Exception {
        Path pdf = Path.of("src", "test", "resources", "AD AUTO TOTAL SRL_20241747776_2024_03_01.PDF");
        if (!Files.exists(pdf)) {
            pdf = Path.of("..", "AD AUTO TOTAL SRL_20241747776_2024_03_01.PDF");
        }

        byte[] bytes = Files.readAllBytes(pdf);
        MockMultipartFile file =
                new MockMultipartFile("file", pdf.getFileName().toString(), "application/pdf", bytes);

        List<InvoiceLineDto> lines = service.extractLines(file);
        assertFalse(lines.isEmpty());
        InvoiceLineDto line = lines.get(0);
        assertEquals("172812F", line.productCode());
        assertEquals("COMUTATOR PORNIRE FEBI", line.productName());
        assertEquals("RON", line.currency());
    }
}
