package com.easyfamily.menu.controller;

import com.easyfamily.common.api.ApiResponse;
import com.easyfamily.menu.dto.MenuDtos.WeeklyMenuResponse;
import com.easyfamily.menu.service.MenuService;
import com.easyfamily.security.AuthContext;
import com.easyfamily.user.service.UserProfileService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/menu")
public class MenuController {

    private final MenuService menuService;
    private final UserProfileService userProfileService;

    public MenuController(MenuService menuService, UserProfileService userProfileService) {
        this.menuService = menuService;
        this.userProfileService = userProfileService;
    }

    /**
     * Returns a weekly menu personalised to the current user's city. If the user has not
     * set a city, the service defaults to "全国". Results are cached per city and week.
     */
    @GetMapping("/weekly")
    public ApiResponse<WeeklyMenuResponse> getWeeklyMenu() {
        var current = AuthContext.currentUser();
        var profile = userProfileService.getProfile(current.userId(), current.phone());
        WeeklyMenuResponse menu = menuService.getWeeklyMenu(profile.city());
        return ApiResponse.ok(menu);
    }
}
