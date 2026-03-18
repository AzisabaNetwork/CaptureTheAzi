package net.azisaba.capturetheazi.util;

import org.jetbrains.annotations.NotNull;

public final class RandomUtil {
    public static int intOfDigits(int digits) {
        return (int) (Math.random() * Math.pow(10, digits));
    }

    public static @NotNull String randomDigits(int len) {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < len; i++) {
            s.append(intOfDigits(1));
        }
        return s.toString();
    }
}
