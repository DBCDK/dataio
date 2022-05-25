package dk.dbc.dataio.commons.javascript;

import dk.dbc.dataio.commons.types.JavaScript;
import dk.dbc.jslib.Environment;
import dk.dbc.jslib.FileSchemeHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static dk.dbc.dataio.commons.utils.lang.StringUtil.base64encode;

public class SpecializedFileSchemeHandler extends FileSchemeHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(SpecializedFileSchemeHandler.class);

    private List<JavaScript> javaScripts = new ArrayList<>();

    public SpecializedFileSchemeHandler(String root) {
        super(root);
    }

    public List<JavaScript> getJavaScripts() {
        return new ArrayList<>(javaScripts);
    }

    @Override
    protected void load(File file, Environment env) throws Exception {
        LOGGER.debug("load file: {}", file.getName());
        final String javascript = readJavascriptFileToUTF8String(file.toPath());
        javaScripts.add(new JavaScript(base64encode(javascript, StandardCharsets.UTF_8), getModuleName(file)));
        env.eval(javascript);
    }

    private String readJavascriptFileToUTF8String(Path file) throws IOException {
        return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
    }

    private String getModuleName(File file) {
        final String filename = file.getName();
        final int index = filename.indexOf(".use.js");
        if (index == -1) {
            return filename;
        }
        return filename.substring(0, index);
    }
}
