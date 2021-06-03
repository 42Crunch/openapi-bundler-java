package com.xliic.openapi.bundler;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.HashMap;

public class Mapping {
    Location value = null;
    HashMap<String, Mapping> children = new HashMap<>();

    public Location find(String pointer) throws UnsupportedEncodingException {
        Mapping current = this;
        JsonPath path = new JsonPointer(pointer).getJsonPath();

        int i = 0;
        for (; i < path.size() && current.children.containsKey(path.get(i)); i++) {
            current = current.children.get(path.get(i));
        }

        Location value = current.value;

        if (value == null) {
            // mapping wasn't found
            return null;
        }

        if (i < path.size()) {
            JsonPath remaining = new JsonPath(path.subList(i, path.size()));
            return new Location(value.uri, value.pointer + remaining.toPointer());
        }

        return value;
    }

    public static class Location {
        public final URI uri;
        public final String pointer;

        public Location(URI uri, JsonPointer pointer) {
            this.uri = uri;
            this.pointer = pointer.getValue();
        }

        public Location(URI uri, String pointer) {
            this.uri = uri;
            this.pointer = pointer;
        }
    }
}