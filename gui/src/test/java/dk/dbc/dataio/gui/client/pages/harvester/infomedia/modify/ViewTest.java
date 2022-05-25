package dk.dbc.dataio.gui.client.pages.harvester.infomedia.modify;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwtmockito.GwtMockitoTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(GwtMockitoTestRunner.class)
public class ViewTest {
    @Mock
    Presenter presenter;
    @Mock
    ValueChangeEvent valueChangeEvent;
    @Mock
    ClickEvent clickEvent;

    private View view;

    @Before
    public void setupMocks() {
        view = new View();
        view.setPresenter(presenter);
        when(view.id.getText()).thenReturn("-id-");
        when(view.schedule.getText()).thenReturn("-schedule-");
        when(view.description.getText()).thenReturn("-description-");
        when(view.destination.getText()).thenReturn("-destination-");
        when(view.format.getText()).thenReturn("-format-");
        when(view.nextPublicationDate.getValue()).thenReturn("-nextPublicationDate-");
        when(view.enabled.getValue()).thenReturn(true);
    }

    @Test
    public void idChanged() {
        view.idChanged(valueChangeEvent);
        verify(presenter).idChanged("-id-");
        verify(presenter).keyPressed();
    }

    @Test
    public void scheduleChanged() {
        view.scheduleChanged(valueChangeEvent);
        verify(presenter).scheduleChanged("-schedule-");
        verify(presenter).keyPressed();
    }

    @Test
    public void descriptionChanged() {
        view.descriptionChanged(valueChangeEvent);
        verify(presenter).descriptionChanged("-description-");
        verify(presenter).keyPressed();
    }

    @Test
    public void destinationChanged() {
        view.destinationChanged(valueChangeEvent);
        verify(presenter).destinationChanged("-destination-");
        verify(presenter).keyPressed();
    }

    @Test
    public void formatChanged() {
        view.formatChanged(valueChangeEvent);
        verify(presenter).formatChanged("-format-");
        verify(presenter).keyPressed();
    }

    @Test
    public void nextPublicationDateChanged() {
        view.nextPublicationDateChanged(valueChangeEvent);
        verify(presenter).nextPublicationDateChanged("-nextPublicationDate-");
        verify(presenter).keyPressed();
    }

    @Test
    public void enabledChanged() {
        view.enabledChanged(valueChangeEvent);
        verify(presenter).enabledChanged(true);
        verify(presenter).keyPressed();
    }

    @Test
    public void saveButtonPressed() {
        view.saveButtonPressed(clickEvent);
        verify(presenter).saveButtonPressed();
    }
}
