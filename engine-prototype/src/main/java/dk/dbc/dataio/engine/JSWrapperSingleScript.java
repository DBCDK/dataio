package dk.dbc.dataio.engine;

import dk.dbc.jslib.Environment;
import dk.dbc.jslib.ModuleHandler;
import dk.dbc.jslib.SchemeURI;
import java.util.List;

public class JSWrapperSingleScript {

    private final Environment jsEnvironment;

    public JSWrapperSingleScript(List<StringSourceSchemeHandler.Script> javascripts) {
        ModuleHandler mh = new ModuleHandler();
        StringSourceSchemeHandler sssh = new StringSourceSchemeHandler(javascripts);
        mh.registerHandler("string", sssh);
        mh.addSearchPath(new SchemeURI("string", "."));
        jsEnvironment = new Environment();
        jsEnvironment.registerUseFunction(mh);
        jsEnvironment.eval(javascripts.get(0).javascript);
    }

    public Object callMethod(String methodName, final Object[] args) {
        return jsEnvironment.callMethod(methodName, args);
    }
}
