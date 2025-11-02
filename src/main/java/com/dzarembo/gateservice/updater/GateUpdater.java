package com.dzarembo.gateservice.updater;

import com.dzarembo.gateservice.cache.FundingCache;
import com.dzarembo.gateservice.clinet.GateApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class GateUpdater {
    private final FundingCache cache;
    private final GateApiClient apiClient;

    @Scheduled(fixedRate = 1 * 60 * 1000) // обновление каждые 1 минут
    public void updateFundingRates() {
        log.info("Updating Gate funding cache...");
        cache.putAll(apiClient.fetchFundingRates());
        log.info("Gate funding cache updated: {} entries", cache.getAll().size());
    }
}
