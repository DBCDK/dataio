package dk.dbc.dataio.engine;

import dk.dbc.jslib.Environment;
import dk.dbc.jslib.FileSchemeHandler;
import dk.dbc.jslib.ModuleHandler;

public class JSWrapperSingleScript {

    private final Environment jsEnvironment;

    public JSWrapperSingleScript(String jsScript) {

        
        ModuleHandler mh = new ModuleHandler();
        FileSchemeHandler fsh = new FileSchemeHandler(".");
        mh.registerHandler("file", fsh);
        jsEnvironment = new Environment();
        // jsEnvironment.registerUseFunction(mh);

        jsEnvironment.eval(jsScript);
    }

    public Object callMethod(String methodName, final Object[] args) {
        return jsEnvironment.callMethod(methodName, args);
    }
}
