package com.example.myboard.domain.member.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
@AllArgsConstructor
public class MemberWithdrawDto {

    @NotBlank(message = "비밀번호를 입력해주세요.")
    private String checkPassword;
}
