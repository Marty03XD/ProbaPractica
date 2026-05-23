package ro.autobrand.proba.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ro.autobrand.proba.config.AppProperties;
import ro.autobrand.proba.dto.ScrapedProductDto;

@Service
public class WebScrapingService {

    private static final Logger log = LoggerFactory.getLogger(WebScrapingService.class);
    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";

    private final AppProperties properties;

    public WebScrapingService(AppProperties properties) {
        this.properties = properties;
    }

    public List<ScrapedProductDto> scrapeConsumables() throws IOException {
        Connection session = login();
        List<ScrapedProductDto> products = new ArrayList<>();
        String productsUrl = properties.scraping().productsUrl();
        Document firstPage = session.newRequest()
                .url(productsUrl)
                .userAgent(USER_AGENT)
                .timeout(30_000)
                .get();

        int maxPages = extractMaxPages(firstPage);
        products.addAll(parseProducts(firstPage));

        for (int page = 2; page <= maxPages; page++) {
            String pageUrl = withPage(productsUrl, page);
            Document document = session.newRequest()
                    .url(pageUrl)
                    .userAgent(USER_AGENT)
                    .timeout(30_000)
                    .get();
            products.addAll(parseProducts(document));
        }

        log.info("Scraped {} consumable products from web-scraping.dev", products.size());
        return products;
    }

    private Connection login() throws IOException {
        AppProperties.Scraping scraping = properties.scraping();
        Connection.Response response = Jsoup.connect(scraping.loginUrl())
                .userAgent(USER_AGENT)
                .method(Connection.Method.POST)
                .data("username", scraping.username())
                .data("password", scraping.password())
                .followRedirects(false)
                .timeout(30_000)
                .execute();

        if (response.statusCode() != 302 && response.statusCode() != 200) {
            throw new IOException("Login failed with HTTP status " + response.statusCode());
        }

        String authCookie = response.cookies().get("auth");
        if (authCookie == null || authCookie.isBlank()) {
            throw new IOException("Login did not return auth cookie");
        }

        log.info("Authenticated on web-scraping.dev");
        return Jsoup.connect(scraping.baseUrl()).cookies(response.cookies());
    }

    private List<ScrapedProductDto> parseProducts(Document page) {
        List<ScrapedProductDto> result = new ArrayList<>();
        Elements productRows = page.select("div.products div.row.product");

        for (Element row : productRows) {
            Element image = row.selectFirst("div.thumbnail img");
            Element titleLink = row.selectFirst("div.description h3 a");
            Element description = row.selectFirst("div.description .short-description");
            Element price = row.selectFirst("div.price-wrap .price");

            if (titleLink == null || price == null) {
                continue;
            }

            String name = titleLink.text().trim();
            String imageUrl = image != null ? image.absUrl("src") : "";
            String desc = description != null ? description.text().trim() : "";
            BigDecimal priceValue = new BigDecimal(price.text().trim());

            result.add(new ScrapedProductDto(name, priceValue, desc, imageUrl));
        }

        return result;
    }

    private int extractMaxPages(Document page) {
        var meta = page.selectFirst("div.paging-meta");
        if (meta == null) {
            return 1;
        }
        Matcher matcher = Pattern.compile("in (\\d+) pages").matcher(meta.text());
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : 1;
    }

    private String withPage(String url, int page) {
        if (url.contains("page=")) {
            return url.replaceAll("page=\\d+", "page=" + page);
        }
        return url + (url.contains("?") ? "&" : "?") + "page=" + page;
    }
}
