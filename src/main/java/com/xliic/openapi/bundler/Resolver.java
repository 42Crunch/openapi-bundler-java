/*
 Copyright (c) 42Crunch Ltd. All rights reserved.
 Licensed under the GNU Affero General Public License version 3. See LICENSE.txt in the project root for license information.
*/

package com.xliic.openapi.bundler;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class Resolver {

    public static boolean isRef(final JsonNode node) {
        return node != null && node.isObject() && node.has("$ref") && node.get("$ref").isTextual();
    }

    public static boolean isExtendedRef(final JsonNode node) {
        return isRef(node) && node.size() > 1;
    }

    public static boolean isExternalRef(final JsonNode node) {
        return isRef(node) && !node.get("$ref").asText().startsWith("#");
    }

    public static JsonPointer resolveReference(Document.Part part, JsonNode ref)
            throws URISyntaxException, UnsupportedEncodingException {
        ArrayList<JsonNode> visited = new ArrayList<>();
        return resolveReference(part, ref, visited);
    }

    private static JsonPointer resolveReference(Document.Part part, JsonNode ref, ArrayList<JsonNode> visited)
            throws URISyntaxException, UnsupportedEncodingException {
        URI target = new URI(ref.get("$ref").asText());

        try {
            Document.Part targetPart = Document.getTargetPart(part, target);
            JsonPointer pointer = new JsonPointer(targetPart, new URI(null, null, target.getFragment()));

            if (visited.contains(ref)) {
                pointer.circular = true;
                pointer.resolvedPart = pointer.part;
                pointer.resolvedPath = pointer.path;
                return pointer;
            }
            visited.add(ref);

            resolvePointer(pointer, visited);
            return pointer;
        } catch (Exception e) {
            throw new RuntimeException(
                    String.format("Failed to resolve reference '%s' in '%s': %s", target, part.location, e));
        }

    }

    private static void resolvePointer(JsonPointer pointer, ArrayList<JsonNode> visited)
            throws UnsupportedEncodingException, URISyntaxException {
        pointer.resolvedPart = pointer.part;
        pointer.resolvedValue = pointer.part.node;
        pointer.resolvedPath = pointer.path;

        for (int i = 0; i < pointer.path.size(); i++) {
            if (resolveIfRef(pointer, visited)) {
                pointer.resolvedPath.addAll(pointer.path.subList(i, pointer.path.size()));
            }

            String key = pointer.path.get(i);
            pointer.resolvedValue = Util.get(pointer.resolvedValue, key);
            if (pointer.resolvedValue == null) {
                throw new RuntimeException("Unable to resolve: " + pointer.target.getFragment() + " in "
                        + pointer.part.location + " key not found: " + key);
            }
        }

        resolveIfRef(pointer, visited);
    }

    private static boolean resolveIfRef(JsonPointer pointer, ArrayList<JsonNode> visited)
            throws UnsupportedEncodingException, URISyntaxException {
        if (isRef(pointer.resolvedValue)) {
            JsonPointer resolved = resolveReference(pointer.resolvedPart, pointer.resolvedValue, visited);
            pointer.indirections = pointer.indirections + resolved.getIndirections();
            pointer.resolvedPart = resolved.resolvedPart;
            pointer.resolvedValue = resolved.resolvedValue;
            pointer.resolvedPath = resolved.resolvedPath;
            pointer.circular = resolved.circular;
            return true;
        }
        return false;
    }

    public static JsonNode mergeExtendedRef(Serializer serializer, JsonNode ref, JsonNode value) {
        ObjectNode merged = serializer.createObjectNode();
        if (value.isObject()) {
            Iterator<String> refFields = ref.fieldNames();
            while (refFields.hasNext()) {
                String fieldName = refFields.next();
                if (!fieldName.equals("$ref")) {
                    merged.set(fieldName, ref.get(fieldName));
                }
            }

            Iterator<String> valueFields = value.fieldNames();
            while (valueFields.hasNext()) {
                String fieldName = valueFields.next();
                if (!merged.has(fieldName)) {
                    merged.set(fieldName, value.get(fieldName));
                }
            }
            return merged;
        } else {
            // TODO cant merge object and array, trow
        }

        return null;
    }

}