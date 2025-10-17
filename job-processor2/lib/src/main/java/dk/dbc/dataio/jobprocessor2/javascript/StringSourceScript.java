package dk.dbc.dataio.jobprocessor2.javascript;

import dk.dbc.jslib.Environment;
import dk.dbc.jslib.ModuleHandler;
import dk.dbc.jslib.SchemeURI;

import java.util.List;

/**
 * Script wrapper for the soon-to-be-deprecated StringSourceSchemeHandler
 */
public class StringSourceScript extends Script {
    public static final String INTERNAL_LOAD_REQUIRE_CACHE = "__internal_load_require_cache";
    public static final String DEFINE_REQUIRE_CACHE_FUNCTION_JAVASCRIPT = "use(\"Require\");\n" +
            "function " + INTERNAL_LOAD_REQUIRE_CACHE + "( json ) {\n" +
            "Require.setCache( JSON.parse(json) );\n" +
            "};";

    public StringSourceScript(String scriptId, String invocationMethod, List<StringSourceSchemeHandler.Script> javascripts, String requireCacheJson) throws Exception {
        final ModuleHandler mh = new ModuleHandler();
        StringSourceSchemeHandler sssh = new StringSourceSchemeHandler(javascripts);
        mh.registerHandler("string", sssh);
        mh.addSearchPath(new SchemeURI("string", "."));
        synchronized (StringSourceScript.class) {
            jsEnvironment = new Environment();
            jsEnvironment.registerUseFunction(mh);
            if (requireCacheJson != null) {
                loadRequireCache(requireCacheJson);
            }
            jsEnvironment.eval(javascripts.get(0).javascript);
            this.scriptId = scriptId;
            this.invocationMethod = invocationMethod;
        }
    }

    private void loadRequireCache(String requireCacheJSON) throws Exception {
        jsEnvironment.eval(DEFINE_REQUIRE_CACHE_FUNCTION_JAVASCRIPT);
        jsEnvironment.callMethod(INTERNAL_LOAD_REQUIRE_CACHE, new Object[]{requireCacheJSON});
    }
}
