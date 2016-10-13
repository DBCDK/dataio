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

package dk.dbc.dataio.gui.client.pages.job.show;

import com.google.gwt.activity.shared.Activity;
import com.google.gwt.place.shared.PlaceTokenizer;
import com.google.gwt.place.shared.Prefix;
import dk.dbc.dataio.gui.client.places.AbstractBasePlace;
import dk.dbc.dataio.gui.util.ClientFactory;

public class ShowAcctestJobsPlace extends AbstractBasePlace {
    private String jobsShowName;

    public ShowAcctestJobsPlace() {
        this.jobsShowName = "";
    }

    public ShowAcctestJobsPlace(String jobsShowName) {
        this.jobsShowName = jobsShowName;
    }

    public String getJobsShowName() {
        return jobsShowName;
    }

    @Prefix("ShowAcctestJobs")
    public static class Tokenizer implements PlaceTokenizer<ShowAcctestJobsPlace> {
        @Override
        public String getToken(ShowAcctestJobsPlace place) {
            return place.getJobsShowName();
        }
           @Override
        public ShowAcctestJobsPlace getPlace(String token) {
            return new ShowAcctestJobsPlace(token);
        }
    }

    @Override
    public Activity createPresenter(ClientFactory clientFactory) {
        return new PresenterAcctestJobsImpl(
                clientFactory.getPlaceController(),
                clientFactory.getGlobalViewsFactory().getAcctestJobsView(),
                commonInjector.getMenuTexts().menu_AcctestJobs());
    }
}

