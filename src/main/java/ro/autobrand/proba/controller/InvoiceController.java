package ro.autobrand.proba.controller;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ro.autobrand.proba.dto.InvoiceLineDto;
import ro.autobrand.proba.service.InvoicePdfService;

@Controller
public class InvoiceController {

    private final InvoicePdfService invoicePdfService;

    public InvoiceController(InvoicePdfService invoicePdfService) {
        this.invoicePdfService = invoicePdfService;
    }

    @PostMapping("/invoice/upload")
    public Object upload(
            @RequestParam("file") MultipartFile file, RedirectAttributes redirectAttributes) {
        try {
            List<InvoiceLineDto> lines = invoicePdfService.extractLines(file);
            byte[] csv = invoicePdfService.toCsv(lines);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=invoice-extract.csv")
                    .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                    .body(csv);
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "Procesare PDF esuata: " + ex.getMessage());
            return "redirect:/";
        }
    }
}
