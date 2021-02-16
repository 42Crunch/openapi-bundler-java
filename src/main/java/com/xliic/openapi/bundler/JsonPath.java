/*
 Copyright (c) 42Crunch Ltd. All rights reserved.
 Licensed under the GNU Affero General Public License version 3. See LICENSE.txt in the project root for license information.
*/

package com.xliic.openapi.bundler;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("serial")
public class JsonPath extends ArrayList<String> {
    public JsonPath(String... keys) {
        super();
        for (String key : keys) {
            add(key);
        }
    }

    public JsonPath(List<String> keys) {
        super(keys);
    }

    public JsonPath(JsonPath path) {
        super(path);
    }

    JsonPath withKeys(List<String> keys) {
        JsonPath copy = new JsonPath(this);
        copy.addAll(keys);
        return copy;
    }

    JsonPath withKey(String key) {
        JsonPath copy = new JsonPath(this);
        copy.add(key);
        return copy;
    }

    public JsonPointer toPointer() {
        return JsonPointer.fromJsonPath(this);
    }

    public boolean isSubPathOf(JsonPath path) {
        if (path.size() > this.size()) {
            return false;
        }

        for (int i = 0; i < path.size(); i++) {
            if (!path.get(i).equals(this.get(i))) {
                return false;
            }
        }
        return true;
    }
}