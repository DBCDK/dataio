package dk.dbc.dataio.harvester.dmat;

import dk.dbc.dataio.commons.types.AddiMetaData;
import dk.dbc.dmat.service.persistence.DMatRecord;

import java.util.Objects;

public class AddiMetaDataWithRecord extends AddiMetaData {

    private DMatRecord dmatRecord;

    public DMatRecord getdmatRecord() {
        return dmatRecord;
    }

    public void setdmatRecord(DMatRecord dmatRecord) {
        this.dmatRecord = dmatRecord;
    }

    public AddiMetaDataWithRecord withDmatRecord(DMatRecord dmatRecord) {
        this.dmatRecord = dmatRecord;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        AddiMetaDataWithRecord that = (AddiMetaDataWithRecord) o;
        return Objects.equals(dmatRecord, that.dmatRecord);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), dmatRecord);
    }

    @Override
    public String toString() {
        return "AddiMetaDataWithRecord{" +
                "dMatRecord=" + dmatRecord +
                "} " + super.toString();
    }
}
