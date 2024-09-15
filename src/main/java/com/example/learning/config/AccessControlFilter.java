package com.example.learning.config;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.util.Random;

@Component
@AllArgsConstructor
public class AccessControlFilter implements WebFilter {
    // WebFilter

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        Random random = new Random();
        System.out.print("----------- isA: ");
        Integer isA = random.nextInt(1000);
        System.out.println(isA);

        System.out.print("----------- isB: ");
        Integer isB = random.nextInt(1000);
        System.out.println(isB);
        return chain.filter(exchange)
                .contextWrite(Context.of("isA", isA,
                        "isB", isB));
    }
}
