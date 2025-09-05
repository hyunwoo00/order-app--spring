package company.orderApp.controller;

import company.orderApp.controller.request.OrderRequest;
import company.orderApp.controller.request.OrderRequestByCart;
import company.orderApp.controller.response.OrderDto;
import company.orderApp.controller.response.ResultResponse;
import company.orderApp.discount.DiscountPolicy;
import company.orderApp.domain.Address;
import company.orderApp.domain.Cart;
import company.orderApp.domain.Delivery;
import company.orderApp.domain.User;
import company.orderApp.domain.item.Item;
import company.orderApp.domain.order.Order;
import company.orderApp.domain.order.OrderItem;
import company.orderApp.domain.order.Receipt;
import company.orderApp.repository.ItemRepository;
import company.orderApp.repository.OrderRepository;
import company.orderApp.repository.UserRepository;
import company.orderApp.service.CartService;
import company.orderApp.service.OrderService;
import company.orderApp.service.exception.NonExistentCartException;
import company.orderApp.service.exception.NonExistentItemException;
import company.orderApp.service.exception.NonExistentUserException;
import company.orderApp.controller.util.ReceiptFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final CartService cartService;
    private final UserRepository userRepository;
    private final ItemRepository itemRepository;
    private final DiscountPolicy discountPolicy;

    /**
     * 주문 내역 보기
     */

    @GetMapping("/details")
    public ResultResponse orderPage(@RequestParam(value = "offset", defaultValue = "0") int offset,
                                    @RequestParam(value = "limit", defaultValue = "100") int limit,
                                    @RequestParam(value = "id") long userId) {
        List<Order> orders = orderRepository.findRecentAllByUser(userId, offset, limit);
        List<OrderDto> result = orders.stream()
                .map(OrderDto::new)
                .toList();

        return new ResultResponse<>(result.size(), result);
    }


    /**
     * 장바구니를 통해 주문하기
     */
    @PostMapping("/cart")
    public ResponseEntity orderByCart(@RequestBody OrderRequestByCart orderRequest) {
        Long userId = orderRequest.getUserId();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NonExistentUserException("존재하지 않는 회원입니다."));

        Cart cart = cartService.findCurrentCart(user)
                .orElseThrow(() -> new NonExistentCartException("존재하지 않는 장바구니입니다."));

        Address address = new Address(orderRequest.getStoreName(), orderRequest.getRoadAddress(), orderRequest.getZoneCode(), orderRequest.getDetail());
        Delivery delivery = Delivery.createDelivery(address);

        Receipt receipt = ReceiptFactory.issueReceipt(orderRequest.getReceipt());

        orderService.orderByCart(userId, cart.getId(), delivery, orderRequest.getPhoneNumber(), receipt, orderRequest.getRequest());

        return new ResponseEntity(HttpStatus.OK);

    }

    /**
     * 바로 구매하기
     */
    @PostMapping("")
    public ResponseEntity order(@RequestBody OrderRequest orderRequest) {
        Long itemId = orderRequest.getItemId();
        int quantity = orderRequest.getQuantity();
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new NonExistentItemException("존재하지 않는 상품입니다."));

        Address address = new Address(orderRequest.getStoreName(), orderRequest.getRoadAddress(), orderRequest.getZoneCode(), orderRequest.getDetail());
        Delivery delivery = Delivery.createDelivery(address);

        Receipt receipt = ReceiptFactory.issueReceipt(orderRequest.getReceipt());



        OrderItem orderItem = OrderItem.createOrderItem(item, item.getPrice(), quantity, discountPolicy.discount(quantity, item.getMinimumQuantityForDiscount()));

        orderService.orderByItem(orderRequest.getUserId(), delivery, orderItem, orderRequest.getPhoneNumber(), receipt, orderRequest.getRequest());

        return new ResponseEntity(HttpStatus.OK);
    }




}
