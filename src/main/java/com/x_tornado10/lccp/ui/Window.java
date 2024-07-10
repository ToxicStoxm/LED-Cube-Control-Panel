package com.x_tornado10.lccp.ui;

import com.x_tornado10.lccp.LCCP;
import com.x_tornado10.lccp.event_handling.EventHandler;
import com.x_tornado10.lccp.event_handling.Events;
import com.x_tornado10.lccp.event_handling.listener.EventListener;
import com.x_tornado10.lccp.task_scheduler.LCCPProcessor;
import com.x_tornado10.lccp.task_scheduler.LCCPRunnable;
import com.x_tornado10.lccp.task_scheduler.LCCPTask;
import com.x_tornado10.lccp.communication.network.Networking;
import com.x_tornado10.lccp.Paths;
import com.x_tornado10.lccp.time.TimeManager;
import com.x_tornado10.lccp.yaml_factory.wrappers.message_wrappers.StatusUpdate;
import com.x_tornado10.lccp.yaml_factory.YAMLSerializer;
import com.x_tornado10.lccp.yaml_factory.YAMLMessage;
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

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static com.x_tornado10.lccp.yaml_factory.AnimationMenu.capitalizeFirstLetter;

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
    private HashMap<String, String> availableAnimations;
    private Map.Entry<String, String> currentAnimation = null;

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
        //setAutoUpdate(LCCP.settings.isAutoUpdateRemote());

        // toast overlay used to display toasts (notification) to the user
        toastOverlay = ToastOverlay.builder().build();

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

        availableAnimations = new HashMap<>();

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
        var TopBox = new Box(Orientation.VERTICAL, 0);
        var CenterBox = Box.builder()
                .setOrientation(Orientation.VERTICAL)
                .setSpacing(0)
                .setValign(Align.CENTER)
                .build();
        // set vertical expanding to true for the center box, so it pushed the north box to the top of the window and the south box to the bottom
        CenterBox.setVexpand(true);
        // aligning the south box to the end (bottom) of the window to ensure it never aligns wrongly when resizing window

        // toggling status bar visibility depending on user preferences
        setBannerVisible(LCCP.settings.isDisplayStatusBar());

        // adding the header bar container to the north box
        TopBox.append(status);

        var TopRevealer = Revealer.builder()
                .setChild(TopBox)
                .setRevealChild(true)
                .build();
        TopRevealer.setTransitionType(RevealerTransitionType.SLIDE_DOWN);
        var CenterRevealer = Revealer.builder()
                .setChild(CenterBox)
                .setRevealChild(true)
                .build();
        CenterRevealer.setTransitionType(RevealerTransitionType.CROSSFADE);

        // adding all alignment boxes to the main window container
        mainContent.append(TopRevealer);
        mainContent.append(CenterRevealer);

        var playPauseButton = Button.builder()
                .setIconName("media-playback-start-symbolic")
                .setName("play")
                .setCssClasses(new String[]{"osd", "circular"})
                .build();
        var stopButton = Button.builder()
                .setIconName("media-playback-stop-symbolic")
                .setName("stop")
                .setCssClasses(new String[]{"osd", "circular"})
                .build();

        var StopRevealer = Revealer.builder()
                .setRevealChild(false)
                .setChild(stopButton)
                .build();
        StopRevealer.setTransitionType(RevealerTransitionType.CROSSFADE);

        TimeManager.initTimeTracker("control_buttons", 500);

        AtomicBoolean allowPlayPause = new AtomicBoolean(true);
        String playState = "play";
        String pauseState = "pause";
        playPauseButton.onClicked(() -> {
            if (!TimeManager.call("control_buttons")) return;
            String state = playPauseButton.getName();
            if (allowPlayPause.get() && currentAnimation != null && availableAnimations.containsKey(currentAnimation.getKey())) {
                if (state.equals(playState)) {
                    try {
                        Networking.Communication.sendYAMLDefaultHost(
                                YAMLMessage.builder()
                                        .setPacketType(YAMLMessage.PACKET_TYPE.request)
                                        .setRequestType(YAMLMessage.REQUEST_TYPE.play)
                                        .setRequestFile(currentAnimation.getKey())
                                        .build(),
                                success -> {
                                    if (!success) {
                                        errorFeedback(playPauseButton, allowPlayPause);
                                        LCCP.logger.error(capitalizeFirstLetter(state)  + " request for " + currentAnimation.getKey() + " failed!");
                                    } else {
                                        playPauseButton.setName("pause");
                                        playPauseButton.setIconName("media-playback-pause-symbolic");
                                        LCCP.logger.debug("Successfully send " + state + " request for " + currentAnimation.getKey() + "!");
                                        StopRevealer.setRevealChild(true);
                                    }
                                }
                        );
                    } catch (ConfigurationException | YAMLSerializer.InvalidReplyTypeException |
                             YAMLSerializer.InvalidPacketTypeException | YAMLSerializer.TODOException _) {
                        LCCP.logger.error(capitalizeFirstLetter(state)  + " request for " + currentAnimation.getKey() + " failed!");
                    }
                } else if (state.equals(pauseState)) {
                    try {
                        Networking.Communication.sendYAMLDefaultHost(
                                YAMLMessage.builder()
                                        .setPacketType(YAMLMessage.PACKET_TYPE.request)
                                        .setRequestType(YAMLMessage.REQUEST_TYPE.pause)
                                        .setRequestFile(currentAnimation.getKey())
                                        .build(),
                                success -> {
                                    if (!success) {
                                        errorFeedback(playPauseButton, allowPlayPause);
                                        LCCP.logger.error(capitalizeFirstLetter(state)  + " request for " + currentAnimation.getKey() + " failed!");
                                        LCCP.logger.error(capitalizeFirstLetter(state)  + " request for " + currentAnimation.getKey() + " failed!");
                                    } else {
                                        playPauseButton.setName("play");
                                        playPauseButton.setIconName("media-playback-start-symbolic");
                                        LCCP.logger.debug("Successfully send " + state + " request for " + currentAnimation.getKey() + "!");
                                    }

                                }
                        );
                    } catch (ConfigurationException | YAMLSerializer.InvalidReplyTypeException |
                             YAMLSerializer.InvalidPacketTypeException | YAMLSerializer.TODOException _) {
                        LCCP.logger.error(capitalizeFirstLetter(state)  + " request for " + currentAnimation.getKey() + " failed!");
                    }
                }
            } else errorFeedback(playPauseButton, allowPlayPause);
        });
        AtomicBoolean allowStop = new AtomicBoolean(true);
        stopButton.onClicked(() -> {
            if (!TimeManager.call("control_buttons")) return;
            if (allowStop.get() && currentAnimation != null && availableAnimations.containsKey(currentAnimation.getKey())) {
                try {
                    Networking.Communication.sendYAMLDefaultHost(
                            YAMLMessage.builder()
                                    .setPacketType(YAMLMessage.PACKET_TYPE.request)
                                    .setRequestType(YAMLMessage.REQUEST_TYPE.stop)
                                    .setRequestFile(currentAnimation.getKey())
                                    .build(),
                            success -> {
                                if (!success) {
                                    errorFeedback(stopButton, allowStop);
                                    LCCP.logger.error("Stop request for " + currentAnimation.getKey() + " failed!");
                                } else {
                                    StopRevealer.setRevealChild(false);
                                    playPauseButton.setIconName("media-playback-start-symbolic");
                                    playPauseButton.setName("play");
                                    LCCP.logger.debug("Successfully send Stop request for " + currentAnimation.getKey() + "!");
                                }
                            }
                    );
                } catch (ConfigurationException | YAMLSerializer.InvalidReplyTypeException |
                         YAMLSerializer.InvalidPacketTypeException | YAMLSerializer.TODOException _) {
                    LCCP.logger.error("Stop request for " + currentAnimation.getKey() + " failed!");

                }
            } else errorFeedback(stopButton, allowStop);
        });

        var controlButtons = Box.builder()
                .setOrientation(Orientation.HORIZONTAL)
                .setSpacing(10)
                .build();
        controlButtons.append(StopRevealer);
        controlButtons.append(playPauseButton);

        var controlButtonsRevealer = Revealer.builder()
                .setChild(controlButtons)
                .setRevealChild(false)
                .build();
        controlButtonsRevealer.setTransitionType(RevealerTransitionType.CROSSFADE);

        var controlButtonsWrapper = Clamp.builder()
                .setChild(controlButtonsRevealer)
                .setOrientation(Orientation.VERTICAL)
                .setMaximumSize(30)
                .setMarginEnd(30)
                .setMarginBottom(25)
                .setTighteningThreshold(30)
                .setHalign(Align.END)
                .build();

        toastOverlay.setValign(Align.END);
        toastOverlay.setHalign(Align.CENTER);
        var mainView = ToolbarView.builder()
                .setContent(mainContent)
                .build();
        mainView.setBottomBarStyle(ToolbarStyle.FLAT);

        mainView.addTopBar(headerBar);

        var overlay = Overlay.builder().setChild(mainView).build();
        overlay.addOverlay(toastOverlay);
        controlButtonsWrapper.setHalign(Align.END);
        controlButtonsWrapper.setValign(Align.END);
        overlay.addOverlay(controlButtonsWrapper);

        mainView.setTopBarStyle(ToolbarStyle.FLAT);

        var overlaySplitView = new OverlaySplitView();

        overlaySplitView.setEnableHideGesture(true);
        overlaySplitView.setEnableShowGesture(true);

        overlaySplitView.setContent(overlay);
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

        AtomicReference<ListBoxRow> current = new AtomicReference<>(new ListBoxRow());

        addFileList.onRowActivated(_ -> {
            controlButtonsRevealer.setRevealChild(false);
            LCCP.logger.debug("Clicked add file row!");
            CenterRevealer.setRevealChild(false);
            if (CenterBox.getFirstChild() != null) CenterBox.remove(CenterBox.getFirstChild());
            CenterBox.setValign(Align.START);
            CenterBox.append(new AddFileDialog());
            animationsList.setSelectionMode(SelectionMode.NONE);
            animationsList.setSelectionMode(SelectionMode.BROWSE);
            CenterRevealer.setRevealChild(true);
        });

        animationsList.onRowActivated(row -> {
            controlButtonsRevealer.setRevealChild(true);
            if (current.get() == row) return;
            current.set(row);
            if (!row.getSelectable()) return;
            if (CenterBox.getFirstChild() != null) {
                CenterRevealer.setRevealChild(false);
                CenterBox.remove(CenterBox.getFirstChild());
            }
            CenterBox.setValign(Align.CENTER);
            AtomicBoolean spinner = new AtomicBoolean(true);
            new LCCPRunnable() {
                @Override
                public void run() {
                    if (spinner.get()) {
                        CenterBox.append(Spinner.builder().setSpinning(true).build());
                        CenterRevealer.setRevealChild(true);
                    }
                }
            }.runTaskLaterAsynchronously(500);
            String rowName = row.getName();
            if (!availableAnimations.containsKey(rowName)) LCCP.logger.warn("Requesting menu for unknown animation menu! Name: '" + rowName + "'");
            else {
                String icon = availableAnimations.get(rowName);
                currentAnimation = new AbstractMap.SimpleEntry<>(rowName, icon);
            }
            try {
                Networking.Communication.sendYAMLDefaultHost(
                        YAMLMessage.builder()
                                .setPacketType(YAMLMessage.PACKET_TYPE.request)
                                .setRequestType(YAMLMessage.REQUEST_TYPE.menu)
                                .setRequestFile(rowName)
                                .build(),
                        result -> {
                            if (result) {
                                LCCP.logger.debug("Requesting animation menu for '" + rowName + "' from server.");
                                /*toastOverlay.addToast(
                                        Toast.builder()
                                                .setTimeout(3)
                                                .setTitle("Loading menu for '" + rowName + "'...")
                                                .build()
                                );*/
                            } else {
                                LCCP.logger.error("Failed to load menu for '" + rowName + "'!");
                                /*toastOverlay.addToast(
                                        Toast.builder()
                                                .setTimeout(0)
                                                .setTitle("Failed to load menu for '" + rowName + "'!")
                                                .build()
                                );*/
                            }
                        },
                        new LCCPProcessor() {
                            @Override
                            public void run(YAMLMessage yaml) throws DefaultHandleException {
                                if (yaml.getPacketType().equals(YAMLMessage.PACKET_TYPE.reply) && yaml.getReplyType().equals(YAMLMessage.REPLY_TYPE.menu)) {
                                    LCCP.logger.debug(yaml.getAnimationMenu().toString());
                                    String id = "[" + yaml.getNetworkID() + "] ";
                                    LCCP.logger.debug(id + "Converting animation menu to displayable menu!");
                                    spinner.set(false);
                                    CenterRevealer.setRevealChild(false);
                                    if (CenterBox.getFirstChild() != null) {
                                        CenterRevealer.setRevealChild(false);
                                        CenterBox.remove(CenterBox.getFirstChild());
                                    }
                                    CenterBox.setValign(Align.START);
                                    CenterBox.append(AnimationMenu.display(yaml.getAnimationMenu()));
                                    LCCP.logger.debug(id + "Displaying converted menu!");
                                    CenterRevealer.setRevealChild(true);
                                } else throw new DefaultHandleException("Unexpected response!");
                            }
                        }
                );
            } catch (ConfigurationException | YAMLSerializer.YAMLException e) {
                LCCP.logger.error("Failed to send / get menu request for: " + rowName);
                animationsList.remove(row);
                LCCP.logger.error(e);
            }
            LCCP.logger.debug("AnimationSelected: " + rowName);
            addFileList.setSelectionMode(SelectionMode.NONE);
            addFileList.setSelectionMode(SelectionMode.BROWSE);
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

        AtomicBoolean temp = new AtomicBoolean(false);

        int min = 680;

        // TODO replace with breakpoint
        new LCCPRunnable() {
            @Override
            public void run() {
                if (getHeight() <= 499) {
                    setSizeRequest(getWidth(), 500);
                    return;
                }
                if (getWidth() <= min) {
                    if (!temp.get()) {
                        temp.set(true);
                        overlaySplitView.setCollapsed(true);
                        sideBarToggleButton.setVisible(true);
                        LCCP.logger.debug("Window with <= " + min + ". Collapsing sidebar");
                    }
                } else {
                    temp.set(false);
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

    public void errorFeedback(Button button, AtomicBoolean bool) {
        if (!bool.get()) return;
        bool.set(false);
        button.setCssClasses(new String[]{"circular", "destructive-action"});
        new LCCPRunnable() {
            @Override
            public void run() {
                button.setCssClasses(new String[]{"circular", "osd"});
                bool.set(true);
            }
        }.runTaskLaterAsynchronously(750);
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
                    .setApplicationIcon("com.x_tornado10.lccp")
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
    protected void getStatus(Networking.Communication.FinishCallback callback) {
        try {
            Networking.Communication.sendYAML(LCCP.server_settings.getIPv4(), LCCP.server_settings.getPort(), new YAMLMessage()
                            .setPacketType(YAMLMessage.PACKET_TYPE.request)
                            .setReplyType(YAMLMessage.REPLY_TYPE.status)
                            .build(),
                    callback,
                    new LCCPProcessor() {
                        @Override
                        public void run(YAMLMessage yaml) {
                            LCCP.eventManager.fireEvent(new Events.Status(StatusUpdate.fromYAMLMessage(yaml)));
                        }
                    }
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
                getStatus(success -> {
                    if (!success) updateStatus(StatusUpdate.notConnected());
                });
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
    /*public void setAutoUpdate(boolean active) {
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
    }*/

    public void resetSettingsDialog() {
        this.sD = null;
    }

    public void updateStatus(StatusUpdate statusUpdate) {
        status.setTitle(statusUpdate.minimal());
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

        availableAnimations = statusUpdate.getAvailableAnimations();

        for (Map.Entry<String, String> entry : availableAnimations.entrySet()) {
            var availableAnimation = Box.builder()
                    .setOrientation(Orientation.HORIZONTAL)
                    .setTooltipText("Open " + entry.getKey() + " settings menu")
                    .setName(entry.getKey())
                    .setSpacing(10)
                    .build();
            availableAnimation.append(Image.fromIconName(entry.getValue()));
            availableAnimation.append(
                    Label.builder()
                            .setLabel(entry.getKey())
                            .setEllipsize(EllipsizeMode.END)
                            .setXalign(0)
                            .build()
            );
            ListBoxRow row = listBoxWrap(availableAnimation);
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

    public ListBoxRow listBoxWrap(Widget widget) {
        return ListBoxRow.builder()
                        .setSelectable(true)
                        .setName(widget.getName())
                        .setChild(
                                widget
                        ).build();
    }

}
