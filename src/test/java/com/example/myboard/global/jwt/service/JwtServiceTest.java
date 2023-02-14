package com.example.myboard.global.jwt.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.example.myboard.domain.member.Member;
import com.example.myboard.domain.member.repository.MemberRepository;
import com.example.myboard.domain.member.role.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartHttpServletRequest;

import javax.persistence.EntityManager;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.Transactional;

import static com.auth0.jwt.algorithms.Algorithm.HMAC512;
import static jdk.nashorn.internal.runtime.regexp.joni.Config.log;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


@SpringBootTest
@Transactional
public class JwtServiceTest {

    @Autowired JwtService jwtService;

    @Autowired
    MemberRepository memberRepository;

    @Autowired
    EntityManager em;

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access.header}")
    private String accessHeader;

    @Value("${jwt.refresh.header}")
    private String refreshHeader;

    private static final String ACCESS_TOKEN_SUBJECT = "AccessToken";
    private static final String REFRESH_TOKEN_SUBJECT = "RefreshToken";
    private static final String USERNAME_CLAIM = "username";
    private static final String BEARER = "Bearer";

    private String username = "username";

    @BeforeEach
    public void init() {
        Member member = Member.builder().username(username).password("123456789").name("Member1").nickName("NickName1").role(Role.USER).age(22).build();
        memberRepository.save(member);
        clear();
    }

    private void clear() {
        em.flush();
        em.clear();
    }

    private DecodedJWT getVerify(String token) {
        return JWT.require(HMAC512(secret)).build().verify(token);
    }

    @Test
    public void createAccessToken_AccessToken_발급() throws Exception {
        // given, when
        String accessToken = jwtService.createAccessToken(username);

        DecodedJWT verify = getVerify(accessToken);

        String subject = verify.getSubject();
        String findUsername = verify.getClaim(USERNAME_CLAIM).asString();

        // then
        assertThat(findUsername).isEqualTo(username);
        assertThat(subject).isEqualTo(ACCESS_TOKEN_SUBJECT);
    }

    @Test
    public void createRefreshToken_RefreshToken_발급() throws Exception {
        // given, when
        String refreshToken = jwtService.createRefreshToken();
        DecodedJWT verify = getVerify(refreshToken);
        String subject = verify.getSubject();
        String username = verify.getClaim(USERNAME_CLAIM).asString();

        // then
        assertThat(subject).isEqualTo(REFRESH_TOKEN_SUBJECT);
        assertThat(username).isNotNull();
    }

    @Test
    public void updateRefreshToken_refreshToken_업데이트() throws Exception {
        // given
        String refreshToken = jwtService.createRefreshToken();
        jwtService.updateRefreshToken(username, refreshToken);
        clear();
        Thread.sleep(3000);

        // when
        String reissuedRefreshToken = jwtService.createRefreshToken();
        jwtService.updateRefreshToken(username, reissuedRefreshToken);

        // then
        assertThrows(Exception.class, () -> memberRepository.findByRefreshToken(refreshToken).get());
        assertThat(memberRepository.findByRefreshToken(reissuedRefreshToken).get().getUsername()).isEqualTo(username);
    }

    @Test
    public void destroyRefreshToken_refreshToken_제거() throws Exception {
        //given
        String refreshToken = jwtService.createRefreshToken();
        jwtService.updateRefreshToken(username, refreshToken);
        clear();

        // when
        jwtService.destroyRefreshToken(username);
        clear();

        // then
        // 1. 리프레쉬 토큰으로 검색했을때 exception 발생
        assertThrows(Exception.class, () -> memberRepository.findByUsername(username).get());
        // 2. 해당맴버의 refreshToken 이 null
        assertThat(memberRepository.findByUsername(username).get().getRefreshToken()).isNull();
    }

    //== AccessToken, RefreshToken 헤더 설정 테스트
    @Test
    public void setAccessTokenHeader_AccessToken_헤더_설정() throws Exception {
        // given
        MockHttpServletResponse mockHttpServletResponse = new MockHttpServletResponse();

        String accessToken = jwtService.createAccessToken(username);
        String refreshToken = jwtService.createRefreshToken();

        jwtService.setAccessTokenHeader(mockHttpServletResponse, accessToken);

        // when
        jwtService.sendAccessToken(mockHttpServletResponse, accessToken);

        // then
        String headerAccessToken = mockHttpServletResponse.getHeader(accessHeader);
        System.out.println("headerAccessToken: " + headerAccessToken);

        assertThat(headerAccessToken.replaceAll(BEARER, "")).isEqualTo(accessToken);
    }

    //== 토큰 전송 테스트 ==//
    @Test
    public void sendToken_토큰_전송() throws Exception {
        // given
        // 1. mockResponse 생성, 2. accessToken 생성, 3. refreshToken 생성
        MockHttpServletResponse mockHttpServletResponse = new MockHttpServletResponse();
        String accessToken = jwtService.createAccessToken(username);
        String refreshToken = jwtService.createRefreshToken();

        // when
        // 토큰을 전송
        jwtService.sendAccessToken(mockHttpServletResponse, accessToken);

        // then
        // 해더에서 access과 refreshToken을 가져온다.
        String headerAccesstoken = mockHttpServletResponse.getHeader(accessHeader);
        String headerRefreshToken = mockHttpServletResponse.getHeader(refreshHeader);


        // 생성한 accessToken과 refreshToken을 비교한다.
        assertThat(headerAccesstoken.replace(BEARER, "")).isEqualTo(accessToken);
        assertThat(headerRefreshToken.replace(BEARER, "")).isEqualTo(refreshToken);
    }

    //== request 헤더에 accessToken, refreshToken 을 셋팅. ==//
    private HttpServletRequest setRequest(String accessToken, String refreshToken) throws Exception {
        MockHttpServletResponse mockHttpServletResponse = new MockHttpServletResponse();
        jwtService.sendAccessAndRefreshToken(mockHttpServletResponse, accessToken, refreshToken);

        String headerAccessToken = mockHttpServletResponse.getHeader(accessHeader);
        String headerRefreshToken = mockHttpServletResponse.getHeader(refreshToken);

        MockHttpServletRequest httpServletRequest = new MockHttpServletRequest();

        httpServletRequest.addHeader(accessHeader, BEARER + headerAccessToken);
        httpServletRequest.addHeader(refreshHeader, BEARER + headerRefreshToken);

        return httpServletRequest;
    }

    @Test
    public void extractAccessToken_AccessToken_추출() throws Exception {
        // given
        // 1. access, refresh Token 생성, 2.request 헤더에 토큰 셋팅
        String accessToken = jwtService.createAccessToken(username);
        String refreshToken = jwtService.createRefreshToken();
        HttpServletRequest request = setRequest(accessToken, refreshToken);

        // when
        // 1. 토큰 추출
        String extractToken = jwtService.extractAccessToken(request).orElseGet(() -> new String(""));

        // then
        // 1. 생성한 토큰과 추출한 토큰을 비교, 2. 추출 토큰으로부터 username 을 추출하여 비교
        assertThat(extractToken.replace(BEARER, "")).isEqualTo(accessToken);
        assertThat(getVerify(extractToken).getClaim(USERNAME_CLAIM).asString()).isEqualTo(username);
    }

}
