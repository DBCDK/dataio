package dk.dbc.dataio.jobprocessor2.javascript;

import dk.dbc.jslib.Environment;
import dk.dbc.jslib.JsarSchemeHandler;
import dk.dbc.jslib.ModuleHandler;
import dk.dbc.jslib.SchemeURI;

/**
 * Script wrapper for the JsarSchemeHandler
 */
public class JsarScript extends Script {

    public JsarScript(String scriptId, String invocationMethod, byte[] jsar) throws Exception {
        final ModuleHandler mh = new ModuleHandler();
        final JsarSchemeHandler jsarSchemeHandler = new JsarSchemeHandler(jsar);
        mh.registerHandler("jsar", jsarSchemeHandler);
        mh.addSearchPath(new SchemeURI("jsar:/"));
        synchronized (JsarScript.class) {
            jsEnvironment = new Environment();
            jsEnvironment.registerUseFunction(mh);
            jsarSchemeHandler.load(new SchemeURI("jsar:" + scriptId), jsEnvironment);
            this.scriptId = scriptId;
            this.invocationMethod = invocationMethod;
        }
    }
}
