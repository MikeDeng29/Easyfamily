package com.easyfamily.menu.scheduler;

import com.easyfamily.menu.service.MenuService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Runs every Sunday at 20:00 (server local time) to pre-generate next week's menu
 * for every registered user, so Monday-morning loads are served from cache.
 */
@Component
public class MenuScheduler {

    private static final Logger log = LoggerFactory.getLogger(MenuScheduler.class);

    private final MenuService menuService;

    public MenuScheduler(MenuService menuService) {
        this.menuService = menuService;
    }

    @Scheduled(cron = "0 0 20 * * SUN")
    public void generateNextWeekMenusForAllUsers() {
        log.info("[MenuScheduler] Starting next-week menu pre-generation");
        List<String[]> users = menuService.getAllUsersWithCity();
        if (users.isEmpty()) {
            log.info("[MenuScheduler] No users to process");
            return;
        }
        ExecutorService pool = Executors.newFixedThreadPool(Math.min(users.size(), 10));
        List<CompletableFuture<Boolean>> futures = new ArrayList<>();
        for (String[] row : users) {
            String userId = row[0];
            String city   = row[1];
            CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() -> {
                try {
                    menuService.generateNextWeek(userId, city);
                    return true;
                } catch (Exception e) {
                    log.warn("[MenuScheduler] Failed for user {}: {}", userId, e.getMessage());
                    return false;
                }
            }, pool);
            futures.add(future);
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        pool.shutdown();
        long success = futures.stream().filter(f -> {
            try { return f.get(); } catch (Exception e) { return false; }
        }).count();
        long failed = users.size() - success;
        log.info("[MenuScheduler] Done — success={}, failed={}, total={}", success, failed, users.size());
    }
}
