package com.x_tornado10.lccp.ui;

import com.x_tornado10.lccp.LCCP;
import com.x_tornado10.lccp.event_handling.Events;
import com.x_tornado10.lccp.task_scheduler.LCCPRunnable;
import com.x_tornado10.lccp.task_scheduler.LCCPTask;
import com.x_tornado10.lccp.util.network.Networking;
import org.gnome.adw.*;
import org.gnome.gtk.*;

import java.io.IOException;

// settings dialog / window
public class SettingsDialog extends PreferencesDialog {
    // boolean list to keep track of current user settings
    private final Boolean[] temp;
    // value to keep track of current brightness level
    private double prev1 = 0.0;
    // auto update remote task
    private LCCPTask autoUpdateRemote = null;
    // preferences group server settings
    private PreferencesGroup serverSettings = null;

    // settings dialog constructor
    public SettingsDialog() {
        // configuring settings window appearance
        setTitle("Settings");
        setSearchEnabled(true);

        // setting the default values
        temp = new Boolean[3];
        temp[0] = true;
        temp[1] = LCCP.settings.isDisplayStatusBar();
        temp[2] = LCCP.settings.isAutoUpdateRemote();

        this.onClosed(() -> LCCP.mainWindow.resetSettingsDialog());
    }

    // function to generate a new settings dialog page
    private PreferencesPage get_user_pref_page() {
        // defining new preference page
        var user_pref_page = new PreferencesPage();
        user_pref_page.setTitle(LCCP.version);

        // defining new preferences group for general settings
        var generalSettings = new PreferencesGroup();
        generalSettings.setTitle("General Settings");

        // creating switch row to toggle the status bar
        var statusBarToggle = SwitchRow.builder()
                .setActive(LCCP.mainWindow.isBannerVisible())
                .setTitle("Toggle status bar")
                .setTooltipText("Toggles the small status bar on the main window.")
                .build();
        // enabled the status bar if it's not already activated
        statusBarToggle.getActivatableWidget().onStateFlagsChanged(_ -> {
            boolean active = statusBarToggle.getActive();
            if (!temp[1] == active) {
                LCCP.logger.debug("StatusToggle: " + active);
                // status bar is activated using the set banner visible function from the main window class
                LCCP.mainWindow.setBannerVisible(active);
                temp[1] = active;
            }
        });

        // adding the status bar toggle to the general settings group
        generalSettings.add(statusBarToggle);

        //
        var autoUpdateRemoteToggle = SwitchRow.builder()
                .setActive(LCCP.settings.isAutoUpdateRemote())
                .setTitle("Toggle autoupdate remote")
                .setTooltipText("Toggles autoupdates for the cube settings. Manual / Auto")
                .build();
        autoUpdateRemoteToggle.getActivatableWidget().onStateFlagsChanged(_ -> {
            boolean active = autoUpdateRemoteToggle.getActive();
            if (!temp[2] == active) {
                LCCP.logger.debug("AutoUpdateRemoteToggle: " + active);
                LCCP.settings.setAutoUpdateRemote(active);
                temp[2] = active;
            }
        });
        generalSettings.add(autoUpdateRemoteToggle);

        user_pref_page.add(generalSettings);


        serverSettings = new PreferencesGroup();
        serverSettings.setTitle("Cube Settings");

        var brightnessRow = SpinRow.withRange(0, 100, 1);
        brightnessRow.setValue(LCCP.server_settings.getLED_Brightness() * 100);
        brightnessRow.setSnapToTicks(true);
        brightnessRow.setWrap(false);
        brightnessRow.setClimbRate(2);
        brightnessRow.setNumeric(true);
        brightnessRow.setTitle("LED - Brightness");
        prev1 = brightnessRow.getValue();
        brightnessRow.onOutput(() -> {
            double val = brightnessRow.getValue();
            if (prev1 != val) {
                LCCP.logger.debug(String.valueOf(brightnessRow.getValue()));
                LCCP.server_settings.setLED_Brightness((float) val);
                prev1 = val;
            }
            return false;
        });
        this.setCanClose(true);
        onClosed(this::stopRemoteUpdate);
        serverSettings.add(brightnessRow);

        var spinner = new Spinner();

        var ipv4Row = EntryRow.builder().setTitle("IPv4").build();
        ipv4Row.setShowApplyButton(true);
        ipv4Row.setText(LCCP.server_settings.getIPv4());
        ipv4Row.setEnableUndo(true);
        ipv4Row.onApply(() -> {
            if (!LCCP.settings.isCheckIPv4()) {
                LCCP.server_settings.setIPv4(ipv4Row.getText());
            } else {
                ipv4Row.setEditable(false);
                ipv4Row.addSuffix(spinner);
                spinner.setSpinning(true);
                new LCCPRunnable() {
                    @Override
                    public void run() {

                        String ip;
                        String text = ipv4Row.getText();

                        if (!LCCP.server_settings.getIPv4().equals(text)) {

                            try {
                                ip = Networking.General.getValidIP(text, false);
                            } catch (IOException e) {
                                LCCP.sysBeep();
                                addToast(
                                        Toast.builder()
                                                .setTitle("Server unreachable!")
                                                .setTimeout(10)
                                                .build()
                                );
                                ip = null;
                            }
                            if (ip != null) {
                                LCCP.server_settings.setIPv4(ip);
                                LCCP.eventManager.fireEvent(new Events.HostChanged("Host was changed by user through settings menu!"));
                            }
                        }

                        spinner.setSpinning(false);
                        ipv4Row.remove(spinner);
                        ipv4Row.setEditable(true);
                    }
                }.runTask();
            }
        });
        serverSettings.add(ipv4Row);

        var spinner1 = new Spinner();

        var port = EntryRow.builder().setTitle("Port").build();
        port.setShowApplyButton(true);
        port.setText(String.valueOf(LCCP.server_settings.getPort()));
        port.setEnableUndo(true);
        port.onApply(() -> {
            spinner1.setSpinning(true);
            port.addSuffix(spinner1);
            port.setEditable(false);
            new LCCPRunnable() {
                @Override
                public void run() {
                    String text = port.getText();
                    try {
                        if (Networking.General.isValidPORT(text)) {
                            LCCP.server_settings.setPort(Integer.parseInt(text));
                        } else {
                            throw new NumberFormatException();
                        }
                    } catch (NumberFormatException e) {
                        LCCP.sysBeep();
                        addToast(
                                Toast.builder()
                                        .setTitle("Invalid Port!")
                                        .setTimeout(10)
                                        .build()
                        );
                    }
                    spinner1.setSpinning(false);
                    port.remove(spinner1);
                    port.setEditable(true);
                }
            }.runTask();
        });
        serverSettings.add(port);

        if (!LCCP.settings.isAutoUpdateRemote()) addManualRemoteApplySwitch();

        user_pref_page.add(serverSettings);
        return user_pref_page;
    }

    private ActionRow manualRemoteApplySwitchRow = null;
    private ActionRow getManualRemoteApplySwitchRow() {
        if (manualRemoteApplySwitchRow == null) {
            manualRemoteApplySwitchRow = new ActionRow();
            manualRemoteApplySwitchRow.setTitle("Apply");

            var applyButton = new Button();
            applyButton.setSizeRequest(40, 40);
            applyButton.setLabel("Apply");
            applyButton.setIconName("emblem-synchronizing-symbolic");
            manualRemoteApplySwitchRow.addSuffix(
                    Clamp.builder()
                            .setMaximumSize(40)
                            .setTighteningThreshold(40)
                            .setOrientation(Orientation.VERTICAL)
                            .setChild(applyButton)
                            .build()
            );
            manualRemoteApplySwitchRow.setActivatableWidget(applyButton);
            applyButton.onClicked(() -> {
                LCCP.logger.debug("Apply remote request through manual button press");
                LCCP.updateRemoteConfig();
            });
        }
        return manualRemoteApplySwitchRow;
    }
    public void addManualRemoteApplySwitch() {
        if (serverSettings != null) serverSettings.add(getManualRemoteApplySwitchRow());
    }

    public void removeManualRemoteApplySwitch() {
        if (serverSettings != null) serverSettings.remove(getManualRemoteApplySwitchRow());
    }

    @Override
    public void present(Widget parent) {
        LCCP.logger.debug("Fulfilling SettingsDialog present request!");
        if (LCCP.settings.isAutoUpdateRemote()) startRemoteUpdate();
        if (temp[0]) {
            add(get_user_pref_page());
            temp[0] = false;
        }
        super.present(parent);
    }

    public void startRemoteUpdate() {
        autoUpdateRemote = new LCCPRunnable() {
            @Override
            public void run() {
                LCCP.updateRemoteConfig();
            }
        }.runTaskTimerAsynchronously(0, Math.round(LCCP.settings.getAutoUpdateRemoteTick() * 1000));
        LCCP.logger.debug("Started autoRemoteUpdateTask!");
    }
    public void stopRemoteUpdate() {
        if (autoUpdateRemote != null) {
            autoUpdateRemote.cancel();
            LCCP.logger.debug("Stopped autoRemoteUpdateTask " + autoUpdateRemote + " !");
        }
    }
}
