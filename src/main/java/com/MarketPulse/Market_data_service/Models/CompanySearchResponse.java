package com.MarketPulse.Market_data_service.Models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CompanySearchResponse {

    private String searchQuery;
    private String matchType; // EXACT, SIMILAR, NO_MATCH, ERROR
    private boolean exactMatch;
    private List<OHLC> companies = new ArrayList<>();
    private int totalResults;
    private String timestamp;

}
