package dk.dbc.dataio.commons.types;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * FlowContent unit tests
 *
 * The test methods of this class uses the following naming convention:
 *
 *  unitOfWork_stateUnderTest_expectedBehavior
 */
public class FlowContentTest {
    private static final String NAME = "name";
    private static final String DESCRIPTION = "description";
    private static final List<FlowComponent> COMPONENTS = Arrays.asList(FlowComponentTest.newFlowComponentInstance());

    @Test(expected = NullPointerException.class)
    public void constructor_nameArgIsNull_throws() {
        new FlowContent(null, DESCRIPTION, COMPONENTS);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_nameArgIsEmpty_throws() {
        new FlowContent("", DESCRIPTION, COMPONENTS);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_descriptionArgIsNull_throws() {
        new FlowContent(NAME, null, COMPONENTS);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_descriptionArgIsEmpty_throws() {
        new FlowContent(NAME, "", COMPONENTS);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_componentsArgIsNull_throws() {
        new FlowContent(NAME, DESCRIPTION, null);
    }

    @Test
    public void constructor_allArgsAreValid_returnsNewInstance() {
        final FlowContent instance = new FlowContent(NAME, DESCRIPTION, COMPONENTS);
        assertThat(instance, is(notNullValue()));
    }

    @Test
    public void constructor_componentsArgIsEmpty_returnsNewInstance() {
        final FlowContent instance = new FlowContent(NAME, DESCRIPTION, new ArrayList<FlowComponent>(0));
        assertThat(instance, is(notNullValue()));
    }

    @Test
    public void verify_defensiveCopyingOfComponentsList() {
        final List<FlowComponent> components = new ArrayList<>();
        components.add(FlowComponentTest.newFlowComponentInstance());
        final FlowContent instance = new FlowContent(NAME, DESCRIPTION, components);
        assertThat(instance.getComponents().size(), is(1));
        components.add(null);
        final List<FlowComponent> returnedComponents = instance.getComponents();
        assertThat(returnedComponents.size(), is(1));
        returnedComponents.add(null);
        assertThat(instance.getComponents().size(), is(1));
    }

    static FlowContent newFlowContentInstance() {
        return new FlowContent(NAME, DESCRIPTION, COMPONENTS);
    }
}
