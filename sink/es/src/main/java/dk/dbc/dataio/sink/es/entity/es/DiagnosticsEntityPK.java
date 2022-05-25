package dk.dbc.dataio.sink.es.entity.es;

import javax.persistence.Column;
import javax.persistence.Id;
import java.io.Serializable;

/**
 * Created by ja7 on 25-11-14.
 * Primary Key for Diagnostics
 */
public class DiagnosticsEntityPK implements Serializable {
    @Id
    @Column(name = "targetreference", nullable = false, insertable = true, updatable = true, precision = 0)
    public Integer id;
    @Id
    @Column(name = "lbnr", nullable = false, insertable = true, updatable = true, precision = 0)
    public Integer lbnr;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DiagnosticsEntityPK that = (DiagnosticsEntityPK) o;

        if (!id.equals(that.id)) return false;
        if (!lbnr.equals(that.lbnr)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + lbnr.hashCode();
        return result;
    }

}
