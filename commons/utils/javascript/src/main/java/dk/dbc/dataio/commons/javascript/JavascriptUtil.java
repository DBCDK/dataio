package dk.dbc.dataio.commons.javascript;

import java.io.Reader;
import java.util.List;
import dk.dbc.jslib.Environment;
import java.io.IOException;
import java.util.ArrayList;
import org.mozilla.javascript.ScriptableObject;
import org.slf4j.ext.XLoggerFactory;
import org.slf4j.ext.XLogger;

public class JavascriptUtil {

     private static XLogger log = XLoggerFactory.getXLogger(JavascriptUtil.class);

    // todo: Add javadoc
    public static List<String> getAllToplevelFunctionsInJavascript(Reader reader, String sourceName) throws IOException {
        Environment jsEnvironment = new Environment();
        return getAllToplevelFunctionsInJavascript(reader, sourceName, jsEnvironment);
    }

    // todo: Add javadoc
    public static List<String> getAllToplevelFunctionsInJavascriptWithFakeUseFunction(Reader reader, String sourceName) throws IOException {
        Environment jsEnvironment = new Environment();
        addFakeUseFunction(jsEnvironment);
        List<String> topLevelFunctionsWithUse = getAllToplevelFunctionsInJavascript(reader, sourceName, jsEnvironment);
        return filterFakeUseFunction(topLevelFunctionsWithUse);
    }

    private static List<String> getAllToplevelFunctionsInJavascript(Reader reader, String sourceName, Environment jsEnvironment) throws IOException {
        // evaluateJavascriptAndThrow(jsEnvironment, reader, sourceName);
        jsEnvironment.eval(reader, sourceName);
        Object[] propertyObjs = jsEnvironment.getAllIds();
        List<String> functionNames = new ArrayList<>();
        for (Object propertyObj : propertyObjs) {
            if (!(propertyObj instanceof String)) {
                continue;
            }
            String property = (String) propertyObj;
            if (isPropertyAFunctionName(jsEnvironment, property)) {
                functionNames.add(property);
            }
        }
        return functionNames;
    }

    private static boolean isPropertyAFunctionName(Environment jsEnvironment, String property) {
        final String FUNCTION_IDENTIFIER = "function";
        Object propObj = jsEnvironment.get(property);
        if (propObj instanceof ScriptableObject) {
            ScriptableObject so = (ScriptableObject) propObj;
            return so.getTypeOf().equals(FUNCTION_IDENTIFIER);
        } else {
            return false;
        }
    }

    private static void addFakeUseFunction(Environment jsEnvironment) {
        jsEnvironment.eval("function use(x) {}");
    }

    private static List<String> filterFakeUseFunction(List<String> list) {
        List<String> result = new ArrayList<>(list);
        result.remove("use");
        return result;
    }
}