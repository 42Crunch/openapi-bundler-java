package com.xliic.openapi.bundler;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

public final class JsonPointer implements Comparable<JsonPointer> {

    private final String pointer;

    public JsonPointer(String pointer) {
        if (pointer == null) {
            throw new IllegalArgumentException("Json pointer can't be null");
        }
        this.pointer = pointer;
    }

    JsonPath getJsonPath() {
        return JsonPointer.toJsonPath(this);
    }

    URI getURI() {
        try {
            return new URI(null, null, pointer);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    public String toString() {
        return this.pointer;
    }

    public String getValue() {
        return pointer;
    }

    static JsonPath toJsonPath(JsonPointer pointer) {
        JsonPath result = new JsonPath();

        if (pointer == null || pointer.getValue() == null || pointer.getValue().equals("")) {
            // return empty path
            return result;
        } else if (pointer.getValue().equals("/")) {
            // return path to "" key
            return result.withKey("");
        }

        for (String segment : pointer.getValue().split("/")) {
            result.add(segment.replace("~1", "/").replace("~0", "~"));
        }

        // remove "" from the result of split
        result.remove(0);

        return result;
    }

    public static JsonPointer fromJsonPath(JsonPath path) {
        if (path.size() == 0) {
            // empty JsonPointer, refers the entire document - return ""
            return new JsonPointer("");
        }

        ArrayList<String> result = new ArrayList<String>();
        for (String key : path) {
            String escaped = key.replace("~", "~0").replace("/", "~1");
            result.add(escaped);
        }

        return new JsonPointer("/" + String.join("/", result));
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof JsonPointer) {
            return this.pointer.equals(((JsonPointer) o).pointer);
        }
        return false;
    }

    @Override
    public final int hashCode() {
        return this.pointer.hashCode();
    }

    @Override
    public int compareTo(JsonPointer o) {
        return pointer.compareTo(o.pointer);
    }

}