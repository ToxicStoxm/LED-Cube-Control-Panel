package com.toxicstoxm.LEDSuite.ui;

import com.toxicstoxm.LEDSuite.Constants;
import com.toxicstoxm.LEDSuite.LEDSuite;
import com.toxicstoxm.LEDSuite.communication.network.Networking;
import com.toxicstoxm.LEDSuite.event_handling.Events;
import com.toxicstoxm.LEDSuite.task_scheduler.LEDSuiteGuiRunnable;
import org.gnome.adw.*;
import org.gnome.gtk.Spinner;
import org.gnome.gtk.Widget;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Represents the settings dialog for the LEDSuite application.
 * This dialog allows users to configure various application settings,
 * including general settings and server settings.
 *
 * @since 1.0.0
 */
public class SettingsDialog extends PreferencesDialog {
    // Boolean list to keep track of current user settings
    private final Boolean[] temp;
    // Value to keep track of the current brightness level
    private double prev1 = 0.0;
    // Preferences group for server settings
    private PreferencesGroup serverSettings = null;

    /**
     * Constructs a new SettingsDialog.
     * Configures the appearance and initializes default values.
     *
     * @since 1.0.0
     */
    public SettingsDialog() {
        // Configuring settings window appearance
        setTitle("Settings");
        setSearchEnabled(true);

        // Setting the default values
        temp = new Boolean[3];
        temp[0] = true;
        temp[1] = LEDSuite.settings.isDisplayStatusBar();

        // Handle dialog closure
        this.onClosed(() -> LEDSuite.mainWindow.resetSettingsDialog());
    }

    /**
     * Generates a new settings dialog page with user preferences and server settings.
     *
     * @return A PreferencesPage containing user and server settings.
     * @since 1.0.0
     */
    private PreferencesPage get_user_pref_page() {
        // Define new preference page
        var user_pref_page = new PreferencesPage();
        // Uncomment to set title with application version
        // user_pref_page.setTitle(Constants.Application.VERSION);

        // Define a new preferences group for general settings
        var generalSettings = new PreferencesGroup();
        generalSettings.setTitle("General Settings");

        // Create a switch row to toggle the status bar
        var statusBarToggle = SwitchRow.builder()
                .setActive(LEDSuite.mainWindow.isBannerVisible())
                .setTitle("Status Bar")
                .setTooltipText("Toggles the small status bar on the main window.")
                .build();

        // Handle state change of status bar toggle
        statusBarToggle.getActivatableWidget().onStateFlagsChanged(_ -> {
            boolean active = statusBarToggle.getActive();
            if (!temp[1] == active) {
                LEDSuite.logger.debug("StatusToggle: " + active);
                // Set the banner visibility based on the toggle
                LEDSuite.mainWindow.setBannerVisible(active);
                temp[1] = active;
            }
        });

        // Add status bar toggle to general settings group
        generalSettings.add(statusBarToggle);
        user_pref_page.add(generalSettings);

        // Define preferences group for server settings
        serverSettings = new PreferencesGroup();
        serverSettings.setTitle("Cube Settings");

        // Create a spin row for LED brightness adjustment
        var brightnessRow = SpinRow.withRange(0, 100, 1);
        brightnessRow.setValue(LEDSuite.server_settings.getLED_Brightness() * 100);
        brightnessRow.setSnapToTicks(true);
        brightnessRow.setWrap(false);
        brightnessRow.setClimbRate(2);
        brightnessRow.setNumeric(true);
        brightnessRow.setTitle("LED - Brightness");
        prev1 = brightnessRow.getValue();

        // Handle brightness value change
        brightnessRow.onOutput(() -> {
            double val = brightnessRow.getValue();
            if (prev1 != val) {
                float newValue = (float) val;
                LEDSuite.logger.debug("Brightness changed: -> " + brightnessRow.getValue());
                LEDSuite.server_settings.setLED_Brightness(newValue);
                LEDSuite.eventManager.fireEvent(new Events.SettingChanged(Constants.Server_Config.BRIGHTNESS, newValue));
                prev1 = val;
            }
            return false;
        });
        this.setCanClose(true);
        serverSettings.add(brightnessRow);

        // Create spinner for async operations
        var spinner = new Spinner();

        // Create an entry row for IPv4 address
        var ipv4Row = EntryRow.builder().setTitle("IPv4").build();
        ipv4Row.setShowApplyButton(true);
        ipv4Row.setText(LEDSuite.server_settings.getIPv4());
        ipv4Row.setEnableUndo(true);
        AtomicReference<String> prevIPv4 = new AtomicReference<>(LEDSuite.server_settings.getIPv4());

        // Handle IPv4 address application
        ipv4Row.onApply(() -> {
            if (!LEDSuite.settings.isCheckIPv4()) {
                LEDSuite.server_settings.setIPv4(ipv4Row.getText());
            } else {
                ipv4Row.setEditable(false);
                ipv4Row.addSuffix(spinner);
                spinner.setSpinning(true);
                new LEDSuiteGuiRunnable() {
                    @Override
                    public void processGui() {

                        String text = ipv4Row.getText();

                        if (!LEDSuite.server_settings.getIPv4().equals(text)) {

                            Networking.Validation.getValidIP(text, false, result -> {
                                if (result == null) {
                                    LEDSuite.sysBeep();
                                    addToast(
                                            Toast.builder()
                                                    .setTitle("Server unreachable!")
                                                    .setTimeout(10)
                                                    .build()
                                    );
                                } else {
                                    try {
                                        LEDSuite.server_settings.setIPv4(result);
                                        Networking.Communication.NetworkHandler.hostChanged();
                                        prevIPv4.set(result);
                                    } catch (Networking.NetworkException e) {
                                        LEDSuite.server_settings.setIPv4(prevIPv4.get());
                                        try {
                                            Networking.Communication.NetworkHandler.hostChanged();
                                            addToast(
                                                    Toast.builder()
                                                            .setTitle("Connection failed! Reconnected to previous host: '" + prevIPv4.get() + "'")
                                                            .setTimeout(10)
                                                            .build()
                                            );
                                        } catch (Networking.NetworkException ex) {
                                            LEDSuite.logger.error("Fallback connection failed! Stopping network communication!");
                                            Networking.Communication.NetworkHandler.cancel();
                                        }
                                    }
                                }
                                spinner.setSpinning(false);
                                ipv4Row.remove(spinner);
                                ipv4Row.setEditable(true);
                            });
                        }
                    }
                }.runTask();
            }
        });
        serverSettings.add(ipv4Row);

        // Create another spinner for port operations
        var spinner1 = new Spinner();

        // Create an entry row for port number
        var port = EntryRow.builder().setTitle("Port").build();
        port.setShowApplyButton(true);
        port.setText(String.valueOf(LEDSuite.server_settings.getPort()));
        port.setEnableUndo(true);
        AtomicReference<String> prevPort = new AtomicReference<>(port.getText());

        // Handle port number application
        port.onApply(() -> {
            spinner1.setSpinning(true);
            port.addSuffix(spinner1);
            port.setEditable(false);
            new LEDSuiteGuiRunnable() {
                @Override
                public void processGui() {
                    String portVal = port.getText();
                    try {
                        int port = Integer.parseInt(portVal);
                        if (LEDSuite.server_settings.getPort() != port) {
                            if (Networking.Validation.isValidPORT(portVal)) {
                                LEDSuite.server_settings.setPort(port);
                                Networking.Communication.NetworkHandler.hostChanged();
                                prevPort.set(portVal);
                            } else {
                                throw new NumberFormatException();
                            }
                        }
                    } catch (NumberFormatException | Networking.NetworkException e) {
                        LEDSuite.sysBeep();
                        addToast(
                                Toast.builder()
                                        .setTitle("Invalid Port!")
                                        .setTimeout(10)
                                        .build()
                        );
                        try {
                            LEDSuite.server_settings.setPort(Integer.parseInt(prevPort.get()));
                            Networking.Communication.NetworkHandler.hostChanged();
                        } catch (NumberFormatException | Networking.NetworkException ex) {
                            LEDSuite.logger.error("Fallback connection failed! Stopping network communication!");
                            Networking.Communication.NetworkHandler.cancel();
                        }
                    } finally {
                        spinner1.setSpinning(false);
                        port.remove(spinner1);
                        port.setEditable(true);
                    }
                }
            }.runTask();
        });
        serverSettings.add(port);

        // Add server settings to the preferences page
        user_pref_page.add(serverSettings);
        return user_pref_page;
    }

    /**
     * Presents the settings dialog to the user.
     * If this is the first time presenting, the user preferences page is created.
     *
     * @param parent The parent widget to which the dialog is attached.
     * @since 1.0.0
     */
    @Override
    public void present(Widget parent) {
        LEDSuite.logger.debug("Fulfilling SettingsDialog present request!");
        // Uncomment to start remote update if auto-update is enabled
        // if (LEDSuite.settings.isAutoUpdateRemote()) startRemoteUpdate();
        if (temp[0]) {
            add(get_user_pref_page());
            temp[0] = false;
        }
        super.present(parent);
    }
}
