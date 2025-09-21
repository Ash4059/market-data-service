package com.MarketPulse.Market_data_service.Models;


import com.upstox.api.MarketQuoteOHLCV3;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OHLC {
    private String companyName;
    private String symbol;
    private String instrumentKey;
    private String exchange;
    private String sector;
    private MarketQuoteOHLCV3 ohlcData;
    private double similarityScore;
    private String matchType; // EXACT, SIMILAR
    private String matchReason;
}
