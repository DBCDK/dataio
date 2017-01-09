/*
 * DataIO - Data IO
 * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 * Denmark. CVR: 15149043
 *
 * This file is part of DataIO.
 *
 * DataIO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DataIO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
 */

package dk.dbc.dataio.commons.javascript;

import dk.dbc.dataio.commons.types.JavaScript;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static dk.dbc.dataio.commons.utils.lang.StringUtil.base64encode;

public class JavaScriptProject {
    private final List<JavaScript> javaScripts;
    private final String requireCache;

    public JavaScriptProject(List<JavaScript> javaScripts, String requireCache) {
        this.javaScripts = javaScripts;
        this.requireCache = requireCache;
    }

    public List<JavaScript> getJavaScripts() {
        return javaScripts;
    }

    public String getRequireCache() {
        return requireCache;
    }

    /**
     * Creates self-contained javascript project for given script
     * @param script path to javascript
     * @param searchPath path used to find script dependencies
     * @return {@link JavaScriptProject}
     * @throws Exception on failure to create self-contained javascript project
     */
    public static JavaScriptProject of(Path script, Path searchPath) throws Exception {
        final JavascriptUtil.JavascriptDependencies scriptsUsed =
                JavascriptUtil.getJavascriptDependencies(searchPath, script);

        final List<JavaScript> javaScripts = new ArrayList<>();
        javaScripts.add(read(script));
        javaScripts.addAll(scriptsUsed.javaScripts);
        return new JavaScriptProject(javaScripts, scriptsUsed.requireCache);
    }

    private static JavaScript read(Path script) throws IOException {
        final String content = new String(Files.readAllBytes(script), StandardCharsets.UTF_8);
        return new JavaScript(base64encode(content, StandardCharsets.UTF_8), "");
    }
}
