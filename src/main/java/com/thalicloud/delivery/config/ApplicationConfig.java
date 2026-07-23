package com.thalicloud.delivery.config;

import com.thalicloud.delivery.entity.DeliveryPartner;
import com.thalicloud.delivery.repository.DeliveryPartnerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
@RequiredArgsConstructor
public class ApplicationConfig {

    private final DeliveryPartnerRepository partnerRepository;

    // JWT subject is "PARTNER:+91XXXXXXXXXX" (see DeliveryPartner.USERNAME_PREFIX,
    // set by auth-service at OTP verification) — strip the prefix to look up by phone.
    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            String phone = username.startsWith(DeliveryPartner.USERNAME_PREFIX)
                    ? username.substring(DeliveryPartner.USERNAME_PREFIX.length())
                    : username;
            return partnerRepository.findByPhone(phone)
                    .orElseThrow(() -> new UsernameNotFoundException("Delivery partner not found: " + phone));
        };
    }

    // Used by OrderStatusCallbackClient to notify order-service after a
    // pickup/delivery OTP match. Short timeouts so a slow/unavailable
    // order-service degrades the callback instead of hanging the verification.
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(2))
                .setReadTimeout(Duration.ofSeconds(3))
                .build();
    }
}
