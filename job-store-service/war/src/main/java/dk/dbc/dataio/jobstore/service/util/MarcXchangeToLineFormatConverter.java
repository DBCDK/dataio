/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.jobstore.service.util;

import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.marc.binding.ControlField;
import dk.dbc.marc.binding.MarcRecord;
import dk.dbc.marc.reader.MarcReaderException;
import dk.dbc.marc.reader.MarcXchangeV1Reader;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This class chooses between DanMARC2 and MARC21 conversion output
 * based on the first chunk item converted.
 *
 * This class is NOT thread safe.
 */
public class MarcXchangeToLineFormatConverter extends AbstractToLineFormatConverter {
    private static final Set<String> MARC21_CONTROL_FIELDS = new HashSet<>(
            Arrays.asList("001", "002", "003", "004", "005", "006", "007", "008", "009"));

    private ChunkItemConverter converter = null;

    @Override
    public byte[] convert(ChunkItem chunkItem, Charset encodedAs, List<Diagnostic> diagnostics)
            throws JobStoreException {
        if (converter == null) {
            if (looksLikeMarc21(chunkItem)) {
                converter = new MarcXchangeToMarc21LineFormatConverter();
            } else {
                converter = new MarcXchangeToDanMarc2LineFormatConverter();
            }
        }
        return converter.convert(chunkItem, encodedAs, diagnostics);
    }

    private boolean looksLikeMarc21(ChunkItem chunkItem) throws JobStoreException {
        final MarcRecord record;
        try {
            record = new MarcXchangeV1Reader(getChunkItemInputStream(chunkItem), chunkItem.getEncoding()).read();
        } catch (MarcReaderException e) {
            throw new JobStoreException("Error reading chunk item data as MarcXchange", e);
        }

        if (record != null) {
            final List<ControlField> controlFields = record.getFields().stream()
                    .filter(ControlField.class::isInstance)
                    .map(ControlField.class::cast)
                    .collect(Collectors.toList());
            if (controlFields.isEmpty()) {
                return false;
            }
            for (ControlField controlField : controlFields) {
                if (!MARC21_CONTROL_FIELDS.contains(controlField.getTag())) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }
}
