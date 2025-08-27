package company.orderApp.controller;


import company.orderApp.controller.request.UpdateUserRequest;
import company.orderApp.controller.response.*;
import company.orderApp.domain.User;
import company.orderApp.repository.UserRepository;
import company.orderApp.service.UserService;
import company.orderApp.service.exception.NonExistentUserException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;


@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;




    @GetMapping("")
    public UserResponse user(@AuthenticationPrincipal User user){
        return new UserResponse(user);
    }

    @PutMapping("")
    public ResponseEntity<?> updateUser(@AuthenticationPrincipal User user, @RequestBody @Valid UpdateUserRequest request) {

        userService.update(user.getId(), request.getName(), request.getPhoneNumber(), request.getAddress());

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @DeleteMapping("{id}")
    public ResponseEntity<?> deleteUser(@AuthenticationPrincipal User user) {
        userService.withdrawMember(user.getId());

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("test")
    public ResponseEntity test() {
        return new ResponseEntity<>(HttpStatus.OK);
    }




}
