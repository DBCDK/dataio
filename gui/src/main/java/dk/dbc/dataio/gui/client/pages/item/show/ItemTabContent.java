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

import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HTML;
import dk.dbc.dataio.gui.client.model.ItemModel;
import dk.dbc.dataio.gui.client.proxies.JobStoreProxyAsync;

public class ItemTabContent extends HTML{
    private Texts texts;
    private JobStoreProxyAsync jobStoreProxy;
    private final static String NBSP = new String(new char[4]).replace("\0", "\u00A0");

    public ItemTabContent(Texts texts, JobStoreProxyAsync jobStoreProxy, ItemModel itemModel, ItemModel.LifeCycle lifeCycle) {
        this.texts = texts;
        this.jobStoreProxy = jobStoreProxy;
        getItemData(itemModel, lifeCycle);
    }

    private void getItemData(ItemModel itemModel, ItemModel.LifeCycle lifeCycle) {
        jobStoreProxy.getItemData(itemModel, lifeCycle, new GetItemDataAsyncCallback());
    }

    /*
     * Callback class to be instantiated in the call to getChunkItem in jobStoreProxy
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
