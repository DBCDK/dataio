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
package dk.dbc.dataio.gui.client.pages.submitter.modify;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceTokenizer;
import com.google.gwt.place.shared.Prefix;
import dk.dbc.dataio.gui.client.model.SubmitterModel;


/**
 * EditPlace
 */
public class EditPlace extends Place {
    private Long submitterId;

    public EditPlace(String url) {
        this.submitterId = Long.valueOf(url);
    }

    public EditPlace(SubmitterModel model) {
        this.submitterId = model.getId();
    }

    public Long getSubmitterId() {
        return submitterId;
    }

    @Prefix("EditSubmitter")
    public static class Tokenizer implements PlaceTokenizer<EditPlace> {
        @Override
        public String getToken(EditPlace place) {
            return String.valueOf(place.getSubmitterId());
        }
        @Override
        public EditPlace getPlace(String token) {
            return new EditPlace(token);
        }
    }

}
