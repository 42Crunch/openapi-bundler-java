package com.xliic.openapi.bundler;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.HashSet;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.xliic.openapi.bundler.Inventory.Entry;

import org.junit.jupiter.api.Test;

public class InventoryTest {
    Inventory parse(String dirname, String filename)
            throws JsonProcessingException, IOException, URISyntaxException, InterruptedException {
        TestWorkspace workspace = new TestWorkspace(dirname);
        Parser parser = new Parser(workspace);
        Serializer serializer = new Serializer();
        Bundler bundler = new Bundler(serializer);
        Document document = parser.parse(workspace.resolve(filename));
        bundler.crawl(document.root, document.root.node, null, new JsonPath(), new HashSet<String>());
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
    void one() throws JsonProcessingException, IOException, URISyntaxException, InterruptedException {
        Inventory inventory = parse("simple", "one-ref.yaml");
        Entry entry = find(inventory, "/bar");
        assertEquals(1, inventory.size());
        assertEquals(false, entry.circular);
        assertEquals(false, entry.external);
        assertEquals(1, entry.depth);
        assertEquals("/foo", entry.pointer);
    }

    @Test
    void two() throws JsonProcessingException, IOException, URISyntaxException, InterruptedException {
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
    void indirect() throws JsonProcessingException, IOException, URISyntaxException, InterruptedException {
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
    void refDeep() throws JsonProcessingException, IOException, URISyntaxException, InterruptedException {
        Inventory inventory = parse("simple", "ref-deep.yaml");
        Entry bar = find(inventory, "/bar");
        Entry baz = find(inventory, "/baz");
        assertEquals("/foo", bar.pointer);
        assertEquals("/foo/foo1", baz.pointer);
        assertEquals(0, bar.indirections);
        assertEquals(1, baz.indirections);
    }
}