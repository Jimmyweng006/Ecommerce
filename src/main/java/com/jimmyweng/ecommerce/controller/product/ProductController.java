package com.jimmyweng.ecommerce.controller.product;

import com.jimmyweng.ecommerce.config.OpenApiConfig;
import com.jimmyweng.ecommerce.controller.common.doc.EnvelopeErrorDoc;
import com.jimmyweng.ecommerce.controller.common.doc.ProductListEnvelopeDoc;
import com.jimmyweng.ecommerce.controller.common.doc.ProductResponseEnvelopeDoc;
import com.jimmyweng.ecommerce.controller.product.dto.ProductListResponse;
import com.jimmyweng.ecommerce.controller.product.dto.ProductResponse;
import com.jimmyweng.ecommerce.model.product.Product;
import com.jimmyweng.ecommerce.service.product.ProductQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductQueryService productQueryService;

    public ProductController(ProductQueryService productQueryService) {
        this.productQueryService = productQueryService;
    }

    @Operation(
            summary = "Browse public products with optional filtering",
            security = @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Products retrieved",
                content = @Content(schema = @Schema(implementation = ProductListEnvelopeDoc.class)))
    })
    @GetMapping
    public ProductListResponse listProducts(
            @Parameter(description = "Page number (0-indexed)", example = "0")
                    @RequestParam(defaultValue = "0")
                    @Min(0)
                    int page,
            @Parameter(description = "Page size", example = "20")
                    @RequestParam(defaultValue = "20")
                    @Min(1)
                    int size,
            @Parameter(description = "Filter by category") @RequestParam(required = false) String category,
            @Parameter(description = "Keyword search across title and description")
                    @RequestParam(name = "search", required = false)
                    String keyword) {
        Pageable pageable = PageRequest.of(page, size);
        Slice<Product> sliceResult = productQueryService.listProducts(category, keyword, pageable);

        return ProductListResponse.from(sliceResult);
    }

    @Operation(
            summary = "Retrieve a product by id",
            security = @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Product details",
                content = @Content(schema = @Schema(implementation = ProductResponseEnvelopeDoc.class))),
        @ApiResponse(responseCode = "404", description = "Product not found",
                content = @Content(schema = @Schema(implementation = EnvelopeErrorDoc.class)))
    })
    @GetMapping("/{productId}")
    public ProductResponse getProduct(@PathVariable Long productId) {
        return ProductResponse.from(productQueryService.getProduct(productId));
    }
}
