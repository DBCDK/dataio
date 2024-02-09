package dk.dbc.dataio.jobstore.service.util;

import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.marc.binding.DataField;
import dk.dbc.marc.binding.MarcRecord;
import dk.dbc.marc.binding.SubField;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.util.List;

public abstract class AbstractToLineFormatConverter implements ChunkItemConverter {

    BufferedInputStream getChunkItemInputStream(ChunkItem chunkItem) {
        return new BufferedInputStream(new ByteArrayInputStream(chunkItem.getData()));
    }

    void addDiagnosticsToMarcRecord(List<Diagnostic> diagnostics, MarcRecord record) {
        if (diagnostics != null) {
            for (Diagnostic diagnostic : diagnostics) {
                if (diagnostic.getLevel() == Diagnostic.Level.ERROR
                        && "Exception caught during javascript processing".equals(diagnostic.getMessage())) {
                    continue;
                }

                DataField dataField = new DataField().setTag("e01").setInd1('0').setInd2('0');
                if (diagnostic.getTag() != null) {
                    dataField.addSubField(new SubField().setCode('b').setData(diagnostic.getTag()));
                }
                if (diagnostic.getAttribute() != null) {
                    dataField.addSubField(new SubField().setCode('c').setData(diagnostic.getAttribute()));
                }
                dataField.addSubField(new SubField().setCode('a').setData(diagnostic.getMessage()));
                record.addField(dataField);
            }
        }
    }
}
