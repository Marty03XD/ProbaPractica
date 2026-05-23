package ro.autobrand.proba.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.xml.parsers.DocumentBuilderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.jsoup.Jsoup;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import ro.autobrand.proba.config.AppProperties;
import ro.autobrand.proba.dto.ExchangeRatesView;

@Service
public class ExchangeRateService {

    private static final Logger log = LoggerFactory.getLogger(ExchangeRateService.class);

    private final AppProperties properties;
    private final Map<String, BigDecimal> cache = new ConcurrentHashMap<>();
    private LocalDate cacheDate = LocalDate.MIN;
    private boolean liveFromBnr;

    public ExchangeRateService(AppProperties properties) {
        this.properties = properties;
    }

    public BigDecimal getRateToRon(String currency) {
        if (currency == null || currency.isBlank() || "RON".equalsIgnoreCase(currency)) {
            return BigDecimal.ONE;
        }

        refreshIfNeeded();
        return cache.getOrDefault(currency.toUpperCase(), BigDecimal.ONE);
    }

    public BigDecimal convertToRon(BigDecimal amount, String currency) {
        if (amount == null) {
            return null;
        }
        return amount.multiply(getRateToRon(currency)).setScale(2, RoundingMode.HALF_UP);
    }

    public String defaultCurrency() {
        return properties.exchange().defaultCurrency();
    }

    public ExchangeRatesView getRatesView() {
        refreshIfNeeded();
        return new ExchangeRatesView(
                cache.getOrDefault("USD", new BigDecimal("4.60")),
                cache.getOrDefault("EUR", new BigDecimal("4.97")),
                cacheDate,
                liveFromBnr ? "BNR (nbrfxrates.xml)" : "Curs estimativ (offline)",
                liveFromBnr);
    }

    public synchronized void forceRefresh() {
        cacheDate = LocalDate.MIN;
        refreshIfNeeded();
    }

    private synchronized void refreshIfNeeded() {
        LocalDate today = LocalDate.now();
        if (today.equals(cacheDate) && !cache.isEmpty()) {
            return;
        }

        try {
            String xml = Jsoup.connect(properties.exchange().bnrUrl())
                    .ignoreContentType(true)
                    .timeout(20_000)
                    .execute()
                    .body();

            Document document = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder()
                    .parse(new java.io.ByteArrayInputStream(xml.getBytes()));

            NodeList rates = document.getElementsByTagName("Rate");
            cache.clear();
            for (int i = 0; i < rates.getLength(); i++) {
                Element rate = (Element) rates.item(i);
                String currency = rate.getAttribute("currency");
                BigDecimal value = new BigDecimal(rate.getTextContent().trim());
                int multiplier = Integer.parseInt(rate.getAttribute("multiplier"));
                BigDecimal unitRate = value.divide(BigDecimal.valueOf(multiplier), 6, RoundingMode.HALF_UP);
                cache.put(currency, unitRate);
            }
            cacheDate = today;
            liveFromBnr = true;
            log.info("Loaded BNR exchange rates for {}", today);
        } catch (Exception ex) {
            log.warn("Could not load BNR rates, using fallback USD/EUR: {}", ex.getMessage());
            cache.clear();
            cache.put("USD", new BigDecimal("4.60"));
            cache.put("EUR", new BigDecimal("4.97"));
            cacheDate = today;
            liveFromBnr = false;
        }
    }
}
