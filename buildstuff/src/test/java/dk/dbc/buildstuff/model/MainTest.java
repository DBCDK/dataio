package dk.dbc.buildstuff.model;

import dk.dbc.buildstuff.Main;
import freemarker.template.TemplateException;
import jakarta.xml.bind.JAXBException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

class MainTest {
    private Path xmlConfig = path("test.xml");

    @Test
    void testGenerateOk() throws TemplateException, JAXBException, IOException, SAXException, GitAPIException {
        new Main(Main.Command.GENERATE, xmlConfig.toString(), "t0", "").generate();
        Path path = xmlConfig.getParent().resolve("test0/deploy-diff-sink.yml");
        List<String> result = Files.readAllLines(path);
        List<String> expected = Files.readAllLines(path("expect/sink.yml"));
        Assertions.assertEquals(expected.size(), result.size(), "The files line count differs");
        for (int i = 0; i < result.size(); i++) {
            Assertions.assertEquals(expected.get(i), result.get(i), "Generated files differs in line " + (i + 1));
        }
    }

    @Test
    void testGenerateMissingProp() {
        Main main = new Main(Main.Command.GENERATE, xmlConfig.toString(), "f0", "");
        Assertions.assertThrows(IllegalStateException.class, main::generate);
    }

    @Test
    void testGenerateMissingEnvProp() {
        Main main = new Main(Main.Command.GENERATE, xmlConfig.toString(), "f1", "");
        Assertions.assertThrows(IllegalStateException.class, main::generate);
    }

    private static Path path(String file) {
        return Path.of(MainTest.class.getClassLoader().getResource(file).getFile());
    }
}
