package com.javeme.duobao.service;

import com.javeme.duobao.common.BaseContext;
import com.javeme.duobao.dto.OrderDTO;
import com.javeme.duobao.entity.*;
import com.javeme.duobao.repository.*;
import com.javeme.duobao.vo.OrderVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class StandardOrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final AddressBookRepository addressBookRepository;
    private final UserRepository userRepository;
    private final StringRedisTemplate stringRedisTemplate;
    private final RabbitTemplate rabbitTemplate;

    @Transactional
    public OrderVO submitSync(OrderDTO orderDTO) {

        //1. Get current user and generate order number
        Long userId = BaseContext.getCurrentID();
        String orderNumber = UUID.randomUUID().toString();

        //Find user, product and address by id
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        Product product = productRepository.findById(orderDTO.getProductId()).orElseThrow(() -> new RuntimeException("Product not found"));
        AddressBook address = addressBookRepository.findById(orderDTO.getAddressBookId()).orElseThrow(() -> new RuntimeException("Address not found"));

        //check stock
        if (product.getStock() < orderDTO.getQuantity()) {
            throw new RuntimeException("Out of stock");
        }

        //count total amount
        BigDecimal totalAmount = product.getPrice().multiply(new BigDecimal(orderDTO.getQuantity()));

        //create order object
        Order order = new Order();
        order.setStatus(Order.TO_BE_CONFIRMED);
        order.setOrderNumber(orderNumber);
        order.setAddressBookId(orderDTO.getAddressBookId());
        order.setOrderTime(LocalDateTime.now());
        order.setPayStatus(Order.UN_PAID);
        order.setAmount(totalAmount);
        order.setRemark(orderDTO.getRemark());
        order.setUserId(userId);
        order.setUsername(user.getUsername());
        order.setPhone(user.getPhone());
        order.setAddress(address.getAddress());
        order.setPostcode(address.getPostcode());
        order.setConsignee(address.getConsignee());

        //save order
        Order savedOrder = orderRepository.save(order);
        productRepository.deductStock(product.getId(), orderDTO.getQuantity());

        //create order item
        OrderItem orderItem = new OrderItem();
        orderItem.setOrderId(savedOrder.getId());
        orderItem.setProductId(product.getId());
        orderItem.setProductName(product.getProductName());
        orderItem.setProductImage(product.getImage());
        orderItem.setQuantity(orderDTO.getQuantity());
        orderItem.setAmount(product.getPrice());

        OrderMessage orderMessage = new OrderMessage();
        orderMessage.setOrderNumber(orderNumber);
        orderMessage.setProductId(product.getId());
        orderMessage.setQuantity(orderDTO.getQuantity());
        //use rabbitMQ to wait for 15 minutes, so user can complete the payment
        rabbitTemplate.convertAndSend("order.delay.queue", orderMessage);
        //save orderItem
        orderItemRepository.save(orderItem);

        //return orderVO
        return OrderVO.builder()
                .orderNumber(orderNumber)
                .status(order.getStatus())
                .build();
    }
}
