package dk.dbc.dataio.jobstore.service.digest;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;

public class Md5 {
    private final static String MD5_ALGORITHM = "MD5";

    /* Private constructor in order to keep class static */
    private Md5() {
    }

    /**
     * Calculates the MD5 digest and returns the value as a 32 character hex string.
     *
     * @param data data to digest
     * @return MD5 digest as a hex string
     * @throws NullPointerException  when given null valued data argument
     * @throws IllegalStateException when unable to utilize md5 algorithm
     */
    public static String asHex(byte[] data) throws NullPointerException, IllegalStateException {
        // MessageDigest algorithm implementations are not tread safe, so we
        // create a new instance each time
        MessageDigest md;
        try {
            md = MessageDigest.getInstance(MD5_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            // transform NoSuchAlgorithmException into run-time exception
            throw new IllegalStateException(e);
        }
        md.update(data);
        return hashAsString(md.digest());
    }

    private static String hashAsString(byte[] hashBytes) {
        StringBuilder hexString = new StringBuilder(hashBytes.length * 2);
        try(Formatter hexStringFormatter = new Formatter(hexString)) {
            for (byte b : hashBytes) {
                // each java byte is represented as a 2-digit hex string
                hexStringFormatter.format("%02x", b);
            }
            return hexString.toString();
        }
    }
}
