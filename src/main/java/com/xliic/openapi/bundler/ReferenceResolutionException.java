package com.xliic.openapi.bundler;

import java.net.URI;

public class ReferenceResolutionException extends Exception {

    private static final long serialVersionUID = 1L;
    public final URI file;
    public final String pointer;
    public final URI target;

    public ReferenceResolutionException(URI file, String pointer, URI target) {
        super(String.format("Failed to resolve reference to '%s' in '%s': at path '%s'", target, file, pointer));
        this.file = file;
        this.pointer = pointer;
        this.target = target;
    }
}