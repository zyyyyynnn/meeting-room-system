package com.example.mrs.stats;

import com.example.mrs.common.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "运营概览")
@RestController
@RequestMapping("/api/stats")
public class StatsController {
  private final StatsService statsService;

  public StatsController(StatsService statsService) {
    this.statsService = statsService;
  }

  @PreAuthorize("isAuthenticated()")
  @GetMapping("/overview")
  public ApiResponse<StatsDtos.OverviewResp> overview() {
    return ApiResponse.ok(statsService.overview());
  }

  @PreAuthorize("isAuthenticated()")
  @GetMapping("/dashboard")
  public ApiResponse<StatsDtos.DashboardResp> dashboard() {
    return ApiResponse.ok(statsService.dashboard());
  }
}
