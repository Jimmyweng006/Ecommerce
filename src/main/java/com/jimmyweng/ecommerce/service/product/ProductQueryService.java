package com.jimmyweng.ecommerce.service.product;

import com.jimmyweng.ecommerce.constant.ErrorMessages;
import com.jimmyweng.ecommerce.exception.ResourceNotFoundException;
import com.jimmyweng.ecommerce.model.product.Product;
import com.jimmyweng.ecommerce.repository.product.ProductRepository;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional(readOnly = true)
public class ProductQueryService {

    private static final Logger log = LoggerFactory.getLogger(ProductQueryService.class);

    private final ProductRepository productRepository;
    private final boolean fullTextEnabled;
    private final int fullTextMinLength;

    public ProductQueryService(
            ProductRepository productRepository,
            @Value("${feature.fulltext.enabled:true}") boolean fullTextEnabled,
            @Value("${feature.fulltext.min-length:3}") int fullTextMinLength) {
        this.productRepository = productRepository;
        this.fullTextEnabled = fullTextEnabled;
        this.fullTextMinLength = fullTextMinLength;
    }

    public Slice<Product> listProducts(String category, String keyword, Pageable pageable) {
        String normalizedCategory = StringUtils.hasText(category) ? category.trim().toLowerCase(Locale.ROOT) : null;
        String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;

        if (log.isDebugEnabled()) {
            log.debug(
                    "listProducts invoked (category={}, keyword={}, readOnlyTx={})",
                    normalizedCategory,
                    normalizedKeyword,
                    org.springframework.transaction.support.TransactionSynchronizationManager.isCurrentTransactionReadOnly());
        }

        boolean useFullText = fullTextEnabled
                && StringUtils.hasText(normalizedKeyword)
                && normalizedKeyword.length() >= fullTextMinLength;
        if (useFullText) {
            return productRepository.searchActiveProductsFullText(normalizedCategory, normalizedKeyword, pageable);
        }

        return productRepository.searchActiveProductsLike(normalizedCategory, normalizedKeyword, pageable);
    }

    public Product getProduct(Long productId) {
        return productRepository
                .findByIdAndDeletedAtIsNull(productId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.productNotFound(productId)));
    }
}
