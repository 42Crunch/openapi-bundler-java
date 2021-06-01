/*
 Copyright (c) 42Crunch Ltd. All rights reserved.
 Licensed under the GNU Affero General Public License version 3. See LICENSE.txt in the project root for license information.
*/

package com.xliic.openapi.bundler;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import com.xliic.common.ContentType;
import com.xliic.common.Workspace;

class TestWorkspace implements Workspace {

    private URI workspace;

    TestWorkspace(String subfolder) throws IOException {
        this.workspace = new File("src/test/resources", subfolder).getCanonicalFile().toURI();
    }

    @Override
    public String read(URI uri) throws IOException, InterruptedException {
        File file = new File(uri.getPath());
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }

    @Override
    public boolean exists(URI uri) throws IOException, InterruptedException {
        File file = new File(uri);
        return file.exists();
    }

    @Override
    public URI resolve(String filename) {
        try {
            return workspace.resolve(new URI(null, filename, null));
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    @Override
    public URI relativize(URI uri) {
        return workspace.relativize(uri);
    }

    @Override
    public ContentType getContentType(URI uri) {
        if (uri.getPath().toLowerCase().endsWith(".json")) {
            return ContentType.JSON;
        }
        return ContentType.YAML;
    }
}