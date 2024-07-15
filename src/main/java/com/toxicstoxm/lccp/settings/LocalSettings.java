package com.toxicstoxm.lccp.settings;

import com.toxicstoxm.lccp.LCCP;
import com.toxicstoxm.lccp.Constants;
import lombok.Getter;
import org.apache.commons.configuration2.YAMLConfiguration;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.configuration2.ex.ConversionException;
import org.apache.commons.configuration2.io.FileHandler;

import java.io.*;
import java.net.URL;
import java.util.*;


// settings class to store config settings on runtime
@Getter
public class LocalSettings extends Settings {
    // default settings
    private final Type type = Type.LOCAL;
    private final String name = "Main-Config";
    private boolean WindowResizeable = true;
    private int WindowDefWidth = 1280;
    private int WindowDefHeight = 720;
    private int LogLevel = 4;
    private String selectionDir = System.getProperty("user.home");
    private boolean DisplayStatusBar = false;
    private boolean CheckIPv4 = true;
    private boolean AutoPlayAfterUpload = true;
    private int NetworkingCommunicationClockSpeed = 10;

    private LocalSettings backup;

    // get the default configuration values from internal resource folder and save them to config.yaml
    @Override
    public void saveDefaultConfig() throws IOException, NullPointerException {
        LCCP.logger.debug("Loading default config values...");
        LCCP.logger.debug("Note: this only happens if config.yaml does not exist or couldn't be found!");
        LCCP.logger.debug("If your settings don't work and this message is shown");
        LCCP.logger.debug(Constants.Messages.WARN.OPEN_GITHUB_ISSUE);
        // get the internal resource folder and default config values
        URL url = getClass().getClassLoader().getResource("config.yaml");
        // if the path is null or not found an exception is thrown
        if (url == null) throw new NullPointerException();
        // try to open a new input stream to read the default values
        try(InputStream inputStream = url.openStream()) {
            // defining config.yaml file to save the values to
            File outputFile = new File(Constants.File_System.config);
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
        LCCP.logger.debug("Successfully loaded default config values!");
    }
    // load the config settings from config.yaml and store them in an instance of this class
    @Override
    public void load(YAMLConfiguration config) {
        LCCP.logger.debug("Loading config values to memory...");
        try {
            // setting values to parsed config values
            this.WindowResizeable = config.getBoolean(Constants.Config.WINDOW_RESIZABLE);

            // handle potential ConversionExceptions gracefully
            try {
                this.WindowDefHeight = config.getInt(Constants.Config.WINDOW_DEFAULT_HEIGHT);
                this.WindowDefWidth = config.getInt(Constants.Config.WINDOW_DEFAULT_WIDTH);
            } catch (ConversionException e) {
                LCCP.logger.error("Error while parsing Window-Default-Height and Window-Default-Width! Not a valid Number!");
                LCCP.logger.warn("There was an error while reading the config file, some settings may be broken!");
            }

            try {
                this.LogLevel = config.getInt(Constants.Config.LOG_LEVEL);
            } catch (ConversionException e) {
                LCCP.logger.error("Error while parsing Log-Level! Not a valid Number!");
                LCCP.logger.warn("There was an error while reading the config file, some settings may be broken!");
            }

            try {
                double temp = config.getDouble(Constants.Config.NETWORK_COMMUNICATION_CLOCK_SPEED);
                this.NetworkingCommunicationClockSpeed = (int) Math.round(temp * 1000);
            } catch (ClassCastException | ConversionException e) {
                LCCP.logger.error("Error while parsing NetworkingCommunicationClockSpeed! Not a valid time argument (seconds)!");
                LCCP.logger.warn("There was an error while reading the config file, some settings may be broken!");
            }

            this.selectionDir = config.getString(Constants.Config.SELECTION_DIR);

            DisplayStatusBar = config.getBoolean(Constants.Config.DISPLAY_STATUS_BAR);
            CheckIPv4 = config.getBoolean(Constants.Config.CHECK_IPV4);
            AutoPlayAfterUpload = config.getBoolean(Constants.Config.AUTO_PLAY_AFTER_UPLOAD);

            LCCP.logger.debug("Loaded config values to memory!");
        } catch (NoSuchElementException e){
            LCCP.logger.error("Error while parsing config! Settings / values missing! Your probably using an old config file!");
            LCCP.logger.warn("Program halted to prevent any further errors!");
            LCCP.logger.warn("Please delete the old config file from your .config folder and restart the application!");
            LCCP.exit(1);
        }
    }

    // copy the settings of another settings class
    @Override
    public void copy(Settings settings1) {
        // check if other settings class type is compatible
        if (settings1.getType() != type) {
            if (settings1.getType() != Type.UNDEFINED) {
                LCCP.logger.error("Can't copy settings from " + settings1.getName() + " Type: " + settings1.getType() + " to " + getName() + " Type: " + type);
                return;
            }
            LCCP.logger.info("Can't confirm settings type! Type = UNDEFINED");
        }
        // casting to compatible settings type after check
        LocalSettings settings = (LocalSettings) settings1;
        LCCP.logger.debug("Loading settings from " + settings.getName() + "...");
        // copying settings
        this.WindowResizeable = settings.isWindowResizeable();
        this.WindowDefWidth = settings.getWindowDefWidth();
        this.WindowDefHeight = settings.getWindowDefHeight();
        this.LogLevel = settings.getLogLevel();
        this.selectionDir = settings.getSelectionDir();
        this.DisplayStatusBar = settings.DisplayStatusBar;
        this.NetworkingCommunicationClockSpeed = settings.NetworkingCommunicationClockSpeed;
        this.CheckIPv4 = settings.CheckIPv4;
        this.AutoPlayAfterUpload = settings.AutoPlayAfterUpload;
        LCCP.logger.debug("Successfully loaded settings from " + settings.getName() + "!");
        LCCP.logger.debug(getName() + " now inherits all values from " + settings.getName());
    }

    // saving current settings to disk
    @Override
    public void save() {
        // check for changes to avoid unnecessary save
        if (this.equals(backup)) {
            LCCP.logger.debug("Didn't save " + name + " because nothing changed!");
            return;
        }
        LCCP.logger.debug("Saving " + name + " values to config.yaml...");

        // loading config file
        YAMLConfiguration conf;
        FileHandler fH;
        HashMap<Integer, String> comments;
        try {
            conf = new YAMLConfiguration();
            fH = new FileHandler(conf);
            fH.load(Constants.File_System.config);
            comments = new HashMap<>(CommentPreservation.extractComments(Constants.File_System.config));
        } catch (ConfigurationException e) {
            LCCP.logger.error("Error occurred while writing config values to config.yaml!");
            LCCP.logger.warn("Please restart the application to prevent further errors!");
            return;
        }

        // writing config settings to file
        try {
            conf.setProperty(Constants.Config.WINDOW_RESIZABLE, WindowResizeable);
            conf.setProperty(Constants.Config.WINDOW_DEFAULT_WIDTH, WindowDefWidth);
            conf.setProperty(Constants.Config.WINDOW_DEFAULT_HEIGHT, WindowDefHeight);
            conf.setProperty(Constants.Config.LOG_LEVEL, LogLevel);
            conf.setProperty(Constants.Config.SELECTION_DIR, selectionDir);
            conf.setProperty(Constants.Config.DISPLAY_STATUS_BAR, DisplayStatusBar);
            conf.setProperty(Constants.Config.NETWORK_COMMUNICATION_CLOCK_SPEED, NetworkingCommunicationClockSpeed / 1000);
            conf.setProperty(Constants.Config.CHECK_IPV4, CheckIPv4);
            conf.setProperty(Constants.Config.AUTO_PLAY_AFTER_UPLOAD, AutoPlayAfterUpload);
            // saving settings
            fH.save(Constants.File_System.config);
            CommentPreservation.insertComments(Constants.File_System.config, comments);
        } catch (ConfigurationException e)  {
            LCCP.logger.error("Something went wrong while saving the config values for config.yaml!");
            LCCP.logger.warn("Please restart the application to prevent further errors!");
            LCCP.logger.warn("Previously made changes to the config may be lost!");
            LCCP.logger.warn("If this message appears on every attempt to save config changes please open an issue on GitHub!");
            return;
        } catch (IOException e) {
            LCCP.logger.error("Something went wrong while saving the config comments for config.yaml!");
            LCCP.logger.warn("Please restart the application to prevent further errors!");
            LCCP.logger.warn("Previously made changes to the config may be lost!");
            LCCP.logger.warn("If this message appears on every attempt to save config changes please open an issue on GitHub!");
            return;
        }

        LCCP.logger.debug("Successfully saved server-config values to config.yaml!");
    }

    // creating clone for unnecessary saving check
    @Override
    public void startup() {
        this.backup = new LocalSettings().cloneS();
    }

    // creating a clone of this config class
    @Override
    public LocalSettings cloneS() {
        LocalSettings settings1 = new LocalSettings();
        settings1.copy(LCCP.settings);
        return settings1;
    }

    public void setSelectionDir(String selectionDir) {
        this.selectionDir = selectionDir;
        reload("selectionDir -> " + selectionDir);
    }

    public void setDisplayStatusBar(boolean displayStatusBar) {
        DisplayStatusBar = displayStatusBar;
        reload("DisplayStatusBar -> " + displayStatusBar);
    }

    public void setAutoPlayAfterUpload(boolean autoPlayAfterUpload) {
        AutoPlayAfterUpload = autoPlayAfterUpload;
        reload("AutoPlayAfterUpload -> " + AutoPlayAfterUpload);
    }

    public String getSelectionDir() {
        if (selectionDir.contains("%")) {
            String temp = selectionDir.replaceAll("%",  "");
            try {

                return System.getProperty(temp);
            } catch (Exception e) {
                LCCP.logger.debug("System property placeholder: '" + selectionDir + "'");
                LCCP.logger.debug("System property parsed: '" + temp + "'");
                LCCP.logger.warn("Invalid system property: '" + temp + "'");
                return System.getProperty("user.home");
            }
        }
        return selectionDir;
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
        LocalSettings other = (LocalSettings) obj;
        return WindowResizeable == other.WindowResizeable &&
                WindowDefWidth == other.WindowDefWidth &&
                WindowDefHeight == other.WindowDefHeight &&
                LogLevel == other.LogLevel &&
                //AutoUpdateRemote == other.AutoUpdateRemote &&
                DisplayStatusBar == other.DisplayStatusBar &&
                CheckIPv4 == other.CheckIPv4 &&
                AutoPlayAfterUpload == other.AutoPlayAfterUpload &&
                Objects.equals(selectionDir, other.selectionDir) &&
                Objects.equals(NetworkingCommunicationClockSpeed, other.NetworkingCommunicationClockSpeed);
                //Objects.equals(AutoUpdateRemoteTick, other.AutoUpdateRemoteTick);
    }

    // generate hash code for current settings
    @Override
    public int hashCode() {
        return Objects.hash(WindowResizeable, WindowDefHeight, WindowDefWidth, LogLevel, selectionDir, /*AutoUpdateRemote,*/ DisplayStatusBar, /*AutoUpdateRemoteTick,*/ CheckIPv4, AutoPlayAfterUpload, NetworkingCommunicationClockSpeed);
    }

}
