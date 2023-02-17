package com.example.myboard.global.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class ExceptionAdvice {

    @ExceptionHandler(BaseException.class)
    public ResponseEntity handleBaseEx(BaseException exception) {

        return new ResponseEntity(new ExceptionDto(exception.getExceptionType().getErrorCode()), exception.getExceptionType().getHttpStatus());
    }


    @ExceptionHandler(Exception.class)
    public ResponseEntity handleMemberEx(Exception exception) {
        exception.printStackTrace();
        return new ResponseEntity(exception.getMessage(), HttpStatus.OK);
    }


    @Data
    @AllArgsConstructor
    static class ExceptionDto {
        private Integer errorCode;
    }
}
