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
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.ui.AcceptsOneWidget;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by sma on 25/04/14.
 */
public class HarvestersShowActivity extends AbstractActivity implements HarvestersShowPresenter {

    //TODO indkommenter når tilføjet til ClientFactory
//    private final ClientFactory clientFactory;
//    private HarvestersShowView harvestersShowView;
//    private final FlowStoreProxyAsync flowStoreProxy;
//    private final PlaceController placeController;
//
//    public HarvestersShowActivity(ClientFactory clientFactory) {
//        this.clientFactory = clientFactory;
//        flowStoreProxy = clientFactory.getFlowStoreProxyAsync();
//        placeController = clientFactory.getPlaceController();
//    }

    private void bind() {
//        harvestersShowView = clientFactory.getHarvestersShowView();
//        harvestersShowView.setPresenter(this);
    }

    @Override
    public void start(AcceptsOneWidget containerWidget, EventBus eventBus) {
        bind();
//        containerWidget.setWidget(harvestersShowView.asWidget());
        fetchHarvesters();
    }

    // Local methods

    // TODO the "dummy harvester" needs to be replaced with a real object.
    private void fetchHarvesters() {

        List<String> dummyListForHarvesters = new ArrayList<String>();
        dummyListForHarvesters.add("DummyHarvester");
//        harvestersShowView.setHarvesters(dummyListForHarvesters);
    }
}
