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

    //when the script runs, redis locks its door. It checks the stock, deduct the stock
    //and returns the result in one single
    //1 is 1st item in lua script
    private static final String STOCK_LUA =
            //get the value out from the redis key, if null, then put 0
            "local stock = tonumber(redis.call('get', KEYS[1]) or '0'); " +
            "local quantity = tonumber(ARGV[1]); " + //argument which is from dto, here is order quantity
            "if (stock >= quantity) then " + //the stock >= quantity
            "   redis.call('decrby', KEYS[1], quantity); " + // redis key decrease the value based on quantity
            "   return 1; " + //return 1 which mean deduct successful
            "else " +
            "   return 0; " + //return 0 which mean deduct unsuccessful
            "end; ";


    public OrderVO submitAsync(OrderDTO orderDTO) {
        // 1. Get current user and generate receipt number
        Long userId = BaseContext.getCurrentID();
        String orderNumber = UUID.randomUUID().toString();

        // 2. DEDUCT STOCK IN REDIS (The Bouncer)
        String stockKey = "stock:product:" + orderDTO.getProductId();
        //create redis script to set script and return clas
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptText(STOCK_LUA);
        redisScript.setResultType(Long.class);

        Long result = stringRedisTemplate.execute(
                redisScript, //put the lua script and set the result type
                Collections.singletonList(stockKey), //put the redis key here that we published
                orderDTO.getQuantity().toString() //order quantity by user
        );

        //if the result is null or 0, throw exception
        if (result == null || result == 0L) {
            throw new RuntimeException("this item is out of stock");
        }
        //create Order Message, and then drop it into rabbitMQ queue
        OrderMessage orderMessage = new OrderMessage();
        orderMessage.setOrderNumber(orderNumber);
        orderMessage.setUserId(userId);
        orderMessage.setProductId(orderDTO.getProductId());
        orderMessage.setQuantity(orderDTO.getQuantity());
        orderMessage.setAddressBookId(orderDTO.getAddressBookId());
        orderMessage.setRemark(orderDTO.getRemark());

        //RabbitMQ will process the task to slowly save this order into the MySQL database in the background
        rabbitTemplate.convertAndSend("flash.sale.order.queue", orderMessage);
        //return orderVO
        return OrderVO.builder().orderNumber(orderNumber).status(Order.TO_BE_CONFIRMED).build();
    }
}
