package com.x_tornado10.Settings;

import com.x_tornado10.Main;
import com.x_tornado10.util.Networking;
import com.x_tornado10.util.Paths;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.configuration2.FileBasedConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.configuration2.ex.ConversionException;
import java.io.File;


import java.io.*;
import java.net.Inet4Address;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Objects;

@Setter
@Getter
public class Server_Settings extends Settings {

    private String name = "Server-Config";
    private Type type = Type.SERVER;
    private int Port = 12345;
    private String IPv4 = "127.0.0.1";
    private float LED_Brightness = 0.20F;

    private Server_Settings backup;

    // get the default configuration values from internal resource folder and save them to config.yaml
    @Override
    public void saveDefaultConfig() throws IOException, NullPointerException {
        Main.logger.debug("Loading default server config values...");
        Main.logger.debug("Note: this only happens if server_config.yaml does not exist or couldn't be found!");
        Main.logger.debug("If your settings don't work and this message is shown please seek support on the projects GitHub page: " + Paths.Links.Project_GitHub);
        // get the internal resource folder and default config values
        URL url = getClass().getClassLoader().getResource("server_config.yaml");
        // if the path is null or not found an exception is thrown
        if (url == null) throw new NullPointerException();
        // try to open a new input stream to read the default values
        try(InputStream inputStream = url.openStream()) {
            // defining config.yaml file to save the values to
            File outputFile = new File(Paths.server_config);
            // try to open a new output stream to save the values to the new config file
            try (OutputStream outputStream = new FileOutputStream(outputFile)) {
                byte[] buffer = new byte[1024];
                // if the buffer isn't empty the write function writes the read bytes using the stored length in bytesRead var below
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }
        }
        Main.logger.debug("Successfully loaded default server config values!");
    }

    // copy settings from another settings class
    @Override
    public void copy(Settings settings1) {
        // check if other settings class type is compatible
        if (settings1.getType() != type) {
            // send error message if config type is not compatible
            if (settings1.getType() != Type.UNDEFINED) {
                Main.logger.error("Can't copy settings from " + settings1.getName() + " Type: " + settings1.getType() + " to " + getName() + " Type: " + type);
                return;
            }
            // send info message if other config class type is undefined
            Main.logger.debug("Can't confirm settings type! Type = UNDEFINED");
        }
        // casting other settings class to compatible type
        Server_Settings settings = (Server_Settings) settings1;
        // copy settings
        Main.logger.debug("Loading settings from " + settings.getName() + "...");
        this.Port = settings.getPort();
        this.IPv4 = settings.getIPv4();
        this.LED_Brightness = settings.getLED_Brightness();
        Main.logger.debug("Successfully loaded settings from " + settings.getName() + "!");
        Main.logger.debug(getName() + " now inherits all values from " + settings.getName());
    }

    // loading settings from config file
    @Override
    public void load(FileBasedConfiguration config) {
        // loading settings
        try {
            String tempIPv4 = config.getString(Paths.Server_Config.IPV4);
            // check if user specified an IPv4 address
            if (!Networking.isValidIP(tempIPv4)) {
                // check if the domain or host-name provided by the user is valid using a new thread to offload work from the main thread
                new Thread(() -> {
                    try {
                        // check host-name or domain using java.net and getting IPv4 if possible
                        Main.server_settings.setIPv4(String.valueOf(Inet4Address.getByName(tempIPv4)).split("/")[1].trim());
                    } catch (UnknownHostException e) {
                        Main.logger.error("Error while parsing Server-IP! Invalid IPv4 address or host name!");
                        Main.logger.warn("Invalid IPv4! Please restart the application!");
                        Main.logger.warn("IPv4 address does not match the following format: 0.0.0.0 - 255.255.255.255 or the provided host-name is invalid");
                        Main.logger.warn("There was an error while reading the config file, some settings may be broken!");
                    }
                }).start();
            } else {
                this.IPv4 = tempIPv4;
            }
            int tempPort = config.getInt(Paths.Server_Config.PORT);
            // checking if provided port is in the valid port range
            if (!Networking.isValidPORT(String.valueOf(tempPort))) {
                Main.logger.error("Error while parsing Server-Port! Invalid Port!");
                Main.logger.warn("Port is outside the valid range of 0-65535!");
                Main.logger.warn("There was an error while reading the config file, some settings may be broken!");
            } else {
                this.Port = tempPort;
            }
        } catch (ConversionException | NullPointerException e) {
            Main.logger.error("Error while parsing Server-IP and Server-Port! Not a valid number!");
            Main.logger.warn("Invalid port and / or IPv4 address! Please restart the application!");
            Main.logger.warn("There was an error while reading the config file, some settings may be broken!");
        }
        this.LED_Brightness = (float) config.getInt(Paths.Server_Config.BRIGHTNESS) / 100;
    }

    // save current settings to config file
    @Override
    public void save() {
        // check for changes to avoid unnecessary save
        if (this.equals(backup)) {
            Main.logger.debug("Didn't save " + name + " because nothing changed!");
            return;
        }
        Main.logger.debug("Saving " + name + " values to server-config.yaml...");
        // loading config file
        File file = new File(Paths.server_config);
        Configurations configs = new Configurations();
        FileBasedConfiguration config;
        Parameters parameters = new Parameters();
        // loading config file to memory
        try {
            config = configs.properties(file);
        } catch (ConfigurationException e) {
            Main.logger.error("Error occurred while writing server-config values to server-config.yaml!");
            Main.logger.warn("Please restart the application to prevent further errors!");
            return;
        }

        // initializing new config builder
        FileBasedConfigurationBuilder<FileBasedConfiguration> builder =
                new FileBasedConfigurationBuilder<FileBasedConfiguration>(config.getClass())
                        .configure(parameters.fileBased()
                                .setFile(file));

        // writing config settings to file
        try {
            builder.getConfiguration().setProperty(Paths.Server_Config.BRIGHTNESS, Math.round(LED_Brightness * 100));
            builder.getConfiguration().setProperty(Paths.Server_Config.IPV4, IPv4);
            builder.getConfiguration().setProperty(Paths.Server_Config.PORT, Port);
            // saving settings
            builder.save();
        } catch (ConfigurationException e)  {
            Main.logger.error("Something went wrong while saving the config values for server-config.yaml!");
            Main.logger.warn("Please restart the application to prevent further errors!");
            Main.logger.warn("Previously made changes to the server-config may be lost!");
            Main.logger.warn("If this message appears on every attempt to save config changes please open an issue on GitHub!");
            return;
        }

        Main.logger.debug("Successfully saved server-config values to server-config.yaml!");
    }

    // creating clone for unnecessary saving check
    @Override
    public void startup() {
        this.backup = new Server_Settings().cloneS();
    }

    // creating a clone of this config class
    @Override
    public Server_Settings cloneS() {
        Server_Settings settings1 = new Server_Settings();
        settings1.copy(Main.server_settings);
        return settings1;
    }

    public void setLED_Brightness(float LED_Brightness) {
        this.LED_Brightness = LED_Brightness / 100;
    }

    // used to check if current settings equal another settings class
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        Server_Settings other = (Server_Settings) obj;
        return LED_Brightness == other.LED_Brightness &&
                Objects.equals(IPv4, other.IPv4) &&
                Objects.equals(Port, other.Port);
    }

    // generate hash code for current settings
    @Override
    public int hashCode() {
        return Objects.hash(LED_Brightness, IPv4, Port);
    }
}