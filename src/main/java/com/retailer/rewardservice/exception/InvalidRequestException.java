package com.retailer.rewardservice.exception;

/**
 * Thrown when an incoming request carries invalid or out-of-range parameters.
 */
public class InvalidRequestException extends RuntimeException {

    public InvalidRequestException(String message) {
        super(message);
    }
}
