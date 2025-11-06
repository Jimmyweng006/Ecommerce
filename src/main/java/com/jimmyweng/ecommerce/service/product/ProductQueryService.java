package com.jimmyweng.ecommerce.service.product;

import com.jimmyweng.ecommerce.constant.ErrorMessages;
import com.jimmyweng.ecommerce.exception.ResourceNotFoundException;
import com.jimmyweng.ecommerce.model.product.Product;
import com.jimmyweng.ecommerce.repository.product.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional(readOnly = true)
public class ProductQueryService {

    private final ProductRepository productRepository;

    public ProductQueryService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public Page<Product> listProducts(String category, String keyword, Pageable pageable) {
        String normalizedCategory = StringUtils.hasText(category) ? category.trim() : null;
        String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;
        return productRepository.searchActiveProducts(normalizedCategory, normalizedKeyword, pageable);
    }

    public Product getProduct(Long productId) {
        return productRepository
                .findByIdAndDeletedAtIsNull(productId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.productNotFound(productId)));
    }
}
