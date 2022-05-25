package dk.dbc.dataio.cli;

import java.util.ArrayList;
import java.util.List;

public class Datafile {
    private final String fileId;
    private final List<Integer> jobs;

    public Datafile(String fileId) {
        this.fileId = fileId;
        this.jobs = new ArrayList<>();
    }

    public String getFileId() {
        return fileId;
    }

    public List<Integer> getJobs() {
        return new ArrayList<>(jobs);
    }

    public Datafile withJob(int job) {
        jobs.add(job);
        return this;
    }
}
