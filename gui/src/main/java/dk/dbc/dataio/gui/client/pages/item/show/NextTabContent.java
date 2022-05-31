package dk.dbc.dataio.gui.client.pages.item.show;

import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HTML;
import dk.dbc.dataio.gui.client.model.ItemModel;
import dk.dbc.dataio.gui.client.proxies.JobStoreProxyAsync;

public class NextTabContent extends HTML {

    private Texts texts;
    private JobStoreProxyAsync jobStoreProxy;
    private final static String NBSP = new String(new char[4]).replace("\0", "\u00A0");

    public NextTabContent(Texts texts, JobStoreProxyAsync jobStoreProxy, ItemModel itemModel) {
        this.texts = texts;
        this.jobStoreProxy = jobStoreProxy;
        getProcessedNextResult(itemModel);
    }

    private void getProcessedNextResult(ItemModel itemModel) {
        jobStoreProxy.getProcessedNextResult(
                Long.valueOf(itemModel.getJobId()).intValue(),
                Long.valueOf(itemModel.getChunkId()).intValue(),
                Long.valueOf(itemModel.getItemId()).shortValue(),
                new GetItemDataAsyncCallback());
    }

    /*
     * Callback class to be instantiated in the call to getProcessedNextResult in jobStoreProxy
     */
    class GetItemDataAsyncCallback implements AsyncCallback<String> {

        @Override
        public void onFailure(Throwable throwable) {
            setText(texts.error_CouldNotFetchData());
        }

        @Override
        public void onSuccess(String data) {
            setHTML(formatXml(data));
        }
    }

    /**
     * This method replaces:
     * \n with <br></br> (new line)
     * \t with 8 times &nbsp (non-breaking spaces.)
     * This is done in order to make the log easier readable when presented in the view.
     *
     * @param data The text to be formatted
     * @return the formatted data String
     */
    private String formatXml(String data) {
        data = data.replace("\t", NBSP);
        data = data.replace("/\n/g", "<br />");
        SafeHtmlBuilder safeHtmlBuilder = new SafeHtmlBuilder().appendEscapedLines(data);
        return safeHtmlBuilder.toSafeHtml().asString();
    }
}
