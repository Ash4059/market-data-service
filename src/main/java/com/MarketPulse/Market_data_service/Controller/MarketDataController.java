package com.MarketPulse.Market_data_service.Controller;

import com.MarketPulse.Market_data_service.Service.UpstoxApiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class MarketDataController {
    private static final Logger logger = LoggerFactory.getLogger(MarketDataController.class);
    private final UpstoxApiService upstoxApiService;

    public MarketDataController(UpstoxApiService upstoxApiService){
        this.upstoxApiService = upstoxApiService;
    }
}
