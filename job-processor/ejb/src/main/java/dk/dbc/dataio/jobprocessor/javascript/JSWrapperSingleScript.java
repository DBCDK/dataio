package dk.dbc.dataio.jobprocessor.javascript;

import dk.dbc.jslib.Environment;
import dk.dbc.jslib.ModuleHandler;
import dk.dbc.jslib.SchemeURI;

import java.util.List;

public class JSWrapperSingleScript {
    private final Environment jsEnvironment;
    private final String invocationMethod;
    private final String scriptId;

    public JSWrapperSingleScript(String scriptId, String invocationMethod,
                                 List<StringSourceSchemeHandler.Script> javascripts) {
        final ModuleHandler mh = new ModuleHandler();
        StringSourceSchemeHandler sssh = new StringSourceSchemeHandler(javascripts);
        mh.registerHandler("string", sssh);
        mh.addSearchPath(new SchemeURI("string", "."));
        jsEnvironment = new Environment();
        jsEnvironment.registerUseFunction(mh);
        jsEnvironment.eval(javascripts.get(0).javascript);
        this.scriptId = scriptId;
        this.invocationMethod = invocationMethod;
    }

    public Object invoke(final Object[] args) {
        return jsEnvironment.callMethod(invocationMethod, args);
    }

    public Object eval(String s) {
        return jsEnvironment.eval(s);
    }

    public String getScriptId() {
        return scriptId;
    }

    public String getInvocationMethod() {
        return invocationMethod;
    }
}
