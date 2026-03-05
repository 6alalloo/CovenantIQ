package com.covenantiq.exception;

import java.util.Map;

public class WorkflowTransitionConflictException extends ConflictException {

    private final Map<String, Object> diagnostics;

    public WorkflowTransitionConflictException(String message, Map<String, Object> diagnostics) {
        super(message);
        this.diagnostics = diagnostics;
    }

    public Map<String, Object> getDiagnostics() {
        return diagnostics;
    }
}
