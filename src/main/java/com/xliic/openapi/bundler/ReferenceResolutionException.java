package com.xliic.openapi.bundler;

import java.net.URI;

public class ReferenceResolutionException extends Exception {

    private static final long serialVersionUID = 1L;
    public final URI sourceFile;
    public final String sourcePointer;
    public final String target;

    public ReferenceResolutionException(String message, URI file, String pointer, String target) {
        super(message);
        this.sourceFile = file;
        this.sourcePointer = pointer;
        this.target = target;
    }
}