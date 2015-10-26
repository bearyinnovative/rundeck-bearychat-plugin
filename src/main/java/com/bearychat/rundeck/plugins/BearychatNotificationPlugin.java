package com.bearychat.rundeck.plugins;

import com.dtolabs.rundeck.core.plugins.Plugin;
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription;
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty;
import com.dtolabs.rundeck.plugins.notification.NotificationPlugin;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.*;

import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

@Plugin(service= "Notification", name="BearyChatNotification")
@PluginDescription(title="BearyChat Rundeck WebHook", description="Sends Rundeck Notifications to BearyChat")
public class BearyChatNotificationPlugin implements NotificationPlugin {

    private static final String BEARYCHAT_MESSAGE_COLOR_GREEN = "#D2F3D8";
    private static final String BEARYCHAT_MESSAGE_COLOR_YELLOW = "#FFEFAA";
    private static final String BEARYCHAT_MESSAGE_COLOR_RED = "#FFB39A";

    private static final String BEARYCHAT_MESSAGE_FROM_NAME = "Rundeck";
    private static final String BEARYCHAT_MESSAGE_TEMPLATE = "bearychat-rundeck-message.ftl";

    private static final String TRIGGER_START = "start";
    private static final String TRIGGER_SUCCESS = "success";
    private static final String TRIGGER_FAILURE = "failure";

    private static final Map<String, BearyChatNotificationData> TRIGGER_NOTIFICATION_DATA = new HashMap<String, BearyChatNotificationData>();

    private static final Configuration FREEMARKER_CFG = new Configuration();

    @PluginProperty(title = "WebHook URL", description = "BearyChat Rundeck WebHook URL", required = true)
    private String webhook_url;

    /**
     * Sends a message to a BearyChat room when a job notification event is raised by Rundeck.
     *
     * @param trigger name of job notification event causing notification
     * @param executionData job execution data
     * @param config plugin configuration
     * @throws BearyChatNotificationPluginException when any error occurs sending the BearyChat message
     * @return true, if the BearyChat API response indicates a message was successfully delivered to a chat room
     */
    public boolean postNotification(String trigger, Map executionData, Map config) {

        String ACTUAL_BEARYCHAT_TEMPLATE;

        ClassTemplateLoader builtInTemplate = new ClassTemplateLoader(BearyChatNotificationPlugin.class, "/templates");
        TemplateLoader[] loaders = new TemplateLoader[]{builtInTemplate};
        MultiTemplateLoader mtl = new MultiTemplateLoader(loaders);
        FREEMARKER_CFG.setTemplateLoader(mtl);
        ACTUAL_BEARYCHAT_TEMPLATE = BEARYCHAT_MESSAGE_TEMPLATE;

        TRIGGER_NOTIFICATION_DATA.put(TRIGGER_START, new BearyChatNotificationData(ACTUAL_BEARYCHAT_TEMPLATE, BEARYCHAT_MESSAGE_COLOR_YELLOW));
        TRIGGER_NOTIFICATION_DATA.put(TRIGGER_SUCCESS, new BearyChatNotificationData(ACTUAL_BEARYCHAT_TEMPLATE, BEARYCHAT_MESSAGE_COLOR_GREEN));
        TRIGGER_NOTIFICATION_DATA.put(TRIGGER_FAILURE, new BearyChatNotificationData(ACTUAL_BEARYCHAT_TEMPLATE, BEARYCHAT_MESSAGE_COLOR_RED));

        try {
            FREEMARKER_CFG.setSetting(Configuration.CACHE_STORAGE_KEY, "strong:20, soft:250");
            FREEMARKER_CFG.setDefaultEncoding("UTF-8");
        }catch(Exception e){
            System.err.printf("Got and exception from Freemarker: %s", e.getMessage());
        }

        if (!TRIGGER_NOTIFICATION_DATA.containsKey(trigger)) {
            throw new IllegalArgumentException("Unknown trigger type: [" + trigger + "].");
        }

        String message = generateMessage(trigger, executionData, config);
        String bearychatResponse = invokeBearyChatAPIMethod(webhook_url, message);
        String ms = "payload=" + URLEncoder.encode(message);

        if ("{\"code\":0,\"result\":null}".equals(bearychatResponse)) {
            return true;
        } else {
            throw new BearyChatNotificationPluginException("Unknown status returned from BearyChat API: [" + bearychatResponse + "]." + "\n" + ms);
        }
    }

    private String generateMessage(String trigger, Map executionData, Map config) {
        String templateName = TRIGGER_NOTIFICATION_DATA.get(trigger).template;
        String color = TRIGGER_NOTIFICATION_DATA.get(trigger).color;

        HashMap<String, Object> model = new HashMap<String, Object>();
        model.put("trigger", trigger);
        model.put("color", color);
        model.put("executionData", executionData);
        model.put("config", config);

        StringWriter sw = new StringWriter();
        try {
            Template template = FREEMARKER_CFG.getTemplate(templateName, "utf-8");
            template.setEncoding("UTF-8");
            template.process(model,sw);

        } catch (IOException ioEx) {
            throw new BearyChatNotificationPluginException("Error loading BearyChat notification message template: [" + ioEx.getMessage() + "].", ioEx);
        } catch (TemplateException templateEx) {
            throw new BearyChatNotificationPluginException("Error merging BearyChat notification message template: [" + templateEx.getMessage() + "].", templateEx);
        }

        return sw.toString();
    }

    private String urlEncode(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (UnsupportedEncodingException unsupportedEncodingException) {
            throw new BearyChatNotificationPluginException("URL encoding error: [" + unsupportedEncodingException.getMessage() + "].", unsupportedEncodingException);
        }
    }

    private String invokeBearyChatAPIMethod(String webhook_url, String message) {
        URL requestUrl = toURL(webhook_url);

        HttpURLConnection connection = null;
        InputStream responseStream = null;
        String body = "payload=" + URLEncoder.encode(message);
        try {
            connection = openConnection(requestUrl);
            putRequestStream(connection, body);
            responseStream = getResponseStream(connection);
            return getBearyChatResponse(responseStream);

        } finally {
            closeQuietly(responseStream);
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private URL toURL(String url) {
        try {
            return new URL(url);
        } catch (MalformedURLException malformedURLEx) {
            throw new BearyChatNotificationPluginException("BearyChat API URL is malformed: [" + malformedURLEx.getMessage() + "].", malformedURLEx);
        }
    }

    private HttpURLConnection openConnection(URL requestUrl) {
        try {
            return (HttpURLConnection) requestUrl.openConnection();
        } catch (IOException ioEx) {
            throw new BearyChatNotificationPluginException("Error opening connection to BearyChat URL: [" + ioEx.getMessage() + "].", ioEx);
        }
    }

    private void putRequestStream(HttpURLConnection connection, String message) {
        try {
            connection.setRequestMethod("POST");
            connection.setRequestProperty("charset", "utf-8");

            connection.setDoInput(true);
            connection.setDoOutput(true);
            DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
            wr.writeBytes(message);
            wr.flush();
            wr.close();
        } catch (IOException ioEx) {
            throw new BearyChatNotificationPluginException("Error putting data to BearyChat URL: [" + ioEx.getMessage() + "].", ioEx);
        }
    }

    private InputStream getResponseStream(HttpURLConnection connection) {
        InputStream input = null;
        try {
            input = connection.getInputStream();
        } catch (IOException ioEx) {
            input = connection.getErrorStream();
        }
        return input;
    }

    private int getResponseCode(HttpURLConnection connection) {
        try {
            return connection.getResponseCode();
        } catch (IOException ioEx) {
            throw new BearyChatNotificationPluginException("Failed to obtain HTTP response: [" + ioEx.getMessage() + "].", ioEx);
        }
    }

    private String getBearyChatResponse(InputStream responseStream) {
        try {
            return new Scanner(responseStream,"UTF-8").useDelimiter("\\A").next();
        } catch (Exception ioEx) {
            throw new BearyChatNotificationPluginException("Error reading BearyChat API JSON response: [" + ioEx.getMessage() + "].", ioEx);
        }
    }

    private void closeQuietly(InputStream input) {
        if (input != null) {
            try {
                input.close();
            } catch (IOException ioEx) {
                // ignore
            }
        }
    }

    private static class BearyChatNotificationData {
        private String template;
        private String color;
        public BearyChatNotificationData(String template, String color) {
            this.color = color;
            this.template = template;
        }
    }
}
