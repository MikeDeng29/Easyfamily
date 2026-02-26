package com.easyfamily.query.provider;

public interface BindingQueryProvider {

    String key();

    ProviderResult queryBinding(String phone, String queryType);

    record ProviderResult(boolean bankBound, boolean socialBound, String providerName) {
    }
}
