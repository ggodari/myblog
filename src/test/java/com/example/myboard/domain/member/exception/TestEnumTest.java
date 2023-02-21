package com.example.myboard.domain.member.exception;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class TestEnumTest {

    @Test
    public void test() {
        String tt = TestEnum.ALREADY_EXIST_MEMBER.name();

        System.out.println("tt : " + tt);

        TestEnum testEnum = TestEnum.ALREADY_EXIST_MEMBER;

        System.out.println("testEnum :" + testEnum.getErrorMessage());
    }
}