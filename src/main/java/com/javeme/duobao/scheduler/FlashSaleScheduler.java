package com.javeme.duobao.scheduler;

import com.javeme.duobao.repository.FlashSaleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class FlashSaleScheduler {

    private static FlashSaleRepository flashSaleRepository;

    //This run automatically every 60 seconds
    @Scheduled(fixedRate = 60000)
    public void updateFlashSaleStatuses() {

        LocalDateTime now = LocalDateTime.now();

        //Find upcoming(0) sales where start time has passed, set to active (1)
        flashSaleRepository.updateToActive(now);

        //Find active(1) sales where end time has passed, set to expired (2)
        flashSaleRepository.updateToExpired(now);

    }
}
