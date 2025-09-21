package com.MarketPulse.Market_data_service.Utils;

import com.MarketPulse.Market_data_service.Repository.UserRepository;
import com.MarketPulse.Market_data_service.Service.MarketDataService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@EnableScheduling
@Slf4j
public class CleanUpScheduler {

    private final UserRepository repository;
    private final MarketDataService marketDataService;

    public CleanUpScheduler(UserRepository repository, MarketDataService marketDataService) {
        this.repository = repository;
        this.marketDataService = marketDataService;
    }

    @Scheduled(cron = "0 30 15 * * *", zone = "Asia/Kolkata")
    @Transactional
    public void cleanupExpiredData(){
        log.info("Starting cleanup of expired data");
        int deletedExpiredData = repository.deleteExpiredData(LocalDateTime.now());
        log.info("Cleanup {} expired data completed", deletedExpiredData);
    }

    @Scheduled(fixedRate = 60 * 1000)
    public void logActiveUser(){
        long activeCount = repository.countActiveUsers(LocalDateTime.now());
        log.info("Active user count: {}", activeCount);
    }

    @Scheduled(cron = "0 0 6 * * *")
    public void RefreshInstrumentCache(){
        this.marketDataService.refreshInstrumentsCache();
    }

}
