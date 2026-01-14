function testLauncher() {
    return {
        // Discovery state
        packageFilter: '',
        testTree: null,
        discovering: false,
        discoveryError: null,
        
        // Selection state
        selectedTests: [],
        expandedClasses: [],
        
        // Execution state
        executing: false,
        executionId: null,
        executionStatus: null,
        logs: [],
        testResults: [],
        reportId: null,
        eventSource: null,
        
        init() {
            // Auto-discover tests on page load
            this.discoverTests();
        },
        
        // Discovery methods
        async discoverTests() {
            this.discovering = true;
            this.discoveryError = null;
            this.testTree = null;
            this.selectedTests = [];
            this.expandedClasses = [];
            
            try {
                const url = this.packageFilter 
                    ? `/api/discover?packageFilter=${encodeURIComponent(this.packageFilter)}`
                    : '/api/discover';
                    
                const response = await fetch(url);
                
                if (!response.ok) {
                    throw new Error(`Discovery failed: ${response.statusText}`);
                }
                
                this.testTree = await response.json();
            } catch (error) {
                this.discoveryError = error.message;
                console.error('Discovery error:', error);
            } finally {
                this.discovering = false;
            }
        },
        
        // Selection methods
        toggleExpand(classId) {
            const index = this.expandedClasses.indexOf(classId);
            if (index > -1) {
                this.expandedClasses.splice(index, 1);
            } else {
                this.expandedClasses.push(classId);
            }
        },
        
        isClassSelected(testClass) {
            if (!testClass.testMethods || testClass.testMethods.length === 0) {
                return false;
            }
            return testClass.testMethods.every(method => 
                this.selectedTests.includes(method.uniqueId)
            );
        },
        
        toggleClass(testClass) {
            const isSelected = this.isClassSelected(testClass);
            
            if (isSelected) {
                // Deselect all methods in this class
                testClass.testMethods.forEach(method => {
                    const index = this.selectedTests.indexOf(method.uniqueId);
                    if (index > -1) {
                        this.selectedTests.splice(index, 1);
                    }
                });
            } else {
                // Select all methods in this class
                testClass.testMethods.forEach(method => {
                    if (!this.selectedTests.includes(method.uniqueId)) {
                        this.selectedTests.push(method.uniqueId);
                    }
                });
            }
        },
        
        toggleTest(testId) {
            const index = this.selectedTests.indexOf(testId);
            if (index > -1) {
                this.selectedTests.splice(index, 1);
            } else {
                this.selectedTests.push(testId);
            }
        },
        
        // Execution methods
        async executeTests() {
            if (this.selectedTests.length === 0) {
                return;
            }
            
            this.executing = true;
            this.logs = [];
            this.testResults = [];
            this.reportId = null;
            this.executionStatus = 'RUNNING';
            
            try {
                const response = await fetch('/api/execute', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify({
                        selectedTestIds: this.selectedTests
                    })
                });
                
                if (!response.ok) {
                    throw new Error(`Execution failed: ${response.statusText}`);
                }
                
                const executionResponse = await response.json();
                this.executionId = executionResponse.executionId;
                
                // Start streaming logs
                this.streamLogs();
                
                // Poll for execution status
                this.pollExecutionStatus();
                
            } catch (error) {
                this.executionStatus = 'FAILED';
                this.logs.push(`ERROR: ${error.message}`);
                console.error('Execution error:', error);
                this.executing = false;
            }
        },
        
        streamLogs() {
            if (!this.executionId) {
                console.error('No execution ID available for streaming');
                return;
            }
            
            console.log('Starting log stream for execution:', this.executionId);
            
            // Close existing connection if any
            if (this.eventSource) {
                this.eventSource.close();
            }
            
            // Create new EventSource for SSE
            const streamUrl = `/api/stream/${this.executionId}`;
            console.log('Connecting to SSE endpoint:', streamUrl);
            this.eventSource = new EventSource(streamUrl);
            
            // Listen for 'connected' event
            this.eventSource.addEventListener('connected', (event) => {
                console.log('SSE connected:', event.data);
            });
            
            // Listen for 'log' events (backend sends with name "log")
            this.eventSource.addEventListener('log', (event) => {
                console.log('Received log event:', event.data);
                const data = event.data;
                // Split by newlines and add each line separately
                const lines = data.split('\n').filter(line => line.trim() !== '');
                lines.forEach(line => {
                    this.logs.push(line);
                });
                
                // Auto-scroll to bottom
                this.$nextTick(() => {
                    const logsOutput = this.$refs.logsOutput;
                    if (logsOutput) {
                        logsOutput.scrollTop = logsOutput.scrollHeight;
                    }
                });
            });
            
            // Also listen for default message events as fallback
            this.eventSource.onmessage = (event) => {
                console.log('Received message event:', event.data);
                const data = event.data;
                this.logs.push(data);
                
                // Auto-scroll to bottom
                this.$nextTick(() => {
                    const logsOutput = this.$refs.logsOutput;
                    if (logsOutput) {
                        logsOutput.scrollTop = logsOutput.scrollHeight;
                    }
                });
            };
            
            this.eventSource.onopen = () => {
                console.log('SSE connection opened');
            };
            
            this.eventSource.onerror = (error) => {
                console.error('SSE error:', error);
                console.error('SSE readyState:', this.eventSource?.readyState);
                // Only close if it's a real error, not just end of stream
                if (this.eventSource && this.eventSource.readyState === EventSource.CLOSED) {
                    console.log('SSE connection closed by server');
                    this.eventSource.close();
                    this.eventSource = null;
                }
            };
        },
        
        async pollExecutionStatus() {
            const pollInterval = setInterval(async () => {
                try {
                    const response = await fetch(`/api/status/${this.executionId}`);
                    
                    if (!response.ok) {
                        clearInterval(pollInterval);
                        return;
                    }
                    
                    const status = await response.json();
                    this.executionStatus = status.status;
                    
                    // Update test results if available
                    if (status.testResults) {
                        this.testResults = status.testResults;
                    }
                    
                    // Check if execution is complete
                    if (status.status === 'COMPLETED' || status.status === 'CANCELLED' || status.status === 'FAILED') {
                        clearInterval(pollInterval);
                        this.executing = false;
                        
                        // Close SSE connection
                        if (this.eventSource) {
                            this.eventSource.close();
                            this.eventSource = null;
                        }
                        
                        // Get report ID if available
                        if (status.reportId) {
                            this.reportId = status.reportId;
                        }
                    }
                } catch (error) {
                    console.error('Status polling error:', error);
                    clearInterval(pollInterval);
                }
            }, 1000); // Poll every second
        },
        
        async cancelExecution() {
            if (!this.executionId) {
                return;
            }
            
            try {
                const response = await fetch(`/api/cancel/${this.executionId}`, {
                    method: 'POST'
                });
                
                if (!response.ok) {
                    throw new Error(`Cancellation failed: ${response.statusText}`);
                }
                
                this.logs.push('Execution cancelled by user');
                
            } catch (error) {
                this.logs.push(`ERROR: ${error.message}`);
                console.error('Cancellation error:', error);
            }
        },
        
        // Result methods
        countResults(status) {
            return this.testResults.filter(r => r.status === status).length;
        },
        
        getResultIcon(status) {
            switch (status) {
                case 'PASSED':
                    return '✓';
                case 'FAILED':
                    return '✗';
                case 'SKIPPED':
                    return '⊘';
                default:
                    return '?';
            }
        }
    };
}
