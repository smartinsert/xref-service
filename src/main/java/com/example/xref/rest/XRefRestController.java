package com.example.xref.rest;

import com.example.xref.model.Symbol;
import com.example.xref.service.CacheRefreshService;
import com.example.xref.service.CrossReferenceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/xref")
public class XRefRestController {

    @Autowired
    private CrossReferenceService crossReferenceService;

    @Autowired
    private CacheRefreshService cacheRefreshService;

    @GetMapping("/lookup")
    public ResponseEntity<Map<String, String>> lookup(
            @RequestParam String idType,
            @RequestParam String idValue) {

        Map<String, String> result = crossReferenceService.lookupSymbol(idType, idValue);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/symbol")
    public ResponseEntity<String> addSymbol(@RequestBody Symbol symbol) {
        crossReferenceService.addSymbol(symbol);
        return ResponseEntity.ok("Symbol added successfully");
    }

    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Integer>> refreshCache() {
        Map<String, Integer> stats = cacheRefreshService.refreshCache();
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }
}