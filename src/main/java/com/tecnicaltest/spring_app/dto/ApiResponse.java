package com.tecnicaltest.spring_app.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private int code;
    private String status;
    private String message;
    private T data;

    @Builder.Default
    private Instant timestamp = Instant.now();

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .code(200)
                .status("success")
                .message("Success")
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .code(200)
                .status("success")
                .message(message)
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> created(String message, T data) {
        return ApiResponse.<T>builder()
                .code(201)
                .status("success")
                .message(message)
                .data(data)
                .build();
    }

    public static ApiResponse<Void> error(int code, String status, String message) {
        return ApiResponse.<Void>builder()
                .code(code)
                .status(status)
                .message(message)
                .build();
    }

    public static <T> ApiResponse<T> error(int code, String status, String message, T data) {
        return ApiResponse.<T>builder()
                .code(code)
                .status(status)
                .message(message)
                .data(data)
                .build();
    }
}
