package com.jimmyweng.ecommerce.controller.product;

import com.jimmyweng.ecommerce.config.OpenApiConfig;
import com.jimmyweng.ecommerce.controller.common.doc.EnvelopeErrorDoc;
import com.jimmyweng.ecommerce.controller.product.dto.CreateProductRequest;
import com.jimmyweng.ecommerce.controller.product.dto.ProductResponse;
import com.jimmyweng.ecommerce.controller.common.doc.ProductResponseEnvelopeDoc;
import com.jimmyweng.ecommerce.controller.product.dto.UpdateProductRequest;
import com.jimmyweng.ecommerce.model.product.Product;
import com.jimmyweng.ecommerce.service.product.AdminProductService;
import com.jimmyweng.ecommerce.service.product.dto.CreateProductCommand;
import com.jimmyweng.ecommerce.service.product.dto.UpdateProductCommand;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/admin/products")
@PreAuthorize("hasRole('ADMIN')")
public class AdminProductController {

    private final AdminProductService adminProductService;

    public AdminProductController(AdminProductService adminProductService) {
        this.adminProductService = adminProductService;
    }

    @Operation(
            summary = "Create a new product",
            security = @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME))
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Product created",
                content = @Content(schema = @Schema(implementation = ProductResponseEnvelopeDoc.class))),
        @ApiResponse(responseCode = "400", description = "Validation failed",
                content = @Content(schema = @Schema(implementation = EnvelopeErrorDoc.class))),
        @ApiResponse(responseCode = "401", description = "Authentication required",
                content = @Content(schema = @Schema(implementation = EnvelopeErrorDoc.class))),
        @ApiResponse(responseCode = "403", description = "Only admins may create products",
                content = @Content(schema = @Schema(implementation = EnvelopeErrorDoc.class)))
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody CreateProductRequest request) {
        Product product = adminProductService.createProduct(
                new CreateProductCommand(
                        request.title(),
                        request.description(),
                        request.category(),
                        request.price(),
                        request.stock()));
        return new ResponseEntity<>(ProductResponse.from(product), HttpStatus.CREATED);
    }

    @Operation(
            summary = "Update an existing product",
            security = @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Product updated",
                content = @Content(schema = @Schema(implementation = ProductResponseEnvelopeDoc.class))),
        @ApiResponse(responseCode = "400", description = "Validation failed",
                content = @Content(schema = @Schema(implementation = EnvelopeErrorDoc.class))),
        @ApiResponse(responseCode = "401", description = "Authentication required",
                content = @Content(schema = @Schema(implementation = EnvelopeErrorDoc.class))),
        @ApiResponse(responseCode = "403", description = "Only admins may update products",
                content = @Content(schema = @Schema(implementation = EnvelopeErrorDoc.class))),
        @ApiResponse(responseCode = "404", description = "Product not found",
                content = @Content(schema = @Schema(implementation = EnvelopeErrorDoc.class))),
        @ApiResponse(responseCode = "409", description = "Product version conflict",
                content = @Content(schema = @Schema(implementation = EnvelopeErrorDoc.class)))
    })
    @PutMapping("/{productId}")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable Long productId, @Valid @RequestBody UpdateProductRequest request) {
        Product product = adminProductService.updateProduct(
                productId,
                new UpdateProductCommand(
                        request.title(),
                        request.description(),
                        request.category(),
                        request.price(),
                        request.stock(),
                        request.version()));
        return ResponseEntity.ok(ProductResponse.from(product));
    }

    @Operation(
            summary = "Soft delete a product",
            security = @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME))
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Product deleted"),
        @ApiResponse(responseCode = "401", description = "Authentication required",
                content = @Content(schema = @Schema(implementation = EnvelopeErrorDoc.class))),
        @ApiResponse(responseCode = "403", description = "Only admins may delete products",
                content = @Content(schema = @Schema(implementation = EnvelopeErrorDoc.class))),
        @ApiResponse(responseCode = "404", description = "Product not found",
                content = @Content(schema = @Schema(implementation = EnvelopeErrorDoc.class)))
    })
    @DeleteMapping("/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProduct(@PathVariable Long productId) {
        adminProductService.deleteProduct(productId);
    }
}
