package com.lumind.api.ai;

import com.lumind.api.ai.dto.request.ProductivityAnalysisRequest;
import com.lumind.api.ai.dto.response.ProductivityAnalysisResponse;
import com.lumind.api.ai.service.ProductivityAnalysisService;
import com.lumind.api.auth.model.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "AI", description = "AI-powered productivity analysis for the authenticated user")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/ai")
public class AiController {

    private final ProductivityAnalysisService productivityAnalysisService;

    public AiController(ProductivityAnalysisService productivityAnalysisService) {
        this.productivityAnalysisService = productivityAnalysisService;
    }

    @Operation(
            summary = "Generate productivity analysis",
            description = """
                    Generates an AI-assisted productivity analysis for the authenticated user in the \
                    requested period. Returns a narrative summary, insights and actionable recommendations \
                    derived from aggregated statistics. Depends on an external language model service."""
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Productivity analysis generated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid period or malformed request body"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid access token"),
            @ApiResponse(responseCode = "429", description = "AI analysis rate limit exceeded"),
            @ApiResponse(responseCode = "500", description = "Unexpected server error"),
            @ApiResponse(responseCode = "503", description = "AI analysis service unavailable or not configured"),
            @ApiResponse(responseCode = "504", description = "AI analysis request timed out")
    })
    @PostMapping("/productivity-analysis")
    public ResponseEntity<ProductivityAnalysisResponse> generateProductivityAnalysis(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody ProductivityAnalysisRequest request
    ) {
        return ResponseEntity.ok(
                productivityAnalysisService.analyze(authenticatedUser.id(), request)
        );
    }
}
