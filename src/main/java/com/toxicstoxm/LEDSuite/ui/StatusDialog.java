package com.toxicstoxm.LEDSuite.ui;

import com.toxicstoxm.LEDSuite.LEDSuite;
import com.toxicstoxm.LEDSuite.event_handling.EventHandler;
import com.toxicstoxm.LEDSuite.event_handling.Events;
import com.toxicstoxm.LEDSuite.event_handling.listener.EventListener;
import com.toxicstoxm.LEDSuite.task_scheduler.LEDSuiteGuiRunnable;
import com.toxicstoxm.LEDSuite.task_scheduler.LEDSuiteTask;
import com.toxicstoxm.LEDSuite.yaml_factory.wrappers.message_wrappers.StatusUpdate;
import org.gnome.adw.*;
import org.gnome.gtk.Box;
import org.gnome.gtk.Label;
import org.gnome.gtk.Orientation;
import org.gnome.gtk.Widget;

public class StatusDialog extends Dialog implements EventListener {
    private int checksum = 0;
    private ActionRow currentDraw;
    private ActionRow voltage;
    private ActionRow currentFile;
    private ActionRow fileState;
    private ActionRow lidState;
    private StatusPage statusPage;
    private boolean statusInit = false;
    private boolean reboot = false;
    private long lastUpdate;

    private void initialize() {
        currentDraw = ActionRow.builder()
                .setTitle("Current Draw")
                .setSubtitleSelectable(true)
                .setCssClasses(new String[]{"property"})
                .build();
        voltage = ActionRow.builder()
                .setTitle("Voltage")
                .setSubtitleSelectable(true)
                .setCssClasses(new String[]{"property"})
                .build();
        currentFile = ActionRow.builder()
                .setTitle("Current File")
                .setSubtitleSelectable(true)
                .setCssClasses(new String[]{"property"})
                .build();
        fileState = ActionRow.builder()
                .setTitle("Current State")
                .setSubtitleSelectable(true)
                .setCssClasses(new String[]{"property"})
                .build();
        lidState = ActionRow.builder()
                .setTitle("Lid State")
                .setSubtitleSelectable(true)
                .setCssClasses(new String[]{"property"})
                .build();
        statusPage = StatusPage.builder()
                .setIconName("com.toxicstoxm.lccp")
                .setTitle("LED Cube Status")
                .build();

    }

    public StatusDialog() {

        initialize();

        var clamp = Clamp.builder()
                .setMaximumSize(700)
                .setTighteningThreshold(600)
                .setChild(statusPage)
                .build();

        var toolbarView = ToolbarView.builder()
                .setContent(clamp)
                .build();

        var headerBar1 = HeaderBar.builder()
                .setShowTitle(false)
                .setTitleWidget(Label.builder().setLabel("LED Cube Status").build())
                .build();




        toolbarView.addTopBar(headerBar1);

        this.setChild(toolbarView);
        //this.setSearchEnabled(true);

        configure(StatusUpdate.notConnected());
        this.onClosed(() -> {
            if (updateTask != null) {
                LEDSuite.eventManager.unregisterEvents(this);
                updateTask.cancel();
                updateTask = null;
            }
        });

        this.setFollowsContentSize(true);
    }
    private LEDSuiteTask updateTask = null;
    @Override
    public void present(Widget parent) {
        LEDSuite.logger.debug("Fulfilling StatusDialog present request!");
        //checksum = 0;
        updateTask = updateLoop();
        super.present(parent);
    }

    private void initStatus() {
        var statusList = Box.builder()
                .setOrientation(Orientation.VERTICAL)
                .setSpacing(12)
                .setHexpand(true)
                .build();

        var powerUsage = PreferencesGroup.builder()
                .setCssClasses(new String[]{"background"})
                .setTitle("Power")
                .build();

        var fileStats = PreferencesGroup.builder()
                .setCssClasses(new String[]{"background"})
                .setTitle("Animation")
                .build();

        fileStats.add(currentFile);
        fileStats.add(fileState);

        var general = PreferencesGroup.builder()
                .setTitle("General")
                .build();

        general.add(lidState);

        powerUsage.add(voltage);
        powerUsage.add(currentDraw);

        statusList.append(general);
        statusList.append(fileStats);
        statusList.append(powerUsage);

        statusPage.setChild(statusList);
        statusInit = true;
    }

    private void configure(StatusUpdate statusUpdate) {
        if (!statusUpdate.isNotConnected()) lastUpdate = System.currentTimeMillis();
        int checksum = statusUpdate.hashCode();
        if (this.checksum == checksum) {
            LEDSuite.logger.debug("Skipping display request for status update '" + statusUpdate.getNetworkEventID() + "' because checksum '" + checksum + "' didn't change");
            return;
        } else this.checksum = checksum;

        if (statusUpdate.isNotConnected()) {
            if (statusInit) reboot = true;
            var statusList = Box.builder()
                    .setOrientation(Orientation.VERTICAL)
                    .setSpacing(12)
                    .setHexpand(true)
                    .build();

            statusList.append(
                    ActionRow.builder().setTitle("Not connected to Cube!").setSubtitle(LEDSuite.server_settings.getIPv4() + ":" + LEDSuite.server_settings.getPort() + " is currently not responding!").build()
            );
            statusList.append(
                    ActionRow.builder().setTitle("Possible causes").setSubtitle("Waiting for response, Connection failed due to invalid host / port, Connection refused by host").build()
            );

            statusPage.setChild(statusList);
            return;
        }

        if (reboot) {
            this.close();
            StatusDialog dialog = new StatusDialog();
            dialog.configure(statusUpdate);
            dialog.present(LEDSuite.mainWindow);
            return;
        }

        if (!statusInit) {
            initStatus();
        }

        currentDraw.setSubtitle(statusUpdate.getCurrentDraw() + "A");
        voltage.setSubtitle(statusUpdate.getVoltage() + "V");
        if (statusUpdate.isFileLoaded()) {
            currentFile.setSubtitle(statusUpdate.getFileSelected());
            fileState.setSubtitle(statusUpdate.getFileState().name());
        } else {
            currentFile.setSubtitle("N/A");
            fileState.setSubtitle(statusUpdate.getFileState().name());
            fileState.setSubtitle("No animation selected!");
        }

        lidState.setSubtitle(statusUpdate.humanReadableLidState(statusUpdate.isLidState()));
    }

    private void updateStatus(StatusUpdate statusUpdate) {
        configure(statusUpdate);
    }

    private long maxDelay = 10000;

    private LEDSuiteTask updateLoop() {
        LEDSuite.eventManager.registerEvents(this);
        if (!LEDSuite.mainWindow.isBannerVisible()) {
            return new LEDSuiteGuiRunnable() {
                @Override
                public void processGui() {
                    LEDSuite.mainWindow.getStatus(_ -> {});
                    if (System.currentTimeMillis() - lastUpdate >= maxDelay) configure(StatusUpdate.notConnected());
                }
            }.runTaskTimerAsynchronously(0, 1000);
        }
        return new LEDSuiteGuiRunnable() {
            @Override
            public void processGui() {
                LEDSuite.mainWindow.getStatus(_ -> {});
                LEDSuite.logger.debug("Status updater already active. Terminating new instance");
            }
        }.runTask();
    }

    @EventHandler
    public void onStatus(Events.Status e) {
        //LEDSuite.logger.fatal(String.valueOf(e.statusUpdate().isBlank()));
        if (this.getVisible()) updateStatus(e.statusUpdate());
    }
}
