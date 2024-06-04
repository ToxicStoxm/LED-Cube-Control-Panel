package com.x_tornado10.util;

// static utility class for storing paths
public class Paths {
    public static final String appDir = System.getProperty("user.home") + "/.config/LED-Cube-Control-Panel/";
    public static final String config = System.getProperty("user.home") + "/.config/LED-Cube-Control-Panel/config.yaml";
    public static final String server_config = System.getProperty("user.home") + "/.config/LED-Cube-Control-Panel/server_config.yaml";
    public static class Config {
        public static final String DARK_MODE_ENABLED = "Dark-Mode-Enabled";
        public static final String DARK_MODE_COLOR_PRIMARY = "Dark-Mode-Color-Primary";
        public static final String DARK_MODE_COLOR_SECONDARY = "Dark-Mode-Color-Secondary";
        public static final String LIGHT_MODE_COLOR_PRIMARY = "Light-Mode-Color-Primary";
        public static final String LIGHT_MODE_COLOR_SECONDARY = "Light-Mode-Color-Secondary";
        public static final String WINDOW_TITLE = "Window-Title";
        public static final String WINDOW_RESIZABLE = "Window-Resizable";
        public static final String WINDOW_INITIAL_WIDTH = "Window-Initial-Width";
        public static final String WINDOW_INITIAL_HEIGHT = "Window-Initial-Height";
        public static final String WINDOW_SPAWN_CENTER = "Window-Spawn-Center";
        public static final String WINDOW_SPAWN_X = "Window-Spawn-X";
        public static final String WINDOW_SPAWN_Y = "Window-Spawn-Y";
        public static final String STARTUP_FAKE_LOADING_BAR = "Startup-Fake-Loading-Bar";
        public static final String WINDOW_FULL_SCREEN = "Window-Full-screen";
        public static final String WINDOWED_FULL_SCREEN = "Windowed-Full-screen";
        public static final String WINDOW_INITIAL_SCREEN = "Window-Initial-Screen";
        public static final String MOBILE_FRIENDLY = "Mobile-Friendly";
        public static final String MOBILE_FRIENDLY_MODIFIER = "Mobile-Friendly-Modifier";
        public static final String LOG_LEVEL = "Log-Level";
        public static final String SELECTION_DIR = "Default-Selection-Dir";
    }
    public static class Server_Config {
        public static final String BRIGHTNESS = "LED-Brightness";
        public static final String IPV4 = "Server-IP";
        public static final String PORT = "Server-Port";
    }
    public static class Links {
        public static final String Project_GitHub = "https://github.com/ToxicStoxm/LED-Cube-Control-Panel";
    }
    public static class Placeholders {
        public static final String LOOSE_FOCUS = "%LOOSE-FOCUS%";
        public static final String VERSION = "%VERSION%";
    }
}