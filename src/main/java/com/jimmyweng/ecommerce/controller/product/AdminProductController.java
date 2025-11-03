package com.jimmyweng.ecommerce.controller.product;

import com.jimmyweng.ecommerce.controller.product.dto.CreateProductRequest;
import com.jimmyweng.ecommerce.controller.product.dto.ProductResponse;
import com.jimmyweng.ecommerce.controller.product.dto.UpdateProductRequest;
import com.jimmyweng.ecommerce.model.product.Product;
import com.jimmyweng.ecommerce.service.product.AdminProductService;
import com.jimmyweng.ecommerce.service.product.dto.CreateProductCommand;
import com.jimmyweng.ecommerce.service.product.dto.UpdateProductCommand;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse createProduct(@Valid @RequestBody CreateProductRequest request) {
        Product product = adminProductService.createProduct(
                new CreateProductCommand(
                        request.title(),
                        request.description(),
                        request.category(),
                        request.price(),
                        request.stock()));
        return ProductResponse.from(product);
    }

    @PutMapping("/{productId}")
    public ProductResponse updateProduct(
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
        return ProductResponse.from(product);
    }

    @DeleteMapping("/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProduct(@PathVariable Long productId) {
        adminProductService.deleteProduct(productId);
    }
}
