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

import com.google.gwt.activity.shared.Activity;
import com.google.gwt.place.shared.PlaceTokenizer;
import com.google.gwt.place.shared.Prefix;
import dk.dbc.dataio.gui.client.places.AbstractBasePlace;
import dk.dbc.dataio.gui.util.ClientFactory;

public class Place extends AbstractBasePlace {

    public Place() {
    }

    @Override
    public Activity createPresenter(ClientFactory clientFactory) {
        return new dk.dbc.dataio.gui.client.pages.ftp.show.PresenterImpl();  // To be replaced by failedFtps
    }

    @Prefix("ShowFailedFtps")
    public static class Tokenizer implements PlaceTokenizer<Place> {
        @Override
        public String getToken(Place place) {
            return "";
        }
        @Override
        public Place getPlace(String token) {
            return new Place();
        }
    }

}
