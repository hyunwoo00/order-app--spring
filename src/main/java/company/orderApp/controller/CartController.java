package company.orderApp.controller;


import company.orderApp.controller.request.CartRequest;
import company.orderApp.controller.response.CartDto;
import company.orderApp.discount.DiscountPolicy;
import company.orderApp.domain.Cart;
import company.orderApp.domain.User;
import company.orderApp.domain.item.Item;
import company.orderApp.domain.order.OrderItem;
import company.orderApp.repository.CartRepository;
import company.orderApp.repository.ItemRepository;
import company.orderApp.repository.UserRepository;
import company.orderApp.service.CartService;
import company.orderApp.service.exception.NonExistentItemException;
import company.orderApp.service.exception.NonExistentUserException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/carts")
public class CartController {

    private final ItemRepository itemRepository;
    private final CartService cartService;
    private final UserRepository userRepository;
    private final DiscountPolicy discountPolicy;

    /**
     * 장바구니 조회
     */
    @GetMapping("")
    public CartDto cart(@AuthenticationPrincipal User user) {
        Optional<Cart> optionalCart = cartService.findCurrentCart(user);

        if (optionalCart.isEmpty()) {
            return new CartDto();
        }
        else{
            Cart cart = optionalCart.orElseThrow();

            return new CartDto(cart);
        }

    }

    /**
     * 장바구니 담기
     */
    @PostMapping("")
    public ResponseEntity addItem(@RequestBody CartRequest cartRequest) {

        Long userId = cartRequest.getUserId();
        Long itemId = cartRequest.getItemId();
        int count = cartRequest.getCount();

        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new NonExistentItemException("존재하지 않는 상품입니다."));


        OrderItem orderedItem =
                OrderItem.createOrderItem(item, item.getPrice(), count, discountPolicy.discount(count, item.getMinimumQuantityForDiscount()));

        cartService.addItem(userId, orderedItem);

        return new ResponseEntity(HttpStatus.OK);
    }

    /**
     * 장바구니 상품 삭제
     */
    @DeleteMapping("")
    public ResponseEntity deleteItem(@AuthenticationPrincipal User user, @RequestBody CartRequest cartRequest) {
        cartService.removeItem(user.getId(), cartRequest.getItemId());

        return new ResponseEntity(HttpStatus.OK);
    }

    /**
     * 수량 변경
     */
    @PatchMapping("")
    public ResponseEntity modifyItem(@RequestBody CartRequest cartRequest){
        Long userId = cartRequest.getUserId();
        Long itemId = cartRequest.getItemId();
        int count = cartRequest.getCount();

        cartService.changeItemCount(userId, itemId, count);

        return new ResponseEntity(HttpStatus.OK);
    }

}
