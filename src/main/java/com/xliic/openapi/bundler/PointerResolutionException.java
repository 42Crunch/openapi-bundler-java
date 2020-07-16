package com.xliic.openapi.bundler;

public class PointerResolutionException extends Exception {

    private static final long serialVersionUID = 1L;
    public final String pointer;

    public PointerResolutionException(String pointer) {
        super();
        this.pointer = pointer;
    }

}