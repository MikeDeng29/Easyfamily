package com.easyfamily.monitor;

import com.easyfamily.common.api.ApiResponse;
import com.easyfamily.security.AuthContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/v1/monitor")
public class CardMonitorController {

    private static final Logger log = LoggerFactory.getLogger(CardMonitorController.class);

    private final CardMonitorService cardMonitorService;

    public CardMonitorController(CardMonitorService cardMonitorService) {
        this.cardMonitorService = cardMonitorService;
    }

    @GetMapping("/snapshots")
    public ApiResponse<List<MonitorSnapshotItem>> getSnapshots() {
        String userId = AuthContext.currentUser().userId();
        return ApiResponse.ok(cardMonitorService.getLatestSnapshots(userId));
    }

    @PostMapping("/scan")
    public ResponseEntity<ApiResponse<Void>> triggerScan() {
        String userId = AuthContext.currentUser().userId();
        CompletableFuture.runAsync(() -> {
            try {
                cardMonitorService.scanAllFamilyMembers(userId);
            } catch (Exception e) {
                log.error("Async monitor scan failed for userId={}: {}", userId, e.getMessage(), e);
            }
        });
        return ResponseEntity.accepted().body(ApiResponse.ok(null));
    }
}
