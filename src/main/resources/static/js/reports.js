function reportsAndLogs() {
    return {
        // Reports state
        reports: [],
        loadingReports: false,
        reportsError: null,
        viewingReportId: null,
        
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
