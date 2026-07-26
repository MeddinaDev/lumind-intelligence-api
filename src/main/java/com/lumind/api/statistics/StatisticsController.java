package com.lumind.api.statistics;

import com.lumind.api.auth.model.AuthenticatedUser;
import com.lumind.api.statistics.dto.request.StatisticsPeriodQuery;
import com.lumind.api.statistics.dto.response.HabitStatisticsResponse;
import com.lumind.api.statistics.dto.response.PomodoroStatisticsResponse;
import com.lumind.api.statistics.dto.response.ProductivityOverviewResponse;
import com.lumind.api.statistics.dto.response.TaskStatisticsResponse;
import com.lumind.api.statistics.service.ProductivityStatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Statistics", description = "Read-only productivity statistics for the authenticated user")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/statistics")
public class StatisticsController {

    private final ProductivityStatisticsService productivityStatisticsService;

    public StatisticsController(ProductivityStatisticsService productivityStatisticsService) {
        this.productivityStatisticsService = productivityStatisticsService;
    }

    @Operation(
            summary = "Get productivity overview",
            description = """
                    Returns consolidated productivity KPIs for tasks, pomodoro sessions and habits \
                    in the requested period. Daily trends are excluded; use the detail endpoints instead."""
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Overview retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid period or malformed query parameters"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid access token"),
            @ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    @GetMapping("/overview")
    public ResponseEntity<ProductivityOverviewResponse> getOverview(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @ParameterObject StatisticsPeriodQuery query
    ) {
        return ResponseEntity.ok(
                productivityStatisticsService.getOverview(authenticatedUser.id(), query)
        );
    }

    @Operation(
            summary = "Get task statistics",
            description = """
                    Returns task productivity metrics for the requested period, including daily completed \
                    task counts grouped by UTC date."""
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Task statistics retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid period or malformed query parameters"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid access token"),
            @ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    @GetMapping("/tasks")
    public ResponseEntity<TaskStatisticsResponse> getTaskStatistics(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @ParameterObject StatisticsPeriodQuery query
    ) {
        return ResponseEntity.ok(
                productivityStatisticsService.getTaskStatistics(authenticatedUser.id(), query)
        );
    }

    @Operation(
            summary = "Get pomodoro session statistics",
            description = """
                    Returns pomodoro session metrics for the requested period, including total focus minutes, \
                    completion rate and daily focus minutes grouped by UTC date."""
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pomodoro statistics retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid period or malformed query parameters"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid access token"),
            @ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    @GetMapping("/pomodoro-sessions")
    public ResponseEntity<PomodoroStatisticsResponse> getPomodoroStatistics(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @ParameterObject StatisticsPeriodQuery query
    ) {
        return ResponseEntity.ok(
                productivityStatisticsService.getPomodoroStatistics(authenticatedUser.id(), query)
        );
    }

    @Operation(
            summary = "Get habit statistics",
            description = """
                    Returns the current habit inventory and the number of habits created in the requested period. \
                    Habit completion metrics are not available in the current domain model."""
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Habit statistics retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid period or malformed query parameters"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid access token"),
            @ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    @GetMapping("/habits")
    public ResponseEntity<HabitStatisticsResponse> getHabitStatistics(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @ParameterObject StatisticsPeriodQuery query
    ) {
        return ResponseEntity.ok(
                productivityStatisticsService.getHabitStatistics(authenticatedUser.id(), query)
        );
    }
}
