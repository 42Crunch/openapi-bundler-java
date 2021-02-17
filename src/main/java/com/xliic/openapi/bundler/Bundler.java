/*
 Copyright (c) 42Crunch Ltd. All rights reserved.
 Licensed under the GNU Affero General Public License version 3. See LICENSE.txt in the project root for license information.
*/

package com.xliic.openapi.bundler;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.xliic.openapi.bundler.Inventory.Entry;

public class Bundler {

    private Inventory inventory = new Inventory();
    private List<ReferenceResolutionFailure> failures = new ArrayList<ReferenceResolutionFailure>();
    private Serializer serializer;
    private Parser parser;

    public Bundler(Parser parser, Serializer serializer) throws JsonProcessingException, IOException {
        this.serializer = serializer;
        this.parser = parser;
    }

    public Mapping bundle(Document document)
            throws URISyntaxException, JsonProcessingException, IOException, BundlingException {
        crawl(document.root, document.root.node, null, new JsonPath(), new JsonPath(), new HashSet<>());
        if (failures.size() == 0) {
            return remap(document);
        }
        throw new BundlingException("Failed to bundle OpenAPI file", failures);
    }

    public void crawl(final Document.Part part, final JsonNode parent, String key, JsonPath path, JsonPath pathFromRoot,
            HashSet<URI> visited) throws URISyntaxException, JsonProcessingException, IOException {
        final JsonNode node = key == null ? parent : Util.get(parent, key);

        if (Resolver.isRef(node)) {
            Reference reference = new Reference(part, node, path.toPointer());
            Resolver.resolveReference(parser, reference);
            if (reference.isResolved()) {
                inventory.add(parent, key, pathFromRoot, reference);
                // don't crawl unresolved and circular references
                if (reference.isResolved() && !reference.circular && !visited.contains(reference.getResolvedURI())) {
                    visited.add(reference.getResolvedURI());
                    crawl(reference.resolvedPart, reference.resolvedValue, null, reference.resolvedPath, pathFromRoot,
                            visited);
                }
            } else {
                failures.add(reference.failure);
            }

        } else if (node.isObject()) {
            Iterator<String> iterator = node.fieldNames();
            while (iterator.hasNext()) {
                String fieldName = iterator.next();
                crawl(part, node, fieldName, path.withKey(fieldName), pathFromRoot.withKey(fieldName), visited);
            }
        } else if (node.isArray()) {
            for (int i = 0; i < node.size(); i++) {
                String index = Integer.toString(i);
                crawl(part, node, index, path.withKey(index), pathFromRoot.withKey(index), visited);
            }
        }
    }

    private Mapping remap(Document document) throws UnsupportedEncodingException {
        Mapping mapping = new Mapping();
        inventory.sort();

        URI file = null;
        JsonPath path = null;
        JsonPointer pointer = null;
        JsonPath pathFromRoot = null;
        URI filename = null;

        for (Entry entry : inventory) {
            if (!entry.external) {
                Util.setRef(entry.ref, entry.pointer);
            } else if (entry.file.equals(file) && entry.pointer.equals(pointer)) {
                Util.setRef(entry.ref, pathFromRoot.toPointer());
            } else if (entry.file.equals(file) && entry.path.isSubPathOf(path)) {
                List<String> tail = entry.path.subList(path.size(), entry.path.size());
                Util.setRef(entry.ref, pathFromRoot.withKeys(tail).toPointer());
            } else {
                file = entry.file;
                filename = entry.part.getFilename();
                pointer = entry.pointer;
                path = entry.path;
                pathFromRoot = entry.pathFromRoot;

                JsonNode value = entry.value;
                if (Resolver.isExtendedRef(entry.ref)) {
                    value = Resolver.mergeExtendedRef(serializer, entry.ref, value);
                }

                if (entry.circular) {
                    Util.setRef(entry.ref, pathFromRoot.toPointer());
                }

                if (entry.path.size() >= 3 && entry.path.get(0).equals("components")) {
                    JsonPath remapped = new JsonPath("components", entry.path.get(1),
                            externalEntryToComponentName(entry.part, entry.path));
                    if (entry.path.size() > 3) {
                        remapped.addAll(entry.path.subList(3, entry.path.size()));
                    }
                    Util.set(serializer, document.root.node, remapped, value);
                    pathFromRoot = remapped;
                    Util.setRef(entry.ref, pathFromRoot.toPointer());
                    insertMapping(mapping, remapped, filename, pointer);
                } else {
                    Util.set(entry.parent, entry.key, value);
                    insertMapping(mapping, entry.pathFromRoot, filename, pointer);
                }
            }
        }
        return mapping;
    }

    private void insertMapping(Mapping mapping, JsonPath path, URI filename, JsonPointer pointer) {
        Mapping current = mapping;
        for (String key : path) {
            if (!current.children.containsKey(key)) {
                current.children.put(key, new Mapping());
            }
            current = current.children.get(key);
        }
        // TODO check that current.value is empty
        current.value = new Mapping.Location(filename.getPath(), pointer);
    }

    private String externalEntryToComponentName(Document.Part part, JsonPath path) throws UnsupportedEncodingException {
        String name = part.getFilename().toString() + path.toPointer();
        // FIXME to workaround broken path handling in assessd
        return name.replaceAll("/", "-").replaceAll("#", "-");
    }

    public Inventory getInventory() {
        return inventory;
    }
}