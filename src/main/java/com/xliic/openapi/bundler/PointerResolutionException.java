package com.xliic.openapi.bundler;

import java.net.URI;

public class PointerResolutionException extends Exception {

    private static final long serialVersionUID = 1L;
    public final URI file;
    public final String pointer;

    public PointerResolutionException(URI file, String pointer) {
        super();
        this.file = file;
        this.pointer = pointer;
    }

}