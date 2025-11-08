package com.example.xref.repository;

import com.example.xref.model.Symbol;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.*;

@Slf4j
@Repository
public class SymbolRepository {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private static final String SYMBOL_PREFIX = "symbol:";
    private static final String XREF_PREFIX = "xref:";
    private static final String[] ID_TYPES = {"isin", "cusip", "sedol", "ticker", "bloombergId"};

    /**
     * Find symbol ID by any identifier
     */
    public String findSymbolIdByIdentifier(String idType, String idValue) {
        String key = XREF_PREFIX + idType.toLowerCase() + ":" + idValue.toUpperCase();
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * Get complete symbol data
     */
    public Map<String, String> getSymbolData(String symbolId) {
        String key = SYMBOL_PREFIX + symbolId;
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);

        Map<String, String> result = new HashMap<>();
        entries.forEach((k, v) -> result.put(k.toString(), v.toString()));
        return result;
    }

    /**
     * Save symbol with all cross-references
     */
    public void saveSymbol(Symbol symbol) {
        String symbolId = symbol.getSymbolId();
        String symbolKey = SYMBOL_PREFIX + symbolId;

        // Build attributes map
        Map<String, String> attributes = new HashMap<>();
        attributes.put("symbolId", symbolId);
        if (symbol.getIsin() != null) attributes.put("isin", symbol.getIsin());
        if (symbol.getCusip() != null) attributes.put("cusip", symbol.getCusip());
        if (symbol.getSedol() != null) attributes.put("sedol", symbol.getSedol());
        if (symbol.getTicker() != null) attributes.put("ticker", symbol.getTicker());
        if (symbol.getBloombergId() != null) attributes.put("bloombergId", symbol.getBloombergId());
        if (symbol.getName() != null) attributes.put("name", symbol.getName());

        // Save symbol hash
        redisTemplate.opsForHash().putAll(symbolKey, attributes);

        // Create cross-reference mappings
        for (String idType : ID_TYPES) {
            String value = attributes.get(idType);
            if (value != null && !value.isEmpty()) {
                String xrefKey = XREF_PREFIX + idType + ":" + value.toUpperCase();
                redisTemplate.opsForValue().set(xrefKey, symbolId);
            }
        }

        log.info("Saved symbol: {}", symbolId);
    }

    /**
     * Update symbol and handle identifier changes
     */
    public void updateSymbol(Symbol oldSymbol, Symbol newSymbol) {
        String symbolId = newSymbol.getSymbolId();

        // Remove old cross-references
        if (oldSymbol != null) {
            removeOldCrossReferences(oldSymbol);
        }

        // Save new version
        saveSymbol(newSymbol);
    }

    /**
     * Delete symbol and all cross-references
     */
    public void deleteSymbol(String symbolId) {
        Map<String, String> symbolData = getSymbolData(symbolId);

        // Delete cross-references
        for (String idType : ID_TYPES) {
            String value = symbolData.get(idType);
            if (value != null) {
                String xrefKey = XREF_PREFIX + idType + ":" + value.toUpperCase();
                redisTemplate.delete(xrefKey);
            }
        }

        // Delete symbol hash
        redisTemplate.delete(SYMBOL_PREFIX + symbolId);
        log.info("Deleted symbol: {}", symbolId);
    }

    private void removeOldCrossReferences(Symbol symbol) {
        if (symbol.getIsin() != null) {
            redisTemplate.delete(XREF_PREFIX + "isin:" + symbol.getIsin().toUpperCase());
        }
        if (symbol.getCusip() != null) {
            redisTemplate.delete(XREF_PREFIX + "cusip:" + symbol.getCusip().toUpperCase());
        }
        if (symbol.getSedol() != null) {
            redisTemplate.delete(XREF_PREFIX + "sedol:" + symbol.getSedol().toUpperCase());
        }
        if (symbol.getTicker() != null) {
            redisTemplate.delete(XREF_PREFIX + "ticker:" + symbol.getTicker().toUpperCase());
        }
        if (symbol.getBloombergId() != null) {
            redisTemplate.delete(XREF_PREFIX + "bloombergId:" + symbol.getBloombergId().toUpperCase());
        }
    }

    /**
     * Get all symbol IDs
     */
    public Set<String> getAllSymbolIds() {
        Set<String> keys = redisTemplate.keys(SYMBOL_PREFIX + "*");
        Set<String> symbolIds = new HashSet<>();
        if (keys != null) {
            keys.forEach(key -> symbolIds.add(key.replace(SYMBOL_PREFIX, "")));
        }
        return symbolIds;
    }
}