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

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceTokenizer;
import com.google.gwt.place.shared.Prefix;

/**
 * Created by sma on 25/04/14.
 */
public class HarvestersShowPlace extends Place {
    private String harvestersShowName;

    public HarvestersShowPlace() {
        this.harvestersShowName = "";
    }

    public HarvestersShowPlace(String harvestersShowName) {
        this.harvestersShowName = harvestersShowName;
    }

    public String getHarvestersShowName() {
        return harvestersShowName;
    }

    @Prefix("ShowHarvesters")
    public static class Tokenizer implements PlaceTokenizer<HarvestersShowPlace> {
        @Override
        public String getToken(HarvestersShowPlace place) {
            return place.getHarvestersShowName();
        }

        @Override
        public HarvestersShowPlace getPlace(String token) {
            return new HarvestersShowPlace(token);
        }
    }
}
