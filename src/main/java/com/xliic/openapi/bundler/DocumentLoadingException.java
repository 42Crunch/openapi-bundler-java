package com.xliic.openapi.bundler;

import java.net.URI;

public class DocumentLoadingException extends Exception {

    private static final long serialVersionUID = 1L;
    public final URI file;

    public DocumentLoadingException(String message, URI file) {
        super(message);
        this.file = file;
    }

}