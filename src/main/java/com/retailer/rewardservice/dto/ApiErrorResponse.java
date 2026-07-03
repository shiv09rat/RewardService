package com.retailer.rewardservice.dto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Standard error response body returned by the API on exceptions.
 */
public class ApiErrorResponse {

    private int status;
    private String error;
    private String message;
    private String timestamp;
    private String path;

    public ApiErrorResponse(int status, String error, String message, String path) {
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
        this.timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    public int getStatus() {
        return status;
    }

    public String getError() {
        return error;
    }

    public String getMessage() {
        return message;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getPath() {
        return path;
    }
}
