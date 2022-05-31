package dk.dbc.dataio.gui.client.pages.harvester.periodicjobs.modify;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.harvester.types.PeriodicJobsHarvesterConfig;
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
        when(view.pickupTypeSelection.getSelectedKey())
                .thenReturn(PeriodicJobsHarvesterConfig.PickupType.HTTP.name());
        when(view.name.getText()).thenReturn("-name-");
        when(view.schedule.getText()).thenReturn("-schedule-");
        when(view.description.getText()).thenReturn("-description-");
        when(view.resource.getText()).thenReturn("-resource-");
        when(view.query.getText()).thenReturn("-query-");
        when(view.collection.getText()).thenReturn("-collection-");
        when(view.destination.getText()).thenReturn("-destination-");
        when(view.format.getText()).thenReturn("-format-");
        when(view.submitter.getText()).thenReturn("-submitter-");
        when(view.contact.getText()).thenReturn("-contact-");
        when(view.timeOfLastHarvest.getValue()).thenReturn("-timeOfLastHarvest-");
        when(view.enabled.getValue()).thenReturn(true);
    }

    @Test
    public void pickupTypeChanged() {
        view.pickupTypeSelectionChanged(valueChangeEvent);
        verify(presenter).pickupTypeChanged(PeriodicJobsHarvesterConfig.PickupType.HTTP);
        verify(presenter).keyPressed();
    }

    @Test
    public void nameChanged() {
        view.nameChanged(valueChangeEvent);
        verify(presenter).nameChanged("-name-");
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
    public void resourceChanged() {
        view.resourceChanged(valueChangeEvent);
        verify(presenter).resourceChanged("-resource-");
        verify(presenter).keyPressed();
    }

    @Test
    public void queryChanged() {
        view.queryChanged(valueChangeEvent);
        verify(presenter).queryChanged("-query-");
        verify(presenter).keyPressed();
    }

    @Test
    public void collectionChanged() {
        view.collectionChanged(valueChangeEvent);
        verify(presenter).collectionChanged("-collection-");
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
    public void submitterChanged() {
        view.submitterChanged(valueChangeEvent);
        verify(presenter).submitterChanged("-submitter-");
        verify(presenter).keyPressed();
    }

    @Test
    public void contactChanged() {
        view.contactChanged(valueChangeEvent);
        verify(presenter).contactChanged("-contact-");
        verify(presenter).keyPressed();
    }

    @Test
    public void timeOfLastHarvestChanged() {
        view.timeOfLastHarvestChanged(valueChangeEvent);
        verify(presenter).timeOfLastHarvestChanged("-timeOfLastHarvest-");
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
