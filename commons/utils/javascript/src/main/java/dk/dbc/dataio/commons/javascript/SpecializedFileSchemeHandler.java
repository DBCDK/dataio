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

import dk.dbc.jslib.FileSchemeHandler;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.ScriptableObject;
import org.slf4j.ext.XLoggerFactory;
import org.slf4j.ext.XLogger;

public class SpecializedFileSchemeHandler extends FileSchemeHandler {

    XLogger log = XLoggerFactory.getXLogger(SpecializedFileSchemeHandler.class);

    private List<JS> javascripts = new ArrayList<>();

    public SpecializedFileSchemeHandler(String root) {
        super(root);
    }

    public List<JS> getJavascripts() {
        return new ArrayList<>(javascripts);
    }

    @Override
    protected void load(File file, Context context, ScriptableObject scope) throws RhinoException, IOException {
        log.entry(file, context, scope);
        try {
            String filename = file.getName();
            log.debug("load file: {}", filename);
            String javascript = readJavascriptFileToUTF8String(file.toPath());
            log.debug("Javascript: [{}]", javascript);
            storeJavascript(javascript, file.getAbsolutePath());
            context.setOptimizationLevel(-1);
            context.evaluateString(scope, javascript, filename, 1, null);
        } finally {
            log.exit();
        }
    }

    private String readJavascriptFileToUTF8String(Path file) throws IOException {
        return new String(Files.readAllBytes(file), Charset.forName("UTF-8"));
    }

    private void storeJavascript(String javascript, String filename) {
        this.javascripts.add(new JS(javascript, filename));
    }

    public void addJS(JS js) {
        this.javascripts.add(js);
    }

    public static class JS {
        public final String javascript;
        public final String modulename;
        public JS(String javascript, String filename) {
            this.javascript = javascript;
            this.modulename = filename.substring(filename.lastIndexOf("/")+1, filename.indexOf(".use.js"));
        }
    }
}
