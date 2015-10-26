package com.bearychat.rundeck.plugins;


public class BearyChatNotificationPluginException extends RuntimeException {

    /**
     * Constructor.
     *
     * @param message error message
     */
    public BearyChatNotificationPluginException(String message) {
        super(message);
    }

    /**
     * Constructor.
     *
     * @param message error message
     * @param cause exception cause
     */
    public BearyChatNotificationPluginException(String message, Throwable cause) {
        super(message, cause);
    }

}
