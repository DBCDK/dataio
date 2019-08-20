/*
 * DataIO - Data IO
 * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 * Denmark. CVR: 15149043
 *
 * This file is part of DataIO.
 *
 * DataIO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DataIO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
 */

package dk.dbc.dataio.jobstore.service.util;

import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.invariant.InvariantUtil;
import dk.dbc.dataio.jobstore.types.JobStoreException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class for chunk item exporting
 */
public class ChunkItemExporter {
    private enum ConverterType {
        MARCXCHANGE_TO_DANMARC2_LINEFORMAT,
        RAW
    }

    private AddiUnwrapper addiUnwrapper = new AddiUnwrapper();
    private RawConverter rawConverter = new RawConverter();

    /* Associates wrapping formats with their corresponding unwrap handler */
    private Map<ChunkItem.Type, ChunkItemUnwrapper> wrapperFormats = new HashMap<>();
    {
        wrapperFormats.put(ChunkItem.Type.UNKNOWN, addiUnwrapper);  // ToDo: remove when all chunk items are created with type information
        wrapperFormats.put(ChunkItem.Type.ADDI, addiUnwrapper);
    }

    /* Associates legal conversions with their corresponding converter type */
    private Map<Conversion, ConverterType> conversions = new HashMap<>();
    {
        conversions.put(new Conversion(ChunkItem.Type.MARCXCHANGE, ChunkItem.Type.DANMARC2_LINEFORMAT),
                ConverterType.MARCXCHANGE_TO_DANMARC2_LINEFORMAT);
        conversions.put(new Conversion(ChunkItem.Type.BYTES, ChunkItem.Type.BYTES),
                ConverterType.RAW);
    }

    /**
     * Exports given chunk item as given type in given encoding
     * @param chunkItem chunk item to be exported
     * @param toType type of export
     * @param encodedAs export encoding
     * @param diagnostics diagnostics to include in exported item if supported by conversion
     * @return export as bytes
     * @throws NullPointerException if given null-valued argument
     * @throws JobStoreException on unwrap error, on illegal type conversion, on failure to read input data
     * or on failure to write output data
     */
    public byte[] export(ChunkItem chunkItem, ChunkItem.Type toType, Charset encodedAs, List<Diagnostic> diagnostics)
            throws NullPointerException, JobStoreException {
        InvariantUtil.checkNotNullOrThrow(chunkItem, "chunkItem");
        InvariantUtil.checkNotNullOrThrow(toType, "toType");
        InvariantUtil.checkNotNullOrThrow(encodedAs, "encodedAs");
        InvariantUtil.checkNotNullOrThrow(diagnostics, "diagnostics");

        final List<ChunkItem> chunkItems = unwrap(chunkItem);
        ChunkItem.Type fromType = chunkItems.get(0).getType().get(0);
        if (fromType == ChunkItem.Type.UNKNOWN && isLineFormatType(toType)) {
            // Special case handling of chunk items since the
            // type system is not fully implemented. When fromType
            // is UNKNOWN and toType is a line format variant it is
            // assumed that the chunk item contains MarcXchange.
            fromType = ChunkItem.Type.MARCXCHANGE;
        }
        final Conversion conversion = getConversion(fromType, toType);
        final ChunkItemConverter chunkItemConverter = getChunkItemConverter(conversion);
        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        for (ChunkItem item : chunkItems) {
            try {
                byteArrayOutputStream.write(chunkItemConverter.convert(item, encodedAs, diagnostics));
            } catch (IOException e) {
                throw new JobStoreException("Exception caught while writing output bytes", e);
            }
        }
        return byteArrayOutputStream.toByteArray();
    }

    /*
       Unwraps chunk item.
       Multiple wrapping layers are currently not supported.
    */
    private List<ChunkItem> unwrap(ChunkItem chunkItem) throws JobStoreException {
        final ChunkItem.Type type = chunkItem.getType().get(0);
        if (isWrapperFormat(type)) {
            return wrapperFormats.get(type).unwrap(chunkItem);
        }
        return Collections.singletonList(chunkItem);
    }

    private boolean isWrapperFormat(ChunkItem.Type type) {
        return wrapperFormats.containsKey(type);
    }

    private Conversion getConversion(ChunkItem.Type fromType, ChunkItem.Type toType)
            throws JobStoreException {
        final Conversion conversion = new Conversion(fromType, toType);
        if (toType == ChunkItem.Type.BYTES) {
            // It is always legal to convert to bytes.
            return conversion;
        }
        if (!isLegalConversion(conversion)) {
            throw new JobStoreException("Illegal conversion " + conversion.toString());
        }
        return conversion;
    }

    private ChunkItemConverter getChunkItemConverter(Conversion conversion) throws JobStoreException {
        if (conversion.to == ChunkItem.Type.BYTES) {
            return rawConverter;
        }
        final ConverterType converterType = conversions.get(conversion);
        switch (converterType) {
            case MARCXCHANGE_TO_DANMARC2_LINEFORMAT:
                return new MarcXchangeToDanMarc2LineFormatConverter();
            case RAW:
                return rawConverter;
            default:
                throw new JobStoreException("No converter implementation found for type " + converterType);
        }
    }

    private boolean isLegalConversion(Conversion conversion) {
        return conversions.containsKey(conversion);
    }

    private boolean isLineFormatType(ChunkItem.Type type) {
        return     type == ChunkItem.Type.LINEFORMAT
                || type == ChunkItem.Type.DANMARC2_LINEFORMAT
                || type == ChunkItem.Type.MARC21_LINEFORMAT;
    }

    private static class Conversion {
        private final ChunkItem.Type from;
        private final ChunkItem.Type to;

        public Conversion(ChunkItem.Type from, ChunkItem.Type to) {
            this.from = from;
            this.to = to;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            Conversion that = (Conversion) o;

            if (from != that.from) {
                return false;
            }
            return to == that.to;

        }

        @Override
        public int hashCode() {
            int result = from.hashCode();
            result = 31 * result + to.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "Conversion{" +
                    "from=" + from +
                    ", to=" + to +
                    '}';
        }
    }
}
