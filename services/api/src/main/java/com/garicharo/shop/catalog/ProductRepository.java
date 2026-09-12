package com.garicharo.shop.catalog;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import java.util.Collection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;

public interface ProductRepository extends JpaRepository<Product, UUID> {

  Optional<Product> findBySku(String sku);

  @Query("""
      SELECT p FROM Product p
      WHERE (:category IS NULL OR p.category = :category)
        AND (
          :q IS NULL
          OR LOWER(p.name) LIKE :q
          OR LOWER(p.description) LIKE :q
          OR LOWER(p.category) LIKE :q
          OR LOWER(p.sku) LIKE :q
        )
      """)
  Page<Product> search(@Param("q") String q, @Param("category") String category, Pageable pageable);

  @Query("SELECT DISTINCT p.category FROM Product p ORDER BY p.category ASC")
  List<String> findDistinctCategories();

  @Query(value = "SELECT EXISTS (SELECT 1 FROM order_items WHERE product_id = :productId)", nativeQuery = true)
  boolean existsInOrderItems(@Param("productId") UUID productId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT p FROM Product p WHERE p.sku IN :skus")
  List<Product> findAllBySkuInForUpdate(@Param("skus") Collection<String> skus);
}
