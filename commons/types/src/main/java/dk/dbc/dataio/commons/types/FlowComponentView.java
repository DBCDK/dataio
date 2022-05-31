package dk.dbc.dataio.commons.types;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

/**
 * Brief view for FlowComponent DTO
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FlowComponentView implements Serializable {
    private long id;
    private long version;
    private String name;
    private String description;
    private String revision;
    private String nextRevision;
    private String project;
    private String scriptName;
    private String method;
    private List<String> modules;
    private List<String> nextModules;

    public long getId() {
        return id;
    }

    public FlowComponentView withId(long id) {
        this.id = id;
        return this;
    }

    public long getVersion() {
        return version;
    }

    public FlowComponentView withVersion(long version) {
        this.version = version;
        return this;
    }

    public String getName() {
        return name;
    }

    public FlowComponentView withName(String name) {
        this.name = name;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public FlowComponentView withDescription(String description) {
        this.description = description;
        return this;
    }

    public String getRevision() {
        return revision;
    }

    public FlowComponentView withRevision(String revision) {
        this.revision = revision;
        return this;
    }

    public String getNextRevision() {
        return nextRevision;
    }

    public FlowComponentView withNextRevision(String nextRevision) {
        this.nextRevision = nextRevision;
        return this;
    }

    public String getProject() {
        return project;
    }

    public FlowComponentView withProject(String project) {
        this.project = project;
        return this;
    }

    public String getScriptName() {
        return scriptName;
    }

    public FlowComponentView withScriptName(String scriptName) {
        this.scriptName = scriptName;
        return this;
    }

    public String getMethod() {
        return method;
    }

    public FlowComponentView withMethod(String method) {
        this.method = method;
        return this;
    }

    public List<String> getModules() {
        return modules;
    }

    public FlowComponentView withModules(List<String> modules) {
        this.modules = modules;
        return this;
    }

    public List<String> getNextModules() {
        return nextModules;
    }

    public FlowComponentView withNextModules(List<String> nextModules) {
        this.nextModules = nextModules;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        FlowComponentView that = (FlowComponentView) o;

        if (id != that.id) {
            return false;
        }
        if (version != that.version) {
            return false;
        }
        if (!Objects.equals(name, that.name)) {
            return false;
        }
        if (!Objects.equals(description, that.description)) {
            return false;
        }
        if (!Objects.equals(revision, that.revision)) {
            return false;
        }
        if (!Objects.equals(nextRevision, that.nextRevision)) {
            return false;
        }
        if (!Objects.equals(project, that.project)) {
            return false;
        }
        if (!Objects.equals(scriptName, that.scriptName)) {
            return false;
        }
        if (!Objects.equals(method, that.method)) {
            return false;
        }
        if (!Objects.equals(modules, that.modules)) {
            return false;
        }
        if (!Objects.equals(nextModules, that.nextModules)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = (int) (id ^ (id >>> 32));
        result = 31 * result + (int) (version ^ (version >>> 32));
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (revision != null ? revision.hashCode() : 0);
        result = 31 * result + (nextRevision != null ? nextRevision.hashCode() : 0);
        result = 31 * result + (project != null ? project.hashCode() : 0);
        result = 31 * result + (scriptName != null ? scriptName.hashCode() : 0);
        result = 31 * result + (method != null ? method.hashCode() : 0);
        result = 31 * result + (modules != null ? modules.hashCode() : 0);
        result = 31 * result + (nextModules != null ? nextModules.hashCode() : 0);
        return result;
    }
}
