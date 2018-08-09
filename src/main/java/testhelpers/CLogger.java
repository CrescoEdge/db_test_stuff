package testhelpers;

import java.util.logging.Logger;

public class CLogger {
    private CLogger.Level level;
    private String issuingClassName;
    private String baseClassName;
    private PluginBuilder pluginBuilder;
    private Logger logService;
    private String source;

    public CLogger(PluginBuilder pluginBuilder, String baseClassName, String issuingClassName, CLogger.Level level) {
        this.pluginBuilder = pluginBuilder;
        this.baseClassName = baseClassName;
        this.issuingClassName = issuingClassName.substring(baseClassName.length() + 1, issuingClassName.length());
        this.level = level;
        if (pluginBuilder.getPluginID() != null) {
            this.source = pluginBuilder.getPluginID();
        } else {
            this.source = "agent";
        }

        this.logService = Logger.getLogger(this.source);
    }

    public void error(String logMessage) {
        if (this.level.toShow(CLogger.Level.Error)) {
            this.log(logMessage, CLogger.Level.Error);
        }
    }

    public void error(String logMessage, Object... params) {
        if (this.level.toShow(CLogger.Level.Error)) {
            this.error(this.replaceBrackets(logMessage, params));
        }
    }

    public void warn(String logMessage) {
        if (this.level.toShow(CLogger.Level.Warn)) {
            this.log(logMessage, CLogger.Level.Warn);
        }
    }

    public void warn(String logMessage, Object... params) {
        if (this.level.toShow(CLogger.Level.Warn)) {
            this.warn(this.replaceBrackets(logMessage, params));
        }
    }

    public void info(String logMessage) {
        if (this.level.toShow(CLogger.Level.Info)) {
            this.log(logMessage, CLogger.Level.Info);
        }
    }

    public void info(String logMessage, Object... params) {
        if (this.level.toShow(CLogger.Level.Info)) {
            this.info(this.replaceBrackets(logMessage, params));
        }
    }

    public void debug(String logMessage) {
        if (this.level.toShow(CLogger.Level.Debug)) {
            this.log(logMessage, CLogger.Level.Debug);
        }
    }

    public void debug(String logMessage, Object... params) {
        if (this.level.toShow(CLogger.Level.Debug)) {
            this.debug(this.replaceBrackets(logMessage, params));
        }
    }

    public void trace(String logMessage) {
        if (this.level.toShow(CLogger.Level.Trace)) {
            this.log(logMessage, CLogger.Level.Trace);
        }
    }

    public void trace(String logMessage, Object... params) {
        if (this.level.toShow(CLogger.Level.Trace)) {
            this.trace(this.replaceBrackets(logMessage, params));
        }
    }

    public void log(String messageBody, CLogger.Level level) {
        java.util.logging.Level l2 = null;
        String levelString = level.name();
        byte var6 = -1;
        switch(levelString.hashCode()) {
            case 2283726:
                if (levelString.equals("Info")) {
                    var6 = 2;
                }
                break;
            case 2688678:
                if (levelString.equals("Warn")) {
                    var6 = 3;
                }
                break;
            case 65906227:
                if (levelString.equals("Debug")) {
                    var6 = 1;
                }
                break;
            case 67232232:
                if (levelString.equals("Error")) {
                    var6 = 4;
                }
                break;
            case 81068325:
                if (levelString.equals("Trace")) {
                    var6 = 0;
                }
        }

        switch(var6) {
            case 0:
                l2 = java.util.logging.Level.FINER;
                break;
            case 1:
                l2 = java.util.logging.Level.FINE;
                break;
            case 2:
                l2 = java.util.logging.Level.INFO;
                break;
            case 3:
                l2 = java.util.logging.Level.WARNING;
                break;
            case 4:
                l2 = java.util.logging.Level.SEVERE;
                break;
            default:
                l2 = java.util.logging.Level.SEVERE;
        }

        String logMessage = "[" + this.source + ": " + this.baseClassName + "]";
        logMessage = logMessage + "[" + this.formatClassName(this.issuingClassName) + "]";
        logMessage = logMessage + " " + messageBody;
        this.logService.log(l2, logMessage);
    }

    private String formatClassName(String className) {
        String newName = "";
        int lastIndex = 0;

        for(int nextIndex = className.indexOf(".", lastIndex + 1); nextIndex != -1; nextIndex = className.indexOf(".", lastIndex + 1)) {
            newName = newName + className.substring(lastIndex, lastIndex + 1) + ".";
            lastIndex = nextIndex + 1;
        }

        return newName + className.substring(lastIndex);
    }

    public CLogger.Level getLogLevel() {
        return this.level;
    }

    public void setLogLevel(CLogger.Level level) {
        this.level = level;
    }

    private String replaceBrackets(String logMessage, Object... params) {
        for(int replaced = 0; logMessage.contains("{}") && replaced < params.length; ++replaced) {
            logMessage = logMessage.replaceFirst("\\{\\}", String.valueOf(params[replaced]));
        }

        return logMessage;
    }

    public static enum Level {
        None(-1),
        Error(0),
        Warn(1),
        Info(2),
        Debug(4),
        Trace(8);

        private final int level;

        private Level(int level) {
            this.level = level;
        }

        public int getValue() {
            return this.level;
        }

        public boolean toShow(CLogger.Level check) {
            return check.getValue() <= this.getValue();
        }
    }

}
