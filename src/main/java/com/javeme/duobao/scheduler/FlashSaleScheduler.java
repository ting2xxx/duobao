package com.javeme.duobao.scheduler;

import com.javeme.duobao.entity.FlashSale;
import com.javeme.duobao.repository.FlashSaleRepository;
import com.javeme.duobao.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cglib.core.Local;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class FlashSaleScheduler {

    private final FlashSaleRepository flashSaleRepository;
    private final ProductRepository productRepository;
    private final StringRedisTemplate stringRedisTemplate;

    //This run automatically every 60 seconds
    @Scheduled(fixedRate = 60000)
    public void updateFlashSaleStatuses() {

        LocalDateTime now = LocalDateTime.now();

        //Find upcoming(0) sales where start time has passed, set to active (1)
        flashSaleRepository.updateToActive(now);

        //Find active(1) sales where end time has passed, set to expired (2)
        flashSaleRepository.updateToExpired(now);

    }

    @Scheduled(fixedRate = 5000)
    public void manageFlashSales() {

        //1. Find upcoming (0) sales where start time has passed
        //get the time now
        LocalDateTime now = LocalDateTime.now();
        //find flashsales by status 0 and time now
        List<FlashSale> startingSales = flashSaleRepository.findByStatusAndStartTimeLessThanEqual(0, now);
        //
        for (FlashSale sale : startingSales) {
            //find product by id
            productRepository.findById(sale.getProductId()).ifPresent(product -> {
                //set to redis order id and stock
                String key = "stock:product:" + product.getId();
                stringRedisTemplate.opsForValue().set(key, product.getStock().toString());
                log.info("🚀 Flash Sale Started! Pushed {} stock to Redis for Product {}", product.getStock(), product.getId());

                //set status to active
                sale.setStatus(1);
                flashSaleRepository.save(sale);
            });
        }

        // 2. Expire sale (1 to 2) & secure stock
        List<FlashSale> endingSales = flashSaleRepository.findByStatusAndEndTimeLessThanEqual(1, now);

        for (FlashSale sale : endingSales) {

            //set status to expired(2)
            sale.setStatus(2);
            flashSaleRepository.save(sale);

            String key = "stock:product:" + sale.getProductId();
            stringRedisTemplate.delete(key);
            log.info("🛑 Flash Sale Ended! Status set to 2 and Redis key deleted for Product {}", sale.getProductId());
        }
    }
}
