package com.javeme.duobao.service;

import com.javeme.duobao.common.BaseContext;
import com.javeme.duobao.dto.GetOrderDto;
import com.javeme.duobao.entity.*;
import com.javeme.duobao.repository.*;
import com.javeme.duobao.vo.*;
import lombok.RequiredArgsConstructor;
import org.aspectj.weaver.ast.Or;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final CartService cartService;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final AddressBookRepository addressBookRepository;
    private final StringRedisTemplate stringRedisTemplate;
    private final UserRepository userRepository;
    private final RabbitTemplate rabbitTemplate;

    @Transactional
    public void checkout(Long userId) {

        List<CartVO> cart = cartService.getCart(userId);

        if (cart == null) {

            throw new RuntimeException("Cart is empty");
        }
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> orderItemList = new ArrayList<>();

        for (CartVO cartVO : cart) {

            Product product = productRepository.findById(cartVO.getProductId()).orElseThrow(() ->
                    new RuntimeException("Product " + cartVO.getProductId() + " not found"));

            if (cartVO.getQuantity() > product.getStock()) {

                throw new RuntimeException("Product " + product.getProductName() + " is out of stock!");
            }

            product.setStock(product.getStock() - cartVO.getQuantity());
            productRepository.save(product);

            BigDecimal subTotal = cartVO.getPrice().multiply(new BigDecimal(cartVO.getQuantity()));

            totalAmount = totalAmount.add(subTotal);

            OrderItem item = new OrderItem();
            item.setProductId(cartVO.getProductId());
            item.setQuantity(cartVO.getQuantity());
            item.setAmount(cartVO.getPrice());
            orderItemList.add(item);
        }

        Order order = new Order();
        order.setUserId(userId);
        order.setAmount(totalAmount);
        order.setStatus(Order.TO_BE_CONFIRMED);
        order.setOrderTime(LocalDateTime.now());
        Order savedOrder = orderRepository.save(order);

        for (OrderItem orderItem : orderItemList) {
            orderItem.setOrderId(savedOrder.getId());
            orderItemRepository.save(orderItem);

        }

        rabbitTemplate.convertAndSend("order.delay.queue", savedOrder.getId());
        cartService.clearCart(userId);
    }

    public List<OrderVO> listOrders(Long userId, GetOrderDto getOrderDto) {
        Pageable pageable = PageRequest.of(
                getOrderDto.getPage() - 1,
                getOrderDto.getPageSize()
        );

        Page<Order> orderPage = orderRepository.findOrdersByFilters(
                userId,
                getOrderDto.getStatus(),
                getOrderDto.getStartDate(),
                getOrderDto.getEndDate(),
                pageable
        );

        return orderPage.getContent()
                .stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    private OrderVO convertToVO(Order order) {

        OrderVO vo = new OrderVO();
        vo.setId(order.getId());
        vo.setAmount(order.getAmount());
        vo.setStatus(order.getStatus());
        vo.setOrderTime(order.getOrderTime());
        vo.setOrderNumber(order.getOrderNumber());

        List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());

        if (items != null && !items.isEmpty()) {
            OrderItem firstItem = items.get(0);

            Product firstProduct = productRepository.findById(firstItem.getProductId()).orElseThrow(() ->
                    new RuntimeException("Product not found"));

            if (firstProduct != null) {

                vo.setFirstItemName(firstProduct.getProductName());
                vo.setFirstItemImage(firstProduct.getImage());
            }
        }

        return vo;
    }

    public OrderDetailVO listOrderItem(Long userId, String orderNumber) {

        //Find order with userId and orderNumber
        Order order = orderRepository.findByOrderNumberAndUserId(orderNumber, userId).orElseThrow(() ->
                new RuntimeException("Order not found"));

        //Find addressBook by addressBookId
        AddressBook address = addressBookRepository.findById(order.getAddressBookId()).orElseThrow(() ->
                new RuntimeException("Address not found"));

        //Find user by userId
        User user = userRepository.findById(userId).orElseThrow(() ->
                new RuntimeException("User not found"));

        //Create an orderAddressVO object to store OrderAddress detail
        OrderAddressVO orderAddressVO = new OrderAddressVO();
        orderAddressVO.setDetailAddress(address.getAddress());
        orderAddressVO.setCity(address.getCity());
        orderAddressVO.setProvince(address.getProvince());
        orderAddressVO.setReceiverName(user.getUsername());
        orderAddressVO.setReceiverPhone(user.getPhone());

        //Find OrderItem by orderId
        List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());

        //use stream to convert OrderItem to OrderItemVO
        List<OrderItemVO> itemVOList = items.stream()
                .map(item -> {
                            OrderItemVO orderItemVO = new OrderItemVO();
                            orderItemVO.setProductId(item.getProductId());
                            orderItemVO.setProductName(item.getProductName());
                            orderItemVO.setProductImage(item.getProductImage());
                            orderItemVO.setQuantity(item.getQuantity());
                            orderItemVO.setAmount(item.getAmount());
                            return orderItemVO;
                        }).toList();

        //Create OrderDetailVO to store order detail
        OrderDetailVO orderDetailVO = new OrderDetailVO();
        orderDetailVO.setOrderNumber(orderNumber);
        orderDetailVO.setStatus(order.getStatus());
        orderDetailVO.setTotalAmount(order.getAmount());
        orderDetailVO.setCreateTime(order.getOrderTime());
        orderDetailVO.setPayTime(order.getPayTime());
        orderDetailVO.setShippingAddress(orderAddressVO);
        orderDetailVO.setItems(itemVOList);

        return orderDetailVO;
    }

    @Transactional
    public void cancelOrder(Long userId, String orderNumber) {

        //find Order by orderNumber and userId
        Order order = orderRepository.findByOrderNumberAndUserId(orderNumber, userId).orElseThrow(() ->
                new RuntimeException("Order not found"));

        //check the order status that is not to be confirmed
        if (!order.getStatus().equals(Order.TO_BE_CONFIRMED)) {
            throw new RuntimeException("Order cannot be cancelled in its current status");
        }

        //If current status is to be confirmed, update status
        order.setStatus(Order.CANCELLED);
        order.setCancelTime(LocalDateTime.now());
        orderRepository.save(order);

        //Find order item with orderId to return the holding stock
        List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());

        for (OrderItem item : items) {

            Product product = productRepository.findById(item.getProductId()).get();
            product.setStock(product.getStock() + item.getQuantity());
            productRepository.save(product);

            String redisKey = "flash_sale:stock:" + item.getProductId();
            stringRedisTemplate.opsForValue().increment(redisKey, item.getQuantity());
        }
    }
}
