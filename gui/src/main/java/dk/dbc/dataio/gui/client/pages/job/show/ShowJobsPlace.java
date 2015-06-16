package dk.dbc.dataio.gui.client.pages.job.show;

import com.google.gwt.place.shared.PlaceTokenizer;
import com.google.gwt.place.shared.Prefix;

public class ShowJobsPlace extends com.google.gwt.place.shared.Place {
    private String jobsShowName;

    public ShowJobsPlace() {
        this.jobsShowName = "";
    }

    public ShowJobsPlace(String jobsShowName) {
        this.jobsShowName = jobsShowName;
    }

    public String getJobsShowName() {
        return jobsShowName;
    }

    @Prefix("ShowJobs")
    public static class Tokenizer implements PlaceTokenizer<ShowJobsPlace> {
        @Override
        public String getToken(ShowJobsPlace place) {
            return place.getJobsShowName();
        }
           @Override
        public ShowJobsPlace getPlace(String token) {
            return new ShowJobsPlace(token);
        }
    }
}

