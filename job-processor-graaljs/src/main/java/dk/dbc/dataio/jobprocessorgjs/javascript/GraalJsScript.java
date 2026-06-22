package dk.dbc.dataio.jobprocessorgjs.javascript;

import dk.dbc.commons.graaljs.core.JsInterop;
import dk.dbc.commons.graaljs.core.JsarResourceFileSystem;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

import java.io.IOException;
import java.io.UncheckedIOException;

public class GraalJsScript implements AutoCloseable {

    private final Context context;
    private final String scriptId;
    private final String invocationMethod;

    public GraalJsScript(String scriptId, String invocationMethod, byte[] jsar, Engine engine) {
        this.scriptId = scriptId;
        this.invocationMethod = invocationMethod;

        JsarResourceFileSystem fileSystem;
        try {
            fileSystem = new JsarResourceFileSystem(jsar);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read JSAR", e);
        }

        // The engine is shared across all scripts and consumer threads so compiled JavaScript
        // code is cached and reused; it is owned and closed by the caller, not by this script.
        JsInterop jsInterop = JsInterop.newBuilder()
                .engine(engine)
                .jsFileSystem(fileSystem)
                .build();
        context = jsInterop.createContext();

        // The entry point is an ES module; evaluating it directly would not expose its
        // exports as global bindings. Instead evaluate a synthetic module that imports the
        // entry point's exports and copies them onto globalThis, so the named invocation
        // function becomes reachable via getBindings("js").getMember(invocationMethod).
        // (mirrors dk.dbc.commons.graaljs.cli.runner.JsHandler)
        Source exporter = fileSystem.createSource(globalExporter(scriptId));
        try {
            context.eval(exporter);
        } catch (RuntimeException e) {
            throw new IllegalStateException(
                    "Error loading JavaScript entry point '" + scriptId + "': " + e.getMessage(), e);
        }

        // The backing store is no longer needed once the entry module (and its transitive
        // imports) have been evaluated; release it to reclaim memory.
        fileSystem.pruneUnusedFiles();
    }

    public String invoke(Object[] args) {
        Value function = context.getBindings("js").getMember(invocationMethod);
        if (function == null || function.isNull()) {
            throw new IllegalStateException(
                    "Invocation method '" + invocationMethod + "' not found in script '" + scriptId + "'");
        }
        Value result = function.execute(args);
        return result.isString() ? result.asString() : null;
    }

    // eval is intentional: the expression is either the JMS additionalArgs JSON (non-ADDI items)
    // or the ADDI record metadata JSON (ADDI items), evaluated into a JS object so the script
    // receives it as a native value rather than a string.
    public Object eval(String expression) {
        return context.eval("js", expression);
    }

    public String getScriptId() {
        return scriptId;
    }

    public String getInvocationMethod() {
        return invocationMethod;
    }

    @Override
    public void close() {
        // Closes only this script's context; the shared engine is closed by its owner.
        context.close();
    }

    private static String globalExporter(String scriptId) {
        return "import * as TopLevelModule from '" + scriptId + "';\n"
                + "Object.keys(TopLevelModule).forEach(name => globalThis[name] = TopLevelModule[name]);";
    }
}
