package de.fau.wintechis.sim;

public class IllegalEngineStateException extends RuntimeException {

    public IllegalEngineStateException() {
        super();
    }

    public IllegalEngineStateException(String msg) {
        super(msg);
    }

    public IllegalEngineStateException(Throwable reason) {
        super(reason);
    }

    public IllegalEngineStateException(String msg, Throwable reason) {
        super(msg, reason);
    }

}
