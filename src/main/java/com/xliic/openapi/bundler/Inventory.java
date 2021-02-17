/*
 Copyright (c) 42Crunch Ltd. All rights reserved.
 Licensed under the GNU Affero General Public License version 3. See LICENSE.txt in the project root for license information.
*/

package com.xliic.openapi.bundler;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;

import com.fasterxml.jackson.databind.JsonNode;

public class Inventory implements Iterable<Inventory.Entry> {

    ArrayList<Inventory.Entry> inventory = new ArrayList<Inventory.Entry>();

    public void add(JsonNode parent, String key, JsonPath pathFromRoot, Reference reference)
            throws UnsupportedEncodingException {

        Entry entry = new Inventory.Entry(parent, key, reference.node, reference.resolvedValue, pathFromRoot,
                reference.getResolvedFileURI(), reference.getResolvedPointer(), reference.resolvedPath,
                reference.indirections, reference.circular, reference.resolvedPart);

        Entry existingEntryToRemove = null;
        for (Entry existing : inventory) {
            if (existing.parent == entry.parent && existing.key.equals(entry.key)) {
                if (existing.depth > entry.depth || existing.indirections > entry.indirections) {
                    existingEntryToRemove = entry;
                    break;
                } else {
                    return;
                }
            }
        }

        if (existingEntryToRemove != null) {
            inventory.remove(existingEntryToRemove);
        }

        inventory.add(entry);
    }

    @Override
    public Iterator<Inventory.Entry> iterator() {
        return inventory.iterator();
    }

    public int size() {
        return inventory.size();
    }

    public void sort() {
        inventory.sort(new EntryComparator());
    }

    static class EntryComparator implements Comparator<Entry>, Serializable {

        private static final long serialVersionUID = 1L;

        @Override
        public int compare(Entry a, Entry b) {
            if (!a.file.equals(b.file)) {
                return a.file.compareTo(b.file);
            } else if (!a.pointer.equals(b.pointer)) {
                return a.pointer.compareTo(b.pointer);
            } else if (a.indirections != b.indirections) {
                // TODO test that lower indirections come first
                return Integer.compare(a.indirections, b.indirections);
            } else if (a.circular != b.circular) {
                // TODO check that circular pointers come first
                return Boolean.compare(a.circular, b.circular);
            } else if (a.depth != b.depth) {
                // TODO test that lower depth come first
                return Integer.compare(a.depth, b.depth);
            } else if (a.extended != b.extended) {
                // If the $ref extends the resolved value, then sort it lower than other $refs
                // that don't extend the value
                return a.extended ? +1 : -1;
            }

            return 0;
        }

    }

    public static class Entry {
        Document.Part part;
        JsonNode ref;
        JsonNode parent;
        JsonNode value;
        String key;
        JsonPath pathFromRoot;
        JsonPath path;
        JsonPointer pointer;
        URI file;
        int depth;
        int indirections;
        boolean extended;
        boolean external;
        boolean circular;

        public Entry(JsonNode parent, String key, JsonNode ref, JsonNode value, JsonPath pathFromRoot, URI file,
                JsonPointer pointer, JsonPath path, int indirections, boolean circular, Document.Part part) {
            this.ref = ref;
            this.parent = parent;
            this.value = value;
            this.key = key;
            this.pathFromRoot = pathFromRoot;
            this.part = part;
            this.depth = pathFromRoot.size();
            this.indirections = indirections;
            this.pointer = pointer;
            this.circular = circular;
            this.path = path;
            this.file = file;
            this.extended = Resolver.isExtendedRef(ref);
            this.external = !file.equals(part.getDocumentFile());
        }
    }
}
