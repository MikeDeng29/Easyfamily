package com.easyfamily.phone.controller;

import com.easyfamily.auth.service.AuthService;
import com.easyfamily.common.api.ApiResponse;
import com.easyfamily.phone.dto.PhoneDtos.PhoneBindRequest;
import com.easyfamily.phone.dto.PhoneDtos.PhoneItem;
import com.easyfamily.phone.dto.PhoneDtos.PhoneSetPrimaryRequest;
import com.easyfamily.phone.dto.PhoneDtos.PhoneUnbindRequest;
import com.easyfamily.phone.service.PhoneManagementService;
import com.easyfamily.security.AuthContext;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/phones")
public class PhoneController {

    private final PhoneManagementService phoneManagementService;
    private final AuthService authService;

    public PhoneController(PhoneManagementService phoneManagementService, AuthService authService) {
        this.phoneManagementService = phoneManagementService;
        this.authService = authService;
    }

    @GetMapping("/mine")
    public ApiResponse<List<PhoneItem>> listMyPhones() {
        var current = AuthContext.currentUser();
        return ApiResponse.ok(phoneManagementService.listMyPhones(current.userId(), current.phone()));
    }

    @PostMapping("/bind")
    public ApiResponse<Void> bindPhone(@Valid @RequestBody PhoneBindRequest request) {
        var current = AuthContext.currentUser();
        authService.verifySmsCode(request.phone(), request.smsCode());
        phoneManagementService.bindPhone(current.userId(), request.phone(), current.phone());
        return ApiResponse.ok(null);
    }

    @PostMapping("/unbind")
    public ApiResponse<Void> unbindPhone(@Valid @RequestBody PhoneUnbindRequest request) {
        var current = AuthContext.currentUser();
        phoneManagementService.unbindPhone(current.userId(), request.phone(), current.phone());
        return ApiResponse.ok(null);
    }

    @PostMapping("/primary")
    public ApiResponse<Void> setPrimaryPhone(@Valid @RequestBody PhoneSetPrimaryRequest request) {
        var current = AuthContext.currentUser();
        phoneManagementService.setPrimaryPhone(current.userId(), request.phone(), current.phone());
        return ApiResponse.ok(null);
    }
}
