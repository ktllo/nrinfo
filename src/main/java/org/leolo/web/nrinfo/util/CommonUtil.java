package org.leolo.web.nrinfo.util;

import java.util.UUID;

public final class CommonUtil {

    private static com.fasterxml.uuid.NoArgGenerator UUIDGenerators = null;

    public static int randomInt(int min, int max) {
        return (int) (Math.random() * (max - min) + min);
    }

    public static byte[] uuidToBytes(UUID uuid) {
        if (uuid == null) {
            return null;
        }
        byte[] bytes = new byte[16];
        long msb = uuid.getMostSignificantBits();
        long lsb = uuid.getLeastSignificantBits();
        for (int i = 0; i < 8; i++) {
            bytes[i] = (byte) (msb >>> (8 * (7 - i)));
            bytes[8 + i] = (byte) (lsb >>> (8 * (7 - i)));
        }
        return bytes;
    }

    public static UUID bytesToUUID(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        long msb = 0, lsb = 0;
        for (int i = 0; i < 8; i++) {
            msb = (msb << 8) | (bytes[i] & 0xff);
        }
        for (int i = 8; i < 16; i++) {
            lsb = (lsb << 8) | (bytes[i] & 0xff);
        }
        return new UUID(msb, lsb);
    }

    public static UUID generateUUID() {
        if (UUIDGenerators == null) {
            UUIDGenerators = com.fasterxml.uuid.Generators.timeBasedEpochGenerator();
        }
        return UUIDGenerators.generate();
    }



}
