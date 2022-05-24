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
import dk.dbc.jslib.SchemeURI;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import static dk.dbc.dataio.commons.utils.lang.StringUtil.base64decode;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class SpecializedFileSchemeHandlerTest {
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void testSingleJavascriptLoaded_javascriptAndFilenameCanBeRetrieved() throws Exception {
        final String javascript = "function f(x) { x.toUpperCase(); }";
        final File jsFile = folder.newFile("test.use.js");
        Files.write(jsFile.toPath(), javascript.getBytes(StandardCharsets.UTF_8));

        final Environment env = new Environment();
        final SpecializedFileSchemeHandler schemeHandler = new SpecializedFileSchemeHandler(folder.getRoot().getAbsolutePath());
        schemeHandler.load(new SchemeURI("file", jsFile.getAbsolutePath()), env);

        final List<JavaScript> scripts = schemeHandler.getJavaScripts();
        assertThat(scripts.size(), is(1));
        assertThat(base64decode(scripts.get(0).getJavascript()), is(javascript));
        assertThat(scripts.get(0).getModuleName(), is("test"));
    }

    @Test
    public void testTwoJavascriptsLoaded_javascriptsAndFilenamesCanBeRetrieved() throws Exception {
        final String javascriptUpper = "function f(x) { x.toUpperCase(); }";
        final String javascriptLower = "function f(x) { x.toLowerCase(); }";
        final File jsFileUpper = folder.newFile("to.upper.use.js");
        final File jsFileLower = folder.newFile("lower.use.js");
        Files.write(jsFileUpper.toPath(), javascriptUpper.getBytes("UTF-8"));
        Files.write(jsFileLower.toPath(), javascriptLower.getBytes("UTF-8"));

        final Environment env = new Environment();
        final SpecializedFileSchemeHandler schemeHandler = new SpecializedFileSchemeHandler(folder.getRoot().getAbsolutePath());
        schemeHandler.load(new SchemeURI("file", jsFileUpper.getAbsolutePath()), env);
        schemeHandler.load(new SchemeURI("file", jsFileLower.getAbsolutePath()), env);

        final List<JavaScript> scripts = schemeHandler.getJavaScripts();
        assertThat(scripts.size(), is(2));
        assertThat(base64decode(scripts.get(0).getJavascript()), is(javascriptUpper));
        assertThat(scripts.get(0).getModuleName(), is("to.upper"));
        assertThat(base64decode(scripts.get(1).getJavascript()), is(javascriptLower));
        assertThat(scripts.get(1).getModuleName(), is("lower"));
    }
}
