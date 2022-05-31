package dk.dbc.dataio.commons.javascript;

import dk.dbc.dataio.commons.types.JavaScript;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static dk.dbc.dataio.commons.utils.lang.StringUtil.base64encode;

public class JavaScriptProject {
    private final List<JavaScript> javaScripts;
    private final String requireCache;

    public JavaScriptProject(List<JavaScript> javaScripts, String requireCache) {
        this.javaScripts = javaScripts;
        this.requireCache = requireCache;
    }

    public List<JavaScript> getJavaScripts() {
        return javaScripts;
    }

    public String getRequireCache() {
        return requireCache;
    }

    /**
     * Creates self-contained javascript project for given script
     *
     * @param script     path to javascript
     * @param searchPath path used to find script dependencies
     * @return {@link JavaScriptProject}
     * @throws Exception on failure to create self-contained javascript project
     */
    public static JavaScriptProject of(Path script, Path searchPath) throws Exception {
        final JavascriptUtil.JavascriptDependencies scriptsUsed =
                JavascriptUtil.getJavascriptDependencies(searchPath, script);

        final List<JavaScript> javaScripts = new ArrayList<>();
        javaScripts.add(read(script));
        javaScripts.addAll(scriptsUsed.javaScripts);
        return new JavaScriptProject(javaScripts, scriptsUsed.requireCache);
    }

    private static JavaScript read(Path script) throws IOException {
        final String content = new String(Files.readAllBytes(script), StandardCharsets.UTF_8);
        return new JavaScript(base64encode(content, StandardCharsets.UTF_8), "");
    }
}
