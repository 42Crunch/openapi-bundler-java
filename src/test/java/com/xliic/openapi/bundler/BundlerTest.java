/*
 Copyright (c) 42Crunch Ltd. All rights reserved.
 Licensed under the GNU Affero General Public License version 3. See LICENSE.txt in the project root for license information.
*/

package com.xliic.openapi.bundler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.net.URISyntaxException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xliic.openapi.bundler.Mapping.Location;

import org.junit.jupiter.api.Test;

public class BundlerTest {
    BundledJsonNode bundle(String dirname, String filename)
            throws JsonProcessingException, IOException, URISyntaxException, InterruptedException, BundlingException {
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

    ReferenceResolutionFailure findFailure(BundlingException ex, String file, String pointer) {
        for (ReferenceResolutionFailure failure : ex.getFailures()) {
            if (failure.sourceFile.toString().endsWith(file) && failure.sourcePointer.equals(pointer)) {
                return failure;
            }
        }
        return null;
    }

    @Test
    void testBundling()
            throws JsonProcessingException, IOException, URISyntaxException, InterruptedException, BundlingException {
        BundledJsonNode bundled = bundle("multifile-petstore", "openapi.yaml");
        // check that bundled output has expected structure
        assertEquals("List all pets", bundled.at("/paths/~1pets/get/summary").textValue());
        assertEquals("#/components/schemas/Pets",
                bundled.at("/paths/~1pets/get/responses/200/content/application~1json/schema/$ref").textValue());
        assertEquals("#/components/schemas/Pet", bundled
                .at("/paths/~1pets~1{petId}/get/responses/200/content/application~1json/schema/$ref").textValue());
    }

    @Test
    void testMapping()
            throws JsonProcessingException, IOException, URISyntaxException, InterruptedException, BundlingException {
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
    void testCircular()
            throws JsonProcessingException, IOException, URISyntaxException, InterruptedException, BundlingException {
        BundledJsonNode simple = bundle("circular", "simple-external.yaml");
        BundledJsonNode two = bundle("circular", "two-level.yaml");
        BundledJsonNode multiple = bundle("circular", "multiple-ref-traversal.yml");

        assertEquals("#/definitions/User", simple.at("/definitions/User/$ref").textValue());
        assertEquals("#/definitions/User%7B%7D", simple.at("/definitions/User{}/$ref").textValue());
        assertEquals("#/definitions/User", two.at("/definitions/User/items/$ref").textValue());
        assertEquals("#/definitions/Foo", multiple.at("/definitions/Bar/$ref").textValue());
    }

    @Test
    void testExceptionsRef()
            throws JsonProcessingException, IOException, URISyntaxException, InterruptedException, BundlingException {

        BundlingException ex = assertThrows(BundlingException.class, () -> {
            bundle("broken", "one-ref.yaml");
        });

        assertEquals(1, ex.getFailures().size());

        ReferenceResolutionFailure re = ex.getFailures().get(0);

        assertTrue(re.sourceFile.getPath().endsWith("one-ref.yaml"));
        assertEquals("/bar/$ref", re.sourcePointer);
        assertEquals("/foo-foo", re.target);

    }

    @Test
    void testExceptionExternal()
            throws JsonProcessingException, IOException, URISyntaxException, InterruptedException, BundlingException {

        BundlingException ex = assertThrows(BundlingException.class, () -> {
            bundle("broken", "external.yaml");
        });

        assertEquals(1, ex.getFailures().size());

        ReferenceResolutionFailure re = ex.getFailures().get(0);

        // reference from external.yaml points to one-ref.yaml
        // so exception happens when processing one-ref, and
        // contains only information about one-ref.yaml
        assertTrue(re.sourceFile.getPath().endsWith("one-ref.yaml"));
        assertEquals("/bar/$ref", re.sourcePointer);
        assertEquals("/foo-foo", re.target);
    }

    @Test
    void testExceptionExternalDeep()
            throws JsonProcessingException, IOException, URISyntaxException, InterruptedException, BundlingException {

        BundlingException ex = assertThrows(BundlingException.class, () -> {
            bundle("broken", "external-deep.yaml");
        });

        assertEquals(1, ex.getFailures().size());

        ReferenceResolutionFailure re = ex.getFailures().get(0);

        // bundling starts at external-deep.yaml, goes to external-deep-two.yaml
        // and fails to resolve reference "external-deep-two.yaml#/baz"
        // the path where the reference occurs is /bar
        assertTrue(re.sourceFile.getPath().endsWith("external-deep-one.yaml"));
        assertEquals("/baz", re.target);
        assertEquals("/bar/$ref", re.sourcePointer);
    }

    @Test
    void testExceptionExternalMultifile()
            throws JsonProcessingException, IOException, URISyntaxException, InterruptedException, BundlingException {

        BundlingException ex = assertThrows(BundlingException.class, () -> {
            bundle("broken/multi-file-petstore", "openapi.yaml");
        });

        assertEquals(3, ex.getFailures().size());

        ReferenceResolutionFailure f1 = findFailure(ex, "openapi.yaml", "/paths/~1bar/$ref");
        assertEquals("Failed to resolve JSON Pointer: #/paths/foo", f1.message);

        ReferenceResolutionFailure f2 = findFailure(ex, "schemas/index.yaml", "/Error/$ref");
        assertTrue(f2.message.contains("Failed to load external file:"));

        ReferenceResolutionFailure f3 = findFailure(ex, "paths/pets/post.yaml",
                "/responses/default/content/application~1json/schema/$ref");
        assertTrue(f3.message.contains("Failed to load external file:"));
    }

    @Test
    void testExceptionsMissing()
            throws JsonProcessingException, IOException, URISyntaxException, InterruptedException, BundlingException {

        BundlingException ex = assertThrows(BundlingException.class, () -> {
            bundle("broken", "external-not-exist.yaml");
        });

        assertEquals(1, ex.getFailures().size());

        ReferenceResolutionFailure re = ex.getFailures().get(0);

        assertTrue(re.sourceFile.getPath().endsWith("external-not-exist.yaml"));
        assertTrue(re.message.contains("Failed to load external reference: java.nio.file.NoSuchFileException"));
        assertEquals("one-ref-foo.yaml#/bar", re.target);
        assertEquals("/bar/$ref", re.sourcePointer);

    }

    @Test
    void testExceptionURI()
            throws JsonProcessingException, IOException, URISyntaxException, InterruptedException, BundlingException {

        BundlingException ex = assertThrows(BundlingException.class, () -> {
            bundle("broken", "bad-uri.yaml");
        });

        assertEquals(1, ex.getFailures().size());

        ReferenceResolutionFailure re = ex.getFailures().get(0);

        assertEquals("/bar/$ref", re.sourcePointer);
        assertTrue(re.sourceFile.getPath().endsWith("bad-uri.yaml"));
        assertTrue(re.message.contains("Failed to parse $ref: Illegal character"));
    }

    @Test
    void testSwaggerRemapEmpty()
            throws JsonProcessingException, IOException, URISyntaxException, InterruptedException, BundlingException {
        // make sure that components are mapped to
        // nodes under /definitions and /components
        // even if the main files dont' have these nodes
        BundledJsonNode bundled20 = bundle("minimal", "swagger20.yaml");
        assertNotNull(bundled20.at("/definitions/FooSchema/type"));

        BundledJsonNode bundled30 = bundle("minimal", "openapi30.yaml");
        assertNotNull(bundled30.at("/components/schemas/FooSchema/type"));
    }
}