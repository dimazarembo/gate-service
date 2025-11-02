package com.dzarembo.gateservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FundingRate {
    private String symbol;
    private double fundingRate;
    private long nextFundingTime;
    private int fundingIntervalHours;
}