package com.example.myboard.domain.member.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Optional;

@Data
@AllArgsConstructor
public class MemberUpdateDto {

    private Optional<String> name;
    private Optional<String> nickName;
    private Optional<Integer> age;
}
