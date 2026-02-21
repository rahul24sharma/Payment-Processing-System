package com.payment.service.controller;

import com.payment.service.exception.InvalidAmountException;
import com.payment.service.exception.PaymentNotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/test")
public class TestController {
    
    @GetMapping("/not-found")
    public String testNotFound() {
        throw new PaymentNotFoundException(UUID.randomUUID());
    }
    
    @GetMapping("/invalid-amount")
    public String testInvalidAmount() {
        throw new InvalidAmountException("Amount must be positive");
    }
    
    @GetMapping("/generic-error")
    public String testGenericError() {
        throw new RuntimeException("Something went wrong!");
    }
    
    @GetMapping("/success")
    public String testSuccess() {
        return "Success! Exception handler is working.";
    }
}