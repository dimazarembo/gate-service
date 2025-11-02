package com.dzarembo.gateservice.clinet;

import com.dzarembo.gateservice.model.FundingRate;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
public class GateApiClient {
    private final WebClient webClient;

    public GateApiClient() {
        // Gate.io отдаёт большой JSON → увеличиваем лимит буфера
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(cfg -> cfg.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();

        this.webClient = WebClient.builder()
                .baseUrl("https://api.gateio.ws/api/v4")
                .clientConnector(new ReactorClientHttpConnector(HttpClient.create()))
                .exchangeStrategies(strategies)
                .build();
    }

    /**
     * Получает funding rate по USDT perpetual контрактам.
     */
    public Collection<FundingRate> fetchFundingRates() {
        try {
            List<Item> items = webClient.get()
                    .uri("/futures/usdt/tickers")
                    .retrieve()
                    .bodyToFlux(Item.class)
                    .collectList()
                    .block();

            if (items == null || items.isEmpty()) {
                log.warn("Empty response from Gate.io");
                return List.of();
            }

            return items.stream()
                    // 1️⃣ оставляем только пары с USDT
                    .filter(item -> item.getContract() != null && item.getContract().contains("USDT"))
                    // 2️⃣ исключаем USD контракты
                    .filter(item -> !item.getContract().contains("USD_") && !item.getContract().endsWith("USD"))
                    // 3️⃣ мапим в FundingRate
                    .map(this::mapToFundingRate)
                    .filter(Objects::nonNull)
                    .toList();

        } catch (Exception e) {
            log.error("Failed to fetch funding rates from Gate.io", e);
            return List.of();
        }
    }

    private FundingRate mapToFundingRate(Item item) {
        try {
            double rate = item.getFunding_rate(); // поле в JSON — funding_rate
            long nextFundingTimeSec = item.getFunding_next_apply(); // секунды
            long nextFundingTimeMs = nextFundingTimeSec * 1000L;
            int intervalHours = 8; // фиксированный интервал у Gate

            String normalizedSymbol = normalizeSymbol(item.getContract());

            log.debug("Gate.io: {} rate={}, nextFundingTime(UTC)={}, interval={}h",
                    normalizedSymbol,
                    rate,
                    Instant.ofEpochMilli(nextFundingTimeMs),
                    intervalHours
            );

            return new FundingRate(
                    normalizedSymbol,
                    rate,
                    nextFundingTimeMs,
                    intervalHours
            );

        } catch (Exception e) {
            log.warn("Failed to parse Gate.io item: {}", item, e);
            return null;
        }
    }

    private String normalizeSymbol(String symbol) {
        return symbol == null ? "" : symbol.replace("_", "");
    }

    // ================= DTO =================

    @Data
    public static class Item {
        private String contract;               // e.g. "BTC_USDT"
        private double funding_rate;           // e.g. 0.000099
        private long funding_next_apply;       // e.g. 1761667200 (в секундах)
    }
}
