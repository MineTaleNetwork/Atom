package cc.minetale.atom.util;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.text.MessageFormat;
import java.text.SimpleDateFormat;

public class Logger {

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

    public static void log(Level level, String log) {
        System.out.println("[" + dateFormat.format(System.currentTimeMillis()) + level.format(log));
    }

    @Getter
    @AllArgsConstructor
    public enum Level {
        INFO( " INFO]: {0}"),
        WARNING(" WARNING]: {0}"),
        DEBUG(" DEBUG]: {0}"),
        ERROR(" ERROR]: {0}");

        private final String string;

        public String format(Object... objects) {
            return new MessageFormat(string).format(objects);
        }

    }

}
