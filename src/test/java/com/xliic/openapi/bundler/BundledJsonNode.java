package com.xliic.openapi.bundler;

import java.io.UnsupportedEncodingException;

import com.fasterxml.jackson.databind.JsonNode;

public class BundledJsonNode {
    private JsonNode jsonNode;
    private Mapping mapping;

    public BundledJsonNode(JsonNode jsonNode, Mapping mapping) {
        this.jsonNode = jsonNode;
        this.mapping = mapping;
    }

    public JsonNode at(String pointer) {
        return jsonNode.at(pointer);
    }

    public Mapping.Location original(String pointer) throws UnsupportedEncodingException {
        return this.mapping.find(pointer);
    }
}