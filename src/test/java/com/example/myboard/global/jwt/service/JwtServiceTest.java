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
    public void createAccessToken_AccessToken_??????() throws Exception {
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
    public void createRefreshToken_RefreshToken_??????() throws Exception {
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
    public void updateRefreshToken_refreshToken_????????????() throws Exception {
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
    public void destroyRefreshToken_refreshToken_??????() throws Exception {
        //given
        String refreshToken = jwtService.createRefreshToken();
        jwtService.updateRefreshToken(username, refreshToken);
        clear();

        // when
        jwtService.destroyRefreshToken(username);
        clear();

        // then
        // 1. ???????????? ???????????? ??????????????? exception ??????
        assertThrows(Exception.class, () -> memberRepository.findByUsername(username).get());
        // 2. ??????????????? refreshToken ??? null
        assertThat(memberRepository.findByUsername(username).get().getRefreshToken()).isNull();
    }

    //== AccessToken, RefreshToken ?????? ?????? ?????????
    @Test
    public void setAccessTokenHeader_AccessToken_??????_??????() throws Exception {
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

    //== ?????? ?????? ????????? ==//
    @Test
    public void sendToken_??????_??????() throws Exception {
        // given
        // 1. mockResponse ??????, 2. accessToken ??????, 3. refreshToken ??????
        MockHttpServletResponse mockHttpServletResponse = new MockHttpServletResponse();
        String accessToken = jwtService.createAccessToken(username);
        String refreshToken = jwtService.createRefreshToken();

        // when
        // ????????? ??????
        jwtService.sendAccessToken(mockHttpServletResponse, accessToken);

        // then
        // ???????????? access??? refreshToken??? ????????????.
        String headerAccesstoken = mockHttpServletResponse.getHeader(accessHeader);
        String headerRefreshToken = mockHttpServletResponse.getHeader(refreshHeader);


        // ????????? accessToken??? refreshToken??? ????????????.
        assertThat(headerAccesstoken.replace(BEARER, "")).isEqualTo(accessToken);
        assertThat(headerRefreshToken.replace(BEARER, "")).isEqualTo(refreshToken);
    }

    //== request ????????? accessToken, refreshToken ??? ??????. ==//
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
    public void extractAccessToken_AccessToken_??????() throws Exception {
        // given
        // 1. access, refresh Token ??????, 2.request ????????? ?????? ??????
        String accessToken = jwtService.createAccessToken(username);
        String refreshToken = jwtService.createRefreshToken();
        HttpServletRequest request = setRequest(accessToken, refreshToken);

        // when
        // 1. ?????? ??????
        String extractToken = jwtService.extractAccessToken(request).orElseGet(() -> new String(""));

        // then
        // 1. ????????? ????????? ????????? ????????? ??????, 2. ?????? ?????????????????? username ??? ???????????? ??????
        assertThat(extractToken.replace(BEARER, "")).isEqualTo(accessToken);
        assertThat(getVerify(extractToken).getClaim(USERNAME_CLAIM).asString()).isEqualTo(username);
    }

}
