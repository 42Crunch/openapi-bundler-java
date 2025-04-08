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
import com.fasterxml.jackson.dataformat.yaml.YAMLFactoryBuilder;

import org.yaml.snakeyaml.LoaderOptions;

import com.xliic.common.ContentType;
import com.xliic.common.Workspace;
import com.xliic.common.WorkspaceContent;
import com.xliic.common.WorkspaceException;

public class Parser {
    private ObjectMapper jsonMapper;
    private ObjectMapper yamlMapper;
    private Workspace workspace;

    public static class Options {
        private int maxYamlCodepoints;

        public Options() {
            // Default to 20MB or more (limit is in code points)
            this.maxYamlCodepoints = 20 * 1024 * 1024;
        }

        public int getMaxYamlCodepoints() {
            return maxYamlCodepoints;
        }

        public void setMaxYamlCodepoints(int maxYamlCodepoints) {
            this.maxYamlCodepoints = maxYamlCodepoints;
        }
    }

    public Parser(Workspace workspace) {
        this(workspace, new Options());
    }

    public Parser(Workspace workspace, Options options) {

        LoaderOptions loaderOptions = new LoaderOptions();
        loaderOptions.setCodePointLimit(options.getMaxYamlCodepoints());

        YAMLFactoryBuilder builder = YAMLFactory.builder();
        builder.loaderOptions(loaderOptions);
        YAMLFactory yamlFactory = builder.build();

        this.jsonMapper = new ObjectMapper();
        this.yamlMapper = new ObjectMapper(yamlFactory);
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