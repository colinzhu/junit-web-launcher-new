package com.junit.launcher.service;

import com.junit.launcher.model.TestTree;

/**
 * Service for discovering JUnit test cases in the classpath.
 */
public interface TestDiscoveryService {
    
    /**
     * Discovers all JUnit test cases in the classpath.
     * 
     * @param packageFilter Optional package name to restrict scanning.
     *                      If null or empty, scans all packages.
     * @return Hierarchical structure of test classes and methods
     */
    TestTree discoverTests(String packageFilter);
}
