package dk.dbc.dataio.gui.client.pages.newJob.show;

import com.google.gwt.place.shared.PlaceTokenizer;
import com.google.gwt.place.shared.Prefix;

public class Place extends com.google.gwt.place.shared.Place {
    private String jobsShowName;

    public Place() {
        this.jobsShowName = "";
    }

    public Place(String jobsShowName) {
        this.jobsShowName = jobsShowName;
    }

    public String getJobsShowName() {
        return jobsShowName;
    }

    @Prefix("NewShowJobs")
    public static class Tokenizer implements PlaceTokenizer<Place> {
        @Override
        public String getToken(Place place) {
            return place.getJobsShowName();
        }
           @Override
        public Place getPlace(String token) {
            return new Place(token);
        }
    }
}

