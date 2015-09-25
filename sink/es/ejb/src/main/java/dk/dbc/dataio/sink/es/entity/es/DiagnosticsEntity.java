package dk.dbc.dataio.sink.es.entity.es;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

/**
 * Created by ja7 on 25-11-14.
 * standard Entity for diagnostics the NamedQuery is used by
 * TaskPackageRecordStructureEntity
 */
@Entity
@Table(name = "diagnostics")
@IdClass(DiagnosticsEntityPK.class)
@NamedQueries({
        @NamedQuery(name = "Diagnostics.findById",
                query = "select d from DiagnosticsEntity d where d.id=:id order by d.lbNr asc "
        ),
        @NamedQuery(name = "Diagnostics.findMaxLbNr",
                query = "select max(d.lbNr) from  DiagnosticsEntity d where d.id=:id"
        )
})
public class DiagnosticsEntity {
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "DiagnosticRefSeqName")
    @SequenceGenerator(
            name = "DiagnosticRefSeqName",
            sequenceName = "diagidseq",
            allocationSize = 1
    )
    public Integer id;
    @Id
    public
    @Column(name = "lbnr", nullable = false, insertable = true, updatable = true, precision = 0)
    Integer lbNr;

    @Column(name = "diagnosticsetid")
    public String diagnosticSetId = "1.2.840.10003.4.1";
    @Column(name = "condition")
    public Integer errorCode = 100;
    @Column(name = "addinfo")
    public String additionalInformation;


    public DiagnosticsEntity() {
        this.lbNr = 0;
    }

    public DiagnosticsEntity(int lbNr, String additionalInformation) {
        this.lbNr = lbNr;
        this.additionalInformation = additionalInformation;
    }

    public DiagnosticsEntity(int id, int lbNr, String additionalInformation) {
        this.id = id;
        this.lbNr = lbNr;
        this.additionalInformation = additionalInformation;
    }


    @Override
    public String toString() {
        return "DiagnosticsEntity{" +
                "id=" + id +
                ", lbNr=" + lbNr +
                ", diagnosticSetId='" + diagnosticSetId + '\'' +
                ", errorCode=" + errorCode +
                ", additionalInformation='" + additionalInformation + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DiagnosticsEntity that = (DiagnosticsEntity) o;

        if (additionalInformation != null ? !additionalInformation.equals(that.additionalInformation) : that.additionalInformation != null)
            return false;
        if (diagnosticSetId != null ? !diagnosticSetId.equals(that.diagnosticSetId) : that.diagnosticSetId != null)
            return false;
        if (errorCode != null ? !errorCode.equals(that.errorCode) : that.errorCode != null) return false;
        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (lbNr != null ? !lbNr.equals(that.lbNr) : that.lbNr != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (lbNr != null ? lbNr.hashCode() : 0);
        result = 31 * result + (diagnosticSetId != null ? diagnosticSetId.hashCode() : 0);
        result = 31 * result + (errorCode != null ? errorCode.hashCode() : 0);
        result = 31 * result + (additionalInformation != null ? additionalInformation.hashCode() : 0);
        return result;
    }
}

