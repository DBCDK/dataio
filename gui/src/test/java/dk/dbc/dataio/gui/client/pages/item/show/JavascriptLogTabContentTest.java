package dk.dbc.dataio.gui.client.pages.item.show;


import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.gui.client.exceptions.FilteredAsyncCallback;
import dk.dbc.dataio.gui.client.model.ItemModel;
import dk.dbc.dataio.gui.client.modelBuilders.ItemModelBuilder;
import dk.dbc.dataio.gui.client.proxies.LogStoreProxyAsync;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * PresenterImpl unit tests
 * <p>
 * The test methods of this class uses the following naming convention:
 * <p>
 * unitOfWork_stateUnderTest_expectedBehavior
 */
@RunWith(GwtMockitoTestRunner.class)
public class JavascriptLogTabContentTest {
    final ItemModel TEST_ITEM_MODEL = new ItemModelBuilder().build();
    final String CANNOT_FETCH_JAVASCRIPT_LOG = "Mocked Cannot fetch javascript log";
    private final static String NBSP = new String(new char[8]).replace("\0", "\u00A0");

    @Mock
    LogStoreProxyAsync mockedLogStoreProxy;
    @Mock
    Texts mockedTexts;
    @Mock
    Throwable mockedThrowable;


    class JavascriptLogTabContentConcrete extends JavascriptLogTabContent {
        GetJavaScriptLogFilteredAsyncCallback callback = new GetJavaScriptLogFilteredAsyncCallback();
        String text = "";
        String htmlText = "";

        public JavascriptLogTabContentConcrete(Texts texts, LogStoreProxyAsync logStoreProxy, ItemModel itemModel) {
            super(texts, logStoreProxy, itemModel);
        }

        @Override
        public void setText(String text) {
            this.text = text;
        }

        @Override
        public void setHTML(String html) {
            this.htmlText = html;
        }
    }

    @Before
    public void prepareMocks() {
        when(mockedTexts.error_CannotFetchJavaScriptLog()).thenReturn(CANNOT_FETCH_JAVASCRIPT_LOG);
    }

    @Test
    public void instantiate_instantiate_logStoreIsCalled() {

        // Subject under test
        JavascriptLogTabContent javascriptLogTabContent = new JavascriptLogTabContent(mockedTexts, mockedLogStoreProxy, TEST_ITEM_MODEL);

        // Test Verification
        verify(mockedLogStoreProxy).getItemLog(eq(TEST_ITEM_MODEL.getJobId()), eq(Long.valueOf(TEST_ITEM_MODEL.getChunkId())), eq(Long.valueOf(TEST_ITEM_MODEL.getItemId())), any(FilteredAsyncCallback.class));
    }

    @Test
    public void callback_callOnFailure_errorMessageIsGiven() {
        // Test preparation
        JavascriptLogTabContentConcrete javascriptLogTabContent = new JavascriptLogTabContentConcrete(mockedTexts, mockedLogStoreProxy, TEST_ITEM_MODEL);

        // Subject under test
        javascriptLogTabContent.callback.onFilteredFailure(mockedThrowable);

        // Test verification
        verify(mockedTexts).error_CannotFetchJavaScriptLog();
        assertThat(javascriptLogTabContent.text, is(CANNOT_FETCH_JAVASCRIPT_LOG));
    }

    @Test
    public void callback_callOnSuccess_formatIsCalled() {
        // Test preparation
        JavascriptLogTabContentConcrete javascriptLogTabContent = new JavascriptLogTabContentConcrete(mockedTexts, mockedLogStoreProxy, TEST_ITEM_MODEL);

        // Subject under test
        javascriptLogTabContent.callback.onSuccess("Javascript log <b>Fed</b>\nTabulator\tTo\t\t\n");

        // Test verification
        assertThat(javascriptLogTabContent.htmlText, is("Javascript log &lt;b&gt;Fed&lt;/b&gt;<br>Tabulator" + NBSP + "To" + NBSP + NBSP + "<br>"));
    }

}
