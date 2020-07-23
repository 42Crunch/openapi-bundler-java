/*
 Copyright (c) 42Crunch Ltd. All rights reserved.
 Licensed under the GNU Affero General Public License version 3. See LICENSE.txt in the project root for license information.
*/

package com.xliic.openapi.bundler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

public class Util {
    public static JsonNode get(JsonNode obj, String key) {
        if (obj.isObject()) {
            return obj.get(key);
        }

        if (obj.isArray()) {
            int index = Integer.parseInt(key);
            return obj.get(index);
        }

        return null;
    }

    public static void setRef(JsonNode obj, JsonPointer value) {
        if (obj.isObject()) {
            ((ObjectNode) obj).put("$ref", value.getURI().toString());
        } else {
            throw new IllegalArgumentException("Can't set $ref, argument is not an object");
        }
    }

    public static void set(JsonNode obj, String key, JsonNode value) {
        if (obj.isObject()) {
            ((ObjectNode) obj).set(key, value);
        } else if (obj.isArray()) {
            ((ArrayNode) obj).set(Integer.parseInt(key), value);
        } else {
            throw new IllegalArgumentException("Can't set, argument is not a container");
        }
    }

    public static void set(Serializer serializer, JsonNode obj, JsonPath path, JsonNode value) {
        JsonNode current = obj;
        for (String key : path.subList(0, path.size() - 1)) {
            if (current.has(key)) {
                current = get(current, key);
            } else {
                ObjectNode node = serializer.createObjectNode();
                set(current, key, node);
                current = node;
            }
        }
        String key = path.get(path.size() - 1);

        if (current.has(key)) {
            throw new IllegalArgumentException("Attempting to override existing value at: " + String.join("/", path));
        }

        set(current, key, value);
    }

}