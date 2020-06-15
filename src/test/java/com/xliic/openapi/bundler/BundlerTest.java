package com.xliic.openapi.bundler;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.net.URISyntaxException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

public class BundlerTest {
    JsonNode bundle(String dirname, String filename)
            throws JsonProcessingException, IOException, URISyntaxException, InterruptedException {
        TestWorkspace workspace = new TestWorkspace(dirname);
        Parser parser = new Parser(workspace);
        Serializer serializer = new Serializer();
        Bundler bundler = new Bundler(serializer);
        Document document = parser.parse(workspace.absolutize(filename));
        bundler.bundle(document);
        String json = serializer.serialize(document);
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readTree(json);
    }

    @Test
    void shouldAnswerWithTrue() throws JsonProcessingException, IOException, URISyntaxException, InterruptedException {
        JsonNode bundled = bundle("multifile-petstore", "openapi.yaml");

        // check that bundled output has expected structure
        assertEquals(bundled.at("/paths/~1pets/get/summary").asText(), "List all pets");
        assertEquals(bundled.at("/paths/~1pets/get/responses/200/content/application~1json/schema/$ref").asText(),
                "#/components/schemas/Pets");
    }
}