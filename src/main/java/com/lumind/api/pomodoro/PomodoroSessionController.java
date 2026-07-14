package com.lumind.api.pomodoro;

import com.lumind.api.auth.model.AuthenticatedUser;
import com.lumind.api.pomodoro.dto.request.CreatePomodoroSessionRequest;
import com.lumind.api.pomodoro.dto.request.UpdatePomodoroSessionRequest;
import com.lumind.api.pomodoro.dto.response.PomodoroSessionResponse;
import com.lumind.api.pomodoro.service.PomodoroSessionService;
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

@Tag(name = "Pomodoro Sessions", description = "CRUD operations for user pomodoro sessions")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/pomodoro-sessions")
public class PomodoroSessionController {

    private final PomodoroSessionService pomodoroSessionService;

    public PomodoroSessionController(PomodoroSessionService pomodoroSessionService) {
        this.pomodoroSessionService = pomodoroSessionService;
    }

    @Operation(summary = "List pomodoro sessions", description = "Returns all pomodoro sessions belonging to the authenticated user.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pomodoro sessions retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid access token"),
            @ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    @GetMapping
    public ResponseEntity<List<PomodoroSessionResponse>> getAll(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        return ResponseEntity.ok(pomodoroSessionService.getAllByUserId(authenticatedUser.id()));
    }

    @Operation(summary = "Get pomodoro session by ID", description = "Returns a single pomodoro session owned by the authenticated user.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pomodoro session retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid access token"),
            @ApiResponse(responseCode = "404", description = "Pomodoro session not found"),
            @ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    @GetMapping("/{id}")
    public ResponseEntity<PomodoroSessionResponse> getById(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(pomodoroSessionService.getById(authenticatedUser.id(), id));
    }

    @Operation(summary = "Create pomodoro session", description = "Creates a new pomodoro session for the authenticated user.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Pomodoro session created successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed or malformed request"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid access token"),
            @ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    @PostMapping
    public ResponseEntity<PomodoroSessionResponse> create(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody CreatePomodoroSessionRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(pomodoroSessionService.create(authenticatedUser.id(), request));
    }

    @Operation(summary = "Update pomodoro session", description = "Partially updates a pomodoro session owned by the authenticated user.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pomodoro session updated successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed or malformed request"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid access token"),
            @ApiResponse(responseCode = "404", description = "Pomodoro session not found"),
            @ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    @PatchMapping("/{id}")
    public ResponseEntity<PomodoroSessionResponse> update(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePomodoroSessionRequest request
    ) {
        return ResponseEntity.ok(pomodoroSessionService.update(authenticatedUser.id(), id, request));
    }

    @Operation(summary = "Delete pomodoro session", description = "Deletes a pomodoro session owned by the authenticated user.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Pomodoro session deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid access token"),
            @ApiResponse(responseCode = "404", description = "Pomodoro session not found"),
            @ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable UUID id
    ) {
        pomodoroSessionService.delete(authenticatedUser.id(), id);
        return ResponseEntity.noContent().build();
    }
}
