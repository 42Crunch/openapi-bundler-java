/*
 Copyright (c) 42Crunch Ltd. All rights reserved.
 Licensed under the GNU Affero General Public License version 3. See LICENSE.txt in the project root for license information.
*/

package com.xliic.openapi.bundler;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.xliic.common.Workspace;

public class Parser {
    private ObjectMapper jsonMapper;
    private ObjectMapper yamlMapper;
    private Workspace workspace;

    public Parser(Workspace workspace) {
        this.jsonMapper = new ObjectMapper();
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
        this.workspace = workspace;
    }

    public Document parse(URI uri)
            throws JsonProcessingException, IOException, URISyntaxException, InterruptedException {
        JsonNode root = readTree(uri);
        Document document = new Document(uri, root);
        crawl(document, document.root, document.root.node);
        return document;
    }

    private JsonNode readTree(URI uri)
            throws JsonMappingException, JsonProcessingException, IOException, InterruptedException {
        if (uri.getPath().toLowerCase().endsWith(".json")) {
            return jsonMapper.readTree(workspace.read(uri));
        }
        return yamlMapper.readTree(workspace.read(uri));
    }

    public void crawl(final Document document, final Document.Part part, final JsonNode node)
            throws JsonProcessingException, IOException, URISyntaxException {
        if (Resolver.isExternalRef(node)) {
            Document.Part newPart = loadNewPart(document, part, node);
            if (newPart != null) {
                crawl(document, newPart, newPart.node);
            }
        } else if (node.isContainerNode()) {
            for (JsonNode child : node) {
                crawl(document, part, child);
            }
        }
    }

    public Document.Part loadNewPart(Document document, Document.Part part, JsonNode node)
            throws JsonProcessingException, IOException, URISyntaxException {
        String ref = node.get("$ref").asText();
        try {
            URI refUri = new URI(ref);
            URI fileUri = Document.getTargetPartUri(part, refUri);
            if (document.parts.containsKey(fileUri)) {
                return null;
            }
            JsonNode root = readTree(fileUri);
            return document.createPart(fileUri, root);
        } catch (Exception e) {
            throw new RuntimeException(
                    String.format("Failed to load document referred by '%s' in '%s': %s", ref, part.location, e));
        }
    }
}