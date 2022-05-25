package dk.dbc.dataio.gui.client.pages.harvester.corepo.modify;

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
        when(view.name.getText()).thenReturn("-name-");
        when(view.description.getText()).thenReturn("-description-");
        when(view.resource.getText()).thenReturn("-resource-");
        when(view.rrHarvester.getSelectedKey()).thenReturn("1234");
        when(view.enabled.getValue()).thenReturn(true);
    }

    @After
    public void verifyNoMoreMockCalls() {
        verifyNoMoreInteractions(mockedPresenter);
        verifyNoMoreInteractions(mockedValueChangeEvent);
        verifyNoMoreInteractions(mockedClickEvent);
        verifyNoMoreInteractions(view.name);
        verifyNoMoreInteractions(view.description);
        verifyNoMoreInteractions(view.resource);
        verifyNoMoreInteractions(view.rrHarvester);
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
    public void resourceChanged_call_resourceChanged() {
        // Subject Under Test
        view.resourceChanged(mockedValueChangeEvent);

        // Test verification
        verify(view.resource).getText();
        verify(mockedPresenter).resourceChanged("-resource-");
        verify(mockedPresenter).keyPressed();
    }

    @Test
    public void rrHarvesterChanged_call_rrHarvesterChanged() {
        // Subject Under Test
        view.rrHarvesterChanged(mockedValueChangeEvent);

        // Test verification
        verify(view.rrHarvester).getSelectedKey();
        verify(mockedPresenter).rrHarvesterChanged("1234");
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

}
