/*
 Copyright (c) 42Crunch Ltd. All rights reserved.
 Licensed under the GNU Affero General Public License version 3. See LICENSE.txt in the project root for license information.
*/

package com.xliic.openapi.bundler;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.xliic.openapi.bundler.Inventory.Entry;

import org.junit.jupiter.api.Test;

public class InventoryTest {
    Inventory parse(String dirname, String filename) throws JsonProcessingException, IOException, URISyntaxException,
            InterruptedException, ReferenceResolutionException {
        TestWorkspace workspace = new TestWorkspace(dirname);
        Parser parser = new Parser(workspace);
        Serializer serializer = new Serializer();
        Bundler bundler = new Bundler(serializer);
        Document document = parser.parse(workspace.resolve(filename));
        bundler.crawl(document.root, document.root.node, null, new JsonPath(), new HashSet<URI>());
        return bundler.getInventory();
    }

    Inventory.Entry find(Inventory inventory, String pointer) throws UnsupportedEncodingException {
        for (Entry entry : inventory) {
            if (JsonPath.toPointer(entry.pathFromRoot).equals(pointer)) {
                return entry;
            }
        }
        return null;
    }

    @Test
    void one() throws JsonProcessingException, IOException, URISyntaxException, InterruptedException,
            ReferenceResolutionException {
        Inventory inventory = parse("simple", "one-ref.yaml");
        Entry entry = find(inventory, "/bar");
        assertEquals(1, inventory.size());
        assertEquals(false, entry.circular);
        assertEquals(false, entry.external);
        assertEquals(1, entry.depth);
        assertEquals("/foo", entry.pointer);
    }

    @Test
    void two() throws JsonProcessingException, IOException, URISyntaxException, InterruptedException,
            ReferenceResolutionException {
        Inventory inventory = parse("simple", "two-refs.yaml");
        Entry bar = find(inventory, "/bar");
        Entry baz = find(inventory, "/baz/baz");
        assertEquals(2, inventory.size());
        assertEquals(1, bar.depth);
        assertEquals(2, baz.depth);
        assertEquals("/foo", bar.pointer);
        assertEquals("/foo", baz.pointer);
        assertEquals("baz", baz.key);
        assertEquals("bar", bar.key);
        assertEquals("foofoo", bar.value.textValue());
        assertEquals("foofoo", baz.value.textValue());
        assertEquals(false, baz.external);
        assertEquals(false, bar.external);
        assertEquals(false, baz.extended);
        assertEquals(false, bar.extended);
    }

    @Test
    void indirect() throws JsonProcessingException, IOException, URISyntaxException, InterruptedException,
            ReferenceResolutionException {
        Inventory inventory = parse("simple", "indirect.yaml");
        Entry bar = find(inventory, "/bar");
        Entry baz = find(inventory, "/baz");
        Entry ext = find(inventory, "/ext");
        assertEquals(1, bar.depth);
        assertEquals(1, baz.depth);
        assertEquals(1, bar.indirections);
        assertEquals(0, baz.indirections);
        assertEquals("/foo", bar.pointer);
        assertEquals("/foo", baz.pointer);
        assertEquals(false, bar.extended);
        assertEquals(false, baz.extended);
        assertEquals(true, ext.extended);
    }

    @Test
    void refDeep() throws JsonProcessingException, IOException, URISyntaxException, InterruptedException,
            ReferenceResolutionException {
        Inventory inventory = parse("simple", "ref-deep.yaml");
        Entry bar = find(inventory, "/bar");
        Entry baz = find(inventory, "/baz");
        assertEquals("/foo", bar.pointer);
        assertEquals("/foo/foo1", baz.pointer);
        assertEquals(0, bar.indirections);
        assertEquals(1, baz.indirections);
    }

    @Test
    void external() throws JsonProcessingException, IOException, URISyntaxException, InterruptedException,
            ReferenceResolutionException {
        Inventory inventory = parse("simple", "external.yaml");
        Entry foo = find(inventory, "/foo");
        Entry bar = find(inventory, "/bar");
        Entry baz = find(inventory, "/baz");
        assertEquals(3, inventory.size());
        assertEquals(true, foo.external);
        assertEquals(true, bar.external);
        assertEquals(true, baz.external);
        assertEquals(0, foo.indirections);
        assertEquals(1, bar.indirections);
        assertEquals(1, baz.indirections);
        assertEquals("foofoo", foo.value.textValue());
        assertEquals("foofoo", bar.value.textValue());
        assertEquals("foofoo", baz.value.textValue());
    }
}