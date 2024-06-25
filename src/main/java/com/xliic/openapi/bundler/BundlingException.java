package com.xliic.openapi.bundler;

import java.util.List;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class BundlingException extends Exception {

    private static final long serialVersionUID = 1L;
    private List<ReferenceResolutionFailure> failures;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public BundlingException(String message, List<ReferenceResolutionFailure> failures) {
        super(message);
        this.failures = failures;
    }

    @SuppressFBWarnings("EI_EXPOSE_REP")
    public List<ReferenceResolutionFailure> getFailures() {
        return failures;
    }
}