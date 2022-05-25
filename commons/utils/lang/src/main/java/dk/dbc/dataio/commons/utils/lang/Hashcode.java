package dk.dbc.dataio.commons.utils.lang;

/**
 * HashCode implementation suitable for general hash-based lookup
 * based on the MurmurHash3 algorithm.
 * <p>
 * The MurmurHash3 algorithm was created by Austin Appleby and put into the public domain.
 * See http://code.google.com/p/smhasher/.
 * <p>
 * The java port was authored by Yonik Seeley and was placed into the public domain per
 * https://github.com/yonik/java_util/blob/master/src/util/hash/MurmurHash3.java.
 * </p>
 */
public class Hashcode {
    public Hashcode() {
    }

    /**
     * Generates 32 bit hash from string.
     * Note: that this method is slower than java.lang.String.hashCode() but is
     * less prone to collisions and does not suffer from the "common prefix" problem
     * (see https://dzone.com/articles/what-is-wrong-with-hashcode-in-javalangstring)
     *
     * @param str string to hash
     * @return 32 bit hash of the given string
     */
    public static int of(String str) {
        return Hashcode.of(toBytesWithoutEncoding(str));
    }

    /**
     * Generates 32 bit hash from byte array.
     *
     * @param data byte array to hash
     * @return 32 bit hash of the given array
     */
    public static int of(final byte[] data) {
        return murmurhash32(data, 0, data.length, 0x9747b28c);
    }

    /**
     * Generates 32 bit hash using the MurmurHash3 algorithm from byte array
     * of the given length and seed.
     * For more info on the MurmurHash algorithm see https://en.wikipedia.org/wiki/MurmurHash
     * and http://yonik.com/murmurhash3-for-java/
     *
     * @param data   byte array to hash
     * @param offset array offset
     * @param len    length of the array to hash
     * @param seed   initial seed value
     * @return 32 bit hash of the given array
     */
    public static int murmurhash32(byte[] data, int offset, int len, int seed) {
        final int c1 = 0xcc9e2d51;
        final int c2 = 0x1b873593;

        int h1 = seed;
        int roundedEnd = offset + len & 0xfffffffc;  // round down to 4 byte block

        for (int i = offset; i < roundedEnd; i += 4) {
            // little endian load order
            int k1 = (data[i] & 0xff) | ((data[i + 1] & 0xff) << 8) | ((data[i + 2] & 0xff) << 16) | (data[i + 3] << 24);
            k1 *= c1;
            k1 = (k1 << 15) | (k1 >>> 17);  // ROTL32(k1,15);
            k1 *= c2;

            h1 ^= k1;
            h1 = (h1 << 13) | (h1 >>> 19);  // ROTL32(h1,13);
            h1 = h1 * 5 + 0xe6546b64;
        }

        // tail
        int k1 = 0;

        switch (len & 0x03) {
            case 3:
                k1 = (data[roundedEnd + 2] & 0xff) << 16;
                // fallthrough
            case 2:
                k1 |= (data[roundedEnd + 1] & 0xff) << 8;
                // fallthrough
            case 1:
                k1 |= data[roundedEnd] & 0xff;
                k1 *= c1;
                k1 = (k1 << 15) | (k1 >>> 17);  // ROTL32(k1,15);
                k1 *= c2;
                h1 ^= k1;
        }

        // finalization
        h1 ^= len;

        // fmix(h1);
        h1 ^= h1 >>> 16;
        h1 *= 0x85ebca6b;
        h1 ^= h1 >>> 13;
        h1 *= 0xc2b2ae35;
        h1 ^= h1 >>> 16;

        return h1;
    }

    private static byte[] toBytesWithoutEncoding(String str) {
        int len = str.length();
        int pos = 0;
        byte[] buf = new byte[len << 1];
        for (int i = 0; i < len; i++) {
            char c = str.charAt(i);
            buf[pos++] = (byte) (c & 0xFF);
            buf[pos++] = (byte) (c >> 8);
        }
        return buf;
    }
}
