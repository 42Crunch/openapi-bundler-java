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
        Bundler bundler = new Bundler(parser, serializer);
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
        assertEquals("List all pets", bundled.at("/paths/~1pets/get/summary").textValue());
        assertEquals("#/components/schemas/Pets",
                bundled.at("/paths/~1pets/get/responses/200/content/application~1json/schema/$ref").textValue());
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

        assertEquals("#/definitions/User", simple.at("/definitions/User/$ref").textValue());
        assertEquals("#/definitions/User%7B%7D", simple.at("/definitions/User{}/$ref").textValue());
        assertEquals("#/definitions/User", two.at("/definitions/User/items/$ref").textValue());
        assertEquals("#/definitions/Foo", multiple.at("/definitions/Bar/$ref").textValue());
    }

    @Test
    void testExceptionsRef() throws JsonProcessingException, IOException, URISyntaxException, InterruptedException,
            ReferenceResolutionException {

        ReferenceResolutionException ex = assertThrows(ReferenceResolutionException.class, () -> {
            bundle("broken", "one-ref.yaml");
        });

        assertTrue(ex.sourceFile.getPath().endsWith("one-ref.yaml"));
        assertEquals("/bar/$ref", ex.sourcePointer);
        assertEquals("#/foo-foo", ex.target);

    }

    @Test
    void testExceptionExternal() throws JsonProcessingException, IOException, URISyntaxException, InterruptedException,
            ReferenceResolutionException {

        ReferenceResolutionException ex = assertThrows(ReferenceResolutionException.class, () -> {
            bundle("broken", "external.yaml");
        });

        // reference from external.yaml points to one-ref.yaml
        // so exception happens when processing one-ref, and
        // contains only information about one-ref.yaml
        assertTrue(ex.sourceFile.getPath().endsWith("one-ref.yaml"));
        assertEquals("/bar/$ref", ex.sourcePointer);
        assertEquals("#/foo-foo", ex.target);
    }

    @Test
    void testExceptionExternalDeep() throws JsonProcessingException, IOException, URISyntaxException,
            InterruptedException, ReferenceResolutionException {

        ReferenceResolutionException ex = assertThrows(ReferenceResolutionException.class, () -> {
            bundle("broken", "external-deep.yaml");
        });

        // bundling starts at external-deep.yaml, goes to external-deep-two.yaml
        // and fails to resolve reference "external-deep-two.yaml#/baz"
        // the path where the reference occurs is /bar
        assertTrue(ex.sourceFile.getPath().endsWith("external-deep-one.yaml"));
        assertEquals("#/baz", ex.target);
        assertEquals("/bar/$ref", ex.sourcePointer);
    }

    @Test
    void testExceptionsMissing() throws JsonProcessingException, IOException, URISyntaxException, InterruptedException,
            ReferenceResolutionException {

        ReferenceResolutionException ex = assertThrows(ReferenceResolutionException.class, () -> {
            bundle("broken", "external-not-exist.yaml");
        });

        assertTrue(ex.sourceFile.getPath().endsWith("external-not-exist.yaml"));
        assertTrue(ex.getMessage().startsWith("Failed to load external reference: java.nio.file.NoSuchFileException"));
        assertEquals("one-ref-foo.yaml#/bar", ex.target);
        assertEquals("/bar", ex.sourcePointer);

    }

    @Test
    void testExceptionURI() throws JsonProcessingException, IOException, URISyntaxException, InterruptedException,
            ReferenceResolutionException {

        ReferenceResolutionException ex = assertThrows(ReferenceResolutionException.class, () -> {
            bundle("broken", "bad-uri.yaml");
        });

        assertEquals("/bar", ex.sourcePointer);
        assertTrue(ex.sourceFile.getPath().endsWith("bad-uri.yaml"));
        assertTrue(ex.getMessage().startsWith("Failed to parse reference: Illegal character"));
    }
}