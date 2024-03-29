package dk.dbc.dataio.commons.javascript;

import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class JavascriptUtilTest {
    @Test
    public void testGetAllToplevelFunctionsInJavascript_nullReaderArgument_throws() throws Throwable {
        assertThrows(NullPointerException.class, () -> JavascriptUtil.getAllToplevelFunctionsInJavascript(null, "<inlinetest>"));
    }

    @Test
    public void testGetAllToplevelFunctionsInJavascript_nullStringSourceArgument_throws() throws Throwable {
        String javascript = "function myfunc(s) { }";
        assertThrows(NullPointerException.class, () -> JavascriptUtil.getAllToplevelFunctionsInJavascript(new StringReader(javascript), null));
    }

    @Test
    public void testGetAllToplevelFunctionsInJavascriptWithFakeUseFunction_nullReaderArgument_throws() throws Throwable {
        assertThrows(NullPointerException.class, () -> JavascriptUtil.getAllToplevelFunctionsInJavascriptWithFakeUseFunction(null, "<inlinetest>"));
    }

    @Test
    public void testGetAllToplevelFunctionsInJavascriptWithFakeUseFunction_nullStringSourceArgument_throws() throws Throwable {
        String javascript = "function myfunc(s) { }";
        assertThrows(NullPointerException.class, () -> JavascriptUtil.getAllToplevelFunctionsInJavascriptWithFakeUseFunction(new StringReader(javascript), null));
    }

    @Test
    public void testGetAllToplevelFunctionsInJavascript_singleLegalFunction() throws Throwable {
        String javascript = "function myfunc(s) { }";
        List<String> functionNames = JavascriptUtil.getAllToplevelFunctionsInJavascript(new StringReader(javascript), "<inlinetest>");
        assertThat(functionNames.size(), is(1));
        assertThat(functionNames.get(0), is("myfunc"));
    }

    @Test
    public void testGetAllToplevelFunctionsInJavascript_javascriptContainingUse_throws() throws Throwable {
        String javascript = ""
                + "use(\"Something\");\n"
                + "function myfunc(s) { }";
        assertThrows(Exception.class, () -> JavascriptUtil.getAllToplevelFunctionsInJavascript(new StringReader(javascript), "<inlinetest>"));
    }

    @Test
    public void testGetAllToplevelFunctionsInJavascriptWithFakeUseFunction_singleLegalFunction() throws Throwable {
        String javascript = "function myfunc(s) { }";
        List<String> functionNames = JavascriptUtil.getAllToplevelFunctionsInJavascriptWithFakeUseFunction(new StringReader(javascript), "<inlinetest>");
        assertThat(functionNames.size(), is(1));
        assertThat(functionNames.get(0), is("myfunc"));
    }

    @Test
    public void testGetAllToplevelFunctionsInJavascriptWithFakeUseFunction_singleLegalFunctionWithUse() throws Throwable {
        String javascript = ""
                + "use(\"Something\");\n"
                + "function myfunc(s) { }";
        List<String> functionNames = JavascriptUtil.getAllToplevelFunctionsInJavascriptWithFakeUseFunction(new StringReader(javascript), "<inlinetest>");
        assertThat(functionNames.size(), is(1));
        assertThat(functionNames.get(0), is("myfunc"));
    }

    @Test
    public void testGetAllToplevelFunctionsInJavascript_multipleLegalFunctions() throws Throwable {
        String javascript = ""
                + "function myfunc(s) {};\n"
                + "function anotherfunc(x) {}";
        List<String> functionNames = JavascriptUtil.getAllToplevelFunctionsInJavascript(new StringReader(javascript), "<inlinetest>");
        assertThat(functionNames.size(), is(2));
        assertThat(functionNames.contains("myfunc"), is(true));
        assertThat(functionNames.contains("anotherfunc"), is(true));
    }

    @Test
    public void testGetAllToplevelFunctionsInJavascriptWithFakeUseFunction_multipleLegalFunctions() throws Throwable {
        String javascript = ""
                + "use(\"Something\");\n"
                + "function myfunc(s) {};\n"
                + "function anotherfunc(x) {}";
        List<String> functionNames = JavascriptUtil.getAllToplevelFunctionsInJavascriptWithFakeUseFunction(new StringReader(javascript), "<inlinetest>");
        assertThat(functionNames.size(), is(2));
        assertThat(functionNames.contains("myfunc"), is(true));
        assertThat(functionNames.contains("anotherfunc"), is(true));
    }

    @Test
    public void testGetAllToplevelFunctionsInJavascript_multipleIdenticalFunctions_yieldSingleFunctionNameProperty() throws Throwable {
        String javascript = ""
                + "function f(x) {};\n"
                + "function f(x) {};\n"
                + "function f(x,y) {};";
        List<String> functionNames = JavascriptUtil.getAllToplevelFunctionsInJavascript(new StringReader(javascript), "<inlinetest>");
        assertThat(functionNames.size(), is(1));
        assertThat(functionNames.get(0), is("f"));
    }

    @Test
    public void testGetAllToplevelFunctionsInJavascript_illegalJavascript_throws() {
        String javascript = "function myfunc(x) {";
        assertThrows(Exception.class, () -> JavascriptUtil.getAllToplevelFunctionsInJavascript(new StringReader(javascript), "<inlinetest>"));
    }

    @Test
    public void testGetAllToplevelFunctionsInJavascriptWithFakeUseFunction_illegalJavascript_throws() {
        String javascript = "use(\"Something\");\n"
                + "function myfunc(x) {";
        assertThrows(Exception.class, () -> JavascriptUtil.getAllToplevelFunctionsInJavascriptWithFakeUseFunction(new StringReader(javascript), "<inlinetest>"));
    }

    /*
     * When a javascript with a function named 'use' is given to the method
     * getAllToplevelFunctionsInJavascriptWithFakeUseFunction, then all
     * functions named 'use' will be filtered from the result.
     */
    @Test
    public void testGetAllToplevelFunctionsInJavascriptWithFakeUseFunction_singleLegalAdditionalUseFunction() throws Throwable {
        String javascript = "use(\"Something\");\n"
                + "function use(s) { }";
        List<String> functionNames = JavascriptUtil.getAllToplevelFunctionsInJavascriptWithFakeUseFunction(new StringReader(javascript), "<inlinetest>");
        assertThat(functionNames.size(), is(0));
    }

    /*
     * This is a special case test:
     * If the evaluated javascript contains an object which tries to reference another
     * object which is the target of the use-function, then an exception is thrown.
     */
    @Test
    public void testGetAllToplevelFunctionsInJavascriptWithFakeUseFunction_javascriptWithObjectReferencingUseModule() throws Throwable {
        String javascript = ""
                + "use(\"Something\");\n"
                + "\n"
                + "function f(x) {\n"
                + "    var s = Something.dhandle(x);\n"
                + "    return s;\n"
                + "}\n"
                + "\n"
                + "var Test = function() {\n"
                + "    var s = Something.dhandle();\n"
                + "    var that = {};\n"
                + "    that.f = function(x) {\n"
                + "        var ss = Something.dhandle(x);\n"
                + "        return ss;\n"
                + "    }\n"
                + "    return that;\n"
                + "}();\n";
        assertThrows(Exception.class, () -> JavascriptUtil.getAllToplevelFunctionsInJavascriptWithFakeUseFunction(new StringReader(javascript), "<inlinetest>"));
    }
}
