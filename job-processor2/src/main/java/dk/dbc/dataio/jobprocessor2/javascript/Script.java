package dk.dbc.dataio.jobprocessor2.javascript;

import dk.dbc.jslib.Environment;

public abstract class Script {

    Environment jsEnvironment;
    String invocationMethod;
    String scriptId;

    public Object invoke(final Object[] args) throws Throwable {
        return jsEnvironment.callMethod(invocationMethod, args);
    }

    public Object eval(String s) throws Throwable {
        return jsEnvironment.eval(s);
    }

    public String getScriptId() {
        return scriptId;
    }

    public String getInvocationMethod() {
        return invocationMethod;
    }
}
