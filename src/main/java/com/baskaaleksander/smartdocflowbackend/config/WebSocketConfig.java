package com.baskaaleksander.smartdocflowbackend.config;

import com.baskaaleksander.smartdocflowbackend.security.JwtUtil;
import com.baskaaleksander.smartdocflowbackend.service.CustomUserDetailsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService customUserDetailsService;
    private final Logger log = LoggerFactory.getLogger(WebSocketConfig.class);

    public WebSocketConfig(JwtUtil jwtUtil, CustomUserDetailsService customUserDetailsService) {
        this.jwtUtil = jwtUtil;
        this.customUserDetailsService = customUserDetailsService;
    }

    @Bean
    public DefaultHandshakeHandler handshakeHandler() {
        return new DefaultHandshakeHandler() {
            @Override
            protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler, Map<String, Object> attributes) {
                var query = request.getURI().getQuery();
                String token = null;
                if (query != null) {
                    for (String p : query.split("&")) {
                        var kv = p.split("=", 2);
                        if (kv.length == 2 && kv[0].equals("token")) {
                            token = kv[1];
                            break;
                        }
                    }
                }
                if (token != null) {
                    try {
                        String username = jwtUtil.getUsernameFromAccessToken(token);
                        return () -> username;
                    } catch (Exception ignored) {}
                }
                var auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth != null && auth.isAuthenticated()) return auth;

                return super.determineUser(request, wsHandler, attributes);
            }
        };
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry){
        registry.addEndpoint("/ws")
                .setHandshakeHandler(handshakeHandler())
                .setAllowedOriginPatterns("*");

        registry.addEndpoint("/ws-sockjs")
                .setHandshakeHandler(handshakeHandler())
                .setAllowedOriginPatterns("*").withSockJS();

    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
            registry.enableSimpleBroker("/topic", "/queue");
            registry.setApplicationDestinationPrefixes("/app");
            registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                var acc = StompHeaderAccessor.wrap(message);
                if (acc.getCommand() == null) return message;

                if (acc.getUser() == null) {
                     log.warn("WS frame {} without user, dest={}", acc.getCommand(), acc.getDestination());
                    if (acc.getCommand() != StompCommand.CONNECT) {
                        return null;
                    }
                }

                return message;
            }
        });
    }
}
