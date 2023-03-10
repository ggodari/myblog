package com.example.myboard.global.login.filter;

import com.example.myboard.domain.member.Member;
import com.example.myboard.domain.member.repository.MemberRepository;
import com.example.myboard.domain.member.role.Role;
import com.example.myboard.global.jwt.service.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class JwtAuthenticationProcessingFilterTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    MemberRepository memberRepository;

    @Autowired
    EntityManager em;

    @Autowired
    JwtService jwtService;

    PasswordEncoder delegatingPasswordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access.header}")
    private String accessHeader;

    @Value("${jwt.refresh.header}")
    private String refreshHeader;

    private static String KEY_USERNAME = "username";
    private static String KEY_PASSWORD = "password";
    private static String USERNAME = "username";
    private static String PASSWORD = "1234567";

    private static String LOGIN_RUL = "/login";

    private static final String ACCESS_TOKEN_SUBJECT = "AccessToken";
    private static final String BEARER = "Bearer";

    private ObjectMapper objectMapper = new ObjectMapper();

    private void clear() {
        em.flush();
        em.clear();
    }

    @BeforeEach
    private void init() {
        memberRepository.save(Member.builder().username(USERNAME).password(delegatingPasswordEncoder.encode(PASSWORD)).name("Member1").nickName("NickName1").role(Role.USER).age(22).build());
        clear();
    }

    private Map getUsernamePasswordMap(String username, String password) {
        Map<String, String> map = new HashMap<>();
        map.put(KEY_USERNAME, username);
        map.put(KEY_PASSWORD, password);

        return map;
    }

    private Map getAccessAndRefreshToken() throws Exception {
        Map<String, String> map = getUsernamePasswordMap(USERNAME, PASSWORD);

        MvcResult result = mockMvc.perform(
                post(LOGIN_RUL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(map)))
                .andReturn();

        String accessToken = result.getResponse().getHeader(accessHeader);
        String refreshToken = result.getResponse().getHeader(refreshHeader);

        Map<String, String> tokenMap = new HashMap<>();
        tokenMap.put(accessHeader, accessToken);
        tokenMap.put(refreshHeader, refreshToken);

        return tokenMap;
    }

    /**
     * AccessToken : ???????????? ??????
     * ReFreshToken : ???????????? ??????
     */
    @Test
    public void Access_Refresh_??????_??????_X() throws Exception {
        // when, then
        mockMvc.perform(get(LOGIN_RUL + "123"))
                .andExpect(status().isForbidden());
    }

    /**
     * AccessToken : ??????
     * RefreshToken : ???????????? ??????
     */
    @Test
    public void AccessToken???_?????????_??????() throws Exception {
        // given
        Map accessAndRefreshToken = getAccessAndRefreshToken();
        String accessToken = (String) accessAndRefreshToken.get(accessHeader);

        // when, then
        mockMvc.perform(get(LOGIN_RUL + "123").header(accessHeader, BEARER + accessToken))  //login ??? ?????? ?????? ????????? URL
                .andExpectAll(status().isNotFound());       // ?????? ????????? ??????????????? NotFound? ?????? ???????????? ??????!
    }

}