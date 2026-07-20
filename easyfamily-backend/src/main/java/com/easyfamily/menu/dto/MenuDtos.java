package com.easyfamily.menu.dto;

import java.util.List;

public final class MenuDtos {

    private MenuDtos() {}

    public record WeeklyMenuResponse(
            String weekOf,           // ISO week start date: "2026-07-07"
            String city,             // city used for generation
            List<DayMenu> days,      // 7 days, Monday through Sunday
            String seasonTip         // seasonal cooking tip
    ) {}

    public record DayMenu(
            String date,             // "2026-07-07"
            String dayLabel,         // "周一"
            String breakfast,        // brief description
            String lunch,            // brief description
            String dinner,           // brief description
            List<String> keyVegetables  // 2-3 seasonal vegetables highlighted for this day
    ) {}
}
