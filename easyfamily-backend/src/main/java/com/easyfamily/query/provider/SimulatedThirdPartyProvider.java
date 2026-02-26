package com.easyfamily.query.provider;

import org.springframework.stereotype.Component;

@Component
public class SimulatedThirdPartyProvider implements BindingQueryProvider {

    @Override
    public String key() {
        return "simulated";
    }

    @Override
    public ProviderResult queryBinding(String phone, String queryType) {
        boolean bankBound = phone.chars().sum() % 2 == 0;
        boolean socialBound = phone.chars().sum() % 3 != 0;
        return new ProviderResult(bankBound, socialBound, "provider-simulated");
    }
}
