package com.MarketPulse.Market_data_service.Models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CandleData {
    private String timeStamp;
    private Double open;
    private Double high;
    private Double low;
    private Double close;
    private Double volume;
    private Double openInterst;

    // Helper method to parse from array
    public static CandleData fromArray(Object[] candleArray){
        return CandleData.builder()
                .timeStamp((String) candleArray[0])
                .open(((Number)candleArray[1]).doubleValue())
                .high(((Number)candleArray[2]).doubleValue())
                .low(((Number)candleArray[3]).doubleValue())
                .close(((Number)candleArray[4]).doubleValue())
                .volume(((Number)candleArray[5]).doubleValue())
                .openInterst(((Number)candleArray[6]).doubleValue())
                .build();
    }

    // Helper method to get formatted timestamp
    public LocalDateTime getTimeStampAsDateTime(){
        return LocalDateTime.parse(timeStamp, DateTimeFormatter.ISO_DATE_TIME);
    }

    // Helper method to check if bullish (close > open)
    public boolean isBullish(){
        return close > open;
    }

    // Helper method to get price change
    public double getPriceChange(){
        return close - open;
    }

    // Helper method to get price change percentage
    public double getPriceChangePercentage(){
        return ((close - open) / open) * 100;
    }
}
