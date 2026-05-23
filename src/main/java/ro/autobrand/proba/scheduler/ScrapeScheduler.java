package ro.autobrand.proba.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ro.autobrand.proba.service.ProductService;

@Component
public class ScrapeScheduler {

    private static final Logger log = LoggerFactory.getLogger(ScrapeScheduler.class);

    private final ProductService productService;

    public ScrapeScheduler(ProductService productService) {
        this.productService = productService;
    }

    @Scheduled(cron = "${app.scheduler.cron}", zone = "${app.scheduler.zone}")
    public void scheduledScrape() {
        try {
            int count = productService.scrapeAndSave();
            log.info("Scheduled scrape finished, {} products processed", count);
        } catch (Exception ex) {
            log.error("Scheduled scrape failed: {}", ex.getMessage(), ex);
        }
    }
}
