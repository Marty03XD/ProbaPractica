package ro.autobrand.proba.controller;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ro.autobrand.proba.dto.ProductFormDto;
import ro.autobrand.proba.entity.Product;
import ro.autobrand.proba.service.ExchangeRateService;
import ro.autobrand.proba.service.ProductService;

@Controller
public class ProductController {

    private final ProductService productService;
    private final ExchangeRateService exchangeRateService;

    public ProductController(ProductService productService, ExchangeRateService exchangeRateService) {
        this.productService = productService;
        this.exchangeRateService = exchangeRateService;
    }

    @GetMapping("/")
    public String list(
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String filter,
            Model model) {
        var products = productService.findAll(sort, filter);
        model.addAttribute("products", products);
        model.addAttribute("productCount", products.size());
        model.addAttribute("sort", sort == null ? "nameAsc" : sort);
        model.addAttribute("filter", filter == null ? "" : filter);
        model.addAttribute("exchangeRates", exchangeRateService.getRatesView());
        model.addAttribute("defaultCurrency", exchangeRateService.defaultCurrency());
        return "products/list";
    }

    @PostMapping("/rates/refresh")
    public String refreshRates(RedirectAttributes redirectAttributes) {
        exchangeRateService.forceRefresh();
        redirectAttributes.addFlashAttribute("message", "Cursurile BNR au fost reincarcate.");
        return "redirect:/";
    }

    @PostMapping("/scrape")
    public String scrape(RedirectAttributes redirectAttributes) {
        try {
            int count = productService.scrapeAndSave();
            redirectAttributes.addFlashAttribute("message", "Scraping finalizat: " + count + " produse.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "Scraping esuat: " + ex.getMessage());
        }
        return "redirect:/";
    }

    @GetMapping("/products/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Product product = productService.findById(id);
        model.addAttribute("product", product);
        model.addAttribute("defaultCurrency", exchangeRateService.defaultCurrency());
        return "products/detail";
    }

    @GetMapping("/products/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Product product = productService.findById(id);
        ProductFormDto form = new ProductFormDto();
        form.setId(product.getId());
        form.setName(product.getName());
        form.setPrice(product.getPrice());
        form.setDescription(product.getDescription());
        form.setImageUrl(product.getImageUrl());
        model.addAttribute("productForm", form);
        model.addAttribute("exchangeRates", exchangeRateService.getRatesView());
        model.addAttribute("defaultCurrency", exchangeRateService.defaultCurrency());
        if (product.getPrice() != null) {
            model.addAttribute(
                    "priceRonPreview",
                    exchangeRateService.convertToRon(product.getPrice(), exchangeRateService.defaultCurrency()));
        }
        return "products/edit";
    }

    @PostMapping("/products/{id}/edit")
    public String edit(
            @PathVariable Long id,
            @Valid @ModelAttribute("productForm") ProductFormDto form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("productForm", form);
            model.addAttribute("exchangeRates", exchangeRateService.getRatesView());
            model.addAttribute("defaultCurrency", exchangeRateService.defaultCurrency());
            return "products/edit";
        }
        try {
            productService.update(id, form);
            redirectAttributes.addFlashAttribute("message", "Produs actualizat.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/products/" + id + "/edit";
        }
        return "redirect:/";
    }

    @PostMapping("/products/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        productService.delete(id);
        redirectAttributes.addFlashAttribute("message", "Produs sters.");
        return "redirect:/";
    }
}
