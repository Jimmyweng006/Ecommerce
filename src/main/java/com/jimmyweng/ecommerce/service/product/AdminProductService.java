package com.jimmyweng.ecommerce.service.product;

import com.jimmyweng.ecommerce.constant.ErrorMessages;
import com.jimmyweng.ecommerce.exception.ResourceNotFoundException;
import com.jimmyweng.ecommerce.model.product.Product;
import com.jimmyweng.ecommerce.repository.product.ProductRepository;
import com.jimmyweng.ecommerce.service.product.dto.CreateProductCommand;
import com.jimmyweng.ecommerce.service.product.dto.UpdateProductCommand;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminProductService {

    private final ProductRepository productRepository;
    private final Clock clock;

    public AdminProductService(ProductRepository productRepository, Clock clock) {
        this.productRepository = productRepository;
        this.clock = clock;
    }

    @Transactional
    public Product createProduct(CreateProductCommand command) {
        Product product =
                new Product(command.title(), command.description(), command.category(), command.price(), command.stock());
        return productRepository.saveAndFlush(product);
    }

    @Transactional
    public Product updateProduct(Long productId, UpdateProductCommand command) {
        Product product = loadActiveProduct(productId);
        if (!Objects.equals(product.getVersion(), command.version())) {
            throw new ObjectOptimisticLockingFailureException(Product.class, productId);
        }

        product.applyUpdate(command.title(), command.description(), command.category(), command.price(), command.stock());
        return productRepository.saveAndFlush(product);
    }

    @Transactional
    public void deleteProduct(Long productId) {
        Product product = loadActiveProduct(productId);
        product.markDeleted(Instant.now(clock));
        productRepository.saveAndFlush(product);
    }

    private Product loadActiveProduct(Long productId) {
        return productRepository
                .findByIdAndDeletedAtIsNull(productId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.productNotFound(productId)));
    }
}
