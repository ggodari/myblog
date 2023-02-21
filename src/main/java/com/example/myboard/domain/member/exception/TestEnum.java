package com.example.myboard.domain.member.exception;

import lombok.Data;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum TestEnum {
    ALREADY_EXIST_MEMBER(101, HttpStatus.OK, "이미 존재"),
    NOT_FOUND_MEMBER(103, HttpStatus.OK, "회원 없음"),
    ;

    private int errorCode;
    private HttpStatus httpStatus;
    private String errorMessage;

    TestEnum(int errCode, HttpStatus httpStatus, String errMsg) {
        this.errorCode = errCode;
        this.httpStatus = httpStatus;
        this.errorMessage = errMsg;
    }
}
