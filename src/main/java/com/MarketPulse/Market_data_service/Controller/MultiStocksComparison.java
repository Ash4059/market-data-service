package com.MarketPulse.Market_data_service.Controller;

import com.MarketPulse.Market_data_service.Models.CandleData;
import com.MarketPulse.Market_data_service.Models.Prompt;
import com.MarketPulse.Market_data_service.Models.QueryRequest;
import com.MarketPulse.Market_data_service.Service.MarketDataService;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/companies")
public class MultiStocksComparison {

    private final MarketDataService marketDataService;
    private final RestTemplate restTemplate;
    private static final String FASTAPI_URL = "http://localhost:1234";

    public MultiStocksComparison(MarketDataService marketDataService, RestTemplate restTemplate){
        this.marketDataService = marketDataService;
        this.restTemplate = restTemplate;
    }

    @GetMapping("/{userId}/historical")
    public ResponseEntity<?> getHistoricalDataPastMonth(
            @PathVariable String userId,
            @RequestParam String symbol,
            @RequestParam(defaultValue = "NSE") String exchange
    ){
        String instrumentKey = marketDataService.getInstrumentKeyBySymbol(exchange, symbol);
        String unit = "days";
        Integer interval = 1;
        String toDate = LocalDate.now().toString();
        String fromDate = LocalDate.now().minusDays(3).toString();

        List<CandleData> responseData = marketDataService.getHistoricalCandleData(
                userId, instrumentKey, unit, interval, fromDate, toDate);
        String Question = "What is your analysis on this stock?";

        QueryRequest request = new QueryRequest();
        request.setTemperature(0.7);
        request.setMax_new_tokens(100);
        request.setModel("qwen/qwen3-8b");
        request.setStream(false);
        request.getPrompt().add(new Prompt(responseData, Question));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<?> entity = new HttpEntity<>(request, headers);

        try {

            ResponseEntity<?> response = restTemplate.postForEntity(
                    FASTAPI_URL + "/v1/chat/completions",
                    entity,
                    String.class
            );

            return ResponseEntity.status(200).body(response.getBody());

        }catch (Exception e){
            return ResponseEntity.status(500).body("Error calling FastAPI " + e.getMessage());
        }
    }

}
