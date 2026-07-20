package com.thalicloud.delivery.config;

import com.thalicloud.delivery.service.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

// STOMP CONNECT frames carry the JWT as a frame header (RN's WebSocket can't
// set custom headers on the HTTP upgrade the way a normal REST call does),
// so auth happens here instead of JwtAuthFilter — same extractUsername/
// isTokenValid logic, just applied to the STOMP handshake instead of an
// HttpServletRequest. The resulting Authentication becomes the STOMP
// session's Principal, which @MessageMapping methods can read.
@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String jwt = authHeader.substring(7);
                try {
                    String username = jwtService.extractUsername(jwt);
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                    if (jwtService.isTokenValid(jwt, userDetails)) {
                        var authToken = new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());
                        accessor.setUser(authToken);
                    }
                } catch (Exception ignored) {
                    // Invalid/expired token — leave the session unauthenticated;
                    // the @MessageMapping handler rejects it for lack of a Principal.
                }
            }
        }

        return message;
    }
}
