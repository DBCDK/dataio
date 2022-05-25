package dk.dbc.dataio.sink.es.entity.es;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Table;
import java.util.Objects;

/**
 * Created by ja7 on 23-09-14.
 * <p>
 * Entity Mapping for SuppliedRecords.. ignores originalrecord
 */
@Entity
@Table(name = "suppliedrecords")
@IdClass(SuppliedRecordsEntityPK.class)
public class SuppliedRecordsEntity {
    @Id
    @Column(name = "targetreference")
    public Integer targetreference;
    @Id
    @Column(name = "lbnr", nullable = false, insertable = true, updatable = true, precision = 0)
    public Integer lbnr;
    @Column(name = "supplementalid3", nullable = true, insertable = true, updatable = true, length = 400)
    public String metaData;
    @Column(name = "record", nullable = false, insertable = true, updatable = true)
    public byte[] record;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SuppliedRecordsEntity)) return false;
        SuppliedRecordsEntity that = (SuppliedRecordsEntity) o;
        return Objects.equals(targetreference, that.targetreference) &&
                Objects.equals(lbnr, that.lbnr) &&
                Objects.equals(metaData, that.metaData) &&
                Objects.equals(record, that.record);
    }

    @Override
    public int hashCode() {
        return Objects.hash(targetreference, lbnr, metaData, record);
    }
}
