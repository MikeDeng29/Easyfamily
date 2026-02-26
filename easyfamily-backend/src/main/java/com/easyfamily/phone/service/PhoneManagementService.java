package com.easyfamily.phone.service;

import com.easyfamily.common.exception.BusinessException;
import com.easyfamily.phone.dto.PhoneDtos.PhoneItem;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PhoneManagementService {

    private final Map<String, List<PhoneItem>> userPhones = new ConcurrentHashMap<>();

    public List<PhoneItem> listMyPhones(String userId, String loginPhone) {
        List<PhoneItem> current = userPhones.computeIfAbsent(userId, key -> seedDefaultPhones(loginPhone));
        return List.copyOf(current);
    }

    public void bindPhone(String userId, String phone, String loginPhone) {
        List<PhoneItem> current = userPhones.computeIfAbsent(userId, key -> seedDefaultPhones(loginPhone));
        boolean exists = current.stream().anyMatch(item -> item.phone().equals(phone));
        if (exists) {
            throw new BusinessException("PHONE_ALREADY_BOUND", "phone already bound");
        }
        current.add(new PhoneItem(phone, false, "ACTIVE"));
    }

    public void unbindPhone(String userId, String phone, String loginPhone) {
        List<PhoneItem> current = userPhones.computeIfAbsent(userId, key -> seedDefaultPhones(loginPhone));
        PhoneItem target = current.stream().filter(item -> item.phone().equals(phone)).findFirst()
                .orElseThrow(() -> new BusinessException("PHONE_NOT_FOUND", "phone not found"));
        if (target.isPrimary()) {
            throw new BusinessException("PRIMARY_PHONE_CANNOT_UNBIND", "primary phone cannot be unbound");
        }
        current.removeIf(item -> item.phone().equals(phone));
    }

    private List<PhoneItem> seedDefaultPhones(String loginPhone) {
        List<PhoneItem> seeded = new ArrayList<>();
        seeded.add(new PhoneItem(loginPhone, true, "ACTIVE"));
        return seeded;
    }
}
