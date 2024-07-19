package dk.dbc.dataio.commons.types;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * FlowContent unit tests
 * <p>
 * The test methods of this class uses the following naming convention:
 * <p>
 * unitOfWork_stateUnderTest_expectedBehavior
 */
public class FlowContentTest {

    @Test
    void constructor_validJsar_newInstance() throws IOException {
        byte[] jsarBytes = Files.readAllBytes(Path.of("src", "test", "resources", "flow.jsar"));
        Date timeOfLastModification = new Date();

        final FlowContent flowContent = new FlowContent(jsarBytes, timeOfLastModification);
        assertThat("name", flowContent.getName(), is("TestFlow"));
        assertThat("description", flowContent.getDescription(), is("Used for unit testing"));
        assertThat("entrypointScript", flowContent.getEntrypointScript(), is("entrypoint.js"));
        assertThat("entrypointFunction", flowContent.getEntrypointFunction(), is("callMe"));
        assertThat("jsar", flowContent.getJsar(), is(jsarBytes));
        assertThat("timeOfLastModification", flowContent.getTimeOfLastModification(), is(timeOfLastModification));
    }

    @Test
    void constructor_invalidJsar_throws() throws IOException {
        byte[] jsarBytes = Files.readAllBytes(Path.of("src", "test", "resources", "flow-invalid.jsar"));
        final IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () ->
                new FlowContent(jsarBytes, new Date()));
        assertThat(illegalArgumentException.getMessage(),
                is("Invalid jsar - META-INF/MANIFEST.MF missing value for Flow-Description"));
    }

    /* ##### soon-to-be-deprecated begin ##### */

    private static final String NAME = "name";
    private static final String DESCRIPTION = "description";

    @Test
    public void constructor_allArgsAreValid_returnsNewInstance() {
        FlowContent instance = new FlowContent(NAME, DESCRIPTION, null, null, null, null);
        assertThat(instance, is(notNullValue()));
    }

    public static FlowContent newFlowContentInstance() {
        return new FlowContent(NAME, DESCRIPTION);
    }

    /* ##### soon-to-be-deprecated end ##### */
}
