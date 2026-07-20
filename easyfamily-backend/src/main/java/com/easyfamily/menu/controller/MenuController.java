package com.easyfamily.menu.controller;

import com.easyfamily.common.api.ApiResponse;
import com.easyfamily.menu.dto.MenuDtos.PreferenceRequest;
import com.easyfamily.menu.dto.MenuDtos.WeeklyMenuResponse;
import com.easyfamily.menu.service.MenuService;
import com.easyfamily.security.AuthContext;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/menu")
public class MenuController {

    private final MenuService menuService;

    public MenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    @GetMapping("/weekly")
    public ApiResponse<WeeklyMenuResponse> getWeeklyMenu() {
        var current = AuthContext.currentUser();
        return ApiResponse.ok(menuService.getWeeklyMenuForUser(current.userId()));
    }

    /** Records that the current user likes a dish; increments its weight by 1. */
    @PostMapping("/preference")
    public ApiResponse<Void> markPreference(@RequestBody PreferenceRequest request) {
        var current = AuthContext.currentUser();
        menuService.incrementDishWeight(current.userId(), request.dishName());
        return ApiResponse.ok(null);
    }
}
