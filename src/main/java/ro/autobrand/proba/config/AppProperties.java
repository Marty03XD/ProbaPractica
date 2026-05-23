package ro.autobrand.proba.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        Scraping scraping,
        Exchange exchange,
        Scheduler scheduler
) {
    public record Scraping(String baseUrl, String loginUrl, String productsUrl, String username, String password) {}
    public record Exchange(String bnrUrl, String defaultCurrency) {}
    public record Scheduler(String cron, String zone) {}
}
