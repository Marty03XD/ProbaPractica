# Autobrand – Rezolvare proba practica

Aplicatie **Spring Boot 3** care indeplineste cerintele probei practice Autobrand:

1. **Web scraping** (Jsoup) – autentificare pe [web-scraping.dev](https://www.web-scraping.dev/login), extragere produse `consumables`
2. **Cron** – scraping automat la fiecare ora intre **12:00–18:00** (fus orar `Europe/Bucharest`)
3. **Baza de date H2** – tabela `products` cu constrangere **denumire unica**
4. **Interfata web** – listare, editare, stergere, filtrare, sortare
5. **Upload PDF factura** – extragere linii + descarcare **CSV**
6. **Bonus** – curs BNR (USD→RON), autentificare aplicatie (`admin` / `admin123`)

## Cerinte

- **JDK 17+** ([Adoptium](https://adoptium.net/) sau Oracle JDK)
- **Maven 3.9+** (sau IDE cu Maven integrat: IntelliJ / Eclipse)

## Rulare

```bash
cd autobrand-proba
mvn spring-boot:run
```

Deschide: http://localhost:8080

- **Login aplicatie:** `admin` / `admin123`
- Apasa **Ruleaza scraping acum** pentru prima incarcare
- **Incarca factura PDF** – se descarca automat `invoice-extract.csv`

## Structura proiect

| Pachet | Rol |
|--------|-----|
| `service.WebScrapingService` | Login Jsoup + parsare HTML produse (toate paginile) |
| `scheduler.ScrapeScheduler` | Cron `0 0 12-18 * * *` |
| `service.ProductService` | Persistenta JPA, filtrare, sortare, curs valutar |
| `service.ExchangeRateService` | Cursuri BNR (`nbrfxrates.xml`) |
| `service.InvoicePdfService` | PDFBox + export CSV (OpenCSV) |
| `config.SecurityConfig` | Spring Security form login |

## Configurare

`src/main/resources/application.yml`:

- credentiale scraping: `app.scraping.username` / `password` (implicit `user123` / `password`)
- cron: `app.scheduler.cron`
- baza H2: fisier `./data/autobrand`

## Trimitere

1. `git init` in acest folder
2. Push pe GitHub/GitLab
3. Email la **hr@autobrand.ro** cu subiect: `Rezolvare proba practica [Prenume Nume]`

## Teste

```bash
mvn test
```

Testul PDF foloseste factura din folderul parinte (`AD AUTO TOTAL SRL_...PDF`).
