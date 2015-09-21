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

import dk.dbc.jslib.Environment;
import dk.dbc.jslib.ModuleHandler;
import dk.dbc.jslib.SchemeURI;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.ScriptableObject;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.junit.Ignore;

public class SpecializedFileSchemeHandlerTest {

    XLogger log = XLoggerFactory.getXLogger(SpecializedFileSchemeHandlerTest.class);
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    static {
        org.apache.log4j.BasicConfigurator.configure();
    }

    @Test
    public void testSingleJavascriptLoaded_javascriptAndFilenameCanBeRetrieved() throws RhinoException, IOException {
        String javascript = "function f(x) { x.toUpperCase(); }";
        File jsFile = folder.newFile("test.use.js");
        Files.write(jsFile.toPath(), javascript.getBytes("UTF-8"));

        SpecializedFileSchemeHandler schemeHandler = new SpecializedFileSchemeHandler(folder.getRoot().getAbsolutePath());
        Context context = Context.enter();
        ScriptableObject scope = context.initStandardObjects(null, true);
        schemeHandler.load(new SchemeURI("file", jsFile.getAbsolutePath()), context, scope);
        Context.exit();

        List<SpecializedFileSchemeHandler.JS> javascripts = schemeHandler.getJavascripts();
        assertThat(javascripts.size(), is(1));
        assertThat(javascripts.get(0).javascript, is(javascript));
        assertThat(javascripts.get(0).modulename, is("test"));
    }

    @Test
    public void testTwoJavascriptsLoaded_javascriptsAndFilenamesCanBeRetrieved() throws RhinoException, IOException {
        String javascriptUpper = "function f(x) { x.toUpperCase(); }";
        String javascriptLower = "function f(x) { x.toLowerCase(); }";
        File jsFileUpper = folder.newFile("upper.use.js");
        File jsFileLower = folder.newFile("lower.use.js");
        Files.write(jsFileUpper.toPath(), javascriptUpper.getBytes("UTF-8"));
        Files.write(jsFileLower.toPath(), javascriptLower.getBytes("UTF-8"));

        SpecializedFileSchemeHandler schemeHandler = new SpecializedFileSchemeHandler(folder.getRoot().getAbsolutePath());
        Context context = Context.enter();
        ScriptableObject scope = context.initStandardObjects(null, true);
        schemeHandler.load(new SchemeURI("file", jsFileUpper.getAbsolutePath()), context, scope);
        schemeHandler.load(new SchemeURI("file", jsFileLower.getAbsolutePath()), context, scope);
        Context.exit();

        List<SpecializedFileSchemeHandler.JS> javascripts = schemeHandler.getJavascripts();
        assertThat(javascripts.size(), is(2));
        assertThat(javascripts.get(0).javascript, is(javascriptUpper));
        assertThat(javascripts.get(0).modulename, is("upper"));
        assertThat(javascripts.get(1).javascript, is(javascriptLower));
        assertThat(javascripts.get(1).modulename, is("lower"));
    }

    @Ignore
    @Test
    public void test() throws Throwable {
        Path rootDir1 = (new File("/home/damkjaer/dbc/tmp/jscommon")).toPath();
        Path rootDir2 = (new File("/home/damkjaer/dbc/tmp/datawell-convert")).toPath();

        DirectoriesContainingJavascriptFinder finderJsCommon = new DirectoriesContainingJavascriptFinder();
        Files.walkFileTree(rootDir1, finderJsCommon);
        DirectoriesContainingJavascriptFinder finderDatawell = new DirectoriesContainingJavascriptFinder();
        Files.walkFileTree(rootDir2, finderDatawell);

        ModuleHandler mh = new ModuleHandler();
        SpecializedFileSchemeHandler sfsh = new SpecializedFileSchemeHandler("");
        //FileSchemeHandler sfsh = new FileSchemeHandler("");
        mh.registerHandler("file", sfsh);
        for (Path p : finderJsCommon.getJavascriptDirectories()) {
            mh.addSearchPath(new SchemeURI("file", p.toString()));
        }
        for (Path d : finderDatawell.getJavascriptDirectories()) {
            mh.addSearchPath(new SchemeURI("file", d.toString()));
        }

        Environment jsEnvironment = new Environment();
        jsEnvironment.registerUseFunction(mh);
        jsEnvironment.evalFile("/home/damkjaer/dbc/tmp/datawell-convert/js/xml_datawell_3.0.js");

    }
}