/*
 Copyright (c) 42Crunch Ltd. All rights reserved.
 Licensed under the GNU Affero General Public License version 3. See LICENSE.txt in the project root for license information.
*/

package com.xliic.openapi.bundler;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;

import com.fasterxml.jackson.databind.JsonNode;

public class JsonPointer {
    final Document.Part part;
    final URI target;
    final JsonPath path;

    Document.Part resolvedPart;
    JsonNode resolvedValue;
    JsonPath resolvedPath;
    int indirections = 0;
    boolean circular = false;

    public JsonPointer(Document.Part part, URI target) {
        this.part = part;
        this.target = target;
        this.path = parse(target.getFragment());
    }

    static JsonPath parse(String pointer) {
        JsonPath result = new JsonPath();

        if (pointer == null || pointer.equals("")) {
            // return empty path
            return result;
        } else if (pointer.equals("/")) {
            // return path to "" key
            return result.withKey("");
        }

        String[] segments = pointer.split("/");
        for (String segment : segments) {
            try {
                result.add(URLDecoder.decode(segment.replaceAll("~1", "/").replaceAll("~0", "~"), "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                throw (IllegalArgumentException) new IllegalArgumentException().initCause(e);
            }
        }

        // remove "" from the result of split
        result.remove(0);

        return result;
    }

    public JsonNode getValue() {
        return this.resolvedValue;
    }

    public int getIndirections() {
        return indirections;
    }

    public boolean getCircular() {
        return circular;
    }

    public Document.Part getPart() {
        return this.resolvedPart;
    }

    public JsonPath getPath() {
        return this.resolvedPath;
    }

    public String getPointer() throws UnsupportedEncodingException {
        return JsonPath.toPointer(resolvedPath);
    }

    public String getFile() {
        return this.resolvedPart.location.getScheme() + ":" + this.resolvedPart.location.getSchemeSpecificPart();
    }

    public URI getTargetURI() throws URISyntaxException {
        URI file = this.resolvedPart.location;
        return new URI(file.getScheme(), file.getSchemeSpecificPart(), target.getFragment());
    }
}