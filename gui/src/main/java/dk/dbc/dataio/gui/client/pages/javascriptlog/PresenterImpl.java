package dk.dbc.dataio.gui.client.pages.javascriptlog;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.Place;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import dk.dbc.dataio.gui.client.exceptions.FilteredAsyncCallback;
import dk.dbc.dataio.gui.client.proxies.LogStoreProxyAsync;
import dk.dbc.dataio.gui.util.ClientFactory;

public class PresenterImpl extends AbstractActivity implements Presenter {

    protected ClientFactory clientFactory;
    protected View view;
    protected LogStoreProxyAsync logStoreProxy;
    protected Texts texts;

    protected String jobId;
    protected Long chunkId;
    protected Long failedItemId;
    private final static String NBSP = new String(new char[8]).replace("\0", "\u00A0");

    /**
     * Constructor
     * @param clientFactory
     * @param texts
     */
    public PresenterImpl(Place place, ClientFactory clientFactory, Texts texts) {
        this.clientFactory = clientFactory;
        this.logStoreProxy = clientFactory.getLogStoreProxyAsync();
        this.texts = texts;

        JavaScriptLogPlace javaScriptLogPlace = (JavaScriptLogPlace) place;
        this.jobId = javaScriptLogPlace.getJobId().toString();
        this.chunkId = javaScriptLogPlace.getChunkId();
        this.failedItemId = javaScriptLogPlace.getFailedItemId();
    }

    @Override
    public void start(AcceptsOneWidget containerWidget, EventBus eventBus) {
        view = clientFactory.getJavaScriptLogView();
        view.setPresenter(this);
        containerWidget.setWidget(view.asWidget());
        initialize();
    }

    /**
     * Initialize:
     * Fetch the java script log identified by this.jobId, this.chunkId, this.failedItemId
     */
    public void initialize() {
        getJavaScriptLog(jobId, chunkId, failedItemId);
    }

    private void getJavaScriptLog(final String jobId, final Long chunkId, final Long failedItemId) {
        logStoreProxy.getItemLog(jobId, chunkId, failedItemId, new GetJavaScriptLogFilteredAsyncCallback());
    }

    /**
     * Call back class to be instantiated in the call to getItemLog in logStoreProxy
     */
    class GetJavaScriptLogFilteredAsyncCallback extends FilteredAsyncCallback<String> {
        @Override
        public void onFilteredFailure(Throwable caught) {
            view.setErrorText(texts.error_CannotFetchJavaScriptLog());
        }

        @Override
        public void onSuccess(String log) {
            view.htmlLabel.setHTML(formatLog(log));
        }
    }

    /**
     * This method replaces:
     * \n with <br></br> (new line)
     * \t with 8 times &nbsp (non-breaking spaces.)
     * This is done in order to make the log easier readable when presented in the view.
     *
     * @param log
     * @return the formatted log String
     */
    private String formatLog(String log) {
        log = log.replace("\t", NBSP);
        SafeHtmlBuilder safeHtmlBuilder = new SafeHtmlBuilder().appendEscapedLines(log);
        return safeHtmlBuilder.toSafeHtml().asString();
    }

}
