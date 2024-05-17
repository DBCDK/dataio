package dk.dbc.dataio.commons.types;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

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
    private static final List<FlowComponent> COMPONENTS = Collections.singletonList(FlowComponentTest.newFlowComponentInstance());
    private static final Date TIME_OF_FLOW_COMPONENT_UPDATE = new Date();

    @Test
    public void constructor_allArgsAreValid_returnsNewInstance() {
        FlowContent instance = new FlowContent(NAME, DESCRIPTION, null, null, null, null, COMPONENTS, TIME_OF_FLOW_COMPONENT_UPDATE);
        assertThat(instance, is(notNullValue()));
    }

    @Test
    public void constructor_componentsArgIsEmpty_returnsNewInstance() {
        FlowContent instance = new FlowContent(NAME, DESCRIPTION, null, null, null, null, new ArrayList<>(0), TIME_OF_FLOW_COMPONENT_UPDATE);
        assertThat(instance, is(notNullValue()));
    }

    @Test
    public void constructor_timeOfFlowComponentUpdateArgIsNull_returnsNewInstance() {
        FlowContent instance = new FlowContent(NAME, DESCRIPTION, null, null, null, null, COMPONENTS, null);
        assertThat(instance, is(notNullValue()));
    }

    @Test
    public void withFlowComponents_nullValuedFlowComponent_returnsEmptyListOfFlowComponents() {
        FlowContent instance = new FlowContent(NAME, DESCRIPTION, null, null, null, null, COMPONENTS, null);
        FlowContent flowContent = instance.withComponents(null);
        assertThat(flowContent.getComponents(), is(Collections.emptyList()));
    }

    @Test
    public void withFlowComponents_returnsListOfFlowComponents() {
        FlowContent instance = new FlowContent(NAME, DESCRIPTION, null, null, null, null, COMPONENTS, null);
        FlowContent flowContent = instance.withComponents(FlowComponentTest.newFlowComponentInstance(), null);
        assertThat(flowContent.getComponents().size(), is(1));
    }

    @Test
    public void verify_defensiveCopyingOfComponentsList() {
        List<FlowComponent> components = new ArrayList<>();
        components.add(FlowComponentTest.newFlowComponentInstance());
        FlowContent instance = new FlowContent(NAME, DESCRIPTION, null, null, null, null, components, TIME_OF_FLOW_COMPONENT_UPDATE);
        assertThat(instance.getComponents().size(), is(1));
        components.add(null);
        List<FlowComponent> returnedComponents = instance.getComponents();
        assertThat(returnedComponents.size(), is(1));
        returnedComponents.add(null);
        assertThat(instance.getComponents().size(), is(1));
    }

    public static FlowContent newFlowContentInstance() {
        return new FlowContent(NAME, DESCRIPTION, COMPONENTS);
    }

    /* ##### soon-to-be-deprecated end ##### */
}
