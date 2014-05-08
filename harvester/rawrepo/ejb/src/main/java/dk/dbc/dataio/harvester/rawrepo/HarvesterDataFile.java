package dk.dbc.dataio.harvester.rawrepo;

import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;

public class HarvesterDataFile implements AutoCloseable {
    private final byte[] header;
    private final byte[] footer;

    final Charset charset;
    final OutputStream outputStream;

    public HarvesterDataFile(Charset charset, OutputStream outputStream) throws HarvesterException {
        this.charset = InvariantUtil.checkNotNullOrThrow(charset, "charset");
        this.outputStream = InvariantUtil.checkNotNullOrThrow(outputStream, "outputStream");
        header = "<dataio-havester-datafile>".getBytes(this.charset);
        footer = "</dataio-havester-datafile>".getBytes(this.charset);
        try {
            outputStream.write(header, 0, header.length);
        } catch (IOException e) {
            throw new HarvesterException("Unable to add header to OutputStream", e);
        }
    }

    public void addRecord() throws HarvesterException {
        final byte[] record = "<record>data</record>".getBytes(charset);
        try {
            outputStream.write(record, 0, record.length);
        } catch (IOException e) {
            throw new HarvesterException("Unable to add record to OutputStream", e);
        }
    }

    @Override
    public void close() throws HarvesterException {
        try {
            outputStream.write(footer, 0, footer.length);
        } catch (IOException e) {
            throw new HarvesterException("Unable to add footer to OutputStream", e);
        }
    }
}
