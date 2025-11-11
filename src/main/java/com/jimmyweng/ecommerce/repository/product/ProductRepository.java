package com.jimmyweng.ecommerce.repository.product;

import com.jimmyweng.ecommerce.model.product.Product;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findByIdAndDeletedAtIsNull(Long id);

    List<Product> findAllByIdInAndDeletedAtIsNull(Collection<Long> ids);

    @Modifying(clearAutomatically = true)
    @Query("update Product p set p.stock = p.stock - :quantity "
                    + "where p.id = :productId and p.deletedAt is null and p.stock >= :quantity")
    int decrementStock(@Param("productId") Long productId, @Param("quantity") int quantity);

    @Query("""
            select p from Product p
            where p.deletedAt is null
              and (:category is null or p.category = :category)
              and (:keyword is null or
                   lower(p.title) like lower(concat('%', :keyword, '%')) or
                   lower(coalesce(p.description, '')) like lower(concat('%', :keyword, '%')))
            order by p.createdAt desc
            """)
    Page<Product> searchActiveProductsLike(
            @Param("category") String category, @Param("keyword") String keyword, Pageable pageable);

    @Query(
            value = """
                    select p.*
                    from products p
                    where p.deleted_at is null
                      and (:category is null or p.category = :category)
                      and match(p.title, p.description) against (:keyword in natural language mode)
                    order by p.created_at desc
                    """,
            countQuery = """
                    select count(*) from products p
                    where p.deleted_at is null
                      and (:category is null or p.category = :category)
                      and match(p.title, p.description) against (:keyword in natural language mode)
                    """,
            nativeQuery = true)
    Page<Product> searchActiveProductsFullText(
            @Param("category") String category, @Param("keyword") String keyword, Pageable pageable);

    void deleteByTitleStartingWith(String titlePrefix);
}
