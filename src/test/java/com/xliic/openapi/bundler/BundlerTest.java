/*
 Copyright (c) 42Crunch Ltd. All rights reserved.
 Licensed under the GNU Affero General Public License version 3. See LICENSE.txt in the project root for license information.
*/

package com.xliic.openapi.bundler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xliic.openapi.bundler.Mapping.Location;

import org.junit.jupiter.api.Test;

public class BundlerTest {
    BundledJsonNode bundle(String dirname, String filename) throws JsonProcessingException, IOException,
            URISyntaxException, InterruptedException, ReferenceResolutionException {
        TestWorkspace workspace = new TestWorkspace(dirname);
        Parser parser = new Parser(workspace);
        Serializer serializer = new Serializer();
        Bundler bundler = new Bundler(serializer);
        Document document = parser.parse(workspace.resolve(filename));
        Mapping mapping = bundler.bundle(document);
        String json = serializer.serialize(document);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(json);
        return new BundledJsonNode(jsonNode, mapping);
    }

    @Test
    void testBundling() throws JsonProcessingException, IOException, URISyntaxException, InterruptedException,
            ReferenceResolutionException {
        BundledJsonNode bundled = bundle("multifile-petstore", "openapi.yaml");
        // check that bundled output has expected structure
        assertEquals(bundled.at("/paths/~1pets/get/summary").textValue(), "List all pets");
        assertEquals(bundled.at("/paths/~1pets/get/responses/200/content/application~1json/schema/$ref").textValue(),
                "#/components/schemas/Pets");
        assertEquals("#/components/schemas/Pet", bundled
                .at("/paths/~1pets~1{petId}/get/responses/200/content/application~1json/schema/$ref").textValue());
    }

    @Test
    void testMapping() throws JsonProcessingException, IOException, URISyntaxException, InterruptedException,
            ReferenceResolutionException {
        BundledJsonNode bundled = bundle("multifile-petstore", "openapi.yaml");
        // check that a json pointer in a bundled document can be mapped back to its
        // original file
        Location error = bundled.original("/components/schemas/Error");
        assertEquals("schemas/error.yaml", error.file);
        assertEquals("", error.pointer);
        // for pointers to entities in the main file which don't resolve
        // to an external file, return null
        assertNull(bundled.original("/servers/0/url"));
    }

    @Test
    void testCircular() throws JsonProcessingException, IOException, URISyntaxException, InterruptedException,
            ReferenceResolutionException {
        BundledJsonNode simple = bundle("circular", "simple-external.yaml");
        BundledJsonNode two = bundle("circular", "two-level.yaml");
        BundledJsonNode multiple = bundle("circular", "multiple-ref-traversal.yml");
        assertEquals("#/definitions/Foo", multiple.at("/definitions/Bar/$ref").textValue());
        assertEquals("#/definitions/User", simple.at("/definitions/User/$ref").textValue());
        assertEquals("#/definitions/User", two.at("/definitions/User/items/$ref").textValue());
    }

    @Test
    void testExceptions() throws JsonProcessingException, IOException, URISyntaxException, InterruptedException,
            ReferenceResolutionException {

        ReferenceResolutionException one = assertThrows(ReferenceResolutionException.class, () -> {
            bundle("broken", "one-ref.yaml");
        });

        ReferenceResolutionException external = assertThrows(ReferenceResolutionException.class, () -> {
            bundle("broken", "external.yaml");
        });

        assertTrue(one.file.getPath().endsWith("one-ref.yaml"));
        assertEquals("/bar", one.pointer);
        assertEquals("#/foo-foo", one.target.toString());

        // reference from external.yaml points to one-ref.yaml
        // so exception happens when processing one-ref, and
        // contains only information about one-ref.yaml
        assertTrue(external.file.getPath().endsWith("one-ref.yaml"));
        assertEquals("/bar", one.pointer);
        assertEquals("#/foo-foo", one.target.toString());
    }
}