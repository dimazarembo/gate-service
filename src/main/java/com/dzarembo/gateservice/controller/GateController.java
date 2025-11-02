package com.dzarembo.gateservice.controller;

import com.dzarembo.gateservice.cache.FundingCache;
import com.dzarembo.gateservice.model.FundingRate;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;

@RestController
@RequestMapping("/api/gate")
@RequiredArgsConstructor
public class GateController {
    private final FundingCache cache;

    @GetMapping("/funding")
    public Collection<FundingRate> getFundingRates() {
        return cache.getAll();
    }
}
