package com.MarketPulse.Market_data_service.Controller;

import com.MarketPulse.Market_data_service.Models.InstrumentData;
import com.MarketPulse.Market_data_service.Service.MarketDataService;
import com.upstox.api.GetHistoricalCandleResponse;
import com.upstox.api.GetMarketQuoteOHLCResponseV3;
import com.upstox.api.MarketQuoteOHLCV3;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/company")
public class MarketDataController {

    private final MarketDataService marketDataService;

    public MarketDataController(MarketDataService marketDataService){
        this.marketDataService = marketDataService;
    }

    /**
     * Get OHLC data by symbol (most common usage)
     */
    @GetMapping("/{userId}/ohlc")
    public ResponseEntity<MarketQuoteOHLCV3> getOHLCBySymbol(
            @PathVariable String userId,
            @RequestParam String symbol,
            @RequestParam(defaultValue = "NSE") String exchange,
            @RequestParam(defaultValue = "1d") String interval) {

        try {
            MarketQuoteOHLCV3 response = marketDataService.getOHLCBySymbol(userId, exchange, symbol, interval);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching OHLC for symbol: {} - {}", symbol, e.getMessage());
            return ResponseEntity.status(500).body(null);
        }
    }

    /**
     * Get historical candle data with custom parameters
     */
    @GetMapping("/{userId}/historical/candles")
    public ResponseEntity<GetHistoricalCandleResponse> getHistoricalCandleData(
            @PathVariable String userId,
            @RequestParam String symbol,
            @RequestParam(defaultValue = "NSE") String exchange,
            @RequestParam String unit,           // "minute", "day"
            @RequestParam Integer interval,      // 1, 5, 15, 30, 60
            @RequestParam String fromDate,       // YYYY-MM-DD format
            @RequestParam String toDate) {       // YYYY-MM-DD format

        try {
            String instrumentKey = marketDataService.getInstrumentKeyBySymbol(exchange, symbol);
            GetHistoricalCandleResponse response = marketDataService.getHistoricalCandleData(
                    userId, instrumentKey, unit, interval, fromDate, toDate);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching historical candle data for: {} - {}", symbol, e.getMessage());
            return ResponseEntity.status(500).body(null);
        }
    }

    /**
     * Get intraday data for today (simplified)
     */
    @GetMapping("/{userId}/historical/today")
    public ResponseEntity<GetHistoricalCandleResponse> getHistoricalDataToday(
            @PathVariable String userId,
            @RequestParam String symbol,
            @RequestParam(defaultValue = "NSE") String exchange,
            @RequestParam(defaultValue = "minutes") String unit,
            @RequestParam(defaultValue = "5") Integer interval) {

        try {
            String instrumentKey = marketDataService.getInstrumentKeyBySymbol(exchange, symbol);
            GetHistoricalCandleResponse response = marketDataService.getHistoricalDataToday(
                    userId, instrumentKey, unit, interval);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching today's historical data for: {} - {}", symbol, e.getMessage());
            return ResponseEntity.status(500).body(null);
        }
    }

    /**
     * Get OHLC data for multiple symbols
     */
    @PostMapping("/{userId}/symbols")
    public ResponseEntity<GetMarketQuoteOHLCResponseV3> getOHLCBySymbols(
            @PathVariable String userId,
            @RequestParam List<String> symbols,
            @RequestParam(defaultValue = "NSE") String exchange,
            @RequestParam(defaultValue = "1d") String interval) {

        try {
            GetMarketQuoteOHLCResponseV3 response = marketDataService.getOHLCBySymbols(
                    userId, exchange, symbols, interval);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching OHLC for symbols: {} - {}", symbols, e.getMessage());
            return ResponseEntity.status(500).body(null);
        }
    }

    /**
     * Get indices OHLC data
     */
    @GetMapping("/{userId}/indices")
    public ResponseEntity<GetMarketQuoteOHLCResponseV3> getIndicesOHLC(
            @PathVariable String userId,
            @RequestParam(defaultValue = "1d") String interval) {

        try {
            GetMarketQuoteOHLCResponseV3 response = marketDataService.getIndicesOHLC(userId, interval);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching indices OHLC - {}", e.getMessage());
            return ResponseEntity.status(500).body(null);
        }
    }

    /**
     * Get Nifty 50 stocks OHLC
     */
    @GetMapping("/{userId}/nifty50")
    public ResponseEntity<GetMarketQuoteOHLCResponseV3> getNifty50OHLC(
            @PathVariable String userId,
            @RequestParam(defaultValue = "1d") String interval) {

        try {
            GetMarketQuoteOHLCResponseV3 response = marketDataService.getNifty50OHLC(userId, interval);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching Nifty 50 OHLC - {}", e.getMessage());
            return ResponseEntity.status(500).body(null);
        }
    }

    /**
     * Search companies/instruments
     */
    @GetMapping("/{userId}/search")
    public ResponseEntity<List<InstrumentData>> searchInstruments(
            @PathVariable String userId,
            @RequestParam String query,
            @RequestParam(defaultValue = "NSE") String exchange,
            @RequestParam(defaultValue = "10") int maxResults) {

        try {
            List<InstrumentData> results = marketDataService.searchInstruments(exchange, query, maxResults);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            log.error("Error searching instruments with query: {} - {}", query, e.getMessage());
            return ResponseEntity.status(500).body(null);
        }
    }

    /**
     * Get instrument key by symbol
     */
    @GetMapping("/instrument-key")
    public ResponseEntity<Map<String, String>> getInstrumentKey(
            @RequestParam String symbol,
            @RequestParam(defaultValue = "NSE") String exchange) {

        try {
            String instrumentKey = marketDataService.getInstrumentKeyBySymbol(exchange, symbol);

            Map<String, String> response = new HashMap<>();
            response.put("symbol", symbol);
            response.put("exchange", exchange);
            response.put("instrumentKey", instrumentKey);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting instrument key for: {} - {}", symbol, e.getMessage());
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Validate symbol
     */
    @GetMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateSymbol(
            @RequestParam String symbol,
            @RequestParam(defaultValue = "NSE") String exchange) {

        boolean isValid = marketDataService.isValidSymbol(exchange, symbol);

        Map<String, Object> response = new HashMap<>();
        response.put("symbol", symbol);
        response.put("exchange", exchange);
        response.put("isValid", isValid);

        return ResponseEntity.ok(response);
    }

    /**
     * Get cache statistics
     */
    @GetMapping("/cache/stats")
    public ResponseEntity<Map<String, Object>> getCacheStats() {
        try {
            Map<String, Object> stats = marketDataService.getCacheStatistics();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Error getting cache stats - {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ========== Quick Access Endpoints ==========

    @GetMapping("/{userId}/reliance")
    public ResponseEntity<MarketQuoteOHLCV3> getRelianceOHLC(
            @PathVariable String userId,
            @RequestParam(defaultValue = "1d") String interval) {
        return getOHLCBySymbol(userId, "RELIANCE", "NSE", interval);
    }

    @GetMapping("/{userId}/hdfc")
    public ResponseEntity<MarketQuoteOHLCV3> getHDFCOHLC(
            @PathVariable String userId,
            @RequestParam(defaultValue = "1d") String interval) {
        return getOHLCBySymbol(userId, "HDFCBANK", "NSE", interval);
    }

    @GetMapping("/{userId}/tcs")
    public ResponseEntity<MarketQuoteOHLCV3> getTCSOHLC(
            @PathVariable String userId,
            @RequestParam(defaultValue = "1d") String interval) {
        return getOHLCBySymbol(userId, "TCS", "NSE", interval);
    }
}
