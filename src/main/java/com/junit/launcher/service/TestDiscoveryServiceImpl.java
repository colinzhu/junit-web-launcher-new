package com.junit.launcher.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.discovery.ClassNameFilter;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.springframework.stereotype.Service;

import com.junit.launcher.model.TestClass;
import com.junit.launcher.model.TestMethod;
import com.junit.launcher.model.TestTree;

/**
 * Implementation of TestDiscoveryService using JUnit Platform Launcher API.
 */
@Service
public class TestDiscoveryServiceImpl implements TestDiscoveryService {
    
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    @Override
    public TestTree discoverTests(String packageFilter) {
        // Create launcher
        Launcher launcher = LauncherFactory.create();
        
        // Build discovery request
        LauncherDiscoveryRequest request = buildDiscoveryRequest(packageFilter);
        
        // Discover tests
        TestPlan testPlan = launcher.discover(request);
        
        // Parse TestPlan into TestTree structure
        TestTree testTree = parseTestPlan(testPlan);
        
        // Set discovery timestamp
        testTree.setDiscoveryTimestamp(LocalDateTime.now().format(TIMESTAMP_FORMATTER));
        
        return testTree;
    }
    
    /**
     * Builds a LauncherDiscoveryRequest with optional package filtering.
     */
    private LauncherDiscoveryRequest buildDiscoveryRequest(String packageFilter) {
        LauncherDiscoveryRequestBuilder builder = LauncherDiscoveryRequestBuilder.request();
        
        if (packageFilter != null && !packageFilter.trim().isEmpty()) {
            // Use package selector for specific package
            builder.selectors(DiscoverySelectors.selectPackage(packageFilter.trim()));
            
            // Add class name filter to include only classes in the package and sub-packages
            String packagePrefix = packageFilter.trim() + ".";
            builder.filters(ClassNameFilter.includeClassNamePatterns(
                "^" + packageFilter.trim().replace(".", "\\.") + "\\..+",
                "^" + packageFilter.trim().replace(".", "\\.") + "$"
            ));
        } else {
            // Scan all classpath roots
            builder.selectors(DiscoverySelectors.selectClasspathRoots(
                Set.of(getClasspathRoots())
            ));
        }
        
        return builder.build();
    }
    
    /**
     * Gets classpath roots for scanning.
     */
    private java.nio.file.Path[] getClasspathRoots() {
        String classPath = System.getProperty("java.class.path");
        String[] paths = classPath.split(System.getProperty("path.separator"));
        
        List<java.nio.file.Path> roots = new ArrayList<>();
        for (String path : paths) {
            try {
                java.nio.file.Path p = java.nio.file.Paths.get(path);
                if (java.nio.file.Files.exists(p)) {
                    roots.add(p);
                }
            } catch (Exception e) {
                // Skip invalid paths
            }
        }
        
        return roots.toArray(new java.nio.file.Path[0]);
    }
    
    /**
     * Parses a TestPlan into a TestTree structure.
     */
    private TestTree parseTestPlan(TestPlan testPlan) {
        TestTree testTree = new TestTree();
        List<TestClass> testClasses = new ArrayList<>();
        int totalTests = 0;
        
        // Get all roots (typically engine descriptors)
        Set<TestIdentifier> roots = testPlan.getRoots();
        
        for (TestIdentifier root : roots) {
            // Traverse children to find test classes
            Set<TestIdentifier> children = testPlan.getChildren(root);
            for (TestIdentifier child : children) {
                processIdentifier(child, testPlan, testClasses);
            }
        }
        
        // Count total tests
        for (TestClass testClass : testClasses) {
            totalTests += testClass.getTestMethods().size();
        }
        
        testTree.setTestClasses(testClasses);
        testTree.setTotalTests(totalTests);
        
        return testTree;
    }
    
    /**
     * Recursively processes a test identifier to extract test classes and methods.
     */
    private void processIdentifier(TestIdentifier identifier, TestPlan testPlan, 
                                   List<TestClass> testClasses) {
        // Check if this is a test class (container)
        if (identifier.isContainer() && isTestClass(identifier)) {
            TestClass testClass = createTestClass(identifier, testPlan);
            if (!testClass.getTestMethods().isEmpty()) {
                testClasses.add(testClass);
            }
        } else if (identifier.isContainer()) {
            // Continue traversing for nested containers
            Set<TestIdentifier> children = testPlan.getChildren(identifier);
            for (TestIdentifier child : children) {
                processIdentifier(child, testPlan, testClasses);
            }
        }
    }
    
    /**
     * Checks if an identifier represents a test class.
     */
    private boolean isTestClass(TestIdentifier identifier) {
        // A test class identifier typically has a class source
        Optional<TestSource> source = identifier.getSource();
        return source.isPresent() && source.get() instanceof ClassSource;
    }
    
    /**
     * Creates a TestClass from an identifier.
     */
    private TestClass createTestClass(TestIdentifier classIdentifier, TestPlan testPlan) {
        TestClass testClass = new TestClass();
        
        // Set unique ID
        testClass.setUniqueId(classIdentifier.getUniqueId());
        
        // Extract class information from source
        Optional<TestSource> source = classIdentifier.getSource();
        if (source.isPresent() && source.get() instanceof ClassSource) {
            ClassSource classSource = (ClassSource) source.get();
            Class<?> javaClass = classSource.getJavaClass();
            testClass.setFullyQualifiedName(javaClass.getName());
            testClass.setSimpleName(javaClass.getSimpleName());
        }
        
        // Set display name
        testClass.setDisplayName(classIdentifier.getDisplayName());
        
        // Extract test methods
        List<TestMethod> testMethods = new ArrayList<>();
        Set<TestIdentifier> children = testPlan.getChildren(classIdentifier);
        
        for (TestIdentifier child : children) {
            if (child.isTest()) {
                TestMethod testMethod = createTestMethod(child);
                testMethods.add(testMethod);
            }
        }
        
        testClass.setTestMethods(testMethods);
        
        return testClass;
    }
    
    /**
     * Creates a TestMethod from an identifier.
     */
    private TestMethod createTestMethod(TestIdentifier methodIdentifier) {
        TestMethod testMethod = new TestMethod();
        
        // Set unique ID
        testMethod.setUniqueId(methodIdentifier.getUniqueId());
        
        // Set display name
        testMethod.setDisplayName(methodIdentifier.getDisplayName());
        
        // Extract method name from source
        Optional<TestSource> source = methodIdentifier.getSource();
        if (source.isPresent() && source.get() instanceof MethodSource) {
            MethodSource methodSource = (MethodSource) source.get();
            testMethod.setMethodName(methodSource.getMethodName());
        } else {
            // Fallback to display name
            testMethod.setMethodName(methodIdentifier.getDisplayName());
        }
        
        // Extract tags
        List<String> tags = new ArrayList<>(methodIdentifier.getTags().stream()
            .map(tag -> tag.getName())
            .toList());
        testMethod.setTags(tags);
        
        return testMethod;
    }
}
