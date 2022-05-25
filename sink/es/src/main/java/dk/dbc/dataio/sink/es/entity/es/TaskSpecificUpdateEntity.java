package dk.dbc.dataio.sink.es.entity.es;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.eclipse.persistence.annotations.Customizer;

import javax.persistence.CascadeType;
import javax.persistence.Convert;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by ja7 on 19-09-14.
 * <p>
 * Task scpeifick Update Package.. Loads All SuppliedRecords
 */

@Entity
@DiscriminatorValue("5")
@Table(name = "taskspecificupdate")
@Customizer(SetMultiTableConstraintDependentInheritanceCustomizer.class)

@SuppressFBWarnings(value = {"EI"}, justification = "Entity Class don't own the array")
public class TaskSpecificUpdateEntity extends TaskPackageEntity {


    public enum UpdateStatus {UNKNOWN, SUCCESS, PARTIAL, FAILURE}

    public enum UpdateAction {INSERT, REPLACE, DELETE, ELEMENT_UPDATE, SPECIAL_UPDATE}

    private UpdateAction action;
    private String elementsetname;
    private byte[] actionqualifier;
    private String databasename;
    private String schema;
    private UpdateStatus updatestatus = UpdateStatus.UNKNOWN;

    private List<SuppliedRecordsEntity> suppliedRecords = new ArrayList<>();
    private List<TaskPackageRecordStructureEntity> taskpackageRecordStructures = new ArrayList<>();

    public UpdateAction getAction() {
        return action;
    }

    public void setAction(UpdateAction action) {
        this.action = action;
    }

    public String getElementsetname() {
        return elementsetname;
    }

    public void setElementsetname(String elementsetname) {
        this.elementsetname = elementsetname;
    }

    public byte[] getActionqualifier() {
        return actionqualifier;
    }

    public void setActionqualifier(byte[] actionqualifier) {
        this.actionqualifier = actionqualifier;
    }

    public String getDatabasename() {
        return databasename;
    }

    public void setDatabasename(String databasename) {
        this.databasename = databasename;
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }


    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinColumn(name = "targetreference")
    @OrderBy("lbnr")
    public List<SuppliedRecordsEntity> getSuppliedRecords() {
        return suppliedRecords;
    }

    public void setSuppliedRecords(List<SuppliedRecordsEntity> suppliedRecords) {
        this.suppliedRecords = suppliedRecords;
    }

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinColumn(name = "targetreference")
    @OrderBy("lbnr")
    public List<TaskPackageRecordStructureEntity> getTaskpackageRecordStructures() {
        return taskpackageRecordStructures;
    }

    public void setTaskpackageRecordStructures(List<TaskPackageRecordStructureEntity> taskpackageRecordStructureEntityMap) {
        this.taskpackageRecordStructures = taskpackageRecordStructureEntityMap;
    }

    @Convert(converter = UpdateStatusConverter.class)
    public UpdateStatus getUpdatestatus() {
        return updatestatus;
    }

    public void setUpdatestatus(UpdateStatus updatestatus) {
        this.updatestatus = updatestatus;
    }

    /**
     * Hack Method to ensure the Diagnostics is Loaded.
     *
     * @param entityManager used for loading the diags
     */
    public void loadDiagsIfExists(EntityManager entityManager) {
        // Uses getTaskpackageRecordStructures() instead of local Member
        // to Allow JPA to do Using Dynamic Weaving
        for (TaskPackageRecordStructureEntity recordStructure : getTaskpackageRecordStructures()) {
            recordStructure.loadDiagnosticsEntities(entityManager);
        }
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        TaskSpecificUpdateEntity that = (TaskSpecificUpdateEntity) o;

        if (action != null ? !action.equals(that.action) : that.action != null) return false;
        if (!Arrays.equals(actionqualifier, that.actionqualifier)) return false;
        if (databasename != null ? !databasename.equals(that.databasename) : that.databasename != null) return false;
        if (elementsetname != null ? !elementsetname.equals(that.elementsetname) : that.elementsetname != null)
            return false;
        if (schema != null ? !schema.equals(that.schema) : that.schema != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (action != null ? action.hashCode() : 0);
        result = 31 * result + (elementsetname != null ? elementsetname.hashCode() : 0);
        result = 31 * result + (actionqualifier != null ? Arrays.hashCode(actionqualifier) : 0);
        result = 31 * result + (databasename != null ? databasename.hashCode() : 0);
        result = 31 * result + (schema != null ? schema.hashCode() : 0);
        return result;
    }
}
