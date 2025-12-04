package dk.dbc.dataio.harvester.utils.datafileverifier;

import dk.dbc.marc.binding.DataField;
import dk.dbc.marc.binding.MarcBinding;

import java.util.Objects;

/**
 * Verifier helper class for MARC collection members
 */
public class MarcBindingExpectation {
    private final String bibliographicRecordId;
    private final int agencyId;

    public MarcBindingExpectation(MarcBinding marcBinding) {
        final DataField f001 = marcBinding.getDataField("001");
        if (f001 == null) {
            throw new IllegalArgumentException("field 001 not found");
        }
        this.bibliographicRecordId = f001.getSubField(s -> s.getCode() == 'a')
                .orElseThrow(() -> new IllegalArgumentException("001a not found"))
                .getData();
        this.agencyId = Integer.parseInt(f001.getSubField(s -> s.getCode() == 'a')
                .orElseThrow(() -> new IllegalArgumentException("001b not found"))
                .getData());
    }

    public MarcBindingExpectation(String bibliographicRecordId, int agencyId) {
        this.bibliographicRecordId = bibliographicRecordId;
        this.agencyId = agencyId;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MarcBindingExpectation that = (MarcBindingExpectation) o;
        return agencyId == that.agencyId && Objects.equals(bibliographicRecordId, that.bibliographicRecordId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bibliographicRecordId, agencyId);
    }

    @Override
    public String toString() {
        return "MarcBindingExpectation{" +
                "bibliographicRecordId='" + bibliographicRecordId + '\'' +
                ", agencyId=" + agencyId +
                '}';
    }
}
