import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class LoggingSystem {
    public static void main(String[] args) {
        LogStrategy logStrategy = new ConsoleLog();
        Logger logger = Logger.getLoggerInstance(logStrategy);
        List<LogMessage> logMessageList = new ArrayList<>();
        logMessageList.add(new LogMessage(System.currentTimeMillis(), "1 info msg", 1));
        logMessageList.add(new LogMessage(System.currentTimeMillis(), "1 error msg", 3));
        logMessageList.add(new LogMessage(System.currentTimeMillis(), "1 debug msg", 2));
        logMessageList.add(new LogMessage(System.currentTimeMillis(), "2 error msg", 3));
        logMessageList.add(new LogMessage(System.currentTimeMillis(), "2 debug msg", 2));
        logMessageList.add(new LogMessage(System.currentTimeMillis(), "3 debug msg", 2));
        logMessageList.add(new LogMessage(System.currentTimeMillis(), "2 info msg", 1));

        for (LogMessage logMessage: logMessageList) {
            logger.logMessage(logMessage);
        }
    }
}


class LogMessage {
    long timeStamp;
    String msg;
    int level;
    public LogMessage(long timeStamp, String msg, int level) {
        this.level = level;
        this.msg = msg;
        this.timeStamp = timeStamp;
    }
}

class Logger {
    static Logger logger;
    LoggerAppender loggerAppender;
    private Logger(LoggerAppender loggerAppender) {
        this.loggerAppender = loggerAppender;
    }

    public static Logger getLoggerInstance(LogStrategy logStrategy) {
        if (logger == null) {
            logger = new Logger(new InfoLogAppender(1, logStrategy));
        }
        return logger;
    }

    public void logMessage(LogMessage logMessage) {
        this.loggerAppender.append(logMessage);
    }
}

interface LoggerAppender {
    public void append(LogMessage logMessage);
}

class InfoLogAppender implements LoggerAppender {
    int level;
    LogStrategy logStrategy;
    LoggerAppender nextLogAppender;
    public InfoLogAppender(int level, LogStrategy logStrategy) {
        this.level = level;
        this.logStrategy = logStrategy;
        this.nextLogAppender = new DebugLogAppender(2, logStrategy);
    }

    public void append(LogMessage logMessage) {
        if (this.level < logMessage.level) {
            System.out.println("INFO DELEGATING");
            nextLogAppender.append(logMessage);
        } else {
            System.out.print("INFO ");
            this.logStrategy.log(logMessage);
        }
    }
}

class DebugLogAppender implements LoggerAppender {
    int level;
    LogStrategy logStrategy;
    LoggerAppender nextLogAppender;
    public DebugLogAppender(int level, LogStrategy logStrategy) {
        this.level = level;
        this.logStrategy = logStrategy;
        this.nextLogAppender = new ErrorLogAppender(3, logStrategy);
    }

    public void append(LogMessage logMessage) {
        if (this.level < logMessage.level) {
            System.out.println("DEBUG DELEGATING");
            nextLogAppender.append(logMessage);
        } else {
            System.out.print("DEBUG ");
            this.logStrategy.log(logMessage);
        }
    }
}

class ErrorLogAppender implements LoggerAppender {
    int level;
    LogStrategy logStrategy;
    LoggerAppender nextLogAppender;
    public ErrorLogAppender(int level, LogStrategy logStrategy) {
        this.level = level;
        this.logStrategy = logStrategy;
    }

    public void append(LogMessage logMessage) {
        System.out.print("ERROR ");
        this.logStrategy.log(logMessage);
    }
}

interface LogStrategy {
    public void log(LogMessage logMessage);
}

class ConsoleLog implements LogStrategy {
    public void log(LogMessage logMessage) {
        System.out.println("Logging in console " + logMessage.timeStamp + ":" + logMessage.level + ":" + logMessage.msg);
    }
}

class FileLog implements LogStrategy {
    public void log(LogMessage logMessage) {
        System.out.println("Logging in file " + logMessage.timeStamp + ":" + logMessage.level + ":" + logMessage.msg);
    }
}
