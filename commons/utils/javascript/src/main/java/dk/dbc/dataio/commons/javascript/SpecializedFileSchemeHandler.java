package dk.dbc.dataio.commons.javascript;

import dk.dbc.jslib.FileSchemeHandler;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.ScriptableObject;
import org.slf4j.ext.XLoggerFactory;
import org.slf4j.ext.XLogger;

public class SpecializedFileSchemeHandler extends FileSchemeHandler {

    XLogger log = XLoggerFactory.getXLogger(SpecializedFileSchemeHandler.class);

    private List<JS> javascripts = new ArrayList<>();

    public SpecializedFileSchemeHandler(String root) {
        super(root);
    }

    public List<JS> getJavascripts() {
        return new ArrayList<>(javascripts);
    }

    @Override
    protected void load(File file, Context context, ScriptableObject scope) throws RhinoException, IOException {
        log.entry(file, context, scope);
        try {
            String filename = file.getName();
            log.debug("load file: {}", filename);
            String javascript=  readJavascriptFileToUTF8String(file.toPath());
            storeJavascript(javascript, file.getAbsolutePath());
            context.setOptimizationLevel(-1);
            context.evaluateString(scope, javascript, filename, 1, null);
        } finally {
            log.exit();
        }
    }

    private String readJavascriptFileToUTF8String(Path file) throws IOException {
        String line = "";
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = Files.newBufferedReader(file, Charset.forName("UTF-8"))) {
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
    }

    private void storeJavascript(String javascript, String filename) {
        this.javascripts.add(new JS(javascript, filename));
    }

    public static class JS {
        public final String javascript;
        public final String filename;
        public final String modulename;
        public JS(String javascript, String filename) {
            this.javascript = javascript;
            this.filename = filename;
            this.modulename = filename.substring(filename.lastIndexOf("/")+1, filename.indexOf(".use.js"));
        }
    }
}
