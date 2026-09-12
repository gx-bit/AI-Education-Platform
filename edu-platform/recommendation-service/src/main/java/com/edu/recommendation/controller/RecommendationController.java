package com.edu.recommendation.controller;

import com.edu.common.core.result.Result;
import com.edu.common.security.context.UserContext;
import com.edu.recommendation.dto.*;
import com.edu.recommendation.service.RecommendationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import com.edu.common.core.exception.BusinessException;
import com.edu.common.core.result.ResultCode;
import java.util.Map;

@RestController
@RequestMapping("/api/recommendation")
@RequiredArgsConstructor
public class RecommendationController {
    private final RecommendationService service;

    @GetMapping("/courses")
    public Result<RecommendationResponse> recommend(@Valid RecommendationRequest request) {
        return Result.success(service.recommend(UserContext.getCurrentUserId(), request));
    }

    @PostMapping("/courses")
    public Result<RecommendationResponse> recommendPost(@Valid @RequestBody RecommendationRequest request) {
        return Result.success(service.recommend(UserContext.getCurrentUserId(), request));
    }

    @PostMapping("/behavior")
    public Result<Void> behavior(@Valid @RequestBody BehaviorRequest request) {
        service.recordBehavior(UserContext.getCurrentUserId(), request);
        return Result.success();
    }

    @GetMapping("/admin/config")
    public Result<Map<String, Object>> getConfig() { requireAdmin(); return Result.success(service.getConfig()); }

    @PutMapping("/admin/config")
    public Result<Void> updateConfig(@RequestBody Map<String, Integer> config) { requireAdmin(); service.updateConfig(config); return Result.success(); }

    private void requireAdmin() { if (!UserContext.isAdmin()) throw new BusinessException(ResultCode.FORBIDDEN); }
}
