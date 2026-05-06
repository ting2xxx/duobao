package com.javeme.duobao.service;

import com.javeme.duobao.entity.Order;
import com.javeme.duobao.entity.OrderItem;
import com.javeme.duobao.entity.Product;
import com.javeme.duobao.repository.OrderItemRepository;
import com.javeme.duobao.repository.OrderRepository;
import com.javeme.duobao.repository.ProductRepository;
import com.javeme.duobao.vo.CartVO;
import lombok.RequiredArgsConstructor;
import org.aspectj.weaver.ast.Or;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final CartService cartService;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

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

        cartService.clearCart(userId);
    }
}
