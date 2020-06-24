/*
 Copyright (c) 42Crunch Ltd. All rights reserved.
 Licensed under the GNU Affero General Public License version 3. See LICENSE.txt in the project root for license information.
*/

package com.xliic.openapi.bundler;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.xliic.openapi.bundler.Inventory.Entry;

public class Bundler {

    private Inventory inventory = new Inventory();
    private Serializer serializer;

    public Bundler(Serializer serializer) throws JsonProcessingException, IOException {
        this.serializer = serializer;
    }

    public Mapping bundle(Document document) throws URISyntaxException, JsonProcessingException, IOException {
        crawl(document.root, document.root.node, null, new JsonPath());
        return remap(document);
    }

    public void crawl(final Document.Part part, final JsonNode parent, String key, JsonPath pathFromRoot)
            throws URISyntaxException, JsonProcessingException, IOException {
        final JsonNode node = key == null ? parent : Util.get(parent, key);
        if (Resolver.isRef(node)) {
            addToInventory(part, parent, key, pathFromRoot);
        } else if (node.isObject()) {
            Iterator<String> iterator = node.fieldNames();
            while (iterator.hasNext()) {
                String fieldName = iterator.next();
                crawl(part, node, fieldName, pathFromRoot.withKey(fieldName));
            }
        } else if (node.isArray()) {
            for (int i = 0; i < node.size(); i++) {
                String index = Integer.toString(i);
                crawl(part, node, index, pathFromRoot.withKey(index));
            }
        }
    }

    private Mapping remap(Document document) throws UnsupportedEncodingException {
        Mapping mapping = new Mapping();
        inventory.sort();

        String file = null, pointer = null, pathFromRoot = null;
        JsonPath path = null;
        URI filename = null;

        for (Entry entry : inventory) {
            if (!entry.external) {
                Util.setRef(entry.ref, "#" + entry.pointer);
            } else if (entry.file.equals(file) && entry.pointer.equals(pointer)) {
                Util.setRef(entry.ref, "#" + pathFromRoot);
            } else if (entry.file.equals(file) && entry.path.isSubPathOf(path)) {
                Util.setRef(entry.ref, "#" + pathFromRoot + entry.pointer.substring(pointer.length()));
            } else {
                file = entry.file;
                filename = entry.part.getFilename();
                pointer = entry.pointer;
                path = entry.path;
                pathFromRoot = entry.pathFromRoot.toPointer();

                JsonNode value = entry.value;
                if (Resolver.isExtendedRef(entry.ref)) {
                    value = Resolver.mergeExtendedRef(serializer, entry.ref, value);
                }

                if (entry.path.size() >= 3 && entry.path.get(0).equals("components")) {
                    JsonPath remapped = new JsonPath("components", entry.path.get(1),
                            externalEntryToComponentName(entry.part, entry.path));
                    if (entry.path.size() > 3) {
                        remapped.addAll(entry.path.subList(3, entry.path.size()));
                    }
                    Util.set(serializer, document.root.node, remapped, value);
                    pathFromRoot = remapped.toPointer();
                    Util.setRef(entry.ref, "#" + pathFromRoot);
                    insertMapping(mapping, remapped, filename, pointer);
                } else {
                    Util.set(entry.parent, entry.key, value);
                    insertMapping(mapping, entry.pathFromRoot, filename, pointer);
                }
            }
        }
        return mapping;
    }

    private void insertMapping(Mapping mapping, JsonPath path, URI filename, String pointer) {
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

    private void addToInventory(Document.Part part, JsonNode parent, String key, JsonPath pathFromRoot)
            throws URISyntaxException, JsonProcessingException, IOException {

        JsonNode ref = key == null ? parent : Util.get(parent, key);
        JsonPointer pointer = Resolver.resolveReference(part, ref);
        inventory.add(new Inventory.Entry(parent, key, ref, pointer.getValue(), pathFromRoot, pointer.getFile(),
                pointer.getPointer(), pointer.getPath(), pointer.getIndirections(), pointer.getPart()));
        crawl(pointer.getPart(), pointer.getValue(), null, pathFromRoot);
    }

}