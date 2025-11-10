package com.ankitthakur.xref.service;

import com.ankitthakur.xref.model.Symbol;
import com.ankitthakur.xref.repository.SymbolRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class CrossReferenceService {

    private static final Logger log = LoggerFactory.getLogger(CrossReferenceService.class);

    @Autowired
    private SymbolRepository symbolRepository;

    @RateLimiter(name = "xrefService")
    @CircuitBreaker(name = "xrefService", fallbackMethod = "fallbackLookup")
    public Map<String, String> lookupSymbol(String idType, String idValue) {
        log.debug("Looking up symbol by {}:{}", idType, idValue);

        String symbolId = symbolRepository.findSymbolIdByIdentifier(idType, idValue);
        if (symbolId == null) {
            log.warn("Symbol not found for {}:{}", idType, idValue);
            return Map.of("found", "false", "error", "Symbol not found");
        }

        Map<String, String> symbolData = symbolRepository.getSymbolData(symbolId);
        symbolData.put("found", "true");
        return symbolData;
    }

    public Map<String, String> fallbackLookup(String idType, String idValue, Throwable t) {
        log.error("Fallback triggered for {}:{}", idType, idValue, t);
        return Map.of("found", "false", "error", "Service temporarily unavailable");
    }

    public void addSymbol(Symbol symbol) {
        symbolRepository.saveSymbol(symbol);
    }

    public void updateSymbol(Symbol oldSymbol, Symbol newSymbol) {
        symbolRepository.updateSymbol(oldSymbol, newSymbol);
    }

    public void deleteSymbol(String symbolId) {
        symbolRepository.deleteSymbol(symbolId);
    }
}