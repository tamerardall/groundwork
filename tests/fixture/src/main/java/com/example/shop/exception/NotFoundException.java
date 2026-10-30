package com.example.shop.exception;

public class NotFoundException extends RuntimeException {

    public NotFoundException(String type, Long id) {
        super(type + " not found: " + id);
    }
}
