package dk.dbc.dataio.commons.types;

import java.util.List;
import java.util.Objects;

public class SVNInvocationMethods {
    String project;
    String revision;
    String filename;
    List<String> invocationMethods;

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

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public List<String> getInvocationMethods() {
        return invocationMethods;
    }

    public void setInvocationMethods(List<String> invocationMethods) {
        this.invocationMethods = invocationMethods;
    }

    public SVNInvocationMethods withProject(String project) {
        this.project = project;
        return this;
    }

    public SVNInvocationMethods withFilename(String filename) {
        this.filename = filename;
        return this;
    }

    public SVNInvocationMethods withRevision(String revision) {
        this.revision = revision;
        return this;
    }

    public SVNInvocationMethods withInvocationMethods(List<String> invocationMethods) {
        this.invocationMethods = invocationMethods;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SVNInvocationMethods that = (SVNInvocationMethods) o;
        return Objects.equals(project, that.project) && Objects.equals(revision, that.revision) && Objects.equals(filename, that.filename) &&  Objects.equals(invocationMethods, that.invocationMethods);
    }

    @Override
    public int hashCode() {
        return Objects.hash(project, revision, filename, invocationMethods);
    }
}
