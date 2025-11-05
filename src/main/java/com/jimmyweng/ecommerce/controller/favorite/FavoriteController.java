package com.jimmyweng.ecommerce.controller.favorite;

import com.jimmyweng.ecommerce.config.OpenApiConfig;
import com.jimmyweng.ecommerce.controller.common.doc.EnvelopeErrorDoc;
import com.jimmyweng.ecommerce.controller.common.doc.FavoriteListEnvelopeDoc;
import com.jimmyweng.ecommerce.controller.common.doc.ProductResponseEnvelopeDoc;
import com.jimmyweng.ecommerce.controller.favorite.dto.AddFavoriteRequest;
import com.jimmyweng.ecommerce.controller.product.dto.ProductResponse;
import com.jimmyweng.ecommerce.service.favorite.FavoriteService;
import com.jimmyweng.ecommerce.service.favorite.FavoriteService.AddFavoriteResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/favorites")
@PreAuthorize("hasRole('USER')")
public class FavoriteController {

    private final FavoriteService favoriteService;

    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @Operation(
            summary = "Add a product to the authenticated user's favorites",
            security = @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME))
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Product added to favorites",
                content = @Content(schema = @Schema(implementation = ProductResponseEnvelopeDoc.class))),
        @ApiResponse(responseCode = "200", description = "Product already in favorites",
                content = @Content(schema = @Schema(implementation = ProductResponseEnvelopeDoc.class))),
        @ApiResponse(responseCode = "400", description = "Validation failed",
                content = @Content(schema = @Schema(implementation = EnvelopeErrorDoc.class))),
        @ApiResponse(responseCode = "401", description = "Authentication required",
                content = @Content(schema = @Schema(implementation = EnvelopeErrorDoc.class))),
        @ApiResponse(responseCode = "404", description = "Product not found",
                content = @Content(schema = @Schema(implementation = EnvelopeErrorDoc.class)))
    })
    @PostMapping
    public ResponseEntity<ProductResponse> addFavorite(
            Principal principal, @Valid @RequestBody AddFavoriteRequest request) {
        AddFavoriteResult result = favoriteService.addFavorite(principal.getName(), request.productId());
        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;

        return ResponseEntity.status(status).body(ProductResponse.from(result.product()));
    }

    @Operation(
            summary = "List favorite products for the authenticated user",
            security = @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Favorites retrieved",
                content = @Content(schema = @Schema(implementation = FavoriteListEnvelopeDoc.class))),
        @ApiResponse(responseCode = "401", description = "Authentication required",
                content = @Content(schema = @Schema(implementation = EnvelopeErrorDoc.class)))
    })
    @GetMapping
    public ResponseEntity<List<ProductResponse>> listFavorites(Principal principal) {
        List<ProductResponse> favorites = favoriteService.listFavorites(principal.getName()).stream()
                .map(ProductResponse::from)
                .toList();

        return ResponseEntity.ok(favorites);
    }

    @Operation(
            summary = "Remove a product from favorites",
            security = @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME))
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Favorite removed"),
        @ApiResponse(responseCode = "401", description = "Authentication required",
                content = @Content(schema = @Schema(implementation = EnvelopeErrorDoc.class)))
    })
    @DeleteMapping("/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeFavorite(Principal principal, @PathVariable Long productId) {
        favoriteService.removeFavorite(principal.getName(), productId);
    }
}
