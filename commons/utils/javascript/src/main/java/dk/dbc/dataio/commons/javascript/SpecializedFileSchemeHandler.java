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
import dk.dbc.jslib.Environment;
import dk.dbc.jslib.FileSchemeHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static dk.dbc.dataio.commons.utils.lang.StringUtil.base64encode;

public class SpecializedFileSchemeHandler extends FileSchemeHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(SpecializedFileSchemeHandler.class);

    private List<JavaScript> javaScripts = new ArrayList<>();

    public SpecializedFileSchemeHandler(String root) {
        super(root);
    }

    public List<JavaScript> getJavaScripts() {
        return new ArrayList<>(javaScripts);
    }

    @Override
    protected void load(File file, Environment env) throws Exception {
        LOGGER.debug("load file: {}", file.getName());
        final String javascript = readJavascriptFileToUTF8String(file.toPath());
        javaScripts.add(new JavaScript(base64encode(javascript, StandardCharsets.UTF_8), getModuleName(file)));
        env.eval(javascript);
    }

    private String readJavascriptFileToUTF8String(Path file) throws IOException {
        return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
    }

    private String getModuleName(File file) {
        final String filename = file.getName();
        final int index = filename.indexOf('.');
        if (index == -1) {
            return filename;
        }
        return filename.substring(0, index);
    }
}
