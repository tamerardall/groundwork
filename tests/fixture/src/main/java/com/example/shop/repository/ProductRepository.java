package com.example.shop.repository;

import com.example.shop.domain.Product;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ProductRepository extends JpaRepository<Product, Long> {

    @Query("select count(p) from Product p where p.stock = 0")
    long countSoldOut();

    List<Product> findByStockLessThan(int threshold);
}
