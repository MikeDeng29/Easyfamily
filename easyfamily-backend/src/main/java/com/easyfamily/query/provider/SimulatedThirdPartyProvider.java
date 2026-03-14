package com.easyfamily.query.provider;

import org.springframework.stereotype.Component;

@Component
public class SimulatedThirdPartyProvider implements BindingQueryProvider {

    @Override
    public String key() {
        return "simulated";
    }

    @Override
    public ProviderResult verifyRealName(String phone, String name, String idCardNo) {
        int signal = phone.chars().sum() + name.chars().sum() + (idCardNo == null ? 0 : idCardNo.chars().sum());
        boolean verified = signal % 2 == 0;
        return new ProviderResult(verified, "provider-simulated");
    }
}
