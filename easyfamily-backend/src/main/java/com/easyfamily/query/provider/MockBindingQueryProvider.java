package com.easyfamily.query.provider;

import org.springframework.stereotype.Component;

@Component
public class MockBindingQueryProvider implements BindingQueryProvider {

    @Override
    public String key() {
        return "mock";
    }

    @Override
    public ProviderResult queryBinding(String phone, String queryType) {
        // Placeholder provider logic for MVP. Replace with real third-party API.
        boolean bankBound = phone.endsWith("0") || phone.endsWith("8");
        boolean socialBound = !phone.endsWith("9");
        return new ProviderResult(bankBound, socialBound, "provider-mock");
    }
}
