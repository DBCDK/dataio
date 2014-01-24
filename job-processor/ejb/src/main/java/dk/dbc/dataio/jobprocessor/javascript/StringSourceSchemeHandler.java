package dk.dbc.dataio.jobprocessor.javascript;

import dk.dbc.jslib.ISchemeHandler;
import dk.dbc.jslib.SchemeURI;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.ScriptableObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class StringSourceSchemeHandler implements ISchemeHandler {
    public static final Logger LOGGER = LoggerFactory.getLogger(StringSourceSchemeHandler.class);
    public static final String SCHEME = "string";
    public final List<Script> scripts;

    public StringSourceSchemeHandler(List<Script> scripts) {
        this.scripts = scripts;
    }

    @Override
    public List<String> schemes() {
        return Arrays.asList(SCHEME);
    }

    @Override
    public SchemeURI lookup(SchemeURI uri, String module) {
        SchemeURI result = new SchemeURI();
        if (uri.getScheme().equals(SCHEME)) {
            for (Script script : scripts) {
                if (script.name.equals(module)) {
                    result = new SchemeURI(SCHEME, module);
                    return result;
                }
            }
        }
        return result;
    }

    @Override
    public void load(SchemeURI uri, Context context, ScriptableObject so) throws RhinoException, IOException {
        if (!uri.getScheme().equals(SCHEME)) {
            throw new IllegalArgumentException("Illegal scheme: " + uri.getScheme());
        }
        context.setOptimizationLevel(-1);
        context.evaluateString(so, retrieveScriptByModuleOrThrow(uri.getPath()).javascript, uri.getPath(), 1, null);
    }

    private Script retrieveScriptByModuleOrThrow(String module) {
        for (Script script : scripts) {
            if (script.name.equals(module)) {
                return script;
            }
        }
        throw new IllegalArgumentException("Module: " + module + " is not contained in the scripts");
    }

    public static class Script {
        public final String name;
        public final String javascript;

        public Script(String name, String javascript) {
            this.name = name;
            this.javascript = javascript;
        }
    }
}
