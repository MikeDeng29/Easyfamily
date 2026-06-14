package com.easyfamily.ai.memory;

import com.easyfamily.ai.memory.MemoryDtos.MemoryItem;
import com.easyfamily.common.api.ApiResponse;
import com.easyfamily.security.AuthContext;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/memory")
public class UserMemoryController {

    private final UserMemoryService userMemoryService;

    public UserMemoryController(UserMemoryService userMemoryService) {
        this.userMemoryService = userMemoryService;
    }

    @GetMapping
    public ApiResponse<List<MemoryItem>> list() {
        var user = AuthContext.currentUser();
        return ApiResponse.ok(userMemoryService.list(user.userId()));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        var user = AuthContext.currentUser();
        userMemoryService.delete(user.userId(), id);
        return ApiResponse.ok(null);
    }
}
