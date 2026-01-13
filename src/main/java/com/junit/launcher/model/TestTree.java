package com.junit.launcher.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents the hierarchical structure of discovered tests.
 */
public class TestTree {
    private List<TestClass> testClasses;
    private int totalTests;
    private String discoveryTimestamp;

    public TestTree() {
        this.testClasses = new ArrayList<>();
    }

    public TestTree(List<TestClass> testClasses, int totalTests, String discoveryTimestamp) {
        this.testClasses = testClasses != null ? new ArrayList<>(testClasses) : new ArrayList<>();
        this.totalTests = totalTests;
        this.discoveryTimestamp = discoveryTimestamp;
    }

    public List<TestClass> getTestClasses() {
        return testClasses;
    }

    public void setTestClasses(List<TestClass> testClasses) {
        this.testClasses = testClasses != null ? new ArrayList<>(testClasses) : new ArrayList<>();
    }

    public int getTotalTests() {
        return totalTests;
    }

    public void setTotalTests(int totalTests) {
        this.totalTests = totalTests;
    }

    public String getDiscoveryTimestamp() {
        return discoveryTimestamp;
    }

    public void setDiscoveryTimestamp(String discoveryTimestamp) {
        this.discoveryTimestamp = discoveryTimestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TestTree testTree = (TestTree) o;
        return totalTests == testTree.totalTests &&
               Objects.equals(testClasses, testTree.testClasses) &&
               Objects.equals(discoveryTimestamp, testTree.discoveryTimestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(testClasses, totalTests, discoveryTimestamp);
    }

    @Override
    public String toString() {
        return "TestTree{" +
               "testClasses=" + testClasses +
               ", totalTests=" + totalTests +
               ", discoveryTimestamp='" + discoveryTimestamp + '\'' +
               '}';
    }
}
