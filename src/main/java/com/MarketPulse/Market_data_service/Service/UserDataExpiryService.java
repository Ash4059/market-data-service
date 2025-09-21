package com.MarketPulse.Market_data_service.Service;

import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.MarketPulse.Market_data_service.Entity.UserProfile;
import com.MarketPulse.Market_data_service.Repository.UserRepository;

@Service
@Slf4j
public class UserDataExpiryService {

    private final UserRepository userRepository;

    public UserDataExpiryService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void StoreUserAccessToken(UserProfile userProfile) {
        this.userRepository.save(userProfile);
        log.info("User data saved successfully");
    }

    public Optional<UserProfile> getUserData(String userId) {
        return this.userRepository.findByUserIdAndNotExpired(userId, LocalDateTime.now());
    }

    public Optional<String> getAccessToken(String userId) {
        return this.userRepository.findAccessTokenByUserId(userId, LocalDateTime.now());
    }

    public LocalDateTime calculateExpiryTime() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiryTime = now.toLocalDate().atTime(15, 30);
        if (now.isAfter(expiryTime)) {
            expiryTime = expiryTime.plusDays(1);
        }
        return expiryTime;
    }
}
