/*
 * DataIO - Data IO
 * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 * Denmark. CVR: 15149043
 *
 * This file is part of DataIO.
 *
 * DataIO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DataIO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
 */

package dk.dbc.dataio.sink.es.entity.es;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.eclipse.persistence.annotations.Customizer;

import org.eclipse.persistence.annotations.Customizer;

import javax.persistence.CascadeType;
import javax.persistence.Convert;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.MapKey;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by ja7 on 19-09-14.
 *
 * Task scpeifick Update Package.. Loads All SuppliedRecords
 *
 */

@Entity
@DiscriminatorValue("5")
@Table(name = "taskspecificupdate")
@Customizer(SetMultiTableConstraintDependentInheritanceCustomizer.class)

@SuppressFBWarnings(value = {"EI"}, justification = "Entity Class don't own the array")
public class TaskSpecificUpdateEntity extends TaskPackageEntity {

    public enum UpdateStatus{ UNKNOWN, SUCCESS, PARTIAL, FAILURE}



    private Integer action;
    private String elementsetname;
    private byte[] actionqualifier;
    private String databasename;
    private String schema;
    private UpdateStatus updatestatus;

    private List<SuppliedRecordsEntity> suppliedRecords = new LinkedList<>();
    private Map<BigInteger, TaskPackageRecordStructureEntity> taskpackageRecordStructureEntityMap = new HashMap<>();

    public Integer getAction() {
        return action;
    }

    public void setAction(Integer action) {
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


    @OneToMany(cascade = CascadeType.DETACH)
    @JoinColumn(name = "targetreference")
    @OrderBy("lbnr")
    public List<SuppliedRecordsEntity> getSuppliedRecords() {
        return suppliedRecords;
    }

    public void setSuppliedRecords(List<SuppliedRecordsEntity> suppliedRecords) {
        this.suppliedRecords = suppliedRecords;
    }

    @OneToMany
    @JoinColumn(name = "targetreference")
    @MapKey(name = "lbnr")
    public Map<BigInteger, TaskPackageRecordStructureEntity> getTaskpackageRecordStructureEntityMap() {
        return taskpackageRecordStructureEntityMap;
    }

    public void setTaskpackageRecordStructureEntityMap(Map<BigInteger, TaskPackageRecordStructureEntity> taskpackageRecordStructureEntityMap) {
        this.taskpackageRecordStructureEntityMap = taskpackageRecordStructureEntityMap;
    }

    @Convert(converter = UpdateStatusConverter.class)
    public UpdateStatus getUpdatestatus() {
        return updatestatus;
    }

    public void setUpdatestatus(UpdateStatus updatestatus) {
        this.updatestatus = updatestatus;
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
