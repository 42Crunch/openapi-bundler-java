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
import com.xliic.common.ContentType;
import com.xliic.common.Workspace;
import com.xliic.common.WorkspaceContent;
import com.xliic.common.WorkspaceException;

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
            throws JsonProcessingException, IOException, URISyntaxException, InterruptedException, WorkspaceException {
        JsonNode root = readTree(uri);
        Document document = new Document(uri, root);
        return document;
    }

    public JsonNode readTree(URI uri) throws JsonMappingException, JsonProcessingException, IOException,
            InterruptedException, WorkspaceException {
        WorkspaceContent content = workspace.read(uri);
        if (content.type == ContentType.JSON) {
            return jsonMapper.readTree(content.data);
        } else if (content.type == ContentType.YAML) {
            return yamlMapper.readTree(content.data);
        }
        throw new WorkspaceException(String.format("Unknown content type", uri));
    }
}