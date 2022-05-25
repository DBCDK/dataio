package dk.dbc.dataio.gui.client.components;

import com.google.gwt.dom.client.Style;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwtmockito.GwtMockitoTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.AbstractMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;


/**
 * MapEntry unit tests
 * <p/>
 * The test methods of this class uses the following naming convention:
 * <p/>
 * unitOfWork_stateUnderTest_expectedBehavior
 */
@RunWith(GwtMockitoTestRunner.class)
public class MapEntryTest {
    @SuppressWarnings("deprecation")
    @Mock
    com.google.gwt.user.client.Element mockedElement;
    @SuppressWarnings("deprecation")
    @Mock
    com.google.gwt.user.client.Element mockedKeyElement;
    @SuppressWarnings("deprecation")
    @Mock
    com.google.gwt.user.client.Element mockedValueElement;
    @Mock
    Style mockedKeyStyle;
    @Mock
    Style mockedValueStyle;
    @Mock
    ValueChangeHandler mockedValueChangeHandler;


    MapEntry mapEntry;


    @Test
    public void constructor_nullParams_ok() {
        // Subject Under Test
        mapEntry = new MapEntry(null, null);
    }

    @Test
    public void constructor_emptyParams_ok() {
        // Subject Under Test
        mapEntry = new MapEntry("", "");
    }

    @Test
    public void constructor_promptParams_ok() {
        // Subject Under Test
        mapEntry = new MapEntry("Key Prompt", "Value Prompt");

        // Test Verification
        assertThat(mapEntry.keyBox, is(not(nullValue())));
        assertThat(mapEntry.valueBox, is(not(nullValue())));
        verify(mapEntry.keyBox).setPrompt("Key Prompt");
        verify(mapEntry.valueBox).setPrompt("Value Prompt");
    }

    @Test
    public void setOrientation_nullParam_noAction() {
        // Test Preparation
        mapEntry = new MapEntry("Key Prompt", "Value Prompt");

        // Subject Under Test
        mapEntry.setOrientation(null);

        // Test Verification
        verify(mapEntry.keyBox).setPrompt("Key Prompt");
        verify(mapEntry.valueBox).setPrompt("Value Prompt");
        verifyNoMoreInteractionsOnMocks();
    }

    @Test
    public void setOrientation_emptyParam_noAction() {
        // Test Preparation
        mapEntry = new MapEntry("Key Prompt", "Value Prompt");
        prepareMocks();

        // Subject Under Test
        mapEntry.setOrientation("");

        // Test Verification
        verify(mapEntry.keyBox).getElement();
        verify(mapEntry.valueBox).getElement();
        verify(mockedKeyElement).getStyle();
        verify(mockedValueElement).getStyle();
        verify(mockedKeyStyle).setProperty("display", "inline");
        verify(mockedValueStyle).setProperty("display", "inline");
        verify(mapEntry.keyBox).setPromptStyle("stacked");
        verify(mapEntry.valueBox).setPromptStyle("stacked");
        verifyNoMoreInteractionsOnMocks();
    }

    @Test
    public void setOrientation_verticalParam_noAction() {
        // Test Preparation
        mapEntry = new MapEntry("Key Prompt", "Value Prompt");
        prepareMocks();

        // Subject Under Test
        mapEntry.setOrientation("vertical");

        // Test Verification
        verify(mapEntry.keyBox).getElement();
        verify(mapEntry.valueBox).getElement();
        verify(mockedKeyElement).getStyle();
        verify(mockedValueElement).getStyle();
        verify(mockedKeyStyle).setProperty("display", "block");
        verify(mockedValueStyle).setProperty("display", "block");
        verify(mapEntry.keyBox).setPromptStyle("non-stacked");
        verify(mapEntry.valueBox).setPromptStyle("non-stacked");
        verifyNoMoreInteractionsOnMocks();
    }

    @Test
    public void getValue_validEntry_ok() {
        // Test Preparation
        mapEntry = new MapEntry("Key Prompt", "Value Prompt");
        prepareMocks();
        when(mapEntry.keyBox.getValue()).thenReturn("-key-");
        when(mapEntry.valueBox.getValue()).thenReturn("-value-");

        // Subject Under Test
        Map.Entry<String, String> value = mapEntry.getValue();

        // Test Verification
        verify(mapEntry.keyBox).getValue();
        verify(mapEntry.valueBox).getValue();
        assertThat(value.getKey(), is("-key-"));
        assertThat(value.getValue(), is("-value-"));
        verifyNoMoreInteractionsOnMocks();
    }

    @Test
    public void setValue_validEntry_ok() {
        // Test Preparation
        mapEntry = new MapEntry("Key Prompt", "Value Prompt");
        prepareMocks();

        // Subject Under Test
        mapEntry.setValue(new AbstractMap.SimpleEntry<>("setKey", "setValue"));

        // Test Verification
        verify(mapEntry.keyBox).setValue("setKey");
        verify(mapEntry.valueBox).setValue("setValue");
        verifyNoMoreInteractionsOnMocks();
    }

    @Test
    public void setValue_validEntryWithEventNoEventHandler_ok() {
        // Test Preparation
        mapEntry = new MapEntry("Key Prompt", "Value Prompt");
        prepareMocks();

        // Subject Under Test
        mapEntry.setValue(new AbstractMap.SimpleEntry<>("setKey", "setValue"), true);

        // Test Verification
        verify(mapEntry.keyBox).setValue("setKey");
        verify(mapEntry.valueBox).setValue("setValue");
        verifyNoMoreInteractionsOnMocks();
    }

    @Test
    public void addValueChangeHandler_emptyLambda_ok() {
        // Test Preparation
        mapEntry = new MapEntry("Key Prompt", "Value Prompt");
        prepareMocks();
        assertThat(mapEntry.valueChangeHandler, is(nullValue()));

        // Subject Under Test
        mapEntry.addValueChangeHandler(mockedValueChangeHandler);

        // Test Verification
        assertThat(mapEntry.valueChangeHandler, is(not(nullValue())));
        verify(mapEntry.keyBox).addValueChangeHandler(any());
        verify(mapEntry.valueBox).addValueChangeHandler(any());
        verifyNoMoreInteractionsOnMocks();
    }

    @Test
    public void setValue_validEntryWithEventValidEventHandler_ok() {
        // Test Preparation
        mapEntry = new MapEntry("Key Prompt", "Value Prompt");
        prepareMocks();
        mapEntry.addValueChangeHandler(mockedValueChangeHandler);

        // Subject Under Test
        mapEntry.setValue(new AbstractMap.SimpleEntry<>("setKey", "setValue"), true);

        // Test Verification
        verify(mapEntry.keyBox).setValue("setKey");
        verify(mapEntry.keyBox).addValueChangeHandler(any());
        verify(mapEntry.keyBox).getValue();
        verify(mapEntry.valueBox).setValue("setValue");
        verify(mapEntry.valueBox).addValueChangeHandler(any());
        verify(mapEntry.valueBox).getValue();
        verify(mockedValueChangeHandler).onValueChange(any());
        verifyNoMoreInteractionsOnMocks();
    }

    @Test
    public void handlerRegistration_removeHandler_ok() {
        // Test Preparation
        mapEntry = new MapEntry("Key Prompt", "Value Prompt");
        prepareMocks();
        HandlerRegistration handlerRegistration = mapEntry.addValueChangeHandler(mockedValueChangeHandler);
        assertThat(mapEntry.valueChangeHandler, is(not(nullValue())));

        // Subject Under Test
        handlerRegistration.removeHandler();

        // Test Verification
        assertThat(mapEntry.valueChangeHandler, is(nullValue()));
    }



    /*
     * Private methods
     */

    private void prepareMocks() {
        when(mapEntry.keyBox.getElement()).thenReturn(mockedKeyElement);
        when(mapEntry.valueBox.getElement()).thenReturn(mockedValueElement);
        when(mockedKeyElement.getStyle()).thenReturn(mockedKeyStyle);
        when(mockedValueElement.getStyle()).thenReturn(mockedValueStyle);
        verify(mapEntry.keyBox).setPrompt("Key Prompt");
        verify(mapEntry.valueBox).setPrompt("Value Prompt");
    }

    private void verifyNoMoreInteractionsOnMocks() {
        verifyNoMoreInteractions(mapEntry.keyBox);
        verifyNoMoreInteractions(mapEntry.valueBox);
        verifyNoMoreInteractions(mockedElement);
        verifyNoMoreInteractions(mockedKeyElement);
        verifyNoMoreInteractions(mockedValueElement);
        verifyNoMoreInteractions(mockedKeyStyle);
        verifyNoMoreInteractions(mockedValueStyle);
        verifyNoMoreInteractions(mockedValueChangeHandler);
    }

}
