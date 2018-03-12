/*
 * DataIO - Data IO
 * Copyright (C) 2018 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
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

package dk.dbc.dataio.gui.client.pages.failedftps.show;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Focusable;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.HasValue;
import com.google.gwt.user.client.ui.TextArea;


public class EditTransFileView extends FlowPanel implements HasValue<String>, Focusable {
    interface UiTrafficBinder extends UiBinder<HTMLPanel, EditTransFileView> {
    }

    private static UiTrafficBinder uiBinder = GWT.create(UiTrafficBinder.class);

    public EditTransFileView() {
        add(uiBinder.createAndBindUi(this));
    }

    @UiField HTMLPanel transFileContentContainer;
    @UiField TextArea transFileContent;
    @UiField HTMLPanel mailNotificationContainer;
    @UiField Element mailNotification;


    /**
     * Sets the text for the Mail content
     * @param text The Mail content
     */
    public void setMailContent(String text) {
        mailNotification.setInnerText(text);
    }


    /*
     * Implementation of the abstract methods, declared in HasValue
     */

    @Override
    public String getValue() {
        return transFileContent.getValue();
    }

    @Override
    public void setValue(String value) {
        transFileContent.setValue(value);
    }

    @Override
    public void setValue(String value, boolean fireEvents) {
        transFileContent.setValue(value, fireEvents);
    }

    @Override
    public HandlerRegistration addValueChangeHandler(ValueChangeHandler handler) {
        return transFileContent.addValueChangeHandler(handler);
    }


    /*
     * Implementation of the abstract methods, declared in Focusable
     */
    @Override
    public int getTabIndex() {
        return transFileContent.getTabIndex();
    }

    @Override
    public void setAccessKey(char key) {
        transFileContent.setAccessKey(key);
    }

    @Override
    public void setFocus(boolean focused) {
        transFileContent.setFocus(focused);
    }

    @Override
    public void setTabIndex(int index) {
        transFileContent.setTabIndex(index);
    }

}

