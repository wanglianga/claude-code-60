package com.community.waste.repo;

import com.community.waste.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepo extends JpaRepository<Product, Long> {
    List<Product> findByActiveTrueOrderByCostPointsAsc();
}
