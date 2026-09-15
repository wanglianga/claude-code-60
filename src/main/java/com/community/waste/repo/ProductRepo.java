package com.community.waste.repo;

import com.community.waste.model.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepo extends JpaRepository<Product, Long> {

    List<Product> findByActiveTrueOrderByCostPointsAsc();

    /** 行锁读取（SELECT ... FOR UPDATE）：串行化同一商品的库存与队列操作。 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdForUpdate(@Param("id") Long id);

    /** 条件扣减：库存充足才扣，返回 0 表示库存不足（原子，不超卖）。 */
    @Modifying(flushAutomatically = true)
    @Query("UPDATE Product p SET p.stock = p.stock - :qty WHERE p.id = :id AND p.stock >= :qty")
    int tryDecrementStock(@Param("id") Long id, @Param("qty") int qty);
}
