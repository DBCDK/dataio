package dk.dbc.dataio.gui.client.pages.job.show;

import com.google.gwt.place.shared.PlaceTokenizer;
import com.google.gwt.place.shared.Prefix;

public class ShowTestJobsPlace extends com.google.gwt.place.shared.Place {
    private String jobsShowName;

    public ShowTestJobsPlace() {
        this.jobsShowName = "";
    }

    public ShowTestJobsPlace(String jobsShowName) {
        this.jobsShowName = jobsShowName;
    }

    public String getJobsShowName() {
        return jobsShowName;
    }

    @Prefix("ShowTestJobs")
    public static class Tokenizer implements PlaceTokenizer<ShowTestJobsPlace> {
        @Override
        public String getToken(ShowTestJobsPlace place) {
            return place.getJobsShowName();
        }
           @Override
        public ShowTestJobsPlace getPlace(String token) {
            return new ShowTestJobsPlace(token);
        }
    }
}

