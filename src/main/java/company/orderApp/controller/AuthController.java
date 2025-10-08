package company.orderApp.controller;

import company.orderApp.controller.request.LoginRequest;
import company.orderApp.controller.request.SignUpRequest;
import company.orderApp.controller.response.LoginResponse;
import company.orderApp.controller.response.RefreshResponse;
import company.orderApp.domain.User;
import company.orderApp.jwt.JwtToken;
import company.orderApp.jwt.JwtTokenProvider;
import company.orderApp.service.UserService;
import company.orderApp.service.exception.ExpiredRefreshTokenException;
import company.orderApp.service.exception.NonExistentUserException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import static company.orderApp.controller.request.SignUpRequest.toEntity;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("sign-up")
    public ResponseEntity<?> signUp(@RequestBody @Valid SignUpRequest signUpRequest) {

        userService.join(toEntity(signUpRequest));

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody @Valid LoginRequest loginRequest) {

        log.info("username = " + loginRequest.getUsername() + ", password = " + loginRequest.getPassword());
        JwtToken jwtToken = userService.signIn(loginRequest.getUsername(), loginRequest.getPassword());

        return new LoginResponse(jwtToken.getAccessToken(),jwtToken.getRefreshToken(),userService.findByUserName(loginRequest.getUsername()));
    }

    /**
     * 클라이언트 asyncStorage에서 token을 없애기 때문에 서버로 요청을 보내지 않아도 됨.
     */
    @DeleteMapping("/logout")
    public ResponseEntity logout() {

        return new ResponseEntity(HttpStatus.OK);
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(HttpServletRequest request) {

        String accessToken = resolveToken(request);
        String refreshToken = resolveRefreshToken(request);
        Long userId = resolveUserId(request);

        User user = userService.findUserById(userId)
                .orElseThrow(() -> new NonExistentUserException("존재하지 않는 회원입니다."));


        //accessToken 만료
        if(!jwtTokenProvider.validateToken(accessToken)){
            try {
                String newAccessToken = userService.reIssueAccessToken(userId, refreshToken);
                RefreshResponse refreshResponse = new RefreshResponse();
                refreshResponse.setAccessToken(newAccessToken);

                return new ResponseEntity<>(refreshResponse, HttpStatus.OK); //200
            }
            //refreshToken 만료
            catch(ExpiredRefreshTokenException e){
                return new ResponseEntity<>(HttpStatus.UNAUTHORIZED); //401
            }
        }
        //accessToken 만료X
        else {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST); //400
        }
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    private String resolveRefreshToken(HttpServletRequest request) {
        return request.getHeader("refresh");
    }

    private Long resolveUserId(HttpServletRequest request) {
        return Long.parseLong(request.getHeader("userId"));
    }
}
