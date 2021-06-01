/*
 Copyright (c) 42Crunch Ltd. All rights reserved.
 Licensed under the GNU Affero General Public License version 3. See LICENSE.txt in the project root for license information.
*/

package com.xliic.openapi.bundler;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.xliic.common.WorkspaceException;
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

    public static void resolveReference(Parser parser, Reference reference) {
        resolveReference(parser, reference, new ArrayList<URI>());
    }

    public static void resolveReference(Parser parser, Reference reference, ArrayList<URI> visited) {

        URI ref;
        try {
            ref = new URI(reference.node.get("$ref").asText());
        } catch (URISyntaxException e) {
            reference.failure = new ReferenceResolutionFailure(
                    String.format("Failed to parse $ref: %s", e.getMessage()), reference.part.location,
                    reference.pointer.toString() + "/$ref", "", e);

            return;
        }

        Document.Part part;
        try {
            part = getPart(parser, reference.part, ref);
        } catch (WorkspaceException | DocumentLoadingException | URISyntaxException e) {
            reference.failure = new ReferenceResolutionFailure(
                    String.format("Failed to load external file: %s", e.getMessage()), reference.part.location,
                    reference.pointer.toString() + "/$ref", ref.toString(), e);
            return;
        }

        if (visited.contains(reference.getURI())) {
            resolveCircular(reference);
            return;
        }

        visited.add(reference.getURI());

        JsonPointer pointer = ref.getFragment() == null ? new JsonPointer("") : new JsonPointer(ref.getFragment());
        JsonPath path = pointer.getJsonPath();
        JsonNode resolved = part.node;
        JsonPath resolvedPath = new JsonPath();
        int indirections = 0;
        boolean circular = false;

        for (int i = 0; i < path.size(); i++) {
            String key = path.get(i);
            resolved = Util.get(resolved, key);
            resolvedPath.add(key);
            if (resolved == null) {
                reference.failure = new ReferenceResolutionFailure(
                        String.format("Failed to resolve JSON Pointer: %s", ref), reference.part.location,
                        reference.pointer.toString() + "/$ref", resolvedPath.toPointer().toString(), null);
                return;
            }

            if (isRef(resolved)) {
                Reference indirect = new Reference(part, resolved, resolvedPath.toPointer());
                resolveReference(parser, indirect, visited);
                if (indirect.isResolved()) {
                    indirections = indirections + indirect.indirections + 1;
                    resolved = indirect.resolvedValue;
                    part = indirect.resolvedPart;
                    resolvedPath = indirect.resolvedPath;
                    circular = indirect.circular;
                } else {
                    reference.failure = indirect.failure;
                    return;
                }
            }
        }

        reference.resolvedValue = resolved;
        reference.resolvedPart = part;
        reference.resolvedPath = resolvedPath;
        reference.indirections = indirections;
        reference.circular = circular;

    }

    private static Reference resolveCircular(Reference reference) {
        reference.circular = true;
        reference.resolvedPart = reference.part;
        reference.resolvedPath = reference.pointer.getJsonPath();
        reference.resolvedValue = reference.node;
        return reference;
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
            throws DocumentLoadingException, URISyntaxException, WorkspaceException {
        Document.Part targetPart = Document.getTargetPart(part, target);
        if (targetPart == null) {
            URI targetFileUri = Document.getTargetPartUri(part, target);
            try {
                JsonNode root = parser.readTree(targetFileUri);
                targetPart = part.getDocument().createPart(targetFileUri, root);
            } catch (WorkspaceException e) {
                throw e;
            } catch (Exception e) {
                throw new DocumentLoadingException(String.format("Failed to load external reference: %s", e),
                        targetFileUri);
            }
        }
        return targetPart;
    }

}