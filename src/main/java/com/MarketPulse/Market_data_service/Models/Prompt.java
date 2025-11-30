package com.MarketPulse.Market_data_service.Models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class Prompt {

    @JsonProperty("role")
    private String role;

    @JsonProperty("content")
    private String response;

    public Prompt(List<CandleData> dataList, String question){
        response = "data : " + CandleData.serializeCandleDataToJson(dataList) + "\n question = " + question;
        role = "user";
    }

}
