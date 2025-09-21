package com.MarketPulse.Market_data_service.Controller;


import com.MarketPulse.Market_data_service.Entity.UserProfile;
import com.MarketPulse.Market_data_service.Service.UserDataExpiryService;
import com.upstox.ApiClient;
import com.upstox.ApiException;
import com.upstox.Configuration;
import com.upstox.api.GetProfileResponse;
import com.upstox.api.ProfileData;
import com.upstox.api.TokenResponse;
import com.upstox.auth.OAuth;
import io.swagger.client.api.LoginApi;
import io.swagger.client.api.UserApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
public class UpStoxAuthController {

    @Value("${upstox.api-key}")
    private String clientId;

    @Value("${upstox.client-secret}")
    private String clientSecret;

    @Value("${upstox.redirect-uri}")
    private String redirectUri;

    private final UserDataExpiryService userDataExpiryService;

    public UpStoxAuthController(UserDataExpiryService userDataExpiryService) {
        this.userDataExpiryService = userDataExpiryService;
    }

    @GetMapping("/login")
    public ResponseEntity<?> initiateLogin(){
        String authUrl = "https://api.upstox.com/v2/login/authorization/dialog?" +
                "response_type=code" +
                "&client_id=" + clientId +
                "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8) +
                "&state=" + generateRandomState();

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(authUrl))
                .build();
    }

    // Option 2: Get URL to copy-paste (helpful for testing)
    @GetMapping("/get-url")
    public ResponseEntity<Map<String, String>> getAuthorizationUrl(){
        String authUrl = "https://api.upstox.com/v2/login/authorization/dialog?" +
                "response_type=code" +
                "&client_id=" + clientId +
                "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8) +
                "&state=" + generateRandomState();

        Map<String, String> response = new HashMap<>();
        response.put("authUrl", authUrl);
        response.put("instructions", "Copy this URL and paste in browser to login");

        return ResponseEntity.ok(response);
    }

    @GetMapping("/callback")
    public ResponseEntity<?> handleCallback(
            @RequestParam("code") String code,
            @RequestParam(value = "state", required = false) String state,
            @RequestParam(value = "error", required = false) String error) {

        if (error != null) {
            return ResponseEntity.badRequest().body("Authorization failed: " + error);
        }

        try {
            LoginApi loginApi = new LoginApi();
            TokenResponse tokenResponse = loginApi.token("2.0", code,
                    this.clientId, this.clientSecret, this.redirectUri, "authorization_code");

            String accessToken = tokenResponse.getAccessToken();
            ProfileData userProfile = getUserProfile(accessToken);

            UserProfile oCurrentUserProfile = new UserProfile();
            oCurrentUserProfile.setUserId(userProfile.getUserId());
            oCurrentUserProfile.setAccessToken(accessToken);
            oCurrentUserProfile.setStatus("Active");
            oCurrentUserProfile.setEmail(userProfile.getEmail());
            oCurrentUserProfile.setUserName(userProfile.getUserName());
            oCurrentUserProfile.setBroker(userProfile.getBroker());
            oCurrentUserProfile.setExpiryTime(userDataExpiryService.calculateExpiryTime());
            oCurrentUserProfile.setCreatedAt(LocalDateTime.now());
            userDataExpiryService.StoreUserAccessToken(oCurrentUserProfile);

            return ResponseEntity.ok(oCurrentUserProfile);

        } catch (ApiException e) {
            System.err.println("API Exception: " + e.getResponseBody());
            return ResponseEntity.badRequest().body("Token exchange failed: " + e.getResponseBody());
        } catch (Exception e) {
            System.err.println("General Exception: " + e.getMessage());
            return ResponseEntity.status(500).body("Internal error: " + e.getMessage());
        }
    }

    private ProfileData getUserProfile(String accessToken){
        try{
            ApiClient defaultClient = Configuration.getDefaultApiClient();
            OAuth oAuth = (OAuth) defaultClient.getAuthentication("OAUTH2");
            oAuth.setAccessToken(accessToken);
            UserApi userApi = new UserApi();
            GetProfileResponse response = userApi.getProfile("2.0");
            return response.getData();
        } catch (ApiException e) {
            throw new RuntimeException(e);
        }
    }

    private String generateRandomState(){
        return UUID.randomUUID().toString();
    }

}
