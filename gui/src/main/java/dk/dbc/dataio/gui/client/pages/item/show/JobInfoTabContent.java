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

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Label;
import dk.dbc.dataio.gui.client.components.PromptedLabel;

public class JobInfoTabContent extends Composite {
    interface JobInfoTabContentUiBinder extends UiBinder<HTMLPanel, JobInfoTabContent> {
    }

    private static JobInfoTabContentUiBinder ourUiBinder = GWT.create(JobInfoTabContentUiBinder.class);

    public JobInfoTabContent() {
        initWidget(ourUiBinder.createAndBindUi(this));
    }

    @UiField PromptedLabel packaging;
    @UiField PromptedLabel format;
    @UiField PromptedLabel charset;
    @UiField PromptedLabel destination;
    @UiField PromptedLabel mailForNotificationAboutVerification;
    @UiField PromptedLabel mailForNotificationAboutProcessing;
    @UiField PromptedLabel resultMailInitials;
    @UiField PromptedLabel type;
    @UiField PromptedLabel jobCreationTime;
    @UiField PromptedLabel jobCompletionTime;
    @UiField Label exportLinksHeader;
    @UiField PromptedLabel exportLinkItemsFailedInPartitioning;
    @UiField PromptedLabel exportLinkItemsFailedInProcessing;
    @UiField PromptedLabel exportLinkItemsFailedInDelivering;

    /**
     * Ui Handler to catch click events on the Back button
     * @param event Clicked event
     */
    @UiHandler("backButton")
    void backButtonPressed(ClickEvent event) {
        History.back();
    }

}