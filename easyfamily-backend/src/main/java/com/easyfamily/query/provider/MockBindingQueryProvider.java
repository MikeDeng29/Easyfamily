package com.easyfamily.query.provider;

import org.springframework.stereotype.Component;

@Component
public class MockBindingQueryProvider implements BindingQueryProvider {

    @Override
    public String key() {
        return "mock";
    }

    @Override
    public ProviderResult verifyRealName(String phone, String name, String idCardNo) {
        // Placeholder logic for local testing; replace with real third-party API.
        boolean verified;
        if (idCardNo == null || idCardNo.isBlank()) {
            verified = !name.isBlank() && Character.getNumericValue(phone.charAt(phone.length() - 1)) % 2 == 0;
        } else {
            verified = phone.charAt(phone.length() - 1) == idCardNo.charAt(idCardNo.length() - 1)
                    && !name.isBlank();
        }
        return new ProviderResult(verified, "provider-mock");
    }
}
