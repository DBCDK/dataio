package dk.dbc.dataio.commons.types;

import org.junit.jupiter.api.Test;

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
    private static final String NAME = "name";
    private static final String DESCRIPTION = "description";
    private static final List<FlowComponent> COMPONENTS = Collections.singletonList(FlowComponentTest.newFlowComponentInstance());
    private static final Date TIME_OF_FLOW_COMPONENT_UPDATE = new Date();

    @Test
    public void constructor_nameArgIsNull_throws() {
        assertThrows(NullPointerException.class, () -> new FlowContent(null, DESCRIPTION, COMPONENTS, TIME_OF_FLOW_COMPONENT_UPDATE));
    }

    @Test
    public void constructor_nameArgIsEmpty_throws() {
        assertThrows(IllegalArgumentException.class, () -> new FlowContent("", DESCRIPTION, COMPONENTS, TIME_OF_FLOW_COMPONENT_UPDATE));
    }

    @Test
    public void constructor_descriptionArgIsNull_throws() {
        assertThrows(NullPointerException.class, () -> new FlowContent(NAME, null, COMPONENTS, TIME_OF_FLOW_COMPONENT_UPDATE));
    }

    @Test
    public void constructor_descriptionArgIsEmpty_throws() {
        assertThrows(IllegalArgumentException.class, () -> new FlowContent(NAME, "", COMPONENTS, TIME_OF_FLOW_COMPONENT_UPDATE));
    }

    @Test
    public void constructor_componentsArgIsNull_throws() {
        assertThrows(NullPointerException.class, () -> new FlowContent(NAME, DESCRIPTION, null, TIME_OF_FLOW_COMPONENT_UPDATE));
    }

    @Test
    public void constructor_allArgsAreValid_returnsNewInstance() {
        FlowContent instance = new FlowContent(NAME, DESCRIPTION, COMPONENTS, TIME_OF_FLOW_COMPONENT_UPDATE);
        assertThat(instance, is(notNullValue()));
    }

    @Test
    public void constructor_componentsArgIsEmpty_returnsNewInstance() {
        FlowContent instance = new FlowContent(NAME, DESCRIPTION, new ArrayList<>(0), TIME_OF_FLOW_COMPONENT_UPDATE);
        assertThat(instance, is(notNullValue()));
    }

    @Test
    public void constructor_timeOfFlowComponentUpdateArgIsNull_returnsNewInstance() {
        FlowContent instance = new FlowContent(NAME, DESCRIPTION, COMPONENTS, null);
        assertThat(instance, is(notNullValue()));
    }

    @Test
    public void withFlowComponents_nullValuedFlowComponent_returnsEmptyListOfFlowComponents() {
        FlowContent instance = new FlowContent(NAME, DESCRIPTION, COMPONENTS, null);
        FlowContent flowContent = instance.withComponents(null);
        assertThat(flowContent.getComponents(), is(Collections.emptyList()));
    }

    @Test
    public void withFlowComponents_returnsListOfFlowComponents() {
        FlowContent instance = new FlowContent(NAME, DESCRIPTION, COMPONENTS, null);
        FlowContent flowContent = instance.withComponents(FlowComponentTest.newFlowComponentInstance(), null);
        assertThat(flowContent.getComponents().size(), is(1));
    }

    @Test
    public void verify_defensiveCopyingOfComponentsList() {
        List<FlowComponent> components = new ArrayList<>();
        components.add(FlowComponentTest.newFlowComponentInstance());
        FlowContent instance = new FlowContent(NAME, DESCRIPTION, components, TIME_OF_FLOW_COMPONENT_UPDATE);
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
}
