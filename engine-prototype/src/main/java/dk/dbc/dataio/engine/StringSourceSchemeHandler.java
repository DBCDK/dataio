package dk.dbc.dataio.engine;

import dk.dbc.jslib.ISchemeHandler;
import dk.dbc.jslib.SchemeURI;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.ScriptableObject;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;

public class StringSourceSchemeHandler implements ISchemeHandler {

    public static XLogger log = XLoggerFactory.getXLogger(StringSourceSchemeHandler.class);
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
        log.entry(uri, module);
        SchemeURI result = new SchemeURI();
        try {
            if (uri.getScheme().equals(SCHEME)) {
                for (Script script : scripts) {
                    if (script.name.equals(module)) {
                        System.out.println("Module Found: " + module);
                        result = new SchemeURI(SCHEME, module);
                        return result;
                    }
                }
            }
        } finally {
            log.exit(result);
        }
        System.out.println("Module NOT Found: " + module);
        return result;
    }

    @Override
    public void load(SchemeURI uri, Context context, ScriptableObject so) throws RhinoException, IOException {
        log.entry(uri, context, so);
        try {
            if (!uri.getScheme().equals(SCHEME)) {
                throw new IllegalArgumentException("Illegal scheme: " + uri.getScheme());
            }
            context.setOptimizationLevel(-1);
            context.evaluateString(so, retrieveScriptByModuleOrThrow(uri.getPath()).javascript, uri.getPath(), 1, null);
        } finally {
            log.exit();
        }
    }

    private Script retrieveScriptByModuleOrThrow(String module) {
        for (Script script : scripts) {
            if (script.name.equals(module)) {
                // log.info("Script \""+ script.name + "\": " + script.javascript);
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
