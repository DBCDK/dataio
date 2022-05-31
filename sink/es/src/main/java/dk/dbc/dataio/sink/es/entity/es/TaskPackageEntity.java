package dk.dbc.dataio.sink.es.entity.es;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

/**
 * Created by ja7 on 19-09-14.
 * SuperClass for Taskpackages..
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "PACKAGETYPE", discriminatorType = DiscriminatorType.INTEGER)
@Table(name = "taskpackage")
public abstract class TaskPackageEntity {

    public enum TaskStatus {PENDING, ACTIVE, COMPLETE, ABORTED}


    private Integer targetreference;
    private Integer userid;
    private Integer packagetype;
    private String packagename;
    private String creator;
    private String description;
    private TaskStatus taskStatus;

    @Id
    @Column(name = "targetreference", nullable = false, insertable = true, updatable = true, precision = 0)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "taskpackageRefSeqName")
    @SequenceGenerator(
            name = "taskpackageRefSeqName",
            sequenceName = "taskpackageRefSeq",
            allocationSize = 1
    )
    public Integer getTargetreference() {
        return targetreference;
    }

    public void setTargetreference(Integer targetreference) {
        this.targetreference = targetreference;
    }

    @Basic
    @Column(name = "userid", nullable = true, insertable = true, updatable = true, precision = 0)
    public Integer getUserid() {
        return userid;
    }

    // for JPA
    public void setUserid(Integer userid) {
        this.userid = userid;
    }

    // For user code
    public void setUserid(int userid) {
        this.userid = userid;
    }

    @Basic
    @Column(name = "packagetype", nullable = false, insertable = true, updatable = true, precision = 0)
    public Integer getPackagetype() {
        return packagetype;
    }

    public void setPackagetype(Integer packagetype) {
        this.packagetype = packagetype;
    }

    @Basic
    @Column(name = "packagename", nullable = true, insertable = true, updatable = true, length = 200)
    public String getPackagename() {
        return packagename;
    }

    public void setPackagename(String packagename) {
        this.packagename = packagename;
    }

    @Basic
    @Column(name = "creator", nullable = false, insertable = true, updatable = true, length = 200)
    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    @Basic
    @Column(name = "description", nullable = true, insertable = true, updatable = true, length = 200)
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Convert(converter = TaskStatusConverter.class)
    public TaskStatus getTaskStatus() {
        return taskStatus;
    }

    public void setTaskStatus(TaskStatus taskStatus) {
        this.taskStatus = taskStatus;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TaskPackageEntity that = (TaskPackageEntity) o;

        if (creator != null ? !creator.equals(that.creator) : that.creator != null) return false;
        if (description != null ? !description.equals(that.description) : that.description != null) return false;
        if (packagename != null ? !packagename.equals(that.packagename) : that.packagename != null) return false;
        if (packagetype != null ? !packagetype.equals(that.packagetype) : that.packagetype != null) return false;
        if (targetreference != null ? !targetreference.equals(that.targetreference) : that.targetreference != null)
            return false;
        if (userid != null ? !userid.equals(that.userid) : that.userid != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = targetreference != null ? targetreference.hashCode() : 0;
        result = 31 * result + (userid != null ? userid.hashCode() : 0);
        result = 31 * result + (packagetype != null ? packagetype.hashCode() : 0);
        result = 31 * result + (packagename != null ? packagename.hashCode() : 0);
        result = 31 * result + (creator != null ? creator.hashCode() : 0);
        result = 31 * result + (description != null ? description.hashCode() : 0);
        return result;
    }


}
