/*
 Copyright (c) 42Crunch Ltd. All rights reserved.
 Licensed under the GNU Affero General Public License version 3. See LICENSE.txt in the project root for license information.
*/

package com.xliic.openapi.bundler;

import java.net.URI;
import java.net.URISyntaxException;

import com.fasterxml.jackson.databind.JsonNode;

public class Reference {
    final Document.Part sourcePart;
    final JsonPointer sourcePointer;

    final Document.Part targetPart;
    final JsonPointer targetPointer;

    Document.Part resolvedPart;
    JsonNode resolvedValue;
    JsonPath resolvedPath;

    final JsonPath path;
    int indirections = 0;
    boolean circular = false;

    public Reference(Document.Part sourcePart, JsonPointer sourcePointer, Document.Part targetPart,
            JsonPointer targetPointer) {
        this.sourcePart = sourcePart;
        this.sourcePointer = sourcePointer;
        this.targetPart = targetPart;
        this.targetPointer = targetPointer;
        this.path = targetPointer.getJsonPath();
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

    public JsonPointer getPointer() {
        return JsonPointer.fromJsonPath(resolvedPath);
    }

    public String getFile() {
        return this.resolvedPart.location.getScheme() + ":" + this.resolvedPart.location.getSchemeSpecificPart();
    }

    public URI getSourceURI() {
        try {
            return new URI(sourcePart.location.getScheme(), sourcePart.location.getSchemeSpecificPart(),
                    sourcePointer.getValue());
        } catch (URISyntaxException e) {
            throw (IllegalArgumentException) new IllegalArgumentException().initCause(e);
        }
    }

    public URI getResolvedTargetURI() throws URISyntaxException {
        return new URI(resolvedPart.location.getScheme(), resolvedPart.location.getSchemeSpecificPart(),
                resolvedPath.toPointer().getValue());
    }
}