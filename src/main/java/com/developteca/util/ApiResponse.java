package com.developteca.util;

import java.time.LocalDateTime;

public class ApiResponse {

    private boolean success;
    private String message;
    private Object data;
    private LocalDateTime timestamp;
    private Object errors;

    // ========== CONSTRUCTORES ===========

    public ApiResponse(){
        this.timestamp = LocalDateTime.now();
    }

    public ApiResponse(boolean success, String message, Object data){

        this.success = success;
        this.message = message;
        this.data = data;
        this.timestamp = LocalDateTime.now();
        this.errors = null;

    }

    public ApiResponse(boolean success, String message, Object data, Object errors) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.errors = errors;
        this.timestamp = LocalDateTime.now();
    }


    //============ GETTEWR SETTERS ============

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Object getErrors() {
        return errors;
    }

    public void setErrors(Object errors) {
        this.errors = errors;
    }

}
