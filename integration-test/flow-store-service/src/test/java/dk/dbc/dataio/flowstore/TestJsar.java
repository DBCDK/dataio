package dk.dbc.dataio.flowstore;

import dk.dbc.dataio.commons.types.JavaScriptEngine;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

class TestJsar {
    private final String name;
    private JavaScriptEngine engine;

    TestJsar(String name) {
        this.name = name;
    }

    TestJsar withEngine(JavaScriptEngine engine) {
        this.engine = engine;
        return this;
    }

    byte[] build() {
        Manifest manifest = new Manifest();
        Attributes attrs = manifest.getMainAttributes();
        attrs.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        attrs.putValue("Flow-Name", name);
        attrs.putValue("Flow-Description", "Flow for integration test: " + name);
        attrs.putValue("Flow-Entrypoint-Script", "test.js");
        attrs.putValue("Flow-Entrypoint-Function", "transform");
        if (engine != null) {
            attrs.putValue("Flow-JavaScript-Engine", engine.name());
        }
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (ZipOutputStream zip = new ZipOutputStream(baos)) {
                zip.putNextEntry(new ZipEntry("META-INF/MANIFEST.MF"));
                manifest.write(zip);
                zip.closeEntry();
            }
            return baos.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
