package com.toxicstoxm.LEDSuite;

import com.toxicstoxm.LEDSuite.communication.network.Networking;
import com.toxicstoxm.LEDSuite.event_handling.EventHandler;
import com.toxicstoxm.LEDSuite.event_handling.EventManager;
import com.toxicstoxm.LEDSuite.event_handling.Events;
import com.toxicstoxm.LEDSuite.event_handling.listener.EventListener;
import com.toxicstoxm.LEDSuite.logging.Logger;
import com.toxicstoxm.LEDSuite.logging.network.NetworkLogger;
import com.toxicstoxm.LEDSuite.settings.LocalSettings;
import com.toxicstoxm.LEDSuite.settings.ServerSettings;
import com.toxicstoxm.LEDSuite.task_scheduler.LEDSuiteGuiRunnable;
import com.toxicstoxm.LEDSuite.task_scheduler.LEDSuiteScheduler;
import com.toxicstoxm.LEDSuite.time.TickingSystem;
import com.toxicstoxm.LEDSuite.time.TimeManager;
import com.toxicstoxm.LEDSuite.ui.Window;
import com.toxicstoxm.LEDSuite.yaml_factory.YAMLMessage;
import com.toxicstoxm.LEDSuite.yaml_factory.YAMLSerializer;
import com.toxicstoxm.LEDSuite.yaml_factory.wrappers.message_wrappers.ServerError;
import com.toxicstoxm.LEDSuite.yaml_factory.wrappers.message_wrappers.StatusUpdate;
import lombok.Getter;
import org.apache.commons.configuration2.YAMLConfiguration;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.configuration2.io.FileHandler;
import org.gnome.adw.Adw;
import org.gnome.adw.Application;
import org.gnome.adw.Toast;
import org.gnome.gio.ApplicationFlags;
import picocli.CommandLine;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.*;

import static java.awt.Toolkit.getDefaultToolkit;

@CommandLine.Command(name = "LEDSuite", mixinStandardHelpOptions = true,
        description = "Simple front end application that lets you control decorative matrix's.")
public class LEDSuite implements EventListener, Runnable {

    @Getter
    private static LEDSuite instance;
    @Getter
    private final Application app;
    public static LocalSettings settings;
    public static LocalSettings argumentsSettings;
    public static ServerSettings server_settings;
    public static Logger logger;
    public static NetworkLogger networkLogger;
    private static long start;
    public static EventManager eventManager;
    public static Window mainWindow;
    public static LEDSuiteScheduler ledSuiteScheduler;
    public static TickingSystem tickingSystem;

    @CommandLine.Option(names = {"-l", "--log-level"}, description = "Change the log level for the current session.")
    private static Logger.log_level logLevel = null;
    @CommandLine.Option(names = {"-L", "--set-log-level"}, description = "Permanently change the log level.")
    private static Logger.log_level LogLevel = null;

    @CommandLine.Option(names = {"-ww", "--window-width"}, description = "Change the initial window width for the current session.")
    private static int windowDefWidth = -1;
    @CommandLine.Option(names = {"-Ww", "--set-window-width"}, description = "Permanently change the initial window width.")
    private static int WindowDefWidth = -1;

    @CommandLine.Option(names = {"-wh", "--window-height"}, description = "Change the initial window height for the current session.")
    private static int windowDefHeight = -1;
    @CommandLine.Option(names = {"-Wh", "--set-window-height"}, description = "Permanently change the initial window height.")
    private static int WindowDefHeight = -1;

    @CommandLine.Option(names = {"-n", "--networking-clock"}, description = "Change the networking clock for the current session.")
    private static int networkingCommunicationClockSpeed = -1;
    @CommandLine.Option(names = {"-N", "--set-networking-clock"}, description = "Permanently change the networking clock.")
    private static int NetworkingCommunicationClockSpeed = -1;

    @CommandLine.Option(names = {"-R", "--reset-config"}, description = "Reset configuration values to default! Type true to confirm!")
    private static boolean resetConfig;

    @CommandLine.Option(names = {"-p", "--get-paths"}, description = "Displays all important paths.")
    private static boolean paths;

    // main method
    public static void main(String[] args) {

        // create timestamp that is used to calculate starting time
        start = System.currentTimeMillis();

        // initialize picocli and parse the arguments
        CommandLine cmd = new CommandLine(new LEDSuite());
        cmd.getCommandSpec().version(Constants.Application.VERSION_DESC);
        try {
            cmd.parseArgs(args);
        } catch (CommandLine.ParameterException _) {
        }

        // processes commandline arguments and run application
        int statusCode = cmd.execute(args);
        System.exit(statusCode);
    }

    // constructor method
    public LEDSuite() {
        instance = this;
        // create new libadwaita application object
        app = new Application(Constants.Application.DOMAIN, ApplicationFlags.DEFAULT_FLAGS);
        app.setVersion(Constants.Application.VERSION);
        app.setApplicationId(Constants.Application.DOMAIN);
        // define function to be executed on application start
        app.onActivate(this::activate);
        // trigger exit() function
        app.onShutdown(() -> exit(0));
    }

    @Override
    public void run() {
        // initialize config, logger, ...
        logicInit();
        // starts application
        app.run(new String[]{});
    }

    // logic initialization function
    public static void logicInit() {

        //ResourceBundle bundle = ResourceBundle.getBundle("LEDSuite", Locale.getDefault());
        //System.out.println(bundle.getString("test"));

        // program initialization
        // create new settings and server_settings classes to hold config settings
        settings = new LocalSettings();
        argumentsSettings = new LocalSettings();
        server_settings = new ServerSettings();
        // create new logger instance
        logger = new Logger();
        // create new networkLogger instance
        networkLogger = new NetworkLogger();

        // defining config files and log file
        File config_file = new File(Constants.File_System.config);
        File server_config_file = new File(Constants.File_System.server_config);
        File log_file = new File(Constants.File_System.logFile);
        try {
            if (resetConfig) {
                Scanner scanner = new Scanner(System.in);
                String confirmString = "confirm reset";
                String input;

                logger.log("Type '" + confirmString + "' to reset configuration: ", false);
                input = scanner.nextLine();

                if (input.equals(confirmString)) {
                    if (config_file.delete() || server_config_file.delete() || log_file.delete()) {
                        logger.log("Config has been reset successfully! Starting application...");
                    }
                } else {
                    logger.log("Received wrong confirmation string '" + input + "'. Cancelled config reset!");
                    System.exit(0);
                }
            }

            // checking if the config file already exists
            if (!config_file.exists()) {
                // if the config file doesn't exist the program tries to create the parent directory first to prevent errors if it's the first startup
                if (config_file.getParentFile().mkdirs())
                    logger.verbose("Successfully created parent directory for config file: " + config_file.getParentFile().getAbsolutePath());
                // then a new config file is loaded with the default values from internal resources folder using the configs saveDefaultConfig() function
                if (config_file.createNewFile()) {
                    logger.debug("New config file was successfully created: " + config_file.getAbsolutePath());
                    settings.saveDefaultConfig();
                } else {
                    // if the config can't be created for some reason, a waring message is displayed in the console
                    logger.warn("Config couldn't be created!");
                    logger.warn("Please restart the application to prevent wierd behaviour!");
                }
            }
            // checking if the server side config file already exists
            if (!server_config_file.exists()) {
                // if the config file doesn't exist, a new one gets loaded from internal resources folder with its default values using the configs saveDefaultConfig() function
                if (server_config_file.createNewFile()) {
                    logger.debug("New server config file was successfully created: " + server_config_file.getAbsolutePath());
                    server_settings.saveDefaultConfig();
                } else {
                    // if the config can't be created for some reason, a waring message is displayed in the console
                    logger.warn("Server config couldn't be created!");
                    logger.warn("Please restart the application to prevent wierd behaviour!");
                }
            }
            // check if log file already exists
            if (!log_file.exists()) {
                Files.createDirectories(Path.of(log_file.getParent()));
                // if it does not exist, a new one will be created
                if (log_file.createNewFile()) {
                    logger.debug("New log config file was successfully created: " + log_file.getAbsolutePath());
                } else {
                    // if there are any exceptions during the creation of a new log file, a warning message is displayed in the console
                    logger.warn("Log file couldn't be created!");
                    logger.warn("Please restart the application to prevent wierd behaviour!");
                }
            } else {
                // if the log file already exists, its contents are erased using a file writer
                // the file writer is configured to start writing at the beginning of the file
                try (FileWriter writer = new FileWriter(log_file, false)) {
                    // the file writer then writes an empty string to the file
                    // this erases the other file contents
                    writer.write("");
                    logger.debug("Existing log file contents erased: " + log_file.getAbsolutePath());
                }
            }
        } catch (IOException | NullPointerException e) {
            // if any exceptions occur during file creation / modification an error is displayed in the console
            // additionally the program is halted to prevent any further issues or unexpected behavior
            logger.error("Settings failed to load!");
            logger.warn("Application was halted!");
            logger.warn("If this keeps happening please open an issue on GitHub!");
            logger.warn("Please restart the application!");
            exit(1);
            return;
        }

        // user settings are loaded from config files
        loadConfigsFromFile();

        // creating new event manager
        eventManager = new EventManager();
        // registering event listeners for settings classes
        eventManager.registerEvents(settings);
        eventManager.registerEvents(server_settings);

        // initializing task scheduler with a max core pool size of 5
        // this means it can at most run 5 different tasks at the same time
        ledSuiteScheduler = new LEDSuiteScheduler();
        tickingSystem = new TickingSystem();

        TimeManager.initTimeTracker("status", 5000, 10000);
        TimeManager.initTimeTracker("animations", 1000, System.currentTimeMillis() - 10000);

        argumentsSettings.copyImpl(settings, false);
        if (LogLevel != null) {
            argumentsSettings.setLogLevel(LogLevel.getValue());
            settings.setLogLevel(argumentsSettings.getLogLevel());
        } else if (logLevel != null) argumentsSettings.setLogLevel(logLevel.getValue());

        if (WindowDefWidth > 0) {
            argumentsSettings.setWindowDefWidth(WindowDefWidth);
            settings.setWindowDefWidth(argumentsSettings.getWindowDefWidth());
        } else if (windowDefWidth > 0) argumentsSettings.setWindowDefWidth(windowDefWidth);

        if (WindowDefHeight > 0) {
            argumentsSettings.setWindowDefHeight(WindowDefHeight);
            settings.setWindowDefHeight(argumentsSettings.getWindowDefHeight());
        } else if (windowDefHeight > 0) argumentsSettings.setWindowDefHeight(windowDefHeight);

        if (NetworkingCommunicationClockSpeed > -1) {
            argumentsSettings.setNetworkingCommunicationClockSpeed(NetworkingCommunicationClockSpeed);
            settings.setNetworkingCommunicationClockSpeed(argumentsSettings.getNetworkingCommunicationClockSpeed());
        } else if (networkingCommunicationClockSpeed > -1) argumentsSettings.setNetworkingCommunicationClockSpeed(networkingCommunicationClockSpeed);

        if (paths) {
            logger.log("Paths:");
            logger.log(" -> Directories");
            logger.log("    App directory: '" + Constants.File_System.getAppDir() + "'");
            logger.log("    Temp directory: '" + Constants.File_System.getTmpDir() + "'");
            logger.log("    Data directory: '" + Constants.File_System.getDataDir() + "'");
            logger.log(" -> Files");
            logger.log("    Configuration file: '" + Constants.File_System.config + "'");
            logger.log("    Server configuration file: '" + Constants.File_System.server_config + "'");
            logger.log("    Log file: '" + Constants.File_System.logFile + "'");

            System.exit(0);
        }

        // general startup information displayed in the console upon starting the program
        logger.info("Welcome back!");
        logger.info("Starting Program...");

        logger.info("System environment: " + Constants.System.NAME + " " + Constants.System.VERSION);

        // check for window os
        // app does not normally work on windows, since windows doesn't natively support libadwaita
        if (Constants.System.NAME.toLowerCase().contains("windows")) {
            logger.warn("Our application does not have official Windows support. We do not fix any windows only bugs!");
            logger.warn("You will be ignored if you open an issue for a windows only bug! You can fork the repo though and fix the bug yourself!");
        }

        try {
            Networking.Communication.NetworkHandler.init(_ -> {
            });
        } catch (Networking.NetworkException e) {
            throw new RuntimeException(e);
        }

        new LEDSuiteGuiRunnable() {
            @Override
            public void processGui() {
                if (!Networking.Communication.NetworkHandler.connectedAndRunning() && !TimeManager.alternativeCall("status")) {
                    try {
                        Networking.Communication.sendYAMLDefaultHost(
                                YAMLMessage.defaultStatusRequest().build()
                        );
                    } catch (ConfigurationException | YAMLSerializer.InvalidReplyTypeException |
                             YAMLSerializer.InvalidPacketTypeException | YAMLSerializer.TODOException e) {
                        LEDSuite.logger.verbose("Auto status request attempt failed!");
                    }
                }
            }
        }.runTaskTimerAsynchronously(10000, 5000);

        SimpleDateFormat df = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
        eventManager.fireEvent(new Events.Startup("Starting application! Current date and time: " + df.format(new Date())));
    }

    // activate function
    // this is triggered on libadwaita application activate
    public void activate() {
        // registering event listener for this class
        eventManager.registerEvents(this);
        // creating main window of the application
        mainWindow = new Window(app);
        // registering event listener for the main window
        eventManager.registerEvents(mainWindow);
        // showing the main window on screen
        mainWindow.present();
        // trigger started to send started message to console
        // calculating time elapsed during startup and displaying it in the console
        long timeElapsed = System.currentTimeMillis() - start;
        eventManager.fireEvent(new Events.Started("Successfully started program! (took " + timeElapsed / 1000 + "." + timeElapsed % 1000 + "s)"));
    }

    // display start message with starting duration
    public static void started(String message) {
        logger.info(message);
        try {
            Networking.Communication.sendYAMLDefaultHost(YAMLMessage.defaultStatusRequest().build());
        } catch (ConfigurationException | YAMLSerializer.YAMLException e) {
            LEDSuite.logger.error("Failed to send / get available animations list from the server!");
            LEDSuite.logger.error(e);
        }
    }

    // exiting program with specified status code
    public static void exit(int status) {
        // firing new shutdown event
        if (eventManager != null) eventManager.fireEvent(new Events.Shutdown("Shutdown"));
        if (logger != null) LEDSuite.logger.info("Saving...");
        // firing new save event to save user settings
        if (eventManager != null) eventManager.fireEvent(new Events.Save("Shutdown - Save"));
        if (logger != null) LEDSuite.logger.debug("Stopping ticking system!");
        if (tickingSystem != null) tickingSystem.stop();
        if (logger != null) LEDSuite.logger.info("Successfully saved!");
        if (logger != null) LEDSuite.logger.info("Shutting down...");
        if (logger != null) LEDSuite.logger.info("Goodbye!");
        // displaying status code in the console
        if (logger != null) LEDSuite.logger.info("Status code: " + status);
        // exiting program with the specified status code
        //System.exit(status);
    }
    // triggering system specific beep using java.awt.toolkit
    // commonly used when something fails or an error happens
    public static void sysBeep() {
        LEDSuite.logger.verbose("Triggered system beep!");
        getDefaultToolkit().beep();
    }

    // function used to load user settings from config files
    public static void loadConfigsFromFile() {
        // parsing config and loading the values from storage (Default: ./LED-Cube-Control-Panel/config.yaml)
        // using Apache-Commons-Configuration2 and SnakeYaml
        try {
            // defining new YamlConfig object from apache-commons-configuration2 lib
            YAMLConfiguration yamlConfig = new YAMLConfiguration();

            // Loading the YAML file from disk using a file handler
            FileHandler fileHandler = new FileHandler(yamlConfig);
            fileHandler.load(Constants.File_System.config);

            // settings are loaded into the current instance of the settings class, so they can be used during runtime without any IO-Calls
            settings.load(yamlConfig);
        } catch (ConfigurationException e) {
            // if any errors occur during config parsing an error is displayed in the console
            // the program is halted to prevent any further unwanted behavior
            LEDSuite.logger.error("Failed to parse config.yaml!");
            LEDSuite.logger.warn("Application was halted!");
            LEDSuite.logger.warn("If this keeps happening please open an issue on GitHub!");
            LEDSuite.logger.warn("Please restart the application!");
            LEDSuite.exit(1);
            return;
        }

        try {
            // defining new YamlConfig object from apache-commons-configuration2 lib
            YAMLConfiguration yamlConfig = new YAMLConfiguration();

            // Load the YAML file from disk using file manager
            FileHandler fileHandler = new FileHandler(yamlConfig);
            fileHandler.load(Constants.File_System.server_config);

            // settings are loaded into the current instance of the settings class, so they can be used during runtime without any IO-Calls
            server_settings.load(yamlConfig);
        } catch (ConfigurationException e) {
            // if any errors occur during config parsing an error is displayed in the console
            // the program is halted to prevent any further unwanted behavior
            LEDSuite.logger.error("Failed to parse server_config.yaml!");
            LEDSuite.logger.warn("Application was halted!");
            LEDSuite.logger.warn("If this keeps happening please open an issue on GitHub!");
            LEDSuite.logger.warn("Please restart the application!");
            LEDSuite.exit(1);
        }
    }

    public static LEDSuiteScheduler getScheduler() {
        return ledSuiteScheduler;
    }

    // listener function for reload event
    @EventHandler
    public void onReload(Events.Reload e) {
        // default console message response to a reload event
        logger.verbose("Fulfilling reload request: " + e.message());
        // reloading values that may've changed
        mainWindow.setResizable(settings.isWindowResizeable());
    }
    // listener function for startup event
    @EventHandler
    public void onStartup(Events.Startup e) {
        // default console message response to a startup event
        logger.verbose("Fulfilling startup request: " + e.message());
    }
    @EventHandler
    public void onStarted(Events.Started e) {
        started(e.message());
    }
    // listener function for save event
    @EventHandler
    public void onSave(Events.Save e) {
        // default console message response to a save event
        logger.verbose("Fulfilling save request: " + e.message());
    }
    // listener function for shutdown event
    @EventHandler
    public void onShutdown(Events.Shutdown e) {
        // default console message response to a shutdown event
        logger.verbose("Fulfilling shutdown request: " + e.message());
        //server = false;
        networkLogger.printEvents();
        logger.info("New log file was saved to: '" + Constants.File_System.logFile + "'");
    }
    @EventHandler
    public void onDataReceived(Events.DataIn e) {
        YAMLMessage yaml = e.yamlMessage();
        String id = "[" + yaml.getNetworkID() + "] ";
        logger.verbose(id + "-------------------- Internal Data Event ----------------------");
        logger.verbose(id + "Data stream direction: in");
        logger.verbose(id + "Network: Received data!");
        logger.verbose(id + "Data: " + yaml);

        switch (yaml.getPacketType()) {
            case reply -> {
                switch (yaml.getReplyType()) {
                    case status -> eventManager.fireEvent(new Events.Status(StatusUpdate.fromYAMLMessage(yaml)));
                    case menu -> LEDSuite.logger.fatal("Redundancy catcher caught a menu reply while not expecting it!");
                }

            }
            case error -> {
                eventManager.fireEvent(
                        new Events.Error(
                                ServerError.fromYAMLMessage(yaml)
                        )
                );
            }
        }

        logger.debug(id + "---------------------------------------------------------------");
    }
    @EventHandler
    public void onDataSend(Events.DataOut e) {
        YAMLConfiguration yaml = e.yaml();
        String id;
        try {
            id = "[" + yaml.getProperty(Constants.Network.YAML.INTERNAL_NETWORK_ID) + "] ";
            logger.verbose(id + "-------------------- Internal Data Event ----------------------");
        } catch (NoSuchElementException ex) {
            id = "[failed to get id] ";
            logger.verbose(id + "-------------------- Internal Data Event ----------------------");
            logger.error(id + "Failed to get internal network event id from YAML!");
            logger.error(id + "Error message: " + ex.getMessage());
        }
        logger.debug(id + "Data stream direction: out");
        try {
            logger.verbose(id + "Data: " + YAMLSerializer.deserializeYAML(e.yaml()));
        } catch (YAMLSerializer.YAMLException ex) {
            logger.warn(id + "Data: failed to deserialize yaml data");
            logger.warn(id + "Error message: " + ex.getMessage());
        }
        logger.verbose(id + "---------------------------------------------------------------");
    }
    @EventHandler
    public void onStatus(Events.Status e) {
        StatusUpdate status = e.statusUpdate();
        TimeManager.ping("status");
        String id = "[" + status.getNetworkEventID() + "] ";
        logger.debug(id + "Received status update from server!");
        logger.debug(id + "Status: " + status);
    }
    @EventHandler
    public void onSend(Events.DataOut e) {
        YAMLConfiguration yaml = e.yaml();
        String id;
        try {
            id = "[" + yaml.getProperty(Constants.Network.YAML.INTERNAL_NETWORK_ID) + "] ";
            logger.verbose(id + "-------------------- Internal Data Event ----------------------");
        } catch (NoSuchElementException ex) {
            id = "[failed to get id] ";
            logger.verbose(id + "-------------------- Internal Data Event ----------------------");
            logger.error(id + "Failed to get internal network event id from YAML!");
            logger.error(id + "Error message: " + ex.getMessage());
        }

    }
    @EventHandler
    public void onError(Events.Error e) {
        ServerError error = e.serverError();
        String id = "[" + error.getNetworkEventID() + "] ";
        logger.debug(id + "Received error from server!");
        logger.debug("id" + "Error: " + error);
        sysBeep();
        mainWindow.toastOverlay.addToast(
                Toast.builder()
                        .setTitle(error.humanReadable())
                        .setTimeout(Adw.DURATION_INFINITE)
                        .build()
        );
    }
}
