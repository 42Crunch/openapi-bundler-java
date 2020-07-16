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
        return document;
    }

    public JsonNode readTree(URI uri)
            throws JsonMappingException, JsonProcessingException, IOException, InterruptedException {
        if (uri.getPath().toLowerCase().endsWith(".json")) {
            return jsonMapper.readTree(workspace.read(uri));
        }
        return yamlMapper.readTree(workspace.read(uri));
    }
}