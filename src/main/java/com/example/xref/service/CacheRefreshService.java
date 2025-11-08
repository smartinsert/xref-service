package com.example.xref.service;

import com.example.xref.model.Symbol;
import com.example.xref.repository.SymbolRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
public class CacheRefreshService {

    @Autowired
    private DownstreamClient downstreamClient;

    @Autowired
    private SymbolRepository symbolRepository;

    @Autowired
    private CrossReferenceService crossReferenceService;

    /**
     * Scheduled job to refresh cache daily at 11 PM
     */
    @Scheduled(cron = "${cache.refresh.cron:0 0 23 * * ?}")
    public void scheduledCacheRefresh() {
        log.info("Starting scheduled cache refresh");
        refreshCache();
    }

    /**
     * Refresh cache from downstream
     */
    public Map<String, Integer> refreshCache() {
        Map<String, Integer> stats = new HashMap<>();
        int added = 0, updated = 0, deleted = 0;

        try {
            List<Symbol> latestSymbols = downstreamClient.fetchAllSymbols();
            Set<String> existingSymbolIds = symbolRepository.getAllSymbolIds();
            Set<String> latestSymbolIds = new java.util.HashSet<>();

            for (Symbol symbol : latestSymbols) {
                latestSymbolIds.add(symbol.getSymbolId());

                if (existingSymbolIds.contains(symbol.getSymbolId())) {
                    // Check if update needed
                    Map<String, String> existing = symbolRepository.getSymbolData(symbol.getSymbolId());
                    if (hasChanged(existing, symbol)) {
                        crossReferenceService.updateSymbol(mapToSymbol(existing), symbol);
                        updated++;
                    }
                } else {
                    crossReferenceService.addSymbol(symbol);
                    added++;
                }
            }

            // Remove deleted symbols
            for (String symbolId : existingSymbolIds) {
                if (!latestSymbolIds.contains(symbolId)) {
                    crossReferenceService.deleteSymbol(symbolId);
                    deleted++;
                }
            }

            stats.put("added", added);
            stats.put("updated", updated);
            stats.put("deleted", deleted);

            log.info("Cache refresh completed - Added: {}, Updated: {}, Deleted: {}", 
                     added, updated, deleted);

        } catch (Exception e) {
            log.error("Cache refresh failed", e);
            stats.put("error", 1);
        }

        return stats;
    }

    private boolean hasChanged(Map<String, String> existing, Symbol newSymbol) {
        return !existing.getOrDefault("isin", "").equals(newSymbol.getIsin() == null ? "" : newSymbol.getIsin()) ||
               !existing.getOrDefault("cusip", "").equals(newSymbol.getCusip() == null ? "" : newSymbol.getCusip()) ||
               !existing.getOrDefault("sedol", "").equals(newSymbol.getSedol() == null ? "" : newSymbol.getSedol()) ||
               !existing.getOrDefault("ticker", "").equals(newSymbol.getTicker() == null ? "" : newSymbol.getTicker());
    }

    private Symbol mapToSymbol(Map<String, String> data) {
        return Symbol.builder()
                .symbolId(data.get("symbolId"))
                .isin(data.get("isin"))
                .cusip(data.get("cusip"))
                .sedol(data.get("sedol"))
                .ticker(data.get("ticker"))
                .bloombergId(data.get("bloombergId"))
                .name(data.get("name"))
                .build();
    }
}