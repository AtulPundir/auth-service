package com.myapp.authservice.util;

import java.security.SecureRandom;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * CUID (Collision-resistant Unique Identifier) Generator
 *
 * Format: c[timestamp][counter][fingerprint][random]
 * Example: ckz5q0w0x0000qzrmn0mqgqzr
 *
 * Reference: https://github.com/paralleldrive/cuid
 */
public class CuidGenerator {

    private static final String BASE = "0123456789abcdefghijklmnopqrstuvwxyz";
    private static final int BASE_SIZE = BASE.length();
    private static final AtomicInteger counter = new AtomicInteger(0);
    private static final SecureRandom random = new SecureRandom();
    private static final String fingerprint = generateFingerprint();

    /**
     * Generate a CUID
     * @return CUID string (e.g., "ckz5q0w0x0000qzrmn0mqgqzr")
     */
    public static String generate() {
        long timestamp = System.currentTimeMillis();
        int count = counter.getAndIncrement();
        if (count >= 1679616) { // 36^4
            counter.set(0);
        }

        StringBuilder cuid = new StringBuilder("c");
        cuid.append(toBase36(timestamp));
        cuid.append(pad(toBase36(count), 4));
        cuid.append(fingerprint);
        cuid.append(randomBlock());
        cuid.append(randomBlock());

        return cuid.toString();
    }

    /**
     * Convert number to base36
     */
    private static String toBase36(long num) {
        if (num == 0) return "0";

        StringBuilder result = new StringBuilder();
        long n = Math.abs(num);

        while (n > 0) {
            result.insert(0, BASE.charAt((int)(n % BASE_SIZE)));
            n /= BASE_SIZE;
        }

        return result.toString();
    }

    /**
     * Pad string to specified length
     */
    private static String pad(String str, int size) {
        StringBuilder padded = new StringBuilder(str);
        while (padded.length() < size) {
            padded.insert(0, '0');
        }
        return padded.toString().substring(padded.length() - size);
    }

    /**
     * Generate random block of 4 characters
     */
    private static String randomBlock() {
        int num = random.nextInt(1679616); // 36^4
        return pad(toBase36(num), 4);
    }

    /**
     * Generate machine fingerprint (stable across app restarts)
     */
    private static String generateFingerprint() {
        // Use JVM process ID and hostname for fingerprint
        String pid = String.valueOf(ProcessHandle.current().pid());
        String hostname = getHostname();
        int hash = (pid + hostname).hashCode();
        return pad(toBase36(Math.abs(hash)), 4);
    }

    /**
     * Get hostname safely
     */
    private static String getHostname() {
        try {
            return java.net.InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "localhost";
        }
    }
}
