package ro.autobrand.proba.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExchangeRatesView(
        BigDecimal usdToRon,
        BigDecimal eurToRon,
        LocalDate validDate,
        String sourceLabel,
        boolean liveFromBnr) {}
