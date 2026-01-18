package com.junit.launcher.service;

import com.junit.launcher.model.TestClass;
import com.junit.launcher.model.TestMethod;
import com.junit.launcher.model.TestTree;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TestDiscoveryServiceImpl.
 */
class TestDiscoveryServiceImplTest {
    
    private TestDiscoveryService discoveryService;
    
    @BeforeEach
    void setUp() {
        discoveryService = new TestDiscoveryServiceImpl();
    }
    
    @Test
    void testDiscoverTests_withoutFilter() {
        // Discover all tests
        TestTree testTree = discoveryService.discoverTests(null);
        
        // Verify basic structure
        assertNotNull(testTree);
        assertNotNull(testTree.getTestClasses());
        assertNotNull(testTree.getDiscoveryTimestamp());
        
        // Should find at least the SampleTest class
        assertTrue(testTree.getTestClasses().size() > 0, 
            "Should discover at least one test class");
        
        // Verify total test count matches sum of methods
        int expectedTotal = testTree.getTestClasses().stream()
            .mapToInt(tc -> tc.getTestMethods().size())
            .sum();
        assertEquals(expectedTotal, testTree.getTotalTests());
    }
    
    @Test
    void testDiscoverTests_withPackageFilter() {
        // Discover tests in specific package
        TestTree testTree = discoveryService.discoverTests("com.junit.launcher.service");
        
        // Verify structure
        assertNotNull(testTree);
        assertNotNull(testTree.getTestClasses());
        
        // All discovered classes should be in the filtered package
        for (TestClass testClass : testTree.getTestClasses()) {
            assertTrue(testClass.getFullyQualifiedName().startsWith("com.junit.launcher.service"),
                "Test class should be in filtered package: " + testClass.getFullyQualifiedName());
        }
    }
    
    @Test
    void testDiscoverTests_emptyClasspath() {
        // Discover with non-existent package
        TestTree testTree = discoveryService.discoverTests("com.nonexistent.package");
        
        // Should return empty tree, not fail
        assertNotNull(testTree);
        assertNotNull(testTree.getTestClasses());
        assertEquals(0, testTree.getTotalTests());
    }
    
    @Test
    void testTestClassDataCompleteness() {
        // Discover tests
        TestTree testTree = discoveryService.discoverTests("com.junit.launcher.service");
        
        // Find SampleTest
        TestClass sampleTest = testTree.getTestClasses().stream()
            .filter(tc -> tc.getSimpleName().equals("SampleTest"))
            .findFirst()
            .orElse(null);
        
        if (sampleTest != null) {
            // Verify fully qualified name is present
            assertNotNull(sampleTest.getFullyQualifiedName());
            assertTrue(sampleTest.getFullyQualifiedName().contains("SampleTest"));
            
            // Verify test method count
            assertNotNull(sampleTest.getTestMethods());
            assertEquals(2, sampleTest.getTestMethods().size(), 
                "SampleTest should have 2 test methods");
            
            // Verify each test method has required data
            for (TestMethod method : sampleTest.getTestMethods()) {
                assertNotNull(method.getUniqueId(), "Method should have unique ID");
                assertNotNull(method.getMethodName(), "Method should have method name");
                assertNotNull(method.getDisplayName(), "Method should have display name");
            }
        }
    }
    
    @Test
    void testTestOrganizationByClass() {
        // Discover tests
        TestTree testTree = discoveryService.discoverTests(null);
        
        // Verify all test methods are grouped under their class
        for (TestClass testClass : testTree.getTestClasses()) {
            assertNotNull(testClass.getTestMethods(), 
                "Test class should have test methods list");
            
            // Each test method should have a unique ID that references the class
            for (TestMethod method : testClass.getTestMethods()) {
                assertNotNull(method.getUniqueId());
                // Unique ID should contain class reference
                assertTrue(method.getUniqueId().contains(testClass.getSimpleName()) ||
                          method.getUniqueId().contains(testClass.getFullyQualifiedName()),
                    "Method unique ID should reference its class");
            }
        }
    }
}
