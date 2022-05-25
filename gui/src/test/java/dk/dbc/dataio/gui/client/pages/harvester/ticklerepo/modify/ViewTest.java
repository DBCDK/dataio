package dk.dbc.dataio.gui.client.pages.harvester.ticklerepo.modify;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwtmockito.GwtMockitoTestRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;


/**
 * PresenterImpl unit tests
 * <p/>
 * The test methods of this class uses the following naming convention:
 * <p/>
 * unitOfWork_stateUnderTest_expectedBehavior
 */
@RunWith(GwtMockitoTestRunner.class)
public class ViewTest {
    @Mock
    Presenter mockedPresenter;
    @Mock
    ValueChangeEvent mockedValueChangeEvent;
    @Mock
    ClickEvent mockedClickEvent;

    // Subject Under Test
    private View view;


    @Before
    public void setupMocks() {
        view = new View();
        view.setPresenter(mockedPresenter);
        when(view.id.getText()).thenReturn("-id-");
        when(view.name.getText()).thenReturn("-name-");
        when(view.description.getText()).thenReturn("-description-");
        when(view.destination.getText()).thenReturn("-destination-");
        when(view.format.getText()).thenReturn("-format-");
        when(view.type.getSelectedKey()).thenReturn("-type-");
        when(view.enabled.getValue()).thenReturn(true);
    }

    @After
    public void verifyNoMoreMockCalls() {
        verifyNoMoreInteractions(mockedPresenter);
        verifyNoMoreInteractions(mockedValueChangeEvent);
        verifyNoMoreInteractions(mockedClickEvent);
        verifyNoMoreInteractions(view.id);
        verifyNoMoreInteractions(view.name);
        verifyNoMoreInteractions(view.description);
        verifyNoMoreInteractions(view.destination);
        verifyNoMoreInteractions(view.format);
        verifyNoMoreInteractions(view.type);
        verifyNoMoreInteractions(view.enabled);
        verifyNoMoreInteractions(view.status);
    }


    /*
     * Testing starts here...
     */

    @Test
    public void constructor_instantiate_objectCorrectInitialized() {
        // Subject Under Test
        view = new View();
    }

    @Test
    public void idChanged_call_idChanged() {
        // Subject Under Test
        view.idChanged(mockedValueChangeEvent);

        // Test verification
        verify(view.id).getText();
        verify(mockedPresenter).idChanged("-id-");
        verify(mockedPresenter).keyPressed();
    }

    @Test
    public void nameChanged_call_nameChanged() {
        // Subject Under Test
        view.nameChanged(mockedValueChangeEvent);

        // Test verification
        verify(view.name).getText();
        verify(mockedPresenter).nameChanged("-name-");
        verify(mockedPresenter).keyPressed();
    }

    @Test
    public void descriptionChanged_call_descriptionChanged() {
        // Subject Under Test
        view.descriptionChanged(mockedValueChangeEvent);

        // Test verification
        verify(view.description).getText();
        verify(mockedPresenter).descriptionChanged("-description-");
        verify(mockedPresenter).keyPressed();
    }

    @Test
    public void destinationChanged_call_destinationChanged() {
        // Subject Under Test
        view.destinationChanged(mockedValueChangeEvent);

        // Test verification
        verify(view.destination).getText();
        verify(mockedPresenter).destinationChanged("-destination-");
        verify(mockedPresenter).keyPressed();
    }

    @Test
    public void formatChanged_call_formatChanged() {
        // Subject Under Test
        view.formatChanged(mockedValueChangeEvent);

        // Test verification
        verify(view.format).getText();
        verify(mockedPresenter).formatChanged("-format-");
        verify(mockedPresenter).keyPressed();
    }

    @Test
    public void typeChanged_call_typeChanged() {
        // Subject Under Test
        view.typeChanged(mockedValueChangeEvent);

        // Test verification
        verify(view.type).getSelectedKey();
        verify(mockedPresenter).typeChanged("-type-");
        verify(mockedPresenter).keyPressed();
    }

    @Test
    public void enabledChanged_call_enabledChanged() {
        // Subject Under Test
        view.enabledChanged(mockedValueChangeEvent);

        // Test verification
        verify(view.enabled).getValue();
        verify(mockedPresenter).enabledChanged(true);
        verify(mockedPresenter).keyPressed();
    }

    @Test
    public void saveButtonPressed_call_presenterSignalled() {
        // Subject Under Test
        view.saveButtonPressed(mockedClickEvent);

        // Test verification
        verify(mockedPresenter).saveButtonPressed();
    }

    @Test
    public void taskRecordHarvestButtonPressed_call_presenterSignalled() {
        // Subject Under Test
        view.taskRecordHarvestButtonPressed(mockedClickEvent);

        // Test verification
        verify(mockedPresenter).setRecordHarvestCount();
    }

}
