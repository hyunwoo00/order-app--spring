아키텍처
-------------------------------------
<img width="640" height="541" alt="image" src="https://github.com/user-attachments/assets/0a351ffb-2f4b-4c5f-b073-af7678e77051" />


Spring security + Jwt를 이용한 인증.
-------------------------------------
<img width="640" height="424" alt="Image" src="https://github.com/user-attachments/assets/ba96a793-deaf-451b-a26d-4b73c670042d" />

로그인하지 않은 사용자의 경우)
1. 클라이언트 "/auth/login" post 요청.
2. 로그인 정보를 통해 UsernamePasswordAuthenticationToken 생성.
3. AuthenticationManager가 UserDetailService를 상속받은 CustomUserDetailService 내 loadUserByUsername()를 호출해 로그인 정보와 DB에 저장된 정보를 비교.
4. 인증에 성공하면, jwt 토큰을 발급해 클라이언트에 전달.

로그인한 사용자의 경우)
1. securityConfig 설정에 따라 클라이언트의 요청이 있을 경우 JwtAuthenticationFilter를 실행함.(로그인 하지 않은 사용자도 해당 필터를 거치지만 헤더에 토큰이 없으므로 다음 필터체인으로 넘어감)
2. JwtAuthenticationFilter는 클라이언트가 보낸 헤더에 포함된 jwt 토큰을 꺼내 유효성 검사.
3. SecurityContextHolder내 SecurityContext에 사용자 authentication 저장.

-왜 SecurityContext에 저장해야 하는가?

SecurityContextHolder가 모든 인증/인가 판단의 기준

예를 들어, @AuthenticationPrincipal, @PreAuthorize, hasRole() 등은 SecurityContext에 등록된 Authentication을 참조함

필터 체인 이후에도 인증 정보 사용 가능

필터에서 토큰 검증만 하고 SecurityContext에 저장 안 하면

컨트롤러에서 로그인 사용자 정보를 못 받음 (@AuthenticationPrincipal null)

권한 기반 접근 제어 실패 (403 방어 불가)

멀티 필터 환경에서 통일성 유지

다른 SecurityFilter나 커스텀 필터가 SecurityContext에서 인증 정보를 참조함

API
-----
<img width="1103" height="304" alt="장바구니" src="https://github.com/user-attachments/assets/37995182-e5c1-4297-848a-63355c53ff4b" />
<img width="1096" height="248" alt="상품" src="https://github.com/user-attachments/assets/93f3f44f-b531-4988-ac6f-391ab0cc3bf2" />
<img width="1099" height="296" alt="주문" src="https://github.com/user-attachments/assets/e24dba86-0c24-442a-b0ac-280dedb1d376" />
<img width="1098" height="275" alt="사용자" src="https://github.com/user-attachments/assets/90d2b375-8bf2-4b3e-ba0f-88182194c41f" />
<img width="1388" height="422" alt="관리자" src="https://github.com/user-attachments/assets/67ffc8bb-84dc-4139-a584-01b33458a58e" />
<img width="1103" height="241" alt="공지사항" src="https://github.com/user-attachments/assets/01154e92-50cd-45bb-87de-2ed49b6b3958" />
<img width="1108" height="334" alt="인증" src="https://github.com/user-attachments/assets/d33bf949-d51a-42aa-989a-3f929a42dd1a" />
