package dk.dbc.dataio.sink.es.entity.es;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Table;
import java.math.BigInteger;
import java.sql.Date;
import java.util.Arrays;

/**
 * Created by ja7 on 23-09-14.
 *
 * Entity Mapping for SuppliedRecords.. ignores originalrecord
 */
@Entity
@Table(name = "suppliedrecords")
@IdClass(SuppliedRecordsEntityPK.class)
public class SuppliedRecordsEntity {
    @Id
    @Column(name = "targetreference", nullable = false, insertable = true, updatable = true, precision = 0)
    public BigInteger targetreference;
    @Id
    @Column(name = "lbnr", nullable = false, insertable = true, updatable = true, precision = 0)
    public BigInteger lbnr;
    @Column(name = "recordid1", nullable = true, insertable = true, updatable = true, precision = 0)
    public BigInteger recordid1;
    @Column(name = "recordid2", nullable = true, insertable = true, updatable = true, length = 200)
    public String recordid2;
    @Column(name = "recordid3", nullable = true, insertable = true, updatable = true, length = 200)
    public String recordid3;
    @Column(name = "supplementalid1", nullable = true, insertable = true, updatable = true)
    public Date supplementalid1;
    @Column(name = "supplementalid2", nullable = true, insertable = true, updatable = true, length = 200)
    public String supplementalid2;
    @Column(name = "supplementalid3", nullable = true, insertable = true, updatable = true, length = 400)
    public String supplementalid3;
    @Column(name = "correlationinfo", nullable = true, insertable = true, updatable = true, precision = 0)
    public BigInteger correlationinfo;
    @Column(name = "record", nullable = false, insertable = true, updatable = true)
    public byte[] record;


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SuppliedRecordsEntity that = (SuppliedRecordsEntity) o;

        if (correlationinfo != null ? !correlationinfo.equals(that.correlationinfo) : that.correlationinfo != null)
            return false;
        if (lbnr != null ? !lbnr.equals(that.lbnr) : that.lbnr != null) return false;
        if (!Arrays.equals(record, that.record)) return false;
        if (recordid1 != null ? !recordid1.equals(that.recordid1) : that.recordid1 != null) return false;
        if (recordid2 != null ? !recordid2.equals(that.recordid2) : that.recordid2 != null) return false;
        if (recordid3 != null ? !recordid3.equals(that.recordid3) : that.recordid3 != null) return false;
        if (supplementalid1 != null ? !supplementalid1.equals(that.supplementalid1) : that.supplementalid1 != null)
            return false;
        if (supplementalid2 != null ? !supplementalid2.equals(that.supplementalid2) : that.supplementalid2 != null)
            return false;
        if (supplementalid3 != null ? !supplementalid3.equals(that.supplementalid3) : that.supplementalid3 != null)
            return false;
        if (targetreference != null ? !targetreference.equals(that.targetreference) : that.targetreference != null)
            return false;

        return true;
    }


    @Override
    public int hashCode() {
        int result = targetreference != null ? targetreference.hashCode() : 0;
        result = 31 * result + (lbnr != null ? lbnr.hashCode() : 0);
        result = 31 * result + (recordid1 != null ? recordid1.hashCode() : 0);
        result = 31 * result + (recordid2 != null ? recordid2.hashCode() : 0);
        result = 31 * result + (recordid3 != null ? recordid3.hashCode() : 0);
        result = 31 * result + (supplementalid1 != null ? supplementalid1.hashCode() : 0);
        result = 31 * result + (supplementalid2 != null ? supplementalid2.hashCode() : 0);
        result = 31 * result + (supplementalid3 != null ? supplementalid3.hashCode() : 0);
        result = 31 * result + (correlationinfo != null ? correlationinfo.hashCode() : 0);
        result = 31 * result + (record != null ? Arrays.hashCode(record) : 0);
        return result;
    }

}
