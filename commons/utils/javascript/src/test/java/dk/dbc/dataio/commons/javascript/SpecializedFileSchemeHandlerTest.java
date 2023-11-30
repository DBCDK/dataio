package dk.dbc.dataio.commons.javascript;

import dk.dbc.dataio.commons.types.JavaScript;
import dk.dbc.jslib.Environment;
import dk.dbc.jslib.SchemeURI;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static dk.dbc.dataio.commons.utils.lang.StringUtil.base64decode;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class SpecializedFileSchemeHandlerTest {
    @TempDir
    public Path folder;

    @Test
    public void testSingleJavascriptLoaded_javascriptAndFilenameCanBeRetrieved() throws Exception {
        final String javascript = "function f(x) { x.toUpperCase(); }";
        Path jsFile = Files.createFile(folder.resolve("test.use.js"));
        Files.write(jsFile, javascript.getBytes(StandardCharsets.UTF_8));

        Environment env = new Environment();
        SpecializedFileSchemeHandler schemeHandler = new SpecializedFileSchemeHandler(folder.toAbsolutePath().toString());
        schemeHandler.load(new SchemeURI("file", jsFile.toString()), env);

        List<JavaScript> scripts = schemeHandler.getJavaScripts();
        assertThat(scripts.size(), is(1));
        assertThat(base64decode(scripts.get(0).getJavascript()), is(javascript));
        assertThat(scripts.get(0).getModuleName(), is("test"));
    }

    @Test
    public void testTwoJavaScriptsLoaded_javascriptsAndFilenamesCanBeRetrieved() throws Exception {
        final String javascriptUpper = "function f(x) { x.toUpperCase(); }";
        final String javascriptLower = "function f(x) { x.toLowerCase(); }";
        Path jsFileUpper = Files.createFile(folder.resolve("to.upper.use.js"));
        Path jsFileLower = Files.createFile(folder.resolve("lower.use.js"));
        Files.writeString(jsFileUpper, javascriptUpper);
        Files.writeString(jsFileLower, javascriptLower);

        Environment env = new Environment();
        SpecializedFileSchemeHandler schemeHandler = new SpecializedFileSchemeHandler(folder.toString());
        schemeHandler.load(new SchemeURI("file", jsFileUpper.toString()), env);
        schemeHandler.load(new SchemeURI("file", jsFileLower.toString()), env);

        List<JavaScript> scripts = schemeHandler.getJavaScripts();
        assertThat(scripts.size(), is(2));
        assertThat(base64decode(scripts.get(0).getJavascript()), is(javascriptUpper));
        assertThat(scripts.get(0).getModuleName(), is("to.upper"));
        assertThat(base64decode(scripts.get(1).getJavascript()), is(javascriptLower));
        assertThat(scripts.get(1).getModuleName(), is("lower"));
    }
}
