package com.easyfamily.query.provider;

public interface BindingQueryProvider {

    String key();

    ProviderResult verifyRealName(String phone, String name, String idCardNo);

    record ProviderResult(boolean verified, String providerName) {
    }
}
