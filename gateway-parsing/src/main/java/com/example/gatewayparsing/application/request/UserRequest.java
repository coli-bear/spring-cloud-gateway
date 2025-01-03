package com.example.gatewayparsing.application.request;

/**
 * ResponseBody Parsing 요청용 Request 클래스
 */
public record UserRequest(String id, Data data) {
    public record Data(String name, int age) {
    }
}
