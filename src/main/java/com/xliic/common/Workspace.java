/*
 Copyright (c) 42Crunch Ltd. All rights reserved.
 Licensed under the GNU Affero General Public License version 3. See LICENSE.txt in the project root for license information.
*/

package com.xliic.common;

import java.io.IOException;
import java.net.URI;

public interface Workspace {
    public String read(URI uri) throws IOException, InterruptedException;

    public boolean exists(URI uri) throws IOException, InterruptedException;

    public URI relativize(URI uri);

    public URI resolve(String filename);
}
