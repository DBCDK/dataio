package dk.dbc.dataio.gui.client.pages.item.show;

import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.gui.client.model.ItemModel;
import dk.dbc.dataio.gui.client.modelBuilders.ItemModelBuilder;
import dk.dbc.dataio.gui.client.proxies.JobStoreProxyAsync;
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

@RunWith(GwtMockitoTestRunner.class)
public class ItemTabContentTest {
    final String CANNOT_FETCH_ITEM_DATA = "Mocked Cannot fetch chunk data";
    final ItemModel.LifeCycle LIFECYCLE = ItemModel.LifeCycle.PROCESSING;
    final ItemModel TEST_ITEM_MODEL = new ItemModelBuilder().setLifeCycle(ItemModel.LifeCycle.PROCESSING).build();

    @Mock
    JobStoreProxyAsync mockedJobStoreProxy;
    @Mock
    Texts mockedTexts;
    @Mock
    Throwable mockedThrowable;


    class ItemTabContentConcrete extends ItemTabContent {
        GetItemDataAsyncCallback callback = new GetItemDataAsyncCallback();
        String text = "";
        String htmlText = "";

        public ItemTabContentConcrete(Texts texts, JobStoreProxyAsync jobStoreProxy, ItemModel itemModel, ItemModel.LifeCycle lifeCycle) {
            super(texts, jobStoreProxy, itemModel, lifeCycle);
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
        when(mockedTexts.error_CouldNotFetchData()).thenReturn(CANNOT_FETCH_ITEM_DATA);
    }

    @Test
    public void instantiate_instantiate_logStoreIsCalled() {

        // Subject under test
        new ItemTabContent(mockedTexts, mockedJobStoreProxy, TEST_ITEM_MODEL, LIFECYCLE);

        // Test Verification
        verify(mockedJobStoreProxy).getItemData(
                eq(TEST_ITEM_MODEL),
                eq(LIFECYCLE), any(ItemTabContent.GetItemDataAsyncCallback.class));
    }

    @Test
    public void callback_callOnFailure_errorMessageIsGiven() {
        // Test preparation
        ItemTabContentConcrete inputPostTabContentConcrete = new ItemTabContentConcrete(mockedTexts, mockedJobStoreProxy, TEST_ITEM_MODEL, LIFECYCLE);

        // Subject under test
        inputPostTabContentConcrete.callback.onFailure(mockedThrowable);

        // Test verification
        verify(mockedTexts).error_CouldNotFetchData();
        assertThat(inputPostTabContentConcrete.text, is(CANNOT_FETCH_ITEM_DATA));
    }

    @Test
    public void callback_callOnSuccess_formatIsCalled() {
        // Test preparation
        ItemTabContentConcrete inputPostTabContentConcrete = new ItemTabContentConcrete(mockedTexts, mockedJobStoreProxy, TEST_ITEM_MODEL, LIFECYCLE);

        final String data = "chunk item data <datafield ind1=\"0\" ind2=\"0\" tag=\"004\">" +
                "<subfield code=\"r\">n</subfield>" +
                "<subfield code=\"a\">e</subfield>" +
                "</datafield>";

        // Subject under test
        inputPostTabContentConcrete.callback.onSuccess(data);

        // Test verification
        assertThat(inputPostTabContentConcrete.htmlText, is(
                "chunk item data &lt;datafield ind1=&quot;0&quot; " +
                        "ind2=&quot;0&quot; tag=&quot;004&quot;&gt;&lt;subfield " +
                        "code=&quot;r&quot;&gt;n&lt;/subfield&gt;&lt;subfield " +
                        "code=&quot;a&quot;&gt;e&lt;/subfield&gt;&lt;/datafield&gt;"
        ));
    }
}
