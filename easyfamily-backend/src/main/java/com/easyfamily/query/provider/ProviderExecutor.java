package com.easyfamily.query.provider;

import com.easyfamily.common.exception.BusinessException;
import com.easyfamily.query.service.QueryRuntimeSettingsService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
public class ProviderExecutor {

    private final QueryRuntimeSettingsService settingsService;
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final Map<String, Integer> failureCounter = new ConcurrentHashMap<>();
    private final Map<String, Instant> circuitOpenUntil = new ConcurrentHashMap<>();

    public ProviderExecutor(QueryRuntimeSettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public BindingQueryProvider.ProviderResult executeWithGuards(
            String providerKey,
            Callable<BindingQueryProvider.ProviderResult> call
    ) {
        QueryRuntimeSettingsService.RuntimeSettings settings = settingsService.current();

        Instant openUntil = circuitOpenUntil.get(providerKey);
        if (openUntil != null && Instant.now().isBefore(openUntil)) {
            throw new BusinessException("PROVIDER_CIRCUIT_OPEN", "provider circuit is open");
        }

        Exception last = null;
        int attempts = Math.max(1, settings.providerRetryTimes() + 1);
        for (int i = 0; i < attempts; i++) {
            try {
                Future<BindingQueryProvider.ProviderResult> future = executor.submit(call);
                BindingQueryProvider.ProviderResult result = future.get(
                        settings.providerTimeoutMs(), TimeUnit.MILLISECONDS
                );
                failureCounter.put(providerKey, 0);
                return result;
            } catch (TimeoutException e) {
                last = e;
            } catch (ExecutionException e) {
                last = e;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                last = e;
                break;
            }
        }

        int failures = failureCounter.getOrDefault(providerKey, 0) + 1;
        failureCounter.put(providerKey, failures);
        if (failures >= settings.providerCircuitFailureThreshold()) {
            circuitOpenUntil.put(providerKey, Instant.now().plusSeconds(settings.providerCircuitOpenSeconds()));
        }
        if (last instanceof TimeoutException) {
            throw new BusinessException("PROVIDER_TIMEOUT", "provider call timeout");
        }
        throw new BusinessException("PROVIDER_UNAVAILABLE", last == null ? "provider unavailable" : last.getMessage());
    }
}
