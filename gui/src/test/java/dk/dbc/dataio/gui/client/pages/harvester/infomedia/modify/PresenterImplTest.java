package dk.dbc.dataio.gui.client.pages.harvester.infomedia.modify;

import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.harvester.types.InfomediaHarvesterConfig;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.Date;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.isNull;
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

        public TestablePresenterImpl(ViewGinjector viewInjector) {
            super("");
            initializeModel();
            this.viewInjector = viewInjector;
        }

        @Override
        void initializeModel() {
            config = new InfomediaHarvesterConfig(1, 1, new InfomediaHarvesterConfig.Content());
        }

        @Override
        void saveModel() {
            saved = true;
        }

        @Override
        public void deleteButtonPressed() {
        }

        public boolean isSaved() {
            return saved;
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
    public void idChanged() {
        presenter.idChanged("-id-");
        assertThat(presenter.config.getContent().getId(), is("-id-"));
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
    public void nextPublicationDateChanged() {
        presenter.nextPublicationDateChanged("2019-06-10 00:00:00");
        assertThat(presenter.config.getContent().getNextPublicationDate(),
                is(new Date(1560117600000L + 2 * 3600 * 1000)));
    }

    @Test
    public void enabledChanged() {
        presenter.enabledChanged(true);
        assertThat(presenter.config.getContent().isEnabled(), is(true));
        presenter.enabledChanged(false);
        assertThat(presenter.config.getContent().isEnabled(), is(false));
    }

    @Test
    public void saveButtonPressed_fail() {
        presenter.saveButtonPressed();
        verify(texts).error_InputFieldValidationError();
        verify(presenter.getView()).setErrorText(isNull());
    }

    @Test
    public void saveButtonPressed_ok() {
        presenter.config = new InfomediaHarvesterConfig(1, 1,
                new InfomediaHarvesterConfig.Content()
                        .withId("-id-")
                        .withSchedule("-schedule-")
                        .withDescription("-description-")
                        .withDestination("-destination-")
                        .withFormat("-format-")
                        .withEnabled(false));
        presenter.saveButtonPressed();
        assertThat(((TestablePresenterImpl) presenter).isSaved(), is(true));
    }
}
