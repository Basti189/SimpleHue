package app.wolfware.bridge;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public class Config {

    private final static String configFilePath = "./hue.cfg";

    private static String application_name = "HueJava";

    private static String device_name = "Ryzen5900";

    private static String ip = "127.0.0.1";

    private static String token = "";

    public static void init() {
        try {
            FileInputStream fis = new FileInputStream(configFilePath);
            InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
            Properties props = new Properties();
            props.load(isr);

            application_name = props.getProperty("application_name");
            device_name = props.getProperty("device_name");
            ip = props.getProperty("ip");
            token = props.getProperty("token");

            isr.close();
            fis.close();
        } catch (IOException fnf) {
            System.out.println("No config");
            save();
            System.exit(0);
        }
    }

    public static void save() {
        Properties props = new Properties();

        props.setProperty("application_name", application_name);
        props.setProperty("device_name", device_name);
        props.setProperty("ip", ip);
        props.setProperty("token", token);

        Writer fstream = null;
        try {
            fstream = new OutputStreamWriter(new FileOutputStream(configFilePath), StandardCharsets.UTF_8);
            props.store(fstream, "config");
            fstream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getApplication_name() {
        return application_name;
    }

    public static String getDevice_name() {
        return device_name;
    }

    public static String getIp() {
        return ip;
    }

    public static String getToken() {
        return token;
    }
}