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
import com.xliic.openapi.bundler.Document.Part;

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

    public static JsonPointer resolveReference(Parser parser, Document.Part part, JsonNode ref, JsonPath refPath)
            throws URISyntaxException, UnsupportedEncodingException, ReferenceResolutionException {
        ArrayList<JsonNode> visited = new ArrayList<>();
        return resolveReference(parser, part, ref, refPath, visited);
    }

    private static JsonPointer resolveReference(Parser parser, Document.Part part, JsonNode ref, JsonPath refPath,
            ArrayList<JsonNode> visited) throws ReferenceResolutionException {

        String rawTarget = ref.get("$ref").asText();
        try {
            URI target = new URI(rawTarget);

            Document.Part targetPart = getPart(parser, part, target);
            JsonPointer pointer = new JsonPointer(targetPart, new URI(null, null, target.getFragment()));

            if (visited.contains(ref)) {
                pointer.circular = true;
                pointer.resolvedPart = pointer.part;
                pointer.resolvedPath = pointer.path;
                return pointer;
            }
            visited.add(ref);

            resolvePointer(parser, pointer, visited);
            return pointer;
        } catch (PointerResolutionException e) {
            throw new ReferenceResolutionException(String.format("Failed to resolve JSON Pointer: ", e.pointer),
                    part.location, refPath.toPointer(), rawTarget);
        } catch (DocumentLoadingException e) {
            throw new ReferenceResolutionException(e.getMessage(), part.location, refPath.toPointer(), rawTarget);
        } catch (URISyntaxException e) {
            throw new ReferenceResolutionException(String.format("Failed to parse reference: %s", e.getMessage()),
                    part.location, refPath.toPointer(), rawTarget);
        }
    }

    private static void resolvePointer(Parser parser, JsonPointer pointer, ArrayList<JsonNode> visited)
            throws PointerResolutionException, ReferenceResolutionException {
        pointer.resolvedPart = pointer.part;
        pointer.resolvedValue = pointer.part.node;
        pointer.resolvedPath = pointer.path;
        JsonPath refPath = new JsonPath();

        for (int i = 0; i < pointer.path.size(); i++) {
            String key = pointer.path.get(i);
            refPath.add(key);
            pointer.resolvedValue = Util.get(pointer.resolvedValue, key);
            if (pointer.resolvedValue == null) {
                throw new PointerResolutionException(pointer.target.getFragment());
            }

            if (resolveIfRef(parser, pointer, refPath, visited)) {
                pointer.resolvedPath.addAll(pointer.path.subList(i + 1, pointer.path.size()));
            }

        }

        // if pointer.path is empty, still try resolveIfRef
        resolveIfRef(parser, pointer, refPath, visited);
    }

    private static boolean resolveIfRef(Parser parser, JsonPointer pointer, JsonPath refPath,
            ArrayList<JsonNode> visited) throws ReferenceResolutionException {
        if (isRef(pointer.resolvedValue)) {
            JsonPointer resolved = resolveReference(parser, pointer.resolvedPart, pointer.resolvedValue, refPath,
                    visited);
            pointer.indirections = pointer.indirections + resolved.getIndirections() + 1;
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

    private static Part getPart(Parser parser, Document.Part part, URI target)
            throws DocumentLoadingException, URISyntaxException {
        Document.Part targetPart = Document.getTargetPart(part, target);
        if (targetPart == null) {
            URI targetFileUri = Document.getTargetPartUri(part, target);
            try {
                JsonNode root = parser.readTree(targetFileUri);
                targetPart = part.getDocument().createPart(targetFileUri, root);
            } catch (Exception e) {
                throw new DocumentLoadingException(String.format("Failed to load external reference: %s", e),
                        targetFileUri);
            }
        }
        return targetPart;
    }

}