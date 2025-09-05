package company.orderApp.service;


import company.orderApp.domain.Address;
import company.orderApp.domain.User;
import company.orderApp.jwt.JwtToken;
import company.orderApp.jwt.JwtTokenProvider;
import company.orderApp.repository.UserRepository;
import company.orderApp.service.exception.ExpiredRefreshTokenException;
import company.orderApp.service.exception.NonExistentRefreshTokenException;
import company.orderApp.service.exception.NonExistentUserException;
import company.orderApp.service.exception.RegisteredUserException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final JwtTokenProvider jwtTokenProvider;


    /**
     * 회원가입
     */
    @Transactional
    public Long join(User user) {

        //가입된 회원인지 체크
        if(checkingUser(user)){
            throw new RegisteredUserException("존재하는 회원입니다.");
        }

        //비밀번호 암호화해서 저장.
        String rawPassword = user.getPassword();
        String encodedPassword = passwordEncoder.encode(rawPassword);
        user.updatePassword(encodedPassword);

        userRepository.save(user);
        return user.getId();
    }
    /**
     * 가입한 아이디를 통한 중복 회원 체크
     * 가입O -> Exception
     * 가입X -> void
     */
    private boolean checkingUser(User user) {

        if(user == null) throw new NonExistentUserException("검증하려는 USER가 올바르지 않습니다.");


        return userRepository.findByUsername(user.getUsername())
                .isPresent();
    }

    /**
     * 로그인
     * @param username : 아이디
     * @param password
     * @return
     */
    @Transactional
    public JwtToken signIn(String username, String password) {
        //1. username + password를 기반으로 Authentication 객체 생성
        // 이 때 authentication 은 인증 여부를 확인하는 authenticated 값은 false
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(username, password);

        //2. 실제 검증, authentication() 메서드를 통해 요청된 User에 대한 검증 진행
        //authenticate 메서드가 실행될 때 CustomUserDetailService에서 만든 loadUserByUsername 메서드 실행
        //일치하면 Authentication 객체 반환, 불일치하면 예외 throw
        Authentication authentication = authenticationManagerBuilder
                .getObject()
                .authenticate(authToken);

        // AccessToken, RefreshToken 생성.
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NonExistentUserException("존재하지 않는 회원입니다"));

        JwtToken jwtToken = jwtTokenProvider.generateToken(user.getId(), authentication);


        return jwtToken;
    }

    /**
     * AccessToken 재발급.
     * @param id
     * @return
     */
    public String reIssueAccessToken(Long id, String refreshToken){


        //refreshToken의 유효할 경우 accessToken 재발급
        if(jwtTokenProvider.validateToken(refreshToken)) {

            User user = userRepository.findById(id)
                    .orElseThrow(() -> new NonExistentUserException("존재하지 않는 회원입니다"));

            // AccessToken에 저장되는 Role의 형식을 맞춰주기 위함.
            String authorities = user.getRoles().stream()
                    .map(e -> "ROLE_" + e)
                    .collect(Collectors.joining(","));

            return jwtTokenProvider.generateAccessToken(user.getId(), authorities);


        }
        //refreshToken이 유효하지 않은 경우 validateToken에서 에러를 던짐.
        else{
            throw new ExpiredRefreshTokenException("Refresh 토큰이 만료되었습니다.");
        }



    }

    /**
     * 로그아웃
     * 클라이언트에서 async Storage의 Token 값을 비우기 때문에 서버로 요청을 보내지 않음.
     */
    @Transactional
    public void signOut(String accessToken, String username) {

    }




    /**
     * 회원 탈퇴
     */
    @Transactional
    public void withdrawMember(Long userId) {

        userRepository.findById(userId).ifPresentOrElse(
                //존재하는 경우
                userRepository::delete,
                //존재하지 않는 경우
                () -> {
                    throw new NonExistentUserException("존재하지 않는 회원입니다.");
                }
        );
    }

    /**
     * 비밀번호 변경
     */
    @Transactional
    public void updatePassword(Long userId, String newPassword) {

        userRepository.findById(userId).ifPresentOrElse(
                //존재하는 경우
                user ->{
                  user.updatePassword(newPassword);
                },
                //존재하지 않는 경우
                () -> {
                    throw new NonExistentUserException("존재하지 않는 회원입니다.");
                }
        );

    }

    /**
     * 회원 정보 수정
     */
    @Transactional
    public void update(Long id, String name, String phoneNumber, Address address) {
        User user = findUserById(id).orElseThrow();
        user.setName(name);
        user.setPhoneNumber(phoneNumber);
        user.setAddress(address);
    }
    /**
     * 회원 찾기
     */
    public Optional<User> findUserById(Long userId) {
        return userRepository.findById(userId);
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    /**
     * username으로 회원Id 찾기
     */
    public Long findByUserName(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NonExistentUserException("존재하지 않는 회원입니다."));

        return user.getId();
    }

}
