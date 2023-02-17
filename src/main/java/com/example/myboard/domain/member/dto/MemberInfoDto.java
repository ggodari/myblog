package com.example.myboard.domain.member.dto;

import com.example.myboard.domain.member.Member;
import lombok.Builder;
import lombok.Data;

@Data
public class MemberInfoDto {

    private final String name;
    private final String nickName;
    private final String username;
    private final Integer age;

    @Builder
    public MemberInfoDto(Member member) {
        this.name = member.getName();
        this.nickName = member.getNickName();
        this.username = member.getUsername();
        this.age = member.getAge();
    }
}
