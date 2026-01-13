package com.junit.launcher.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.junit.launcher.model.TestTree;
import com.junit.launcher.service.TestDiscoveryService;

/**
 * REST controller for test discovery operations.
 */
@RestController
@RequestMapping("/api")
public class TestDiscoveryController {
    
    private final TestDiscoveryService testDiscoveryService;
    
    public TestDiscoveryController(TestDiscoveryService testDiscoveryService) {
        this.testDiscoveryService = testDiscoveryService;
    }
    
    /**
     * Discovers all JUnit test cases in the classpath.
     * 
     * @param packageFilter Optional package name to restrict scanning
     * @return TestTree JSON response with discovered tests
     */
    @GetMapping("/discover")
    public ResponseEntity<TestTree> discoverTests(
            @RequestParam(required = false) String packageFilter) {
        try {
            TestTree testTree = testDiscoveryService.discoverTests(packageFilter);
            return ResponseEntity.ok(testTree);
        } catch (Exception e) {
            // Log the error and return 500 Internal Server Error
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
