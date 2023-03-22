package dk.dbc.dataio.javascript;

import dk.dbc.jslib.Environment;
import dk.dbc.jslib.ISchemeHandler;
import dk.dbc.jslib.SchemeURI;

import java.util.Collections;
import java.util.List;

public class StringSourceSchemeHandler implements ISchemeHandler {
    public static final String SCHEME = "string";
    public final List<Script> scripts;

    public StringSourceSchemeHandler(List<Script> scripts) {
        this.scripts = scripts;
    }

    @Override
    public List<String> schemes() {
        return Collections.singletonList(SCHEME);
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
    public void load(SchemeURI uri, Environment env) throws Exception {
        if (!uri.getScheme().equals(SCHEME)) {
            throw new IllegalArgumentException("Illegal scheme: " + uri.getScheme());
        }
        env.eval(retrieveScriptByModuleOrThrow(uri.getPath()).javascript);
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
