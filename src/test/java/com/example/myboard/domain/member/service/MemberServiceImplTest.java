package com.example.myboard.domain.member.service;

import com.example.myboard.domain.member.Member;
import com.example.myboard.domain.member.dto.MemberInfoDto;
import com.example.myboard.domain.member.dto.MemberSignUpDto;
import com.example.myboard.domain.member.dto.MemberUpdateDto;
import com.example.myboard.domain.member.repository.MemberRepository;
import com.example.myboard.domain.member.role.Role;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class MemberServiceImplTest {

    @Autowired
    EntityManager em;

    @Autowired
    MemberRepository memberRepository;

    @Autowired
    MemberService memberService;

    @Autowired
    PasswordEncoder passwordEncoder;

    String PASSWORD = "password";

    private void clear() {
        em.flush();
        em.clear();
    }

    private MemberSignUpDto makeMemberSignUpDto() {
        return new MemberSignUpDto("username", PASSWORD, "name", "nickName", 22);
    }

    private MemberSignUpDto setMember() throws Exception {
        MemberSignUpDto memberSignUpDto = makeMemberSignUpDto();
        memberService.signUp(memberSignUpDto);
        clear();
        SecurityContext emptyContext = SecurityContextHolder.createEmptyContext();

        emptyContext.setAuthentication(new UsernamePasswordAuthenticationToken(
                User.builder()
                        .username(memberSignUpDto.getUsername())
                        .password(memberSignUpDto.getPassword())
                        .roles(Role.USER.name())
                        .build(),
                null, null));

        SecurityContextHolder.setContext(emptyContext);

        return memberSignUpDto;
    }


    @AfterEach
    public void removeMember() {
        SecurityContextHolder.createEmptyContext().setAuthentication(null);
    }

    /**
     * 회원가입
     * 회원가입 시 아이디, 비밀번호, 이름, 별명, 나이를 입력하지 않으면 오류
     * 이미 존재하는 아이디가 있으면 오류
     * 회원가입 후 회원의 ROLE 은 USER
     */

    @Test
    public void 회원가입_성공() throws Exception {
        //given
        MemberSignUpDto memberSignUpDto = makeMemberSignUpDto();

        //when
        memberService.signUp(memberSignUpDto);
        clear();

        //then TODO: 여기 MEMBEREXCEPTION으로 고치기
        Member member = memberRepository.findByUsername(memberSignUpDto.getUsername()).orElseThrow(() -> new Exception("회원 정보가 존재하지 않습니다."));
        assertThat(member.getId()).isNotNull();
        assertThat(member.getUsername()).isEqualTo(memberSignUpDto.getUsername());
        // 이름, 별명, 나이 등등 비교
    }

    @Test
    public void 회원가입_실패_원인_아이디중복() throws Exception {
        // given
        MemberSignUpDto memberSignUpDto = makeMemberSignUpDto();
        memberService.signUp(memberSignUpDto);
        clear();

        // when, then TODO : MEMBEREXCEPTION 으로 고쳐야 함
        assertThat(assertThrows(Exception.class, () -> memberService.signUp(memberSignUpDto)).getMessage()).isEqualTo("이미 존재하는 아이디입니다.");
    }

    @Test
    public void 회원가입_실패_입력하지않은_필드가있으면_오류() throws Exception {
        // given
        MemberSignUpDto memberSignUpDto1 = new MemberSignUpDto(null, passwordEncoder.encode(PASSWORD), "name", "nickName", 22);

        // when, then
        assertThrows(Exception.class, () -> memberService.signUp(memberSignUpDto1));
        // 비밀번호, 이름, 별명, 나이 등등 입력하지 않은 필드 체크..
    }

    /**
     * 회원정보수정
     * 회원가입을 하지 않은 사람이 정보수정 시 오류 -> 시큐리티 필터가 알아서 막아줄거임
     * 아이디는 변경 불가
     * 비밀번호 변경시에는, 현재 비밀번호를 입력받아서, 일치한 경우에만 바꿀 수 있음.
     * 비밀번호 변경시에는 오직 비밀번호만 바꿀 수 있음.
     * <p>
     * 비밀번호가 아닌 이름, 별명, 나이 변경시에는, 3개를 한꺼번에 바꿀 수도 있도, 한, 두개만 선택해서 바꿀 수도 있음.
     * 아무것도 바뀌는게 없는데 변경요청을 보내면 오류
     */

    @Test
    public void 회원수정_비밀번호수정_성공() throws Exception {
        //given
        MemberSignUpDto memberSignUpDto = setMember();

        //when
        String toBePassword = "12345678899!@#!##";
        memberService.updatePassword(PASSWORD, toBePassword);
        clear();

        //then
        Member findMember = memberRepository.findByUsername(memberSignUpDto.getUsername()).orElseThrow(() -> new Exception());
        assertThat(findMember.matchPassword(passwordEncoder, toBePassword)).isTrue();
    }

    @Test
    public void 회원수정_이름만수정() throws Exception {
        //given
        MemberSignUpDto memberSignUpDto = setMember();

        //when
        String updateName = "변경할래요";
        memberService.update(new MemberUpdateDto(Optional.ofNullable(updateName), Optional.empty(), Optional.empty()));
        clear();

        //then
        memberRepository.findByUsername(memberSignUpDto.getUsername()).ifPresent(member -> {
            assertThat(member.getName()).isEqualTo(updateName);
            assertThat(member.getAge()).isEqualTo(memberSignUpDto.getAge());
            assertThat(member.getNickName()).isEqualTo(memberSignUpDto.getNickName());
        });
    }

    /**
     * 회원탈퇴
     * 비밀번호를 입력받아서 일치하면 탈퇴 가능
     */
    @Test
    public void 회원탈퇴() throws Exception {
        //given
        MemberSignUpDto memberSignUpDto = setMember();

        //when
        memberService.withdraw(PASSWORD);

        //then
        assertThat(
                assertThrows(Exception.class, () -> memberRepository.findByUsername(memberSignUpDto.getUsername())
                        .orElseThrow(() -> new Exception("회원이 업습니다."))
                ).getMessage()
        ).isEqualTo("회원이 없습니다.");
    }

    @Test
    public void 회원탈퇴_실패_비밀번호가_일치하지않음() throws Exception {
        //given
        MemberSignUpDto memberSignUpDto = setMember();

        //when, then TODO: MemberException 으로 고쳐야 함
        assertThat(assertThrows(Exception.class, () -> memberService.withdraw(PASSWORD + 1)).getMessage()).isEqualTo("비밀번호가 일치하지 않습니다.");
    }

    @Test
    public void 회원정보조회() throws Exception {
        //given
        MemberSignUpDto memberSignUpDto = setMember();
        Member member = memberRepository.findByUsername(memberSignUpDto.getUsername()).orElseThrow(() -> new Exception());
        clear();

        //when
        MemberInfoDto info = memberService.getInfo(member.getId());

        //then
        assertThat(info.getUsername()).isEqualTo(memberSignUpDto.getUsername());
        assertThat(info.getName()).isEqualTo(memberSignUpDto.getName());
        assertThat(info.getAge()).isEqualTo(memberSignUpDto.getAge());
        assertThat(info.getNickName()).isEqualTo(memberSignUpDto.getNickName());
    }

    @Test
    public void 내정보조회() throws Exception {
        //given
        MemberSignUpDto memberSignUpDto = setMember();

        //when
        MemberInfoDto myInfo = memberService.getMyInfo();

        //then
        assertThat(myInfo.getUsername()).isEqualTo(memberSignUpDto.getUsername());
        assertThat(myInfo.getName()).isEqualTo(memberSignUpDto.getName());
        assertThat(myInfo.getAge()).isEqualTo(memberSignUpDto.getAge());
        assertThat(myInfo.getNickName()).isEqualTo(memberSignUpDto.getNickName());
    }
}