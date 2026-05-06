package com.javeme.duobao.service;

import com.javeme.duobao.common.BaseContext;
import com.javeme.duobao.dto.OrderDTO;
import com.javeme.duobao.entity.*;
import com.javeme.duobao.vo.OrderVO;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FlashSalesOrderService {

    private final StringRedisTemplate stringRedisTemplate;
    private final RabbitTemplate rabbitTemplate;

    private static final String STOCK_LUA =
            "local stock = tonumber(redis.call('get', KEYS[1]) or '0'); " +
            "local quantity = tonumber(ARGV[1]); " +
            "if (stock >= quantity) then " +
            "   redis.call('decrby', KEYS[1], quantity); " +
            "   return 1; " +
            "else " +
            "   return 0; " +
            "end; ";
    public OrderVO submitAsync(OrderDTO orderDTO) {
        // 1. Get current user and generate receipt number
        Long userId = BaseContext.getCurrentID();
        String orderNumber = UUID.randomUUID().toString();

        // 2. DEDUCT STOCK IN REDIS (The Bouncer)
        String stockKey = "stock:product:" + orderDTO.getProductId();
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptText(STOCK_LUA);
        redisScript.setResultType(Long.class);

        Long result = stringRedisTemplate.execute(
                redisScript,
                Collections.singletonList(stockKey),
                orderDTO.getQuantity().toString()
        );

        if (result == null || result == 0L) {
            throw new RuntimeException("this item is out of stock");
        }

        OrderMessage orderMessage = new OrderMessage();
        orderMessage.setOrderNumber(orderNumber);
        orderMessage.setUserId(userId);
        orderMessage.setProductId(orderDTO.getProductId());
        orderMessage.setQuantity(orderDTO.getQuantity());
        orderMessage.setAddressBookId(orderDTO.getAddressBookId());
        orderMessage.setRemark(orderDTO.getRemark());

        rabbitTemplate.convertAndSend("flash.sale.order.queue", orderMessage);
        return OrderVO.builder().orderNumber(orderNumber).status(Order.TO_BE_CONFIRMED).build();
    }
}
