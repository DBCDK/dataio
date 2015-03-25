package dk.dbc.dataio.gui.client.pages.item.show;

import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.ui.HTML;
import dk.dbc.dataio.gui.client.exceptions.FilteredAsyncCallback;
import dk.dbc.dataio.gui.client.model.ItemModel;
import dk.dbc.dataio.gui.client.proxies.LogStoreProxyAsync;

public class JavascriptLogTabContent extends HTML {
    private Texts texts;
    private LogStoreProxyAsync logStoreProxy;
    private final static String NBSP = new String(new char[8]).replace("\0", "\u00A0");

    public JavascriptLogTabContent(Texts texts, LogStoreProxyAsync logStoreProxy, ItemModel itemModel) {
        this.texts = texts;
        this.logStoreProxy = logStoreProxy;
        getJavaScriptLog(itemModel.getJobId(), itemModel.getChunkId(), itemModel.getItemId());
    }

    private void getJavaScriptLog(final String jobId, final String chunkId, final String failedItemId) {
        logStoreProxy.getItemLog(jobId, Long.valueOf(chunkId), Long.valueOf(failedItemId), new GetJavaScriptLogFilteredAsyncCallback());
    }

    /**
     * Call back class to be instantiated in the call to getItemLog in logStoreProxy
     */
    class GetJavaScriptLogFilteredAsyncCallback extends FilteredAsyncCallback<String> {
        @Override
        public void onFilteredFailure(Throwable caught) {
            setText(texts.error_CannotFetchJavaScriptLog());
        }

        @Override
        public void onSuccess(String log) {
            setHTML(formatLog(log));
        }
    }

    /**
     * This method replaces:
     * \n with <br></br> (new line)
     * \t with 8 times &nbsp (non-breaking spaces.)
     * This is done in order to make the log easier readable when presented in the view.
     *
     * @param log The text to be formatted
     * @return the formatted log String
     */
    private String formatLog(String log) {
        log = log.replace("\t", NBSP);
        SafeHtmlBuilder safeHtmlBuilder = new SafeHtmlBuilder().appendEscapedLines(log);
        return safeHtmlBuilder.toSafeHtml().asString();
    }


}
