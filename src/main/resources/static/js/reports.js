function reportsAndLogs() {
    return {
        // Reports state
        reports: [],
        loadingReports: false,
        reportsError: null,
        viewingReportId: null,
        rerunningReports: {},
        
        // Re-run state
        rerunExecutionId: null,
        rerunSourceReportId: null,
        rerunStatus: null,
        rerunLogs: [],
        rerunEventSource: null,
        rerunReportId: null,
        
        // Logs state
        logs: [],
        loadingLogs: false,
        logsError: null,
        viewingLogId: null,
        logContent: '',
        
        init() {
            // Check if a specific report should be viewed (from URL parameter)
            const urlParams = new URLSearchParams(window.location.search);
            const reportId = urlParams.get('reportId');
            
            // Load reports and logs
            this.loadReports();
            this.loadLogs();
            
            // If reportId is in URL, view that report
            if (reportId) {
                this.viewReport(reportId);
            }
        },
        
        // Reports methods
        async loadReports() {
            this.loadingReports = true;
            this.reportsError = null;
            
            try {
                const response = await fetch('/api/reports');
                
                if (!response.ok) {
                    throw new Error(`Failed to load reports: ${response.statusText}`);
                }
                
                this.reports = await response.json();
            } catch (error) {
                this.reportsError = error.message;
                console.error('Reports loading error:', error);
            } finally {
                this.loadingReports = false;
            }
        },
        
        viewReport(reportId) {
            this.viewingReportId = reportId;
            // Scroll to viewer
            this.$nextTick(() => {
                const viewer = document.querySelector('.report-viewer');
                if (viewer) {
                    viewer.scrollIntoView({ behavior: 'smooth' });
                }
            });
        },
        
        closeReportViewer() {
            this.viewingReportId = null;
        },
        
        async downloadReport(reportId) {
            try {
                const response = await fetch(`/api/reports/${reportId}/download`);
                
                if (!response.ok) {
                    throw new Error(`Failed to download report: ${response.statusText}`);
                }
                
                // Create blob and download
                const blob = await response.blob();
                const url = window.URL.createObjectURL(blob);
                const a = document.createElement('a');
                a.href = url;
                a.download = `report-${reportId}.zip`;
                document.body.appendChild(a);
                a.click();
                window.URL.revokeObjectURL(url);
                document.body.removeChild(a);
                
            } catch (error) {
                alert(`Download failed: ${error.message}`);
                console.error('Report download error:', error);
            }
        },
        
        async rerunFailedTests(reportId) {
            try {
                // Set loading state
                this.rerunningReports[reportId] = true;
                
                // Call re-run endpoint
                const response = await fetch(`/api/reports/${reportId}/rerun`, {
                    method: 'POST'
                });
                
                if (!response.ok) {
                    if (response.status === 400) {
                        throw new Error('No failed tests found in this report');
                    }
                    throw new Error(`Failed to start re-run: ${response.statusText}`);
                }
                
                const executionResponse = await response.json();
                
                // Set up re-run tracking
                this.rerunExecutionId = executionResponse.executionId;
                this.rerunSourceReportId = reportId;
                this.rerunStatus = executionResponse.status;
                this.rerunLogs = [];
                this.rerunReportId = null;
                
                // Start streaming logs
                this.startRerunLogStreaming();
                
                // Scroll to re-run progress
                this.$nextTick(() => {
                    const progress = document.querySelector('.rerun-progress');
                    if (progress) {
                        progress.scrollIntoView({ behavior: 'smooth' });
                    }
                });
                
            } catch (error) {
                alert(`Re-run failed: ${error.message}`);
                console.error('Re-run error:', error);
            } finally {
                this.rerunningReports[reportId] = false;
            }
        },
        
        startRerunLogStreaming() {
            if (this.rerunEventSource) {
                this.rerunEventSource.close();
            }
            
            this.rerunEventSource = new EventSource(`/api/stream/${this.rerunExecutionId}`);
            
            this.rerunEventSource.onmessage = (event) => {
                try {
                    const data = JSON.parse(event.data);
                    
                    if (data.type === 'log') {
                        this.rerunLogs.push({
                            timestamp: new Date().toISOString(),
                            message: data.message
                        });
                        
                        // Auto-scroll log container
                        this.$nextTick(() => {
                            const logContainer = document.querySelector('.rerun-progress .log-container');
                            if (logContainer) {
                                logContainer.scrollTop = logContainer.scrollHeight;
                            }
                        });
                    } else if (data.type === 'status') {
                        this.rerunStatus = data.status;
                        
                        if (data.status === 'COMPLETED') {
                            // Generate report for the re-run
                            this.generateRerunReport();
                        }
                    }
                } catch (error) {
                    console.error('Error parsing SSE data:', error);
                }
            };
            
            this.rerunEventSource.onerror = (error) => {
                console.error('SSE connection error:', error);
                this.rerunEventSource.close();
                this.rerunEventSource = null;
            };
        },
        
        async generateRerunReport() {
            try {
                // For now, we'll assume the report is generated automatically
                // In a real implementation, you might need to call a separate endpoint
                // to generate the report after execution completes
                
                // Wait a moment for report generation
                setTimeout(async () => {
                    // Refresh reports to get the new report
                    await this.loadReports();
                    
                    // Find the newest report (should be our re-run report)
                    if (this.reports.length > 0) {
                        this.rerunReportId = this.reports[0].reportId;
                    }
                }, 2000);
                
            } catch (error) {
                console.error('Error generating re-run report:', error);
            }
        },
        
        viewRerunReport() {
            if (this.rerunReportId) {
                this.viewReport(this.rerunReportId);
            }
        },
        
        async combineWithOriginal() {
            try {
                if (!this.rerunReportId) {
                    alert('Re-run report not available yet');
                    return;
                }
                
                // Call combine reports endpoint
                const response = await fetch('/api/reports/combine', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify([this.rerunSourceReportId, this.rerunReportId])
                });
                
                if (!response.ok) {
                    throw new Error(`Failed to combine reports: ${response.statusText}`);
                }
                
                const combinedReport = await response.json();
                
                // Refresh reports and view the combined report
                await this.loadReports();
                this.viewReport(combinedReport.reportId);
                
                alert('Reports combined successfully!');
                
            } catch (error) {
                alert(`Failed to combine reports: ${error.message}`);
                console.error('Report combination error:', error);
            }
        },
        
        async cancelRerun() {
            try {
                const response = await fetch(`/api/cancel/${this.rerunExecutionId}`, {
                    method: 'POST'
                });
                
                if (!response.ok) {
                    throw new Error(`Failed to cancel re-run: ${response.statusText}`);
                }
                
                this.rerunStatus = 'CANCELLED';
                
            } catch (error) {
                alert(`Failed to cancel re-run: ${error.message}`);
                console.error('Cancel re-run error:', error);
            }
        },
        
        closeRerunProgress() {
            if (this.rerunEventSource) {
                this.rerunEventSource.close();
                this.rerunEventSource = null;
            }
            
            this.rerunExecutionId = null;
            this.rerunSourceReportId = null;
            this.rerunStatus = null;
            this.rerunLogs = [];
            this.rerunReportId = null;
        },
        
        // Logs methods
        async loadLogs() {
            this.loadingLogs = true;
            this.logsError = null;
            
            try {
                const response = await fetch('/api/logs');
                
                if (!response.ok) {
                    throw new Error(`Failed to load logs: ${response.statusText}`);
                }
                
                this.logs = await response.json();
            } catch (error) {
                this.logsError = error.message;
                console.error('Logs loading error:', error);
            } finally {
                this.loadingLogs = false;
            }
        },
        
        async viewLog(logId) {
            try {
                const response = await fetch(`/api/logs/${logId}`);
                
                if (!response.ok) {
                    throw new Error(`Failed to load log: ${response.statusText}`);
                }
                
                this.logContent = await response.text();
                this.viewingLogId = logId;
                
                // Scroll to viewer
                this.$nextTick(() => {
                    const viewer = document.querySelector('.log-viewer');
                    if (viewer) {
                        viewer.scrollIntoView({ behavior: 'smooth' });
                    }
                });
                
            } catch (error) {
                alert(`Failed to view log: ${error.message}`);
                console.error('Log viewing error:', error);
            }
        },
        
        closeLogViewer() {
            this.viewingLogId = null;
            this.logContent = '';
        },
        
        async downloadLog(logId) {
            try {
                const response = await fetch(`/api/logs/${logId}/download`);
                
                if (!response.ok) {
                    throw new Error(`Failed to download log: ${response.statusText}`);
                }
                
                // Create blob and download
                const blob = await response.blob();
                const url = window.URL.createObjectURL(blob);
                const a = document.createElement('a');
                a.href = url;
                a.download = `log-${logId}.log`;
                document.body.appendChild(a);
                a.click();
                window.URL.revokeObjectURL(url);
                document.body.removeChild(a);
                
            } catch (error) {
                alert(`Download failed: ${error.message}`);
                console.error('Log download error:', error);
            }
        },
        
        // Utility methods
        formatTimestamp(timestamp) {
            if (!timestamp) return 'N/A';
            
            try {
                const date = new Date(timestamp);
                return date.toLocaleString();
            } catch (error) {
                return timestamp;
            }
        },
        
        formatFileSize(bytes) {
            if (!bytes || bytes === 0) return '0 B';
            
            const k = 1024;
            const sizes = ['B', 'KB', 'MB', 'GB'];
            const i = Math.floor(Math.log(bytes) / Math.log(k));
            
            return Math.round((bytes / Math.pow(k, i)) * 100) / 100 + ' ' + sizes[i];
        }
    };
}
