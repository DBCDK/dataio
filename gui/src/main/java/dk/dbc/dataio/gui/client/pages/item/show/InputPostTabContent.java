package dk.dbc.dataio.gui.client.pages.item.show;

import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HTML;
import dk.dbc.dataio.gui.client.model.ItemModel;
import dk.dbc.dataio.gui.client.proxies.JobStoreProxyAsync;

public class InputPostTabContent extends HTML{
    private Texts texts;
    private JobStoreProxyAsync jobStoreProxy;
    private final static String NBSP = new String(new char[8]).replace("\0", "\u00A0");
    private final static String AMP = "\u0026";

    public InputPostTabContent(Texts texts, JobStoreProxyAsync jobStoreProxy, ItemModel itemModel, ItemModel.LifeCycle lifeCycle) {
        this.texts = texts;
        this.jobStoreProxy = jobStoreProxy;
        getItemData(itemModel, lifeCycle);
    }

    private void getItemData(ItemModel itemModel, ItemModel.LifeCycle lifeCycle) {
        jobStoreProxy.getItemData(
                Long.valueOf(itemModel.getJobId()).intValue(),
                Long.valueOf(itemModel.getChunkId()).intValue(),
                Long.valueOf(itemModel.getItemId()).shortValue(),
                lifeCycle,
                new GetItemDataAsyncCallback());
    }

    /*
     * Callback class to be instantiated in the call to getChunkItem in jobStoreProxy
     */
    class GetItemDataAsyncCallback implements AsyncCallback<String> {

        @Override
        public void onFailure(Throwable throwable) {
            setText(texts.error_CouldNotFetchItemData());
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
     * \&amp with &
     * This is done in order to make the log easier readable when presented in the view.
     * It also escapes the tag brackets: "<" and ">" in order to display the text in xml format
     *
     * @param data The text to be formatted
     * @return the formatted data String
     */
    private String formatXml(String data) {
        data = data.replace("\t", NBSP);
        data = data.replace("&amp;", AMP);
        SafeHtmlBuilder safeHtmlBuilder = new SafeHtmlBuilder().appendEscapedLines(data).appendEscaped("<").appendEscaped(">");
        return safeHtmlBuilder.toSafeHtml().asString();
    }
}
