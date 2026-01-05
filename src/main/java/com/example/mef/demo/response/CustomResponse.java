package com.example.mef.demo.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CustomResponse <T>{
    private int status;
    private String message;
    private T data;
    // Getters, setters, and constructors

    public CustomResponse(int status, String message, T data) {
        this.status = status;
        this.message = message;
        this.data = data;
    }

    public String getMessage() {
        return message;
    }

    public int getStatus() {
        return status;
    }

    public T getData() {
        return data;
    }
}
