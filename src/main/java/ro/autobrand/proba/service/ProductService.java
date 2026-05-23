package ro.autobrand.proba.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.autobrand.proba.dto.ProductFormDto;
import ro.autobrand.proba.dto.ScrapedProductDto;
import ro.autobrand.proba.entity.Product;
import ro.autobrand.proba.repository.ProductRepository;

@Service
public class ProductService {

    private final ProductRepository repository;
    private final WebScrapingService scrapingService;
    private final ExchangeRateService exchangeRateService;

    public ProductService(
            ProductRepository repository,
            WebScrapingService scrapingService,
            ExchangeRateService exchangeRateService) {
        this.repository = repository;
        this.scrapingService = scrapingService;
        this.exchangeRateService = exchangeRateService;
    }

    public List<Product> findAll(String sort, String filter) {
        List<Product> products = repository.findAll();
        if (filter != null && !filter.isBlank()) {
            String needle = filter.toLowerCase(Locale.ROOT);
            products = products.stream()
                    .filter(p -> p.getName().toLowerCase(Locale.ROOT).contains(needle)
                            || (p.getDescription() != null
                                    && p.getDescription().toLowerCase(Locale.ROOT).contains(needle)))
                    .toList();
        }

        Comparator<Product> comparator = switch (sort == null ? "nameAsc" : sort) {
            case "nameDesc" -> Comparator.comparing(Product::getName).reversed();
            case "priceAsc" -> Comparator.comparing(Product::getPrice);
            case "priceDesc" -> Comparator.comparing(Product::getPrice).reversed();
            case "ronAsc" -> Comparator.comparing(
                    Product::getPriceRon, Comparator.nullsLast(Comparator.naturalOrder()));
            case "ronDesc" -> Comparator.comparing(
                    Product::getPriceRon, Comparator.nullsLast(Comparator.reverseOrder()));
            default -> Comparator.comparing(Product::getName);
        };

        return products.stream().sorted(comparator).toList();
    }

    public Product findById(Long id) {
        return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Produs inexistent"));
    }

    @Transactional
    public int scrapeAndSave() throws IOException {
        List<ScrapedProductDto> scraped = scrapingService.scrapeConsumables();
        int saved = 0;
        for (ScrapedProductDto dto : scraped) {
            Product product = repository.findByName(dto.name()).orElseGet(Product::new);
            product.setName(dto.name());
            product.setPrice(dto.price());
            product.setDescription(dto.description());
            product.setImageUrl(dto.imageUrl());
            applyExchangeRate(product);
            product.setUpdatedAt(LocalDateTime.now());
            repository.save(product);
            saved++;
        }
        return saved;
    }

    @Transactional
    public Product update(Long id, ProductFormDto form) {
        Product product = findById(id);
        if (repository.existsByNameAndIdNot(form.getName(), id)) {
            throw new IllegalArgumentException("Exista deja un produs cu aceasta denumire");
        }
        product.setName(form.getName());
        product.setPrice(form.getPrice());
        product.setDescription(form.getDescription());
        product.setImageUrl(form.getImageUrl());
        applyExchangeRate(product);
        product.setUpdatedAt(LocalDateTime.now());
        return repository.save(product);
    }

    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
    }

    private void applyExchangeRate(Product product) {
        String currency = exchangeRateService.defaultCurrency();
        var rate = exchangeRateService.getRateToRon(currency);
        product.setExchangeCurrency(currency);
        product.setExchangeRate(rate);
        product.setPriceRon(exchangeRateService.convertToRon(product.getPrice(), currency));
        product.setRateDate(LocalDateTime.now());
    }
}
