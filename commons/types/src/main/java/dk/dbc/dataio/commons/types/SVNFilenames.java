package dk.dbc.dataio.commons.types;

import java.util.List;
import java.util.Objects;

public class SVNFilenames {
    private String project;
    private String revision;
    private List<String> filenames;

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public String getRevision() {
        return revision;
    }

    public void setRevision(String revision) {
        this.revision = revision;
    }

    public List<String> getFilenames() {
        return filenames;
    }

    public void setFilenames(List<String> filenames) {
        this.filenames = filenames;
    }

    public SVNFilenames withProject(String project) {
        this.project = project;
        return this;
    }

    public SVNFilenames withFilenames(List<String> filenames) {
        this.filenames = filenames;
        return this;
    }

    public SVNFilenames withRevision(String revision) {
        this.revision = revision;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SVNFilenames that = (SVNFilenames) o;
        return Objects.equals(project, that.project) && Objects.equals(revision, that.revision) && Objects.equals(filenames, that.filenames);
    }

    @Override
    public int hashCode() {
        return Objects.hash(project, revision, filenames);
    }
}
