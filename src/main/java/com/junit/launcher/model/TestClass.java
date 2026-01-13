package com.junit.launcher.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents a test class containing test methods.
 */
public class TestClass {
    private String uniqueId;
    private String fullyQualifiedName;
    private String simpleName;
    private String displayName;
    private List<TestMethod> testMethods;

    public TestClass() {
        this.testMethods = new ArrayList<>();
    }

    public TestClass(String uniqueId, String fullyQualifiedName, String simpleName, 
                     String displayName, List<TestMethod> testMethods) {
        this.uniqueId = uniqueId;
        this.fullyQualifiedName = fullyQualifiedName;
        this.simpleName = simpleName;
        this.displayName = displayName;
        this.testMethods = testMethods != null ? new ArrayList<>(testMethods) : new ArrayList<>();
    }

    public String getUniqueId() {
        return uniqueId;
    }

    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
    }

    public String getFullyQualifiedName() {
        return fullyQualifiedName;
    }

    public void setFullyQualifiedName(String fullyQualifiedName) {
        this.fullyQualifiedName = fullyQualifiedName;
    }

    public String getSimpleName() {
        return simpleName;
    }

    public void setSimpleName(String simpleName) {
        this.simpleName = simpleName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public List<TestMethod> getTestMethods() {
        return testMethods;
    }

    public void setTestMethods(List<TestMethod> testMethods) {
        this.testMethods = testMethods != null ? new ArrayList<>(testMethods) : new ArrayList<>();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TestClass testClass = (TestClass) o;
        return Objects.equals(uniqueId, testClass.uniqueId) &&
               Objects.equals(fullyQualifiedName, testClass.fullyQualifiedName) &&
               Objects.equals(simpleName, testClass.simpleName) &&
               Objects.equals(displayName, testClass.displayName) &&
               Objects.equals(testMethods, testClass.testMethods);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uniqueId, fullyQualifiedName, simpleName, displayName, testMethods);
    }

    @Override
    public String toString() {
        return "TestClass{" +
               "uniqueId='" + uniqueId + '\'' +
               ", fullyQualifiedName='" + fullyQualifiedName + '\'' +
               ", simpleName='" + simpleName + '\'' +
               ", displayName='" + displayName + '\'' +
               ", testMethods=" + testMethods +
               '}';
    }
}
