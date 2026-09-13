package com.community.waste.web;

import com.community.waste.exception.ApiException;
import com.community.waste.model.Product;
import com.community.waste.repo.ProductRepo;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductRepo productRepo;

    public ProductController(ProductRepo productRepo) {
        this.productRepo = productRepo;
    }

    @GetMapping
    public List<Product> list(@RequestParam(defaultValue = "false") boolean all) {
        return all ? productRepo.findAll() : productRepo.findByActiveTrueOrderByCostPointsAsc();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('PROPERTY','ADMIN')")
    public Product create(@RequestBody Product product) {
        return productRepo.save(product);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('PROPERTY','ADMIN')")
    public Product update(@PathVariable Long id, @RequestBody Product patch) {
        Product product = productRepo.findById(id).orElseThrow(() -> ApiException.notFound("商品不存在"));
        product.setName(patch.getName());
        product.setCategory(patch.getCategory());
        product.setCostPoints(patch.getCostPoints());
        product.setStock(patch.getStock());
        product.setPerUserMonthlyLimit(patch.getPerUserMonthlyLimit());
        product.setPerFamilyMonthlyLimit(patch.getPerFamilyMonthlyLimit());
        product.setValidFrom(patch.getValidFrom());
        product.setValidTo(patch.getValidTo());
        product.setActive(patch.isActive());
        return productRepo.save(product);
    }
}
