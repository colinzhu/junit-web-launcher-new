package com.junit.launcher.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Request payload for test execution.
 */
public class ExecutionRequest {
    private List<String> selectedTestIds;
    private String packageFilter;

    public ExecutionRequest() {
        this.selectedTestIds = new ArrayList<>();
    }

    public ExecutionRequest(List<String> selectedTestIds, String packageFilter) {
        this.selectedTestIds = selectedTestIds != null ? new ArrayList<>(selectedTestIds) : new ArrayList<>();
        this.packageFilter = packageFilter;
    }

    public List<String> getSelectedTestIds() {
        return selectedTestIds;
    }

    public void setSelectedTestIds(List<String> selectedTestIds) {
        this.selectedTestIds = selectedTestIds != null ? new ArrayList<>(selectedTestIds) : new ArrayList<>();
    }

    public String getPackageFilter() {
        return packageFilter;
    }

    public void setPackageFilter(String packageFilter) {
        this.packageFilter = packageFilter;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExecutionRequest that = (ExecutionRequest) o;
        return Objects.equals(selectedTestIds, that.selectedTestIds) &&
               Objects.equals(packageFilter, that.packageFilter);
    }

    @Override
    public int hashCode() {
        return Objects.hash(selectedTestIds, packageFilter);
    }

    @Override
    public String toString() {
        return "ExecutionRequest{" +
               "selectedTestIds=" + selectedTestIds +
               ", packageFilter='" + packageFilter + '\'' +
               '}';
    }
}
