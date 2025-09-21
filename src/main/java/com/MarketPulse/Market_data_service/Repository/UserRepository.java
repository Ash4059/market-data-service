package com.MarketPulse.Market_data_service.Repository;

import com.MarketPulse.Market_data_service.Entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserProfile, Long> {

    @Query("SELECT u FROM UserProfile u WHERE u.userId = :userId AND u.expiryTime > :now")
    Optional<UserProfile> findByUserIdAndNotExpired(@Param("userId") String userId,
                                                    @Param("now") LocalDateTime now);

    @Query("SELECT u.accessToken FROM UserProfile u WHERE u.userId = :userId AND u.expiryTime > :now")
    Optional<String> findAccessTokenByUserId(@Param("userId") String userId,
                                             @Param("now") LocalDateTime now);

    @Modifying
    @Query("DELETE FROM UserProfile u WHERE u.expiryTime <= :now")
    int deleteExpiredData(@Param("now") LocalDateTime now);

    @Query("SELECT COUNT(u) FROM UserProfile u WHERE u.expiryTime > :now")
    long countActiveUsers(@Param("now") LocalDateTime now);
}

