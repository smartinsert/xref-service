package com.example.xref.grpc;

import com.example.xref.model.Symbol;
import com.example.xref.proto.*;
import com.example.xref.service.CacheRefreshService;
import com.example.xref.service.CrossReferenceService;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

@Slf4j
@GrpcService
public class XRefGrpcService extends CrossReferenceServiceGrpc.CrossReferenceServiceImplBase {

    @Autowired
    private CrossReferenceService crossReferenceService;

    @Autowired
    private CacheRefreshService cacheRefreshService;

    @Override
    public void lookupSymbol(LookupRequest request, StreamObserver<LookupResponse> responseObserver) {
        try {
            Map<String, String> result = crossReferenceService.lookupSymbol(
                    request.getIdType(), 
                    request.getIdValue()
            );

            LookupResponse.Builder builder = LookupResponse.newBuilder()
                    .setFound(Boolean.parseBoolean(result.getOrDefault("found", "false")))
                    .putAllAttributes(result);

            if (result.containsKey("symbolId")) {
                builder.setSymbolId(result.get("symbolId"));
            }

            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Error in lookupSymbol", e);
            responseObserver.onNext(LookupResponse.newBuilder()
                    .setFound(false)
                    .setErrorMessage(e.getMessage())
                    .build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void batchLookup(BatchLookupRequest request, StreamObserver<LookupResponse> responseObserver) {
        try {
            for (LookupRequest lookupRequest : request.getRequestsList()) {
                Map<String, String> result = crossReferenceService.lookupSymbol(
                        lookupRequest.getIdType(),
                        lookupRequest.getIdValue()
                );

                LookupResponse.Builder builder = LookupResponse.newBuilder()
                        .setFound(Boolean.parseBoolean(result.getOrDefault("found", "false")))
                        .putAllAttributes(result);

                if (result.containsKey("symbolId")) {
                    builder.setSymbolId(result.get("symbolId"));
                }

                responseObserver.onNext(builder.build());
            }
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Error in batchLookup", e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void addSymbol(AddSymbolRequest request, StreamObserver<AddSymbolResponse> responseObserver) {
        try {
            Symbol symbol = Symbol.builder()
                    .symbolId(request.getSymbolId())
                    .isin(request.getAttributesOrDefault("isin", null))
                    .cusip(request.getAttributesOrDefault("cusip", null))
                    .sedol(request.getAttributesOrDefault("sedol", null))
                    .ticker(request.getAttributesOrDefault("ticker", null))
                    .bloombergId(request.getAttributesOrDefault("bloombergId", null))
                    .name(request.getAttributesOrDefault("name", null))
                    .build();

            crossReferenceService.addSymbol(symbol);

            responseObserver.onNext(AddSymbolResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Symbol added successfully")
                    .build());
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Error in addSymbol", e);
            responseObserver.onNext(AddSymbolResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Error: " + e.getMessage())
                    .build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void refreshCache(RefreshCacheRequest request, StreamObserver<RefreshCacheResponse> responseObserver) {
        try {
            Map<String, Integer> stats = cacheRefreshService.refreshCache();

            responseObserver.onNext(RefreshCacheResponse.newBuilder()
                    .setSymbolsAdded(stats.getOrDefault("added", 0))
                    .setSymbolsUpdated(stats.getOrDefault("updated", 0))
                    .setSymbolsRemoved(stats.getOrDefault("deleted", 0))
                    .setStatus("Success")
                    .build());
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Error in refreshCache", e);
            responseObserver.onNext(RefreshCacheResponse.newBuilder()
                    .setStatus("Failed: " + e.getMessage())
                    .build());
            responseObserver.onCompleted();
        }
    }
}