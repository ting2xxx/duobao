package com.javeme.duobao.scheduler;

import com.javeme.duobao.entity.Product;
import com.javeme.duobao.repository.OrderItemRepository;
import com.javeme.duobao.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisSyncTask {

    private final StringRedisTemplate stringRedisTemplate;
    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;

//    @Scheduled(cron = "0 0 * * * *")
//    public void syncFlashSaleStock() {
//
//        List<Product> productList = productRepository.findByIsFlashSale(true);
//        for (Product product : productList) {
//            if (!stringRedisTemplate.hasKey("stock:product:" + product.getId())){
//
//                stringRedisTemplate.opsForValue().set("stock:product:" + product.getId(), product.getStock().toString());
//                log.info("Redis Sync: Restored missing stock for Product ID {}", product.getId());
//
//            }
//        }
//    }

    @Scheduled(cron = "0 0/10 * * * *")
    public void syncTopSellingProducts() {

        List<Object[]> topSellingProductIds = orderItemRepository.findTopSellingProductIds();

        log.info("Starting Top Selling Products Sync...");
        String key = "product:top_selling";

        // Clear the old leaderboard
        stringRedisTemplate.delete(key);

        for (Object[] result : topSellingProductIds) {

            Number productId = (Number) result[0];
            Number totalSold = (Number) result[1];
            stringRedisTemplate.opsForZSet().add(key, productId.toString(), totalSold.doubleValue());
        }

        log.info("Finished Top Selling Products Sync.");
    }
}
