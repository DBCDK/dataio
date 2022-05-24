/*
 * DataIO - Data IO
 * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 * Denmark. CVR: 15149043
 *
 * This file is part of DataIO.
 *
 * DataIO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DataIO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
 */


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

import static org.hamcrest.core.Is.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * PresenterImpl unit tests
 *
 * The test methods of this class uses the following naming convention:
 *
 *  unitOfWork_stateUnderTest_expectedBehavior
 */
@RunWith(GwtMockitoTestRunner.class)
public class JavascriptLogTabContentTest {
    final ItemModel TEST_ITEM_MODEL = new ItemModelBuilder().build();
    final String CANNOT_FETCH_JAVASCRIPT_LOG = "Mocked Cannot fetch javascript log";
    private final static String NBSP = new String(new char[8]).replace("\0", "\u00A0");

    @Mock LogStoreProxyAsync mockedLogStoreProxy;
    @Mock Texts mockedTexts;
    @Mock Throwable mockedThrowable;


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
