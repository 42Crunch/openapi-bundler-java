package com.xliic.openapi.bundler;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;

public class Mapping {
    Location value = null;
    HashMap<String, Mapping> children = new HashMap<>();

    public Location find(String pointer) throws UnsupportedEncodingException {
        Mapping current = this;
        JsonPath path = JsonPointer.parse(pointer);

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
            return new Location(value.file, value.pointer + remaining.toPointer());
        }

        return value;
    }

    public static class Location {
        public final String file;
        public final String pointer;

        public Location(String file, String pointer) {
            this.file = file;
            this.pointer = pointer;
        }
    }
}