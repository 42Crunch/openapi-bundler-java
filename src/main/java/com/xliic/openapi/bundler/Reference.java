/*
 Copyright (c) 42Crunch Ltd. All rights reserved.
 Licensed under the GNU Affero General Public License version 3. See LICENSE.txt in the project root for license information.
*/

package com.xliic.openapi.bundler;

import java.net.URI;
import java.net.URISyntaxException;

import com.fasterxml.jackson.databind.JsonNode;

public class Reference {
    final Document.Part part;
    final JsonNode node;
    final JsonPointer pointer;

    Document.Part resolvedPart;
    JsonNode resolvedValue;
    JsonPath resolvedPath;

    ReferenceResolutionFailure failure;

    int indirections = 0;
    boolean circular = false;

    public Reference(Document.Part part, JsonNode node, JsonPointer pointer) {
        this.part = part;
        this.node = node;
        this.pointer = pointer;
    }

    public boolean isResolved() {
        return this.resolvedValue != null;
    }

    public JsonPointer getResolvedPointer() {
        return JsonPointer.fromJsonPath(resolvedPath);
    }

    public URI getURI() {
        try {
            return new URI(part.location.getScheme(), part.location.getSchemeSpecificPart(), pointer.getValue());
        } catch (URISyntaxException e) {
            throw (IllegalArgumentException) new IllegalArgumentException().initCause(e);
        }
    }

    public URI getResolvedURI() {
        try {
            return new URI(resolvedPart.location.getScheme(), resolvedPart.location.getSchemeSpecificPart(),
                    resolvedPath.toPointer().getValue());
        } catch (URISyntaxException e) {
            throw (IllegalArgumentException) new IllegalArgumentException().initCause(e);
        }
    }

    public URI getResolvedFileURI() {
        try {
            return new URI(resolvedPart.location.getScheme(), resolvedPart.location.getSchemeSpecificPart(), null);
        } catch (URISyntaxException e) {
            throw (IllegalArgumentException) new IllegalArgumentException().initCause(e);
        }
    }

}