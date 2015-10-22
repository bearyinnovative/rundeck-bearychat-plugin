package com.bearychat.rundeck.plugins;


public class BearychatNotificationPluginException extends RuntimeException {

    /**
     * Constructor.
     *
     * @param message error message
     */
    public BearychatNotificationPluginException(String message) {
        super(message);
    }

    /**
     * Constructor.
     *
     * @param message error message
     * @param cause exception cause
     */
    public BearychatNotificationPluginException(String message, Throwable cause) {
        super(message, cause);
    }

}
