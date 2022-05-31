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
