package com.exteragram.messenger.utils;

import java.io.PrintWriter;
import java.io.StringWriter;

/** IthaGram: version minima de AppUtils de exteraGram. */
public class AppUtils {
    public static int getVersionCode() {
        try {
            android.content.Context c = org.telegram.messenger.ApplicationLoader.applicationContext;
            return c.getPackageManager().getPackageInfo(c.getPackageName(), 0).versionCode;
        } catch (Throwable t) {
            return 0;
        }
    }

    public static String stackTraceToString(Throwable throwable) {
        if (throwable == null) {
            return "";
        }
        StringWriter stringWriter = new StringWriter();
        throwable.printStackTrace(new PrintWriter(stringWriter));
        return stringWriter.toString();
    }

    public static int compareVersionValues(String left, String right) {
        String[] leftParts = left.split("\\.");
        String[] rightParts = right.split("\\.");
        int max = Math.max(leftParts.length, rightParts.length);
        for (int i = 0; i < max; i++) {
            int leftValue = i < leftParts.length ? parse(leftParts[i]) : 0;
            int rightValue = i < rightParts.length ? parse(rightParts[i]) : 0;
            if (leftValue != rightValue) {
                return Integer.compare(leftValue, rightValue);
            }
        }
        return 0;
    }

    private static int parse(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return 0;
        }
    }

    public static boolean compareVersions(String operator, String left, String right) {
        return test(operator, compareVersionValues(left, right));
    }

    public static boolean compareVersions(String operator, int left, int right) {
        return test(operator, Integer.compare(left, right));
    }

    private static boolean test(String operator, int c) {
        switch (operator) {
            case "<": return c < 0;
            case ">": return c > 0;
            case "<=": return c <= 0;
            case "==": return c == 0;
            case ">=": return c >= 0;
            case "!=": return c != 0;
            default: return false;
        }
    }
}
