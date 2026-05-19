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
import java.util.UUID;
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

        //Get cart list
        List<CartVO> cart = cartService.getCart(userId);

        //if cart is null or empty, throw exception
        if (cart == null || cart.isEmpty()) {

            throw new RuntimeException("Cart is empty");
        }

        //find default address of the user
        AddressBook address = addressBookRepository.findByUserIdAndIsDefault(userId, 1);

        //if address is null, find address of the user
        if (address == null) {
            List<AddressBook> addressList = addressBookRepository.findByUserId(userId);
            //if still empty, throw exception
            if (addressList.isEmpty()) throw new RuntimeException("Please add an address");
            //if there is address, get the 1st one
            address = addressList.get(0);
        }


        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> orderItemList = new ArrayList<>();

        //Loop through cartVO
        for (CartVO cartVO : cart) {

            //find product by productId
            Product product = productRepository.findById(cartVO.getProductId()).orElseThrow(() ->
                    new RuntimeException("Product " + cartVO.getProductId() + " not found"));

            //if cart quantity more than product stock, throws exception
            if (cartVO.getQuantity() > product.getStock()) {
                throw new RuntimeException("Product " + product.getProductName() + " is out of stock!");
            }

            //if not, set product stock by deducting cart quantity
            product.setStock(product.getStock() - cartVO.getQuantity());
            productRepository.save(product);

            //calculate subtotal for a single product
            BigDecimal subTotal = cartVO.getPrice().multiply(new BigDecimal(cartVO.getQuantity()));
            //calculate total amount for the cart
            totalAmount = totalAmount.add(subTotal);

            //create orderItem object
            OrderItem item = new OrderItem();
            item.setProductId(cartVO.getProductId());
            item.setProductName(product.getProductName());
            item.setProductImage(product.getImage());
            item.setQuantity(cartVO.getQuantity());
            item.setAmount(cartVO.getPrice());
            orderItemList.add(item);
        }

        //create order object
        Order order = new Order();
        order.setUserId(userId);
        order.setOrderNumber(UUID.randomUUID().toString().replace("-",""));
        order.setAmount(totalAmount);
        order.setStatus(Order.TO_BE_CONFIRMED);
        order.setOrderTime(LocalDateTime.now());
        order.setAddressBookId(address.getId());
        Order savedOrder = orderRepository.save(order);

        for (OrderItem orderItem : orderItemList) {
            orderItem.setOrderId(savedOrder.getId());
            orderItemRepository.save(orderItem);

        }

        //delay 15 minutes to let user finish payment
        rabbitTemplate.convertAndSend("order.delay.queue", savedOrder.getId());
        //clear cart
        cartService.clearCart(userId);
    }

    public List<OrderVO> listOrders(Long userId, GetOrderDto getOrderDto) {

        //Create pageable, set the page and the page size
        Pageable pageable = PageRequest.of(
                getOrderDto.getPage() - 1, //1st page is page 0
                getOrderDto.getPageSize()
        );
        //find orders by specific condition
        //return a Page<Order>, it's not only return the items and also how many pages there are
        //by status, by userId, byStartDate, byEndDate, by page or byNothing
        Page<Order> orderPage = orderRepository.findOrdersByFilters(
                userId,
                getOrderDto.getStatus(),
                getOrderDto.getStartDate(),
                getOrderDto.getEndDate(),
                pageable
        );
        //return OrderVO list
        return orderPage.getContent()//Page extract the Order list
                .stream()
                .map(this::convertToVO)//convert it into orderVO
                .collect(Collectors.toList());//collect it into a OrderVOList
    }

    private OrderVO convertToVO(Order order) {
        //Create OrderVO object
        OrderVO vo = new OrderVO();
        vo.setId(order.getId());
        vo.setAmount(order.getAmount());
        vo.setStatus(order.getStatus());
        vo.setOrderTime(order.getOrderTime());
        vo.setOrderNumber(order.getOrderNumber());

        //Find OrderItem with orderId
        List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());

        //if items is not null or not empty
        if (items != null && !items.isEmpty()) {
            OrderItem firstItem = items.get(0); //get the 1st item

            //find the product with the first item
            Product firstProduct = productRepository.findById(firstItem.getProductId()).orElseThrow(() ->
                    new RuntimeException("Product not found"));

                //set first item name and first item image to the VO
                vo.setFirstItemName(firstProduct.getProductName());
                vo.setFirstItemImage(firstProduct.getImage());
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
