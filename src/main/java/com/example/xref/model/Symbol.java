package com.example.xref.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Symbol {
    private String symbolId;
    private String isin;
    private String cusip;
    private String sedol;
    private String ticker;
    private String bloombergId;
    private String name;
    private Map<String, String> additionalAttributes;
}