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

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dk.dbc.dataio.gui.client.pages.item.show;

import com.google.gwt.activity.shared.Activity;
import com.google.gwt.place.shared.PlaceTokenizer;
import com.google.gwt.place.shared.Prefix;
import dk.dbc.dataio.gui.client.places.AbstractBasePlace;
import dk.dbc.dataio.gui.util.ClientFactory;

/**
 * Place
 */
public class Place extends AbstractBasePlace {
    static final String RECORD_ID = "recordId";
    static final String JOB_ID = "jobId";
    public static final String TOKEN = "ShowItems";

    public Place() {
        super();
    }

    /**
     * Constructor taking a Token
     *
     * @param token The token to be used
     */
    public Place(String token) {
        super(token);
    }

    public Place(String jobId, String recordId) {
        addParameter(JOB_ID, jobId);
        if(recordId != null) {
            addParameter(RECORD_ID, recordId);
        }
    }

    @Override
    public Activity createPresenter(ClientFactory clientFactory) {
        return new PresenterImpl(
                this,
                clientFactory.getPlaceController(),
                clientFactory.getGlobalViewsFactory().getItemsView(),
                commonInjector.getMenuTexts().menu_Items());
    }

    @Prefix(TOKEN)
    public static class Tokenizer implements PlaceTokenizer<Place> {
        @Override
        public String getToken(Place place) {
            return place.getToken();
        }

        @Override
        public Place getPlace(String token) {
            return new Place(token);
        }
    }
}