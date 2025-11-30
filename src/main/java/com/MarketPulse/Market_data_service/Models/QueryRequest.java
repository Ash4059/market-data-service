package com.MarketPulse.Market_data_service.Models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QueryRequest {

    @JsonProperty("messages")
    private List<Prompt> prompt;

    public List<Prompt> getPrompt(){
        if(Objects.nonNull(prompt)){
            return prompt;
        }
        prompt = new ArrayList<>();
        return prompt;
    }

    @JsonProperty("max_new_tokens")
    private Integer max_new_tokens;

    @JsonProperty("temperature")
    private Double temperature;

    @JsonProperty("model")
    private String model;

    @JsonProperty("stream")
    private boolean stream;
}

