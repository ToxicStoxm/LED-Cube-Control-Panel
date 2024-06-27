package com.x_tornado10.lccp.ui;

import com.x_tornado10.lccp.LCCP;
import com.x_tornado10.lccp.event_handling.EventHandler;
import com.x_tornado10.lccp.event_handling.Events;
import com.x_tornado10.lccp.event_handling.listener.EventListener;
import com.x_tornado10.lccp.task_scheduler.LCCPProcessor;
import com.x_tornado10.lccp.task_scheduler.LCCPRunnable;
import com.x_tornado10.lccp.task_scheduler.LCCPTask;
import com.x_tornado10.lccp.util.network.Networking;
import com.x_tornado10.lccp.util.Paths;
import com.x_tornado10.lccp.yaml_factory.message_wrappers.StatusUpdate;
import com.x_tornado10.lccp.yaml_factory.YAMLSerializer;
import com.x_tornado10.lccp.yaml_factory.YAMLMessage;
import org.apache.commons.configuration2.YAMLConfiguration;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.gnome.adw.AboutDialog;
import org.gnome.adw.Application;
import org.gnome.adw.ApplicationWindow;
import org.gnome.adw.HeaderBar;
import org.gnome.adw.*;
import org.gnome.gio.SimpleAction;
import org.gnome.gio.SimpleActionGroup;
import org.gnome.gtk.*;
import org.gnome.pango.AttrList;
import org.gnome.pango.EllipsizeMode;
import org.gnome.pango.Pango;

import java.io.InputStream;
import java.util.*;

// main application window
public class Window extends ApplicationWindow implements EventListener {
    // status banner and toast overlay used in the main window
    // made public to enable global toggling
    public Banner status = new Banner("");
    public ToastOverlay toastOverlay = null;
    private ListBox animationsList = null;
    private ListBox addFileList = null;
    private Box addFile = null;
    public ToolbarView rootView = null;
    public ProgressBar progressBar = null;
    private Revealer SidebarSpinner = null;

    // booleans to keep track of autoUpdate and statusBarEnabled settings
    private boolean statusBarCurrentState = false;
    private boolean autoUpdate = false;
    private boolean sideBarVisible = true;

    // constructor for the main window
    public Window(Application app) {
        super(app);
        // setting title and default size
        this.setTitle(LCCP.settings.getWindowTitle());
        this.setDefaultSize(LCCP.settings.getWindowDefWidth(), LCCP.settings.getWindowDefHeight());
        this.setIconName("LCCP-logo-256x256");

        // settings auto update to user specified value
        setAutoUpdate(LCCP.settings.isAutoUpdateRemote());

        // toast overlay used to display toasts (notification) to the user
        toastOverlay = new ToastOverlay();

        // the applications header bar
        var headerBar = new HeaderBar();

        // create search button and configure it
        var sbutton = new ToggleButton();
        // setting the icon name to gnome icon name
        sbutton.setIconName("system-search-symbolic");
        // executed when th button is toggled
        sbutton.onToggled(() -> {
            // display work in progress toast as the search function is not yet implemented
            var wipToast = new Toast("Work in progress!");
            wipToast.setTimeout(1);
            toastOverlay.addToast(wipToast);
        });

        // creating and configuring menu button
        var mbutton = new MenuButton();
        mbutton.setAlwaysShowArrow(false);
        // setting the icon name to gnome icon name
        mbutton.setIconName("open-menu-symbolic");

        // defining menu drop down list, witch will be displayed when the menu button is clicked
        var menuDropDownList = new ListBox();
        // defining and configuring list rows
        // settings row (open settings dialog on click)
        var settingsRow = ListBoxRow.builder()
                .setChild(Label.builder()
                        .setLabel("Settings")
                        // setting font attributes
                        .setAttributes(getAttrDef())
                        // align the label
                        .setHalign(Align.START)
                        .setMarginEnd(10)
                        .setMarginBottom(5)
                        .build())
                .setName("settings")
                .setSelectable(false)
                .build();
        // status row (opens status dialog on click)
        var statusRow = ListBoxRow.builder()
                .setChild(Label.builder()
                        .setLabel("Status")
                        // setting font attributes
                        .setAttributes(getAttrDef())
                        // align the label
                        .setHalign(Align.START)
                        .setMarginEnd(10)
                        .setMarginBottom(5)
                        .build())
                .setName("status")
                .setSelectable(false)
                .build();
        // about row (opens about dialog on click)
        var aboutRow = ListBoxRow.builder()
                .setChild(Label.builder()
                        .setLabel("About")
                        // setting font attributes
                        .setAttributes(getAttrDef())
                        // align the label
                        .setHalign(Align.START)
                        .setMarginEnd(10)
                        .build())
                .setName("about")
                .setSelectable(false)
                .build();

        // Creating actions used for keyboard shortcuts
        var activateAboutRow = SimpleAction.builder().setName("activateAboutRow").build();
        activateAboutRow.onActivate(_ -> {
            aboutRow.emitActivate(); // Emit the activate signal on the aboutRow
            //aboutRow.emitMoveFocus(DirectionType.TAB_BACKWARD); // deselect current row to close menu list and shift focus to new window
        });
        var activateSettingsRow = SimpleAction.builder().setName("activateSettingsRow").build();
        activateSettingsRow.onActivate(_ -> {
            settingsRow.emitActivate(); // Emit the activate signal on the settingsRow
            //settingsRow.emitMoveFocus(DirectionType.TAB_BACKWARD); // deselect current row to close menu list and shift focus to new window
        });
        var activateStatusRow = SimpleAction.builder().setName("activateStatusRow").build();
        activateStatusRow.onActivate(_ -> {
            statusRow.emitActivate(); // Emit the activate signal on the statusRow
            //statusRow.emitMoveFocus(DirectionType.TAB_BACKWARD); // deselect current row to close menu list and shift focus to new window
        });

        // Add the actions to the window's action group
        var actionGroup = new SimpleActionGroup();
        actionGroup.addAction(activateAboutRow);
        actionGroup.addAction(activateSettingsRow);
        actionGroup.addAction(activateStatusRow);
        this.insertActionGroup("main", actionGroup);

        // Set up a shortcut controller
        var shortcutController = new ShortcutController();

        // Define and add the shortcuts to the controller
        var shortcutAboutRow = new Shortcut(
                ShortcutTrigger.parseString("<Alt>a"), // ALT + A
                ShortcutAction.parseString("action(main.activateAboutRow)") // trigger previously defined action for about row
        );
        var shortcutSettingsRow = new Shortcut(
                ShortcutTrigger.parseString("<Alt>s"), // ALT + S
                ShortcutAction.parseString("action(main.activateSettingsRow)") // trigger previously defined action for settings row
        );
        var shortcutStatusRow = new Shortcut(
                ShortcutTrigger.parseString("<Control>s"), // CTRL + S
                ShortcutAction.parseString("action(main.activateStatusRow)") // trigger previously defined action for status row
        );
        shortcutController.addShortcut(shortcutAboutRow);
        shortcutController.addShortcut(shortcutSettingsRow);
        shortcutController.addShortcut(shortcutStatusRow);

        // Add the controller to the window
        this.addController(shortcutController);

        // listen for about / settings dialog close and shift the focus backwards to focus the main window again
        //getAboutDialog().onClosed(() -> aboutRow.emitMoveFocus(DirectionType.TAB_BACKWARD));
        //getSettingsDialog().onClosed(() -> settingsRow.emitMoveFocus(DirectionType.TAB_BACKWARD));

        // change menu list selection mode to single so the user can only select one entry at a time
        menuDropDownList.setSelectionMode(SelectionMode.BROWSE);

        // adding the list rows to the menu list
        menuDropDownList.append(statusRow);
        menuDropDownList.append(settingsRow);
        menuDropDownList.append(aboutRow);

        // creating new popover (small popup)
        var popover = new Popover();

        // listening for menu list entry click events and opening the window / dialog associated with the licked row
        menuDropDownList.onRowActivated(e -> {
            if (e == null) return;
            switch (e.getName()) {
                case "status" -> {
                    LCCP.logger.debug("User click: status row");
                    StatusDialog sD = new StatusDialog();
                    sD.present(this);
                }
                // opening settings dialog
                case "settings" -> {
                    LCCP.logger.debug("User click: settings row");
                    getSettingsDialog().present(this);
                }
                // opening about dialog
                case "about" -> {
                    LCCP.logger.debug("User click: about row");
                    getAboutDialog().present(this);
                }
            }
            // close the popover
            menuDropDownList.setSelectionMode(SelectionMode.NONE);
            menuDropDownList.unselectAll();
            menuDropDownList.setSelectionMode(SelectionMode.BROWSE);
            popover.emitClosed();
        });

        // adding the menu list to the popover
        popover.setChild(menuDropDownList);
        // adding popover to menu button, so it is displayed when the button is clicked
        mbutton.setPopover(popover);

        mbutton.onActivate(() -> {
           LCCP.logger.debug("Menu button clicked");
        });

        // adding the search button to the start of the header bar and the menu button to its end
        //headerBar.packStart(sbutton);
        headerBar.packEnd(mbutton);

        // creating main container witch will hold the main window content
        var mainContent = new Box(Orientation.VERTICAL, 0);
        mainContent.setHomogeneous(false);
        // creating north / center / south containers to correctly align window content
        var northBox = new Box(Orientation.VERTICAL, 0);
        var centerBox = new Box(Orientation.VERTICAL, 0);
        var southBox = new Box(Orientation.VERTICAL, 0);
        // set vertical expanding to true for the center box, so it pushed the north box to the top of the window and the south box to the bottom
        centerBox.setVexpand(true);
        // aligning the south box to the end (bottom) of the window to ensure it never aligns wrongly when resizing window
        southBox.setValign(Align.END);

        // toggling status bar visibility depending on user preferences
        setBannerVisible(LCCP.settings.isDisplayStatusBar());

        // adding the header bar container to the north box
        northBox.append(status);

        // adding the toast overlay to the south box
        southBox.append(toastOverlay);

        var NorthRevealer = Revealer.builder().setChild(northBox).setRevealChild(true).build();
        var CenterRevealer = Revealer.builder().setChild(centerBox).setRevealChild(true).build();
        var SouthRevealer = Revealer.builder().setChild(southBox).setRevealChild(true).build();


        // adding all alignment boxes to the main window container
        mainContent.append(NorthRevealer);
        mainContent.append(CenterRevealer);
        mainContent.append(SouthRevealer);

        var mainView = ToolbarView.builder().setContent(mainContent).build();
        mainView.addTopBar(headerBar);
        mainView.setTopBarStyle(ToolbarStyle.FLAT);

        var overlaySplitView = new OverlaySplitView();

        overlaySplitView.setEnableHideGesture(true);
        overlaySplitView.setEnableShowGesture(true);

        overlaySplitView.setContent(mainView);
        overlaySplitView.setSidebarWidthUnit(LengthUnit.PX);
        overlaySplitView.setSidebarWidthFraction(0.2);
        overlaySplitView.setShowSidebar(sideBarVisible);

        var smallHeaderBar = HeaderBar.builder().build();
        smallHeaderBar.setTitleWidget(Label.builder().setLabel("File Management").build());
        smallHeaderBar.setHexpand(true);

        smallHeaderBar.setCssClasses(new String[]{"flat"});

        var sidebarContentBox = Box.builder()
                .setOrientation(Orientation.VERTICAL)
                .setSpacing(10)
                .setValign(Align.START)
                .setHexpand(true)
                .build();

        addFileList = ListBox.builder()
                .setSelectionMode(SelectionMode.BROWSE)
                .setCssClasses(
                        new String[]{"navigation-sidebar"}
                )
                .build();
        addFile = Box.builder()
                .setOrientation(Orientation.HORIZONTAL)
                .setTooltipText("Add file to LED-Cube (Upload)")
                .setSpacing(10)
                .build();
        addFile.append(Image.fromIconName("document-send-symbolic"));
        addFile.append(
                Label.builder()
                        .setLabel("Add File")
                        .setEllipsize(EllipsizeMode.END)
                        .setXalign(0)
                        .build()
        );
        addFileList.append(
                ListBoxRow.builder()
                .setSelectable(true)
                .setChild(
                       addFile
                ).build()
        );

        var Animations = Label.builder().setLabel("Animations").build();

        animationsList = ListBox.builder()
                .setSelectionMode(SelectionMode.BROWSE)
                .setCssClasses(
                        new String[]{"navigation-sidebar"}
                )
                .build();

        var spinnerBox = Box.builder().setHalign(Align.CENTER).build();
        spinnerBox.append(Spinner.builder().setSpinning(true).build());

        SidebarSpinner = Revealer.builder().setChild(spinnerBox).setRevealChild(true).build();
        SidebarSpinner.setTransitionType(RevealerTransitionType.CROSSFADE);

        final ListBoxRow[] current = new ListBoxRow[]{new ListBoxRow()};


        addFileList.onRowActivated(_ -> {
            LCCP.logger.debug("Clicked add file row!");
            if (centerBox.getFirstChild() != null) centerBox.remove(centerBox.getFirstChild());
            centerBox.append(new AddFileDialog());
            animationsList.setSelectionMode(SelectionMode.NONE);
            animationsList.setSelectionMode(SelectionMode.BROWSE);
            addFileActivate();
        });

        animationsList.onRowActivated(row -> {
            if (current[0] == row) return;
            current[0] = row;
            if (!row.getSelectable()) return;
            if (centerBox.getFirstChild() != null) {
                CenterRevealer.setRevealChild(false);
                centerBox.remove(centerBox.getFirstChild());
            }
            String rowName = row.getName();
            try {
                Networking.Communication.sendYAMLDefaultHost(
                        YAMLMessage.builder()
                                .setPacketType(YAMLMessage.PACKET_TYPE.request)
                                .setRequestType(YAMLMessage.REQUEST_TYPE.menu)
                                .setRequestFile(rowName)
                                .build()
                );
            } catch (ConfigurationException | YAMLSerializer.YAMLException e) {
                LCCP.logger.error("Failed to send / get menu request for: " + rowName);
                animationsList.remove(row);
                LCCP.logger.error(e);
            }
            LCCP.logger.debug("AnimationSelected: " + rowName);
            addFileList.setSelectionMode(SelectionMode.NONE);
            addFileList.setSelectionMode(SelectionMode.BROWSE);
            CenterRevealer.setRevealChild(true);
            //animationActivate();
        });
        sidebarContentBox.append(addFileList);
        sidebarContentBox.append(Separator.builder().build());
        sidebarContentBox.append(Animations);
        sidebarContentBox.append(animationsList);
        sidebarContentBox.append(SidebarSpinner);

        var sidebarMainBox = new Box(Orientation.VERTICAL, 0);
        sidebarMainBox.append(smallHeaderBar);
        sidebarMainBox.append(sidebarContentBox);

        var scrolledView = ScrolledWindow.builder().setChild(sidebarMainBox).build();

        overlaySplitView.setSidebar(scrolledView);

        var sideBarToggleButton = new ToggleButton();
        sideBarToggleButton.setIconName("sidebar-show-symbolic");
        headerBar.packStart(sideBarToggleButton);
        sideBarToggleButton.setVisible(overlaySplitView.getShowSidebar());

        sideBarToggleButton.onToggled(() -> {
            boolean active = sideBarToggleButton.getActive();
            if (active && !overlaySplitView.getShowSidebar()) {
                sideBarVisible = true;
                overlaySplitView.setShowSidebar(true);
                LCCP.logger.debug("Sidebar show button pressed (toggle:true)");
            } else if (!active && overlaySplitView.getShowSidebar()) {
                sideBarVisible = false;
                overlaySplitView.setCollapsed(false);
                LCCP.logger.debug("Sidebar show button pressed (toggle:false)");
            }
        });

        final boolean[] temp = {false};

        int min = 680;

        // TODO replace with breakpoint
        new LCCPRunnable() {
            @Override
            public void run() {
                if (getWidth() <= min) {
                    if (!temp[0]) {
                        temp[0] = true;
                        overlaySplitView.setCollapsed(true);
                        sideBarToggleButton.setVisible(true);
                        LCCP.logger.debug("Window with <= " + min + ". Collapsing sidebar");
                    }
                } else {
                    temp[0] = false;
                    overlaySplitView.setCollapsed(false);
                    sideBarToggleButton.setVisible(false);
                }
                if (sideBarToggleButton.getActive() && !overlaySplitView.getShowSidebar()) {
                    sideBarToggleButton.setActive(false);
                }
            }
        }.runTaskTimerAsynchronously(0, 10);

        status.onButtonClicked(statusRow::emitActivate);
        status.setButtonLabel("LED Cube Status");

        progressBar = ProgressBar.builder().setFraction(0.0).build();

        rootView = ToolbarView.builder()
                .setContent(overlaySplitView)
                .build();
        rootView.addBottomBar(progressBar);
        rootView.setRevealBottomBars(false);

        // adding the main container to the window
        this.setContent(rootView);
    }

    public HashMap<String, String> constructMap(String regex, String... entries) {
        HashMap<String, String> result = new HashMap<>();
        for (String s : entries) {
            LCCP.logger.debug(s);
            String[] parts = s.split(regex);
            LCCP.logger.debug(Arrays.toString(parts));
            result.put(parts[0], parts[1]);
        }
        return result;
    }

    // about dialog
    private AboutDialog aDialog = null;
    // method to either create a new about dialog or get an already existing one
    // this ensures that only one about dialog is created to prevent the app unnecessarily using up system resources
    private AboutDialog getAboutDialog() {
        // checking if an existing about dialog can be reused
        if (aDialog == null) {
            // if not a new one is created
            aDialog = AboutDialog.builder()
                    .setDevelopers(new String[]{"x_Tornado10", "Bukkit GitHub Repo", "CraftBukkit GitHub Repo"})
                    .setArtists(new String[]{"Hannes Campidell", "GNOME Foundation"})
                    .setVersion(LCCP.version)
                    .setLicenseType(License.GPL_3_0)
                    .setApplicationIcon("LCCP-logo-256x256")
                    .setIssueUrl(Paths.Links.PROJECT_GITHUB + "issues")
                    .setWebsite(Paths.Links.PROJECT_GITHUB)
                    .setApplicationName(LCCP.settings.getWindowTitle())
                    .build();
        }
         return aDialog;
    }
    // generates default font arguments
    public static AttrList getAttrDef() {
        var attr = new AttrList();
        // sets the font to 'Bahnschrift' (does not work :c)
        attr.change(Pango.attrFamilyNew("Bahnschrift"));
        // sets the scale to be 1 (100%)
        attr.change(Pango.attrScaleNew(1));
        return attr;
    }
    protected void getStatus() {
        try {
            Networking.Communication.sendYAMLDefaultHost(new YAMLMessage()
                            .setPacketType(YAMLMessage.PACKET_TYPE.request)
                            .setReplyType(YAMLMessage.REPLY_TYPE.status)
                            .build()
            );
        } catch (YAMLSerializer.TODOException | ConfigurationException | YAMLSerializer.InvalidReplyTypeException |
                 YAMLSerializer.InvalidPacketTypeException e) {
            LCCP.logger.error("Failed to send status request to server! Error message: " + e.getMessage());
            LCCP.logger.error(e);
        }
    }
    // status bar updater
    private LCCPTask statusBarUpdater;
    // creates a new status bar updater
    private void updateStatus() {
        LCCP.logger.debug("Running new StatusBar update Task!");
        statusBarUpdater = new LCCPRunnable() {
            @Override
            public void run() {
                if (!status.getRevealed()) return;
                getStatus();
                // updating status bar to show current status
                //status.setTitle("LED-Cube-Status: " + getStatus());
            }
        }.runTaskTimerAsynchronously(0, 1000);
    }

    // toggle status bar
    public void setBannerVisible(boolean visible) {
        LCCP.logger.debug("---------------------------------------------------------------");
        LCCP.logger.debug("Fulfilling StatusBarToggle: " + statusBarCurrentState + " >> " + visible);
        status.setVisible(true);
        // if status bar is currently turned off and should be activated
        if (!statusBarCurrentState && visible) {
            // the current status is set to true
            statusBarCurrentState = true;
            // and a new update status task is created and started
            updateStatus();
        } else {
            // if status bar is currently turned on and should be deactivated
            // if there is an updater task running
            if (statusBarUpdater != null) statusBarUpdater.cancel(); // trigger the tasks kill switch
            // set the current status to false
            statusBarCurrentState = false;
        }
        // toggle status bar visibility based on the provided value 'visible'
        status.setRevealed(visible);
        // update user settings to match new value
        if (LCCP.mainWindow != null) LCCP.settings.setDisplayStatusBar(visible);
        LCCP.logger.debug("---------------------------------------------------------------");
    }
    // check if status bar is currently visible
    public boolean isBannerVisible() {
        return statusBarCurrentState;

    }
    // settings dialog
    private SettingsDialog sD = null;
    // method to either create a new settings dialog or get an already existing one
    // this ensures that only one settings dialog is created to prevent the app unnecessarily using up system resources
    private SettingsDialog getSettingsDialog() {
        // checking if an existing about dialog can be reused
        if (sD == null) {
            // if not a new one is created
            sD = new SettingsDialog();
        }
        return sD;
    }
    // check if the settings dialog is currently visible
    public boolean isSettingsDialogVisible() {
        return sD != null;
    }
    // toggle auto update option for the settings dialog
    public void setAutoUpdate(boolean active) {
        if (autoUpdate == active) return;
        LCCP.logger.debug("Fulfilling autoUpdateToggle request -> " + active);
        // if auto updating isn't active and should be activated
        if (!autoUpdate && active) {
            // if the settings dialog is already visible
            if (isSettingsDialogVisible()) getSettingsDialog().startRemoteUpdate(); // start the remote updating loop within the settings dialog
            getSettingsDialog().removeManualRemoteApplySwitch(); // remove the manual updating button from the settings dialog
        // if auto updating is active and should be deactivated
        } else if (autoUpdate && !active) {
            // the manual updating button is re added to the settings dialog
            getSettingsDialog().addManualRemoteApplySwitch();
            // if the settings dialog is currently visible the remote updating task is stopped
            if (isSettingsDialogVisible()) getSettingsDialog().stopRemoteUpdate();
        }
        // current auto updating status is set based on the provided value 'active'
        autoUpdate = active;
    }

    public void resetSettingsDialog() {
        this.sD = null;
    }

    public void updateStatus(StatusUpdate statusUpdate) {
        status.setTitle(statusUpdate.toString());
    }

    @EventHandler
    public void onStatus(Events.Status e) {
        StatusUpdate statusUpdate = e.statusUpdate();

        ListBoxRow selectedRow = animationsList.getSelectedRow();
        String name = "";
        if (selectedRow != null) name = selectedRow.getName();
        animationsList.unselectAll();
        animationsList.setSelectionMode(SelectionMode.NONE);
        animationsList.removeAll();

        SidebarSpinner.setRevealChild(true);

        //var box = Box.builder().setHalign(Align.CENTER).build();
        //box.append(Spinner.builder().setSpinning(true).build());

        /*var spinnerRow = ListBoxRow.builder()
                .setSelectable(true)
                .setName(box.getName())
                .setSelectable(false)
                .setFocusable(false)
                .setChild(
                        box
                ).build();
        animationsList.append(spinnerRow);*/

        List<ListBoxRow> anims = new ArrayList<>();

        for (Map.Entry<String, String> entry : statusUpdate.getAvailableAnimations().entrySet()) {


            var b = Box.builder()
                    .setOrientation(Orientation.HORIZONTAL)
                    .setTooltipText("Open " + entry.getKey() + " settings menu")
                    .setName(entry.getKey())
                    .setSpacing(10)
                    .build();
            b.append(Image.fromIconName(entry.getValue()));
            b.append(
                    Label.builder()
                            .setLabel(entry.getKey())
                            .setEllipsize(EllipsizeMode.END)
                            .setXalign(0)
                            .build()
            );
            ListBoxRow row = listBoxWrap(b);
            if (entry.getKey().equals(name)) selectedRow = row;
            anims.add(row);
        }
        SidebarSpinner.setRevealChild(false);
        //animationsList.remove(spinnerRow);
        for (ListBoxRow lbr : anims) {
            animationsList.append(lbr);
        }
        animationsList.setSelectionMode(SelectionMode.BROWSE);
        animationsList.selectRow(selectedRow);
    }

    @EventHandler
    public void onStarted(Events.Started e) {
        addFileList.emitRowSelected(addFileList.getRowAtIndex(0));
        addFileList.emitRowActivated(addFileList.getRowAtIndex(0));
        addFileList.emitSelectedRowsChanged();
    }

    private void addFileActivate() {

    }
    private void animationActivate() {

    }

    public ListBoxRow listBoxWrap(Widget widget) {
        return ListBoxRow.builder()
                        .setSelectable(true)
                        .setName(widget.getName())
                        .setChild(
                                widget
                        ).build();
    }

}
