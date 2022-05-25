package dk.dbc.dataio.sink.dpf.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ProcessingInstructions {
    private int submitter;
    private String id;
    private String title;
    private String updateTemplate;
    private String originalRecordId;
    private DpfRecord.State recordState;
    private List<String> errors = new ArrayList<>(0);

   public int getSubmitter() {
        return submitter;
    }

    public ProcessingInstructions withSubmitter(int submitter) {
        this.submitter = submitter;
        return this;
    }

    public String getId() {
        return id;
    }

    public ProcessingInstructions withId(String id) {
        this.id = id;
        return this;
    }

    public String getTitle() {
        return title;
    }

    public ProcessingInstructions withTitle(String title) {
        this.title = title;
        return this;
    }

    public String getUpdateTemplate() {
        return updateTemplate;
    }

    public ProcessingInstructions withUpdateTemplate(String updateTemplate) {
        this.updateTemplate = updateTemplate;
        return this;
    }

    public String getOriginalRecordId() {
        return originalRecordId;
    }

    public ProcessingInstructions withOriginalRecordId(String originalRecordId) {
        this.originalRecordId = originalRecordId;
        return this;
    }

    public DpfRecord.State getRecordState() {
        return recordState;
    }

    public ProcessingInstructions withRecordState(DpfRecord.State recordState) {
        this.recordState = recordState;
        return this;
    }

    public List<String> getErrors() {
        return errors;
    }

    public ProcessingInstructions withErrors(List<String> errors) {
        this.errors = errors;
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

        ProcessingInstructions that = (ProcessingInstructions) o;

        if (submitter != that.submitter) {
            return false;
        }
        if (!Objects.equals(id, that.id)) {
            return false;
        }
        if (!Objects.equals(title, that.title)) {
            return false;
        }
        if (!Objects.equals(updateTemplate, that.updateTemplate)) {
            return false;
        }
        if (!Objects.equals(originalRecordId, that.originalRecordId)) {
            return false;
        }
        if (recordState != that.recordState) {
            return false;
        }
        return Objects.equals(errors, that.errors);
    }

    @Override
    public int hashCode() {
        int result = submitter;
        result = 31 * result + (id != null ? id.hashCode() : 0);
        result = 31 * result + (title != null ? title.hashCode() : 0);
        result = 31 * result + (updateTemplate != null ? updateTemplate.hashCode() : 0);
        result = 31 * result + (originalRecordId != null ? originalRecordId.hashCode() : 0);
        result = 31 * result + (recordState != null ? recordState.hashCode() : 0);
        result = 31 * result + (errors != null ? errors.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ProcessingInstructions{" +
                "submitter=" + submitter +
                ", id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", updateTemplate='" + updateTemplate + '\'' +
                ", originalRecordId='" + originalRecordId + '\'' +
                ", recordState=" + recordState +
                ", errors=" + errors +
                '}';
    }
}
