package dk.dbc.dataio.harvester.dmat;

import dk.dbc.dataio.commons.types.AddiMetaData;
import dk.dbc.dmat.service.persistence.DMatRecord;

import java.util.Objects;

public class ExtendedAddiMetaData extends AddiMetaData {

    private DMatRecord dmatRecord;
    private String dmatUrl;

    public DMatRecord getdmatRecord() {
        return dmatRecord;
    }

    public void setdmatRecord(DMatRecord dmatRecord) {
        this.dmatRecord = dmatRecord;
    }

    public ExtendedAddiMetaData withDmatRecord(DMatRecord dmatRecord) {
        this.dmatRecord = dmatRecord;
        return this;
    }

    public String getDmatUrl() {
        return dmatUrl;
    }

    public void setDmatUrl(String dmatUrl) {
        this.dmatUrl = dmatUrl;
    }

    public ExtendedAddiMetaData withDmatUrl(String dmatUrl) {
        this.dmatUrl = dmatUrl;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ExtendedAddiMetaData that = (ExtendedAddiMetaData) o;
        return Objects.equals(dmatRecord, that.dmatRecord) && Objects.equals(dmatUrl, that.dmatUrl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), dmatRecord, dmatUrl);
    }

    @Override
    public String toString() {
        return "AddiMetaDataWithRecord{" +
                "dMatRecord=" + dmatRecord +
                "dMatUrl=" + dmatUrl +
                "} " + super.toString();
    }
}
