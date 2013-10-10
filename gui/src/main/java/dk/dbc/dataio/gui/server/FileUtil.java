package dk.dbc.dataio.gui.server;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

public class FileUtil {
    private FileUtil() { }

    public static Reader getReaderForFile(Path file) throws FileNotFoundException, UnsupportedEncodingException {
        return new InputStreamReader(new FileInputStream(file.toFile()), StandardCharsets.UTF_8);
    }
}
