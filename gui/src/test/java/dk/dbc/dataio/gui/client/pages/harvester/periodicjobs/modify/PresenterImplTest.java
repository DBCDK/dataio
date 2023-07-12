package dk.dbc.dataio.gui.client.pages.harvester.periodicjobs.modify;

import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.commons.types.jndi.RawRepo;
import dk.dbc.dataio.harvester.types.HttpPickup;
import dk.dbc.dataio.harvester.types.PeriodicJobsHarvesterConfig;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.Date;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(GwtMockitoTestRunner.class)
public class PresenterImplTest {
    @Mock
    ViewGinjector viewInjector;
    @Mock
    Texts texts;
    @Mock
    View view;

    private PresenterImpl presenter;

    static class TestablePresenterImpl extends PresenterImpl {
        private boolean saved;
        String resourceValueFromView = "";

        public TestablePresenterImpl(ViewGinjector viewInjector) {
            super("");
            initializeModel();
            this.viewInjector = viewInjector;
        }

        @Override
        void initializeModel() {
            config = new PeriodicJobsHarvesterConfig(1, 2,
                    new PeriodicJobsHarvesterConfig.Content());
        }

        @Override
        void saveModel() {
            saved = true;
        }

        @Override
        String getResourceValueFromView() {
            return resourceValueFromView;
        }

        public boolean isSaved() {
            return saved;
        }

        @Override
        public void runButtonPressed() {
        }

        @Override
        public void refreshButtonPressed() {
        }


        @Override
        public void validateSolrButtonPressed() {
        }
    }

    @Before
    public void setupMocks() {
        when(viewInjector.getView()).thenReturn(view);
        when(viewInjector.getTexts()).thenReturn(texts);
    }

    @Before
    public void createPresenter() {
        presenter = new TestablePresenterImpl(viewInjector);
    }

    @Test
    public void nameChanged() {
        presenter.nameChanged("-name-");
        assertThat(presenter.config.getContent().getName(), is("-name-"));
    }

    @Test
    public void scheduleChanged() {
        presenter.scheduleChanged("-schedule-");
        assertThat(presenter.config.getContent().getSchedule(), is("-schedule-"));
    }

    @Test
    public void descriptionChanged() {
        presenter.descriptionChanged("-description-");
        assertThat(presenter.config.getContent().getDescription(), is("-description-"));
    }

    @Test
    public void resourceChanged() {
        presenter.resourceChanged("cisterne");
        assertThat(presenter.config.getContent().getResource(),
                is(RawRepo.fromString("cisterne").getJndiResourceName()));
    }

    @Test
    public void queryChanged() {
        presenter.queryChanged("-query-");
        assertThat(presenter.config.getContent().getQuery(), is("-query-"));
    }

    @Test
    public void collectionChanged() {
        presenter.collectionChanged("-collection-");
        assertThat(presenter.config.getContent().getCollection(), is("-collection-"));
    }

    @Test
    public void destinationChanged() {
        presenter.destinationChanged("-destination-");
        assertThat(presenter.config.getContent().getDestination(), is("-destination-"));
    }

    @Test
    public void formatChanged() {
        presenter.formatChanged("-format-");
        assertThat(presenter.config.getContent().getFormat(), is("-format-"));
    }

    @Test
    public void submitterChanged() {
        presenter.submitterChanged("-submitter-");
        assertThat(presenter.config.getContent().getSubmitterNumber(), is("-submitter-"));
    }

    @Test
    public void contactChanged() {
        presenter.contactChanged("-contact-");
        assertThat(presenter.config.getContent().getContact(), is("-contact-"));
    }

    @Test
    public void timeOfLastHarvestChanged() {
        presenter.timeOfLastHarvestChanged("2019-06-10 00:00:00");
        assertThat(presenter.config.getContent().getTimeOfLastHarvest(),
                is(new Date(1560117600000L)));
    }

    @Test
    public void enabledChanged() {
        presenter.enabledChanged(true);
        assertThat(presenter.config.getContent().isEnabled(), is(true));
        presenter.enabledChanged(false);
        assertThat(presenter.config.getContent().isEnabled(), is(false));
    }

    @Test
    public void saveButtonPressed_failOnMissingInputFields() {
        presenter.saveButtonPressed();
        verify(texts).error_InputFieldValidationError();
    }

    @Test
    public void saveButtonPressed_failOnIllegalResourceValue() {
        ((TestablePresenterImpl) presenter).resourceValueFromView = "illegal";
        presenter.saveButtonPressed();
        verify(texts).error_IllegalResourceValidationError();
    }

    @Test
    public void saveButtonPressed_ok() {
        presenter.config = new PeriodicJobsHarvesterConfig(1, 2,
                new PeriodicJobsHarvesterConfig.Content()
                        .withName("-name-")
                        .withSchedule("-schedule-")
                        .withDescription("-description-")
                        .withResource("cisterne")
                        .withQuery("-query-")
                        .withCollection("-collection-")
                        .withDestination("-destination-")
                        .withFormat("-format-")
                        .withSubmitterNumber("-submitter-")
                        .withContact("-contact-")
                        .withEnabled(false)
                        .withPickup(new HttpPickup()
                                .withReceivingAgency("-receiving-agency-")));
        presenter.saveButtonPressed();
        assertThat(((TestablePresenterImpl) presenter).isSaved(), is(true));
    }
}
