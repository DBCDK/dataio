package dk.dbc.dataio.commons.javascript;

import dk.dbc.dataio.commons.types.JavaScript;
import dk.dbc.invariant.InvariantUtil;
import dk.dbc.jslib.Environment;
import dk.dbc.jslib.ModuleHandler;
import dk.dbc.jslib.SchemeURI;
import jdk.nashorn.api.scripting.JSObject;

import java.io.IOException;
import java.io.Reader;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static dk.dbc.dataio.commons.utils.lang.StringUtil.base64encode;
import static java.util.Comparator.comparing;

public class JavascriptUtil {

    /**
     * Function for retrieving the top-level function names from a javascript.
     * <p>
     * Given a javascript like this:
     * <pre>
     * {@code
     * function myfunc(someVar) {
     *   // body of function
     * }
     *
     * function anotherfunc(anotherVar) {
     *   // body of function
     * } }
     * </pre> the method
     * <code>getAllToplevelFunctionsInJavascript(...)</code> will return a
     * <code>List</code> containing the values
     * <code>myfunc</code> and
     * <code>anotherfunc</code>.
     * <p>
     * <b>Notice:</b> the order of the returned values is unspecified.
     *
     * @param reader     with the javascript.
     * @param sourceName the filename of the source.
     * @return A <code>List</code> of <code>Strings</code> containing the
     * function names from the javascript in unspecified order.
     * @throws IOException if the <code>Reader</code> containing the javascript
     *                     can not be read.
     * @throws Exception   if the javascript could not be evaluated
     * @throws Exception   if an error in the evaluated javascript is found
     */
    public static List<String> getAllToplevelFunctionsInJavascript(Reader reader, String sourceName) throws Throwable {
        Environment jsEnvironment = new Environment();
        return getAllToplevelFunctionsInJavascript(reader, sourceName, jsEnvironment);
    }

    /**
     * Function for retrieving the top-level function names from a javascript
     * for javascripts containing use-functionality. The depended use-modules
     * will not be loaded, instead an empty use-function will be added to the
     * scope before evaluating the javascript. After evaluation the
     * 'use'-function-name will be filtered from the result.
     * <p>
     * Given a javascript like this:
     * <pre>
     * {@code
     * use("SomeModule");
     *
     * function myfunc(someVar) {
     *   // body of function
     * }
     *
     * function anotherfunc(anotherVar) {
     *   // body of function
     * } }
     * </pre> the method
     * <code>getAllToplevelFunctionsInJavascriptWithFakeUseFunction(...)</code>
     * will return a
     * <code>List</code> containing the values
     * <code>myfunc</code> and
     * <code>anotherfunc</code>.
     * <p>
     * <b>Notice:</b> the order of the returned values is unspecified.
     * <p>
     * <b>Notice:</b> there are some problems with this function: If the
     * javascript contains a top-level function named
     * <code>use</code> like this:
     * <pre>
     * {@code
     * use("SomeModule");
     *
     * function myfunc(someVar) {
     *   // body of function
     * }
     *
     * function use(x) {
     *   // body of function
     * } }
     * }
     * </pre> then the method
     * <code>getAllToplevelFunctionsInJavascriptWithFakeUseFunction(...)</code>
     * will return a List containing the single value
     * <code>myfunc</code>. The value
     * <code>use</code> will be filtered from the result.
     * <p>
     * Another problem is if a javascript implements an object, which on the
     * top-level depends on a 'used' module like this:
     * <pre>
     * {@code
     * use("Something");
     *
     * function f(x) { // body of function }
     *
     * var SomeObject = function() {
     *     var s = Something.somefunc();
     *     var that = {};
     *
     *     that.f = function(x) {
     *         return Something.somefunc(x);
     *     }
     *     return that;
     * }(); }
     * </pre> then the function
     * getAllToplevelFunctionsInJavascriptWithFakeUseFunction(...) will throw an
     * {@link Exception} containing a
     * <code>ReferenceError</code> to the line containing the code
     * {@code var s = Something.somefunc();}
     *
     * @param reader     with the javascript.
     * @param sourceName the filename of the source.
     * @return A <code>List</code> of <code>Strings</code> containing the
     * function names from the javascript.
     * @throws IOException if the <code>Reader</code> containing the javascript
     *                     can not be read.
     * @throws Exception   if the javascript could not be evaluated
     * @throws Exception   if an error in the evaluated javascript is found
     */
    public static List<String> getAllToplevelFunctionsInJavascriptWithFakeUseFunction(Reader reader, String sourceName) throws Throwable {
        Environment jsEnvironment = new Environment();
        addFakeUseFunction(jsEnvironment);
        List<String> topLevelFunctionsWithUse = getAllToplevelFunctionsInJavascript(reader, sourceName, jsEnvironment);
        return filterFakeUseFunction(topLevelFunctionsWithUse);
    }

    private static List<String> getAllToplevelFunctionsInJavascript(Reader reader, String sourceName, Environment jsEnvironment) throws Throwable {
        InvariantUtil.checkNotNullOrThrow(reader, "reader");
        InvariantUtil.checkNotNullOrThrow(sourceName, "sourceName");
        jsEnvironment.eval(reader, sourceName);
        Object[] propertyObjs = jsEnvironment.getAllIds();
        List<String> functionNames = new ArrayList<>();
        for (Object propertyObj : propertyObjs) {
            if (!(propertyObj instanceof String)) {
                continue;
            }
            String property = (String) propertyObj;
            if (isPropertyAFunctionName(jsEnvironment, property) && !"uneval".equals(property)) {
                functionNames.add(property);
            }
        }
        return functionNames;
    }

    private static boolean isPropertyAFunctionName(Environment jsEnvironment, String property) {
        final Object propObj = jsEnvironment.get(property);
        if (propObj instanceof JSObject) {
            JSObject jso = (JSObject) propObj;
            return jso.isFunction();
        }
        return false;
    }

    private static void addFakeUseFunction(Environment jsEnvironment) throws Throwable {
        jsEnvironment.eval("function use(x) {}");
    }

    private static List<String> filterFakeUseFunction(List<String> list) {
        List<String> result = new ArrayList<>(list);
        result.remove("use");
        return result;
    }

    public static class JavascriptDependencies implements Serializable {
        public List<JavaScript> javaScripts;
        public String requireCache = null;

        public JavascriptDependencies(List<JavaScript> javaScripts, String requireCache) {
            this.javaScripts = javaScripts;
            if (requireCache != null) {
                this.requireCache = base64encode(requireCache, StandardCharsets.UTF_8);
            }
        }
    }

    /**
     * Resolves all dependencies for given javascript by traversing given folder
     *
     * @param root       folder containing dependencies, sub folders are added to search path in alphabetical sort order
     * @param javascript script to resolve
     * @return dependencies
     * @throws Exception on failure to eval dependencies
     */
    public static JavascriptDependencies getJavascriptDependencies(Path root, Path javascript) throws Exception {
        final DirectoriesContainingJavascriptFinder javascriptDirFinder = new DirectoriesContainingJavascriptFinder();
        Files.walkFileTree(root, javascriptDirFinder);
        final List<Path> javascriptDirs = javascriptDirFinder.getJavascriptDirectories();
        javascriptDirs.sort(comparing(Path::toString));

        final ModuleHandler mh = new ModuleHandler();
        final SpecializedFileSchemeHandler sfsh = new SpecializedFileSchemeHandler(root.toAbsolutePath().toString());
        mh.registerHandler("file", sfsh);
        for (Path path : javascriptDirs) {
            mh.addSearchPath(new SchemeURI("file", path.toString()));
        }

        final Environment jsEnvironment = new Environment();
        try {
            jsEnvironment.registerUseFunction(mh);
            jsEnvironment.evalFile(javascript.toString());

            String requireCache = (String) jsEnvironment.eval(
                    "if( this.hasOwnProperty('Require') ) { JSON.stringify(Require.getCache()); } else { ''; };");
            if (requireCache != null && requireCache.isEmpty()) {
                requireCache = null;
            }

            return new JavascriptDependencies(sfsh.getJavaScripts(), requireCache);
        } catch (Throwable e) {
            throw new IOException("Unknown error trying to eval load-dependencies", e);
        }
    }
}
