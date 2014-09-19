package com.chatwala.android.http;

/**
 * Created with IntelliJ IDEA.
 * User: Eliezer
 * Date: 5/8/2014
 * Time: 5:03 PM
 * To change this template use File | Settings | File Templates.
 */
public enum HttpMethod {
    HEAD("HEAD"),
    GET("GET"),
    POST("POST"),
    PUT("PUT"),
    DELETE("DELETE"),
    OPTIONS("OPTIONS");

    private String method;

    HttpMethod(String method) {
        this.method = method;
    }

    public String toString() {
        return method;
    }
}
