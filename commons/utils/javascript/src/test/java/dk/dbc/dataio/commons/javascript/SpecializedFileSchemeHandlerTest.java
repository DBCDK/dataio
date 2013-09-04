
package dk.dbc.dataio.commons.javascript;

import dk.dbc.jslib.Environment;
import dk.dbc.jslib.ModuleHandler;
import dk.dbc.jslib.SchemeURI;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;

public class SpecializedFileSchemeHandlerTest {

    XLogger log = XLoggerFactory.getXLogger(SpecializedFileSchemeHandlerTest.class);

    static {
        org.apache.log4j.BasicConfigurator.configure();
    }

    @Ignore
    @Test
    public void test() throws IOException {
        Path rootDir = (new File("/home/damkjaer/dbc/tmp/jscommon")).toPath();

        DirectoriesContainingJavascriptFinder finder = new DirectoriesContainingJavascriptFinder();
        Files.walkFileTree(rootDir, finder);
        for(Path path : finder.getJavascriptDirectories()) {
            System.out.println("Path: " + path.toString());
        }

//        ModuleHandler mh = new ModuleHandler();
//        SpecializedFileSchemeHandler sfsh = new SpecializedFileSchemeHandler("");
//        mh.registerHandler("file", sfsh);
//        mh.addSearchPath(new SchemeURI("file", "."));
//        Environment jsEnvironment = new Environment();
    }
}