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

    @Mock JobStoreProxyAsync mockedJobStoreProxy;
    @Mock Texts mockedTexts;
    @Mock Throwable mockedThrowable;


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
