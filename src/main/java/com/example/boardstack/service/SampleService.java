package com.example.boardstack.service;

import org.springframework.stereotype.Service;

@Service
public class SampleService {

    public String getMessage() {
        return "Hello from SampleService!";
    }

    public String processData(String input) {
        return "Processed: " + input;
    }
} 