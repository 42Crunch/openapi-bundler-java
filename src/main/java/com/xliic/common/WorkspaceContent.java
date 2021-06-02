/*
 Copyright (c) 42Crunch Ltd. All rights reserved.
 Licensed under the GNU Affero General Public License version 3. See LICENSE.txt in the project root for license information.
*/

package com.xliic.common;

public class WorkspaceContent {
    public final String data;
    public final ContentType type;

    public WorkspaceContent(String data, ContentType type) {
        this.data = data;
        this.type = type;
    }
}
