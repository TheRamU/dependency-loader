package io.github.theramu.dependencyloader.util;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * @author TheRamU
 * @since 2024/8/25 2:57
 */
public class ExceptionUtil {

    public static String stackTraceToString(Throwable e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }
}
