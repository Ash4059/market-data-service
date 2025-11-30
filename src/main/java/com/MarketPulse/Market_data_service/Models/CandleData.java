package com.MarketPulse.Market_data_service.Models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CandleData {
    @JsonProperty("timeStamp")
    private String timeStamp;
    private Double open;
    private Double high;
    private Double low;
    private Double close;
    private Double volume;

    @JsonProperty("openInterest")
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
    @JsonProperty("timeStampAsDateTime")
    public LocalDateTime getTimeStampAsDateTime(){
        return LocalDateTime.parse(timeStamp, DateTimeFormatter.ISO_DATE_TIME);
    }

    // Helper method to check if bullish (close > open)
    @JsonProperty("bullish")
    public boolean isBullish(){
        return close > open;
    }

    // Helper method to get price change
    @JsonProperty("priceChange")
    public double getPriceChange(){
        return close - open;
    }

    // Helper method to get price change percentage
    @JsonProperty("priceChangePercentage")
    public double getPriceChangePercentage(){
        return ((close - open) / open) * 100;
    }

    // Helper method to get candle range
    @JsonProperty("range")
    public double getRange(){
        return high - low;
    }

    // Helper method to get body percentage
    @JsonProperty("bodyPercentage")
    public double getBodyPercentage(){
        if (getRange() == 0) return 0;
        return (Math.abs(close - open) / getRange()) * 100;
    }

    public static String serializeCandleDataToJson(List<CandleData> data){
        try {
            ObjectMapper objectMapper = new ObjectMapper();

            objectMapper.registerModule(new JavaTimeModule());

            objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

            return objectMapper.writeValueAsString(data);
        }
        catch (IOException exception){
            System.out.println(exception.getMessage());
        }
        return "";
    }

}
