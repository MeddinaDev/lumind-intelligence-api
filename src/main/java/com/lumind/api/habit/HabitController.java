package com.lumind.api.habit;

import com.lumind.api.auth.model.AuthenticatedUser;
import com.lumind.api.habit.dto.request.CreateHabitRequest;
import com.lumind.api.habit.dto.request.UpdateHabitRequest;
import com.lumind.api.habit.dto.response.HabitResponse;
import com.lumind.api.habit.service.HabitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Habits", description = "CRUD operations for user habits")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/habits")
public class HabitController {

    private final HabitService habitService;

    public HabitController(HabitService habitService) {
        this.habitService = habitService;
    }

    @Operation(summary = "List habits", description = "Returns all habits belonging to the authenticated user.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Habits retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid access token"),
            @ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    @GetMapping
    public ResponseEntity<List<HabitResponse>> getAll(@AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return ResponseEntity.ok(habitService.getAllByUserId(authenticatedUser.id()));
    }

    @Operation(summary = "Get habit by ID", description = "Returns a single habit owned by the authenticated user.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Habit retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid access token"),
            @ApiResponse(responseCode = "404", description = "Habit not found"),
            @ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    @GetMapping("/{id}")
    public ResponseEntity<HabitResponse> getById(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(habitService.getById(authenticatedUser.id(), id));
    }

    @Operation(summary = "Create habit", description = "Creates a new habit for the authenticated user.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Habit created successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed or malformed request"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid access token"),
            @ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    @PostMapping
    public ResponseEntity<HabitResponse> create(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody CreateHabitRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(habitService.create(authenticatedUser.id(), request));
    }

    @Operation(summary = "Update habit", description = "Partially updates a habit owned by the authenticated user.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Habit updated successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed or malformed request"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid access token"),
            @ApiResponse(responseCode = "404", description = "Habit not found"),
            @ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    @PatchMapping("/{id}")
    public ResponseEntity<HabitResponse> update(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateHabitRequest request
    ) {
        return ResponseEntity.ok(habitService.update(authenticatedUser.id(), id, request));
    }

    @Operation(summary = "Delete habit", description = "Deletes a habit owned by the authenticated user.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Habit deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid access token"),
            @ApiResponse(responseCode = "404", description = "Habit not found"),
            @ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable UUID id
    ) {
        habitService.delete(authenticatedUser.id(), id);
        return ResponseEntity.noContent().build();
    }
}
