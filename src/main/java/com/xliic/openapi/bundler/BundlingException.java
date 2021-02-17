package com.xliic.openapi.bundler;

import java.util.List;

public class BundlingException extends Exception {

    private static final long serialVersionUID = 1L;
    private List<ReferenceResolutionFailure> failures;

    public BundlingException(String message, List<ReferenceResolutionFailure> failures) {
        super(message);
        this.failures = failures;
    }

    public List<ReferenceResolutionFailure> getFailures() {
        return failures;
    }
}