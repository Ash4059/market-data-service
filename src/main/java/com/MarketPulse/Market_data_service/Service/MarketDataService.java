package com.MarketPulse.Market_data_service.Service;

import com.MarketPulse.Market_data_service.Entity.UserProfile;
import com.MarketPulse.Market_data_service.Models.InstrumentData;
import com.MarketPulse.Market_data_service.Repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.upstox.ApiClient;
import com.upstox.ApiException;
import com.upstox.Configuration;
import com.upstox.api.GetHistoricalCandleResponse;
import com.upstox.api.GetMarketQuoteOHLCResponseV3;
import com.upstox.api.MarketQuoteOHLCV3;
import com.upstox.auth.OAuth;

import io.swagger.client.api.HistoryV3Api;
import io.swagger.client.api.MarketQuoteV3Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

@Service
@Slf4j
public class MarketDataService {

    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    // Instrument cache
    private final Map<String, List<InstrumentData>> instrumentsCache = new ConcurrentHashMap<>();
    private final Map<String, String> symbolToKeyCache = new ConcurrentHashMap<>();
    private LocalDateTime lastCacheUpdate = LocalDateTime.now().minusHours(25);

    private static final String COMPLETE_INSTRUMENTS_URL =
            "https://assets.upstox.com/market-quote/instruments/exchange/complete.json.gz";

    public MarketDataService(UserRepository userRepository, ObjectMapper objectMapper, RestTemplate restTemplate) {
        this.userRepository = userRepository;
        this.objectMapper = new ObjectMapper();
        this.restTemplate = restTemplate;
    }

    @PostConstruct
    public void initialize() {
        log.info("Initializing MarketDataService...");
        refreshInstrumentsCache();
    }

    // ========== OHLC DATA METHODS ==========

    /**
     * Get OHLC data by instrument keys
     */
    public GetMarketQuoteOHLCResponseV3 getOHLCData(String userId, List<String> instrumentKeys, String interval) {
        try {
            configAPIClient(userId);

            MarketQuoteV3Api marketQuoteV3Api = new MarketQuoteV3Api();
            String instrumentParam = String.join(",", instrumentKeys);

            GetMarketQuoteOHLCResponseV3 response = marketQuoteV3Api.getMarketQuoteOHLC(interval, instrumentParam);

            log.info("OHLC data fetched for {} instruments", instrumentKeys.size());
            return response;

        } catch (ApiException e) {
            log.error("Upstox API error for user {}: {}", userId, e.getResponseBody(), e);
            throw new RuntimeException("Failed to fetch OHLC data: " + e.getResponseBody(), e);
        } catch (Exception e) {
            log.error("Error fetching OHLC data for user {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch OHLC data: " + e.getMessage(), e);
        }
    }

    /**
     * Get OHLC data by single symbol
     */
    public MarketQuoteOHLCV3 getOHLCBySymbol(String userId, String exchange, String symbol, String interval) {
        try {
            String instrumentKey = getInstrumentKeyBySymbol(exchange, symbol);
            GetMarketQuoteOHLCResponseV3 response = getOHLCData(userId, List.of(instrumentKey), interval);

            if (response.getData() != null && !response.getData().isEmpty()) {
                return response.getData().values().iterator().next();
            }

            throw new RuntimeException("No OHLC data found for symbol: " + symbol);

        } catch (Exception e) {
            log.error("Error fetching OHLC data for symbol: {} on {}", symbol, exchange, e);
            throw new RuntimeException("Failed to fetch OHLC data for symbol: " + symbol, e);
        }
    }

    /**
     * Get OHLC data by multiple symbols
     */
    public GetMarketQuoteOHLCResponseV3 getOHLCBySymbols(String userId, String exchange,
                                                         List<String> symbols, String interval) {
        try {
            List<String> instrumentKeys = symbols.stream()
                    .map(symbol -> getInstrumentKeyBySymbol(exchange, symbol))
                    .collect(Collectors.toList());

            return getOHLCData(userId, instrumentKeys, interval);

        } catch (Exception e) {
            log.error("Error fetching OHLC data for symbols: {} on {}", symbols, exchange, e);
            throw new RuntimeException("Failed to fetch OHLC data for symbols: " + symbols, e);
        }
    }

    /**
     * Get OHLC data by company name (fuzzy search)
     */
    public GetMarketQuoteOHLCResponseV3 getOHLCByCompanyName(String userId, String exchange,
                                                             String companyName, String interval) {
        try {
            // Search for company by name
            List<InstrumentData> matchingInstruments = searchInstruments(exchange, companyName, 5);

            if (matchingInstruments.isEmpty()) {
                throw new RuntimeException("No company found matching: " + companyName);
            }

            // Get OHLC for first 3 matches
            List<String> instrumentKeys = matchingInstruments.stream()
                    .limit(3)
                    .map(InstrumentData::getInstrumentKey)
                    .collect(Collectors.toList());

            return getOHLCData(userId, instrumentKeys, interval);

        } catch (Exception e) {
            log.error("Error fetching OHLC data for company: {} on {}", companyName, exchange, e);
            throw new RuntimeException("Failed to fetch OHLC data for company: " + companyName, e);
        }
    }

    /**
     * Get indices OHLC data
     */
    public GetMarketQuoteOHLCResponseV3 getIndicesOHLC(String userId, String interval) {
        List<String> indicesKeys = Arrays.asList(
                "NSE_INDEX|Nifty 50",
                "NSE_INDEX|Nifty Bank",
                "NSE_INDEX|Nifty IT",
                "BSE_INDEX|SENSEX"
        );

        return getOHLCData(userId, indicesKeys, interval);
    }

    /**
     * Get Nifty 50 stocks OHLC
     */
    public GetMarketQuoteOHLCResponseV3 getNifty50OHLC(String userId, String interval) {
        List<String> nifty50Symbols = Arrays.asList(
                "RELIANCE", "TCS", "HDFCBANK", "INFY", "ICICIBANK",
                "SBIN", "BHARTIARTL", "ITC", "KOTAKBANK", "LT"
        );

        return getOHLCBySymbols(userId, "NSE", nifty50Symbols, interval);
    }

    // ========== INSTRUMENT KEY METHODS ==========

    /**
     * Get instrument key by symbol
     */
    public String getInstrumentKeyBySymbol(String exchange, String symbol) {
        if (exchange == null || symbol == null) {
            throw new IllegalArgumentException("Exchange and symbol cannot be null");
        }

        try {
            refreshCacheIfNeeded();

            // Try cache first
            String cacheKey = createCacheKey(exchange, symbol);
            String cachedKey = symbolToKeyCache.get(cacheKey);
            if (cachedKey != null) {
                return cachedKey;
            }

            // Find instrument key
            String instrumentKey = findInstrumentKey(exchange, symbol);

            if (instrumentKey != null) {
                symbolToKeyCache.put(cacheKey, instrumentKey);
                log.debug("Found and cached instrument key: {} -> {}", symbol, instrumentKey);
                return instrumentKey;
            }

            throw new RuntimeException("Instrument key not found for symbol: " + symbol + " on exchange: " + exchange);

        } catch (Exception e) {
            log.error("Error finding instrument key for symbol: {} on {}", symbol, exchange, e);
            throw new RuntimeException("Failed to find instrument key for symbol: " + symbol, e);
        }
    }

    /**
     * Search instruments by partial name or symbol
     */
    public List<InstrumentData> searchInstruments(String exchange, String query, int maxResults) {
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>();
        }

        refreshCacheIfNeeded();

        List<InstrumentData> instruments = instrumentsCache.getOrDefault(exchange, new ArrayList<>());
        String normalizedQuery = query.toLowerCase().trim();

        return instruments.stream()
                .filter(instrument -> "EQ".equals(instrument.getInstrumentType()))
                .filter(instrument -> matchesQuery(instrument, normalizedQuery))
                .limit(maxResults)
                .collect(Collectors.toList());
    }

    /**
     * Validate if symbol exists
     */
    public boolean isValidSymbol(String exchange, String symbol) {
        try {
            getInstrumentKeyBySymbol(exchange, symbol);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * CORRECTED: Get historical candle data with both fromDate and toDate
     */
    public GetHistoricalCandleResponse getHistoricalCandleData(String userId, String instrumentKey,
                                                               String unit, Integer interval,
                                                               String fromDate, String toDate) {
        try {
            configAPIClient(userId);

            HistoryV3Api historyApi = new HistoryV3Api();

            // The actual API signature likely requires both dates
            GetHistoricalCandleResponse response = historyApi.getHistoricalCandleData1(
                    instrumentKey, unit, interval, toDate, fromDate);

            log.info("Retrieved historical candle data for instrument: {}, unit: {}, interval: {}, from: {}, to: {}",
                    instrumentKey, unit, interval, fromDate, toDate);

            return response;

        } catch (ApiException e) {
            log.error("Upstox API error for historical data: {}", e.getResponseBody(), e);
            throw new RuntimeException("Failed to fetch historical data: " + e.getResponseBody(), e);
        } catch (Exception e) {
            log.error("Error fetching historical candle data", e);
            throw new RuntimeException("Failed to fetch historical data: " + e.getMessage(), e);
        }
    }

    /**
     * Get historical data for today (intraday)
     */
    public GetHistoricalCandleResponse getHistoricalDataToday(String userId, String instrumentKey,
                                                              String unit, Integer interval) {
        LocalDate today = LocalDate.now();
        String todayStr = today.toString(); // YYYY-MM-DD format

        return getHistoricalCandleData(userId, instrumentKey, unit, interval, todayStr, todayStr);
    }

    /**
     * Get historical data for last N days
     */
    public GetHistoricalCandleResponse getHistoricalDataLastNDays(String userId, String instrumentKey,
                                                                  int days, String unit, Integer interval) {
        LocalDate toDate = LocalDate.now();
        LocalDate fromDate = toDate.minusDays(days);

        return getHistoricalCandleData(userId, instrumentKey, unit, interval,
                fromDate.toString(), toDate.toString());
    }

    // ========== PRIVATE HELPER METHODS ==========

    private void configAPIClient(String userId) throws Exception {
        try {
            Optional<UserProfile> userProfile = userRepository.findByUserIdAndNotExpired(userId, LocalDateTime.now());
            if (userProfile.isPresent()) {
                String accessToken = userProfile.get().getAccessToken();
                ApiClient apiClient = new ApiClient();
                OAuth oAuth = (OAuth) apiClient.getAuthentication("OAUTH2");
                oAuth.setAccessToken(accessToken);
                Configuration.setDefaultApiClient(apiClient);
            } else {
                throw new RuntimeException("Valid user profile not found for userId: " + userId);
            }
        } catch (Exception e) {
            throw new Exception("User authentication failed: " + e.getMessage());
        }
    }

    private void refreshCacheIfNeeded() {
        if (shouldRefreshCache()) {
            refreshInstrumentsCache();
        }
    }

    private boolean shouldRefreshCache() {
        return lastCacheUpdate.isBefore(LocalDateTime.now().minusHours(24)) || instrumentsCache.isEmpty();
    }

    // Refresh daily at 6 AM IST
    public void refreshInstrumentsCache() {
        try {
            log.info("Refreshing instruments cache from Upstox (gzipped)...");

            // Download and decompress the gzipped JSON
            InstrumentData[] instruments = downloadAndDecompressInstruments();

            if (instruments == null || instruments.length == 0) {
                log.error("Failed to download instruments data or empty response");
                return;
            }

            // Clear caches
            instrumentsCache.clear();
            symbolToKeyCache.clear();

            // Group by exchange
            Map<String, List<InstrumentData>> newCache = Arrays.stream(instruments)
                    .filter(instrument -> instrument.getExchange() != null)
                    .filter(instrument -> instrument.getInstrumentType() != null)
                    .collect(Collectors.groupingBy(InstrumentData::getExchange));

            instrumentsCache.putAll(newCache);
            lastCacheUpdate = LocalDateTime.now();

            // Pre-populate symbol cache for equity
            newCache.values().stream()
                    .flatMap(List::stream)
                    .filter(instrument -> "EQ".equals(instrument.getInstrumentType()))
                    .forEach(instrument -> {
                        String cacheKey = createCacheKey(instrument.getExchange(), instrument.getTradingSymbol());
                        symbolToKeyCache.put(cacheKey, instrument.getInstrumentKey());
                    });

            log.info("Instruments cache refreshed. Exchanges: {}, Total instruments: {}, Equity symbols cached: {}",
                    newCache.size(),
                    newCache.values().stream().mapToInt(List::size).sum(),
                    symbolToKeyCache.size());

        } catch (Exception e) {
            log.error("Error refreshing instruments cache", e);
        }
    }

    private InstrumentData[] downloadAndDecompressInstruments(){
        try{
            HttpHeaders headers = new HttpHeaders();
            headers.set("Accept", "application/json");
            headers.set("Accept-Encoding", "gzip");
            headers.set("User-Agent", "MarketDataService/");
            HttpEntity<?> entity = new HttpEntity<>(headers);
            log.info("Downloading gzipped instruments from: {}", COMPLETE_INSTRUMENTS_URL);

            // Downloading the gZipped data as byte array
            ResponseEntity<byte[]> response = restTemplate.exchange(
                    COMPLETE_INSTRUMENTS_URL,
                    HttpMethod.GET,
                    entity,
                    byte[].class
            );

            if(response.getStatusCode() != HttpStatus.OK || response.getBody() == null){
                log.error("Failed to download instrument data. status: {}", response.getStatusCode());
                return null;
            }

            byte [] gZippedData = response.getBody();
            log.info("Downloaded {} bytes of gzipped data", gZippedData.length);

            // Decompress the gzipped data
            String jsonData = decompressGzipData(gZippedData);
            log.info("Decompressed to {} characters of JSON data", jsonData.length());

            // Parse JSON to InstrumentData array
            InstrumentData[] instruments = objectMapper.readValue(jsonData, InstrumentData[].class);
            log.info("Successfully parsed {} instruments", instruments.length);

            return instruments;

        }catch (Exception e){
            log.error("Error downloading and decompressing instruments data", e);
            return null;
        }
    }

    /**
     * Decompress gzipped byte array to JSON string
     */
    public String decompressGzipData(byte [] gZippedData){
        try (ByteArrayInputStream bais = new ByteArrayInputStream(gZippedData);
             GZIPInputStream gzipInputStream = new GZIPInputStream(bais);){
            return new String(gzipInputStream.readAllBytes(), "UTF-8");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String findInstrumentKey(String exchange, String symbol) {
        List<InstrumentData> instruments = instrumentsCache.get(exchange);
        if (instruments == null || instruments.isEmpty()) {
            return null;
        }

        // Try exact match
        Optional<InstrumentData> exactMatch = instruments.stream()
                .filter(instrument -> "EQ".equals(instrument.getInstrumentType()))
                .filter(instrument -> symbol.equalsIgnoreCase(instrument.getTradingSymbol()))
                .findFirst();

        if (exactMatch.isPresent()) {
            return exactMatch.get().getInstrumentKey();
        }

        // Try variations
        List<String> variations = generateSymbolVariations(symbol);
        for (String variation : variations) {
            Optional<InstrumentData> match = instruments.stream()
                    .filter(instrument -> "EQ".equals(instrument.getInstrumentType()))
                    .filter(instrument -> variation.equalsIgnoreCase(instrument.getTradingSymbol()))
                    .findFirst();

            if (match.isPresent()) {
                return match.get().getInstrumentKey();
            }
        }

        return null;
    }

    private List<String> generateSymbolVariations(String symbol) {
        List<String> variations = new ArrayList<>();
        variations.add(symbol);
        variations.add(symbol.toUpperCase());
        variations.add(symbol.toLowerCase());

        if (symbol.endsWith("-BE")) {
            variations.add(symbol.substring(0, symbol.length() - 3));
        }
        if (symbol.endsWith("_EQ")) {
            variations.add(symbol.substring(0, symbol.length() - 3));
        }
        if (!symbol.endsWith("-EQ")) {
            variations.add(symbol + "-EQ");
        }
        if (symbol.contains("&")) {
            variations.add(symbol.replace("&", ""));
            variations.add(symbol.replace("&", "AND"));
        }
        if (symbol.contains("-")) {
            variations.add(symbol.replace("-", ""));
        }

        return variations.stream().distinct().collect(Collectors.toList());
    }

    private boolean matchesQuery(InstrumentData instrument, String query) {
        return (instrument.getTradingSymbol() != null && instrument.getTradingSymbol().toLowerCase().contains(query)) ||
                (instrument.getName() != null && instrument.getName().toLowerCase().contains(query)) ||
                (instrument.getShortName() != null && instrument.getShortName().toLowerCase().contains(query));
    }

    private String createCacheKey(String exchange, String symbol) {
        return exchange + ":" + symbol.toUpperCase();
    }

    /**
     * Get cache statistics
     */
    public Map<String, Object> getCacheStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("lastUpdate", lastCacheUpdate);
        stats.put("totalExchanges", instrumentsCache.size());
        stats.put("totalInstruments", instrumentsCache.values().stream().mapToInt(List::size).sum());
        stats.put("cachedSymbols", symbolToKeyCache.size());

        Map<String, Integer> exchangeBreakdown = instrumentsCache.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().size()));
        stats.put("exchangeBreakdown", exchangeBreakdown);

        return stats;
    }
}
