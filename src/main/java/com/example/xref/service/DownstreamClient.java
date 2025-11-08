package com.example.xref.service;

import com.example.xref.model.Symbol;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
public class DownstreamClient {

    @Value("${downstream.endpoint:http://downstream-service/api/symbols}")
    private String downstreamEndpoint;

    private final RestTemplate restTemplate = new RestTemplate();

    @CircuitBreaker(name = "downstreamService", fallbackMethod = "fallbackFetchSymbols")
    public List<Symbol> fetchAllSymbols() {
        log.info("Fetching symbols from downstream: {}", downstreamEndpoint);

        Symbol[] symbols = restTemplate.getForObject(downstreamEndpoint, Symbol[].class);
        return symbols != null ? Arrays.asList(symbols) : List.of();
    }

    public List<Symbol> fallbackFetchSymbols(Throwable t) {
        log.error("Failed to fetch from downstream", t);
        return List.of();
    }
}