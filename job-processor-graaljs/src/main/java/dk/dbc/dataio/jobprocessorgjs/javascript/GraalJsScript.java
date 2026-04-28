package dk.dbc.dataio.jobprocessorgjs.javascript;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class GraalJsScript implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(GraalJsScript.class);

    private final Context context;
    private final String scriptId;
    private final String invocationMethod;

    public GraalJsScript(String scriptId, String invocationMethod, byte[] jsar) {
        this.scriptId = scriptId;
        this.invocationMethod = invocationMethod;

        Map<String, String> jsFiles = extractJsFiles(jsar);

        context = Context.newBuilder("js")
                .allowHostAccess(HostAccess.ALL)
                .allowHostClassLookup(name -> name.startsWith("dk.dbc.javascript.recordprocessing."))
                .build();

        context.getBindings("js").putMember("use", (ProxyExecutable) args -> {
            String name = args[0].asString();
            String source = findModule(jsFiles, name);
            if (source == null) {
                throw new RuntimeException("Module not found in JSAR: " + name);
            }
            try {
                context.eval(Source.newBuilder("js", source, name).build());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            return null;
        });

        String entrySource = jsFiles.get(scriptId);
        if (entrySource == null) {
            throw new IllegalArgumentException("Entry point script not found in JSAR: " + scriptId);
        }
        try {
            context.eval(Source.newBuilder("js", entrySource, scriptId).build());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
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
        context.close();
    }

    private static Map<String, String> extractJsFiles(byte[] jsar) {
        Map<String, String> jsFiles = new HashMap<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(jsar))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (!entry.isDirectory() && entry.getName().endsWith(".js")) {
                    jsFiles.put(entry.getName(), new String(zip.readAllBytes(), StandardCharsets.UTF_8));
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to extract JSAR", e);
        }
        return jsFiles;
    }

    private static String findModule(Map<String, String> jsFiles, String name) {
        String source = jsFiles.get(name);
        if (source != null) return source;
        String withExt = name.endsWith(".js") ? name : name + ".js";
        source = jsFiles.get(withExt);
        if (source != null) return source;
        for (Map.Entry<String, String> entry : jsFiles.entrySet()) {
            if (entry.getKey().endsWith("/" + withExt)) {
                return entry.getValue();
            }
        }
        LOGGER.warn("Module '{}' not found in JSAR, available: {}", name, jsFiles.keySet());
        return null;
    }
}
