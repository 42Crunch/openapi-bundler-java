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

    public static Reference resolveReference(Parser parser, Document.Part part, JsonNode ref, JsonPath refPath)
            throws URISyntaxException, UnsupportedEncodingException, ReferenceResolutionException {
        return resolveReference(parser, part, ref, refPath, new ArrayList<URI>());
    }

    private static Reference resolveReference(Parser parser, Document.Part part, JsonNode refNode, JsonPath refPath,
            ArrayList<URI> visited) throws ReferenceResolutionException {
        Reference reference = createReference(parser, part, refNode.get("$ref").asText(), refPath);

        if (visited.contains(reference.getSourceURI())) {
            return resolveCircular(reference);
        }
        visited.add(reference.getSourceURI());

        JsonPath path = new JsonPath();
        for (int i = 0; i < reference.path.size(); i++) {
            String key = reference.path.get(i);
            path.add(key);
            reference.resolvedValue = Util.get(reference.resolvedValue, key);
            if (reference.resolvedValue == null) {
                throw new ReferenceResolutionException(
                        String.format("Failed to resolve JSON Pointer \"%s\"", reference.targetPointer),
                        reference.sourcePart.location, reference.sourcePointer.getValue(),
                        reference.targetPointer.getURI().toString());
            }

            if (resolveIfRef(parser, reference, path, visited)) {
                reference.resolvedPath.addAll(reference.path.subList(i + 1, reference.path.size()));
            }
        }

        // if pointer.path is empty, still try resolveIfRef
        resolveIfRef(parser, reference, path, visited);

        return reference;
    }

    private static Reference createReference(Parser parser, Document.Part part, String rawTarget, JsonPath refPath)
            throws ReferenceResolutionException {
        try {
            URI target = new URI(rawTarget);
            Document.Part targetPart = getPart(parser, part, target);
            JsonPointer targetPointer = target.getFragment() == null ? new JsonPointer("")
                    : new JsonPointer(target.getFragment());

            Reference reference = new Reference(part, refPath.toPointer(), targetPart, targetPointer);
            reference.resolvedPart = reference.targetPart;
            reference.resolvedValue = reference.targetPart.node;
            reference.resolvedPath = reference.path;

            return reference;

        } catch (DocumentLoadingException e) {
            throw new ReferenceResolutionException(e.getMessage(), part.location, refPath.toPointer().getValue(),
                    rawTarget);
        } catch (URISyntaxException e) {
            throw new ReferenceResolutionException(String.format("Failed to parse reference: %s", e.getMessage()),
                    part.location, refPath.toPointer().getValue(), rawTarget);
        }
    }

    private static Reference resolveCircular(Reference reference) {
        reference.circular = true;
        reference.resolvedPart = reference.targetPart;
        reference.resolvedPath = reference.path;
        return reference;
    }

    private static boolean resolveIfRef(Parser parser, Reference pointer, JsonPath refPath, ArrayList<URI> visited)
            throws ReferenceResolutionException {
        if (isRef(pointer.resolvedValue)) {
            Reference resolved = resolveReference(parser, pointer.resolvedPart, pointer.resolvedValue, refPath,
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
            // TODO cant merge object and array, throw
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