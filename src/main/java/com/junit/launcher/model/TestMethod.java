package com.junit.launcher.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents an individual test method within a test class.
 */
public class TestMethod {
    private String uniqueId;
    private String methodName;
    private String displayName;
    private List<String> tags;

    public TestMethod() {
        this.tags = new ArrayList<>();
    }

    public TestMethod(String uniqueId, String methodName, String displayName, List<String> tags) {
        this.uniqueId = uniqueId;
        this.methodName = methodName;
        this.displayName = displayName;
        this.tags = tags != null ? new ArrayList<>(tags) : new ArrayList<>();
    }

    public String getUniqueId() {
        return uniqueId;
    }

    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags != null ? new ArrayList<>(tags) : new ArrayList<>();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TestMethod that = (TestMethod) o;
        return Objects.equals(uniqueId, that.uniqueId) &&
               Objects.equals(methodName, that.methodName) &&
               Objects.equals(displayName, that.displayName) &&
               Objects.equals(tags, that.tags);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uniqueId, methodName, displayName, tags);
    }

    @Override
    public String toString() {
        return "TestMethod{" +
               "uniqueId='" + uniqueId + '\'' +
               ", methodName='" + methodName + '\'' +
               ", displayName='" + displayName + '\'' +
               ", tags=" + tags +
               '}';
    }
}
