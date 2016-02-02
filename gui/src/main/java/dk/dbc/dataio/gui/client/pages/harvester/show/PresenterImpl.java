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

package dk.dbc.dataio.gui.client.pages.harvester.show;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.gui.client.util.CommonGinjector;
import dk.dbc.dataio.harvester.types.OpenAgencyTarget;
import dk.dbc.dataio.harvester.types.RawRepoHarvesterConfig;


/**
 * This class represents the show harvesters presenter implementation
 */
public class PresenterImpl extends AbstractActivity implements Presenter {

    ViewGinjector viewInjector = GWT.create(ViewGinjector.class);
    CommonGinjector commonInjector = GWT.create(CommonGinjector.class);


    /**
     * Default constructor
     */
    public PresenterImpl() {
    }


    /**
     * start method
     * Is called by PlaceManager, whenever the Place is being invoked
     * This method is the start signal for the presenter
     *
     * @param containerWidget the widget to use
     * @param eventBus        the eventBus to use
     */
    @Override
    public void start(AcceptsOneWidget containerWidget, EventBus eventBus) {
        getView().setPresenter(this);
        getView().setHeader(commonInjector.getMenuTexts().menu_Harvesters());
        containerWidget.setWidget(getView().asWidget());
        fetchHarvesters();
    }


    /*
     * Private methods
     */

    /**
     * This method fetches all harvesters, and sends them to the view
     */
    private void fetchHarvesters() {
        // Preliminary data setup until Presenter is here
        RawRepoHarvesterConfig config = new RawRepoHarvesterConfig();

        RawRepoHarvesterConfig.Entry entry1 = new RawRepoHarvesterConfig.Entry();
        entry1.setId("broend-sync(id)");
        entry1.setResource("jdbc/dataio/rawrepo");
        OpenAgencyTarget openAgencyTarget1 = new OpenAgencyTarget();
        openAgencyTarget1.setUrl("http://openagency.dbc.dk/noget");
        entry1.setOpenAgencyTarget(openAgencyTarget1);
        entry1.setConsumerId("broend-sync(consumerid)");
        entry1.setBatchSize(10000);
        entry1.setFormatOverride(870970, "basis");
        entry1.setIncludeRelations(true);
        entry1.setDestination("testbroend-i01");
        entry1.setFormat("katalog");
        entry1.setType(JobSpecification.Type.TEST);
        config.addEntry(entry1);

        RawRepoHarvesterConfig.Entry entry2 = new RawRepoHarvesterConfig.Entry();
        entry2.setId("broend-sync(id)2");
        entry2.setResource("jdbc/dataio/rawrepo2");
        OpenAgencyTarget openAgencyTarget2 = new OpenAgencyTarget();
        openAgencyTarget2.setUrl("http://openagency.dbc.dk/noget2");
        entry2.setOpenAgencyTarget(openAgencyTarget2);
        entry2.setConsumerId("broend-sync(consumerid)2");
        entry2.setBatchSize(10001);
        entry2.setFormatOverride(870970, "basis2");
        entry2.setIncludeRelations(false);
        entry2.setDestination("testbroend-i02");
        entry2.setFormat("katalog2");
        entry2.setType(JobSpecification.Type.TRANSIENT);
        config.addEntry(entry2);

        RawRepoHarvesterConfig.Entry entry3 = new RawRepoHarvesterConfig.Entry();
        entry3.setId("broend-sync(id)3");
        entry3.setResource("jdbc/dataio/rawrepo3");
        OpenAgencyTarget openAgencyTarget3 = new OpenAgencyTarget();
        openAgencyTarget3.setUrl("http://openagency.dbc.dk/noget3");
        entry3.setOpenAgencyTarget(openAgencyTarget3);
        entry3.setConsumerId("broend-sync(consumerid)3");
        entry3.setBatchSize(10003);
        entry3.setFormatOverride(870970, "basis3");
        entry3.setIncludeRelations(false);
        entry3.setDestination("testbroend-i03");
        entry3.setFormat("katalog3");
        entry3.setType(JobSpecification.Type.TRANSIENT);
        config.addEntry(entry3);

        getView().setHarvesters(config);
    }


    /*
     * Private methods
     */

    private View getView() {
        return viewInjector.getView();
    }
}
