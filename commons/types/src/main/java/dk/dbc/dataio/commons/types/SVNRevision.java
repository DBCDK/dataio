package dk.dbc.dataio.commons.types;

import java.util.List;
import java.util.Objects;

public class SVNRevision {
    String project;
    List<String > revisions;

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public List<String> getRevisions() {
        return revisions;
    }

    public void setRevisions(List<String> revisions) {
        this.revisions = revisions;
    }

    public SVNRevision withProject(String  project) {
        this.project = project;
        return this;
    }

    public SVNRevision withRevisions(List<String> revisions) {
        this.revisions = revisions;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SVNRevision that = (SVNRevision) o;
        return Objects.equals(project, that.project) && Objects.equals(revisions, that.revisions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(project, revisions);
    }

    @Override
    public String toString() {
        return "SVNRevision{" +
                "project='" + project + '\'' +
                ", revisions=" + revisions +
                '}';
    }
}
