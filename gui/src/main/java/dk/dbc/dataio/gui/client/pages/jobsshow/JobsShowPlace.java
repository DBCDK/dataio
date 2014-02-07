package dk.dbc.dataio.gui.client.pages.jobsshow;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceTokenizer;
import com.google.gwt.place.shared.Prefix;

public class JobsShowPlace  extends Place {
    private String jobsShowName;

    public JobsShowPlace() {
        this.jobsShowName = "";
    }

    public JobsShowPlace(String jobsShowName) {
        this.jobsShowName = jobsShowName;
    }

    public String getJobsShowName() {
        return jobsShowName;
    }

    @Prefix("ShowJobs")
    public static class Tokenizer implements PlaceTokenizer<JobsShowPlace> {
        @Override
        public String getToken(JobsShowPlace place) {
            return place.getJobsShowName();
        }

        @Override
        public JobsShowPlace getPlace(String token) {
            return new JobsShowPlace(token);
        }
    }

}

