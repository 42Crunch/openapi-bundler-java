/*
 Copyright (c) 42Crunch Ltd. All rights reserved.
 Licensed under the GNU Affero General Public License version 3. See LICENSE.txt in the project root for license information.
*/

package com.xliic.openapi.bundler;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;

import com.fasterxml.jackson.databind.JsonNode;

public class Document {
    public final Part root;
    Parts parts = new Parts();
    URI base;

    public Document(URI location, JsonNode root) {
        this.base = location.resolve("."); // strip filename to get base URI
        this.root = new Part(location, root);
        this.parts.put(location, this.root);
    }

    public static Part getTargetPart(Part part, URI refUri) throws URISyntaxException {
        URI targetPartUri = getTargetPartUri(part, refUri);
        return part.getDocument().getPart(targetPartUri);
    }

    public static URI getTargetPartUri(Part part, URI refUri) throws URISyntaxException {
        URI locationWithFragment = part.location.resolve(refUri);
        // return URI without fragment
        return new URI(locationWithFragment.getScheme(), locationWithFragment.getSchemeSpecificPart(), null);
    }

    public Part createPart(URI location, JsonNode node) {
        Part part = new Part(location, node);
        parts.put(location, part);
        return part;
    }

    public Part getPart(URI location) throws URISyntaxException {
        URI partURI = new URI(location.getScheme(), location.getSchemeSpecificPart(), null);
        return parts.get(partURI);
    }

    public class Part {
        public final URI location;
        public final JsonNode node;

        private Part(URI location, JsonNode node) {
            this.location = location;
            this.node = node;
        }

        public URI resolve(URI ref) {
            return this.location.resolve(ref);
        }

        public Document getDocument() {
            return Document.this;
        }

        public URI getDocumentFile() {
            return Document.this.root.location;
        }

        public URI getFilename() {
            return Document.this.base.relativize(this.location);
        }
    }

    @SuppressWarnings("serial")
    public static class Parts extends HashMap<URI, Part> {

    }
}
