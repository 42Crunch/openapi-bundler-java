package com.xliic.openapi.bundler;

import java.net.URI;

public class ReferenceResolutionFailure {

    public final String message;
    public final URI sourceFile;
    public final String sourcePointer;
    public final String target;

    public ReferenceResolutionFailure(String message, URI file, String pointer, String target) {
        this.message = message;
        this.sourceFile = file;
        this.sourcePointer = pointer;
        this.target = target;
    }

    @Override
    public String toString() {
        return String.format("%s in %s at $s", message, sourceFile, sourcePointer);
    }
}