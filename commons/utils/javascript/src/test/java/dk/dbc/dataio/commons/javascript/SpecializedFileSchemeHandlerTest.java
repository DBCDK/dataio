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
        Path rootDir1 = (new File("/home/damkjaer/dbc/tmp/jscommon")).toPath();
        Path rootDir2 = (new File("/home/damkjaer/dbc/tmp/datawell-convert")).toPath();

        DirectoriesContainingJavascriptFinder finderJsCommon = new DirectoriesContainingJavascriptFinder();
        Files.walkFileTree(rootDir1, finderJsCommon);
        DirectoriesContainingJavascriptFinder finderDatawell = new DirectoriesContainingJavascriptFinder();
        Files.walkFileTree(rootDir2, finderDatawell);
//        for (Path path : finderJsCommon.getJavascriptDirectories()) {
//            System.out.println("Path: " + path.toString());
//        }
        ModuleHandler mh = new ModuleHandler();
        SpecializedFileSchemeHandler sfsh = new SpecializedFileSchemeHandler("");
        mh.registerHandler("file", sfsh);
        for (Path p : finderJsCommon.getJavascriptDirectories()) {
            mh.addSearchPath(new SchemeURI("file", p.toString()));
        }
        for (Path d : finderDatawell.getJavascriptDirectories()) {
            mh.addSearchPath(new SchemeURI("file", d.toString()));
        }

        Environment jsEnvironment = new Environment();
        jsEnvironment.registerUseFunction(mh);
        jsEnvironment.evalFile("/home/damkjaer/dbc/tmp/datawell-convert/js/xml_datawell_3.0.js");

    }
}