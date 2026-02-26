package com.easyfamily.query.provider;

import com.easyfamily.common.exception.BusinessException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class BindingQueryProviderRouter {

    private final Map<String, BindingQueryProvider> providers;

    public BindingQueryProviderRouter(List<BindingQueryProvider> providers) {
        this.providers = providers.stream().collect(Collectors.toMap(BindingQueryProvider::key, Function.identity()));
    }

    public BindingQueryProvider resolve(String key) {
        BindingQueryProvider provider = providers.get(key);
        if (provider == null) {
            throw new BusinessException("INVALID_PROVIDER_KEY", "provider key not supported");
        }
        return provider;
    }
}
