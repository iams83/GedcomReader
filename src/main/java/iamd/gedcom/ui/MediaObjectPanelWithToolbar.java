package iamd.gedcom.ui;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;

import iamd.gedcom.datamodel.MediaObject;
import iamd.gedcom.rsrc.Resources;
import iamd.gedcom.ui.MediaObjectDisplayPanel.MediaObjectDisplayListener;

/**
 * A wrapper panel for MediaObjectDisplayPanel that adds a toggleable,
 * self-exclusive toolbar at the top to switch between selection and crop tools,
 * plus an "open with system default tool" button after a separator.
 */
public class MediaObjectPanelWithToolbar extends JPanel
{
    public enum ToolMode
    {
        SELECTION,
        CROP
    }

    private final MediaObjectDisplayPanel displayPanel;
    private final JToolBar toolbar;

    private final JToggleButton selectionButton;
    private final JToggleButton cropButton;
    private final JButton openFileButton;

    private ToolMode currentMode = ToolMode.SELECTION;

    public MediaObjectPanelWithToolbar()
    {
        super(new BorderLayout());

        // Create the underlying display panel
        this.displayPanel = new MediaObjectDisplayPanel();

        // Create the toolbar
        this.toolbar = new JToolBar();
        this.toolbar.setFloatable(false);
        this.toolbar.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

        // Create toggle buttons using existing icons
        this.selectionButton = new JToggleButton(Resources.ExploreIcon);
        this.selectionButton.setToolTipText("Selection tool - click rectangles to navigate to individuals");
        this.selectionButton.setFocusable(false);
        this.selectionButton.setSelected(true); // Default

        this.cropButton = new JToggleButton(Resources.CropImageIcon);
        this.cropButton.setToolTipText("Crop tool - drag on the image to define a crop area");
        this.cropButton.setFocusable(false);

        // Non-toggleable "open with system default tool" button. It is a
        // plain JButton (never a JToggleButton) and is intentionally kept
        // outside the selection/crop ButtonGroup so it does not affect the
        // current tool mode when clicked.
        this.openFileButton = new JButton(Resources.OpenFileIcon);
        this.openFileButton.setToolTipText("Open the media file with the system's default tool");
        this.openFileButton.setFocusable(false);
        this.openFileButton.setEnabled(false); // Re-enabled by setModel when a usable file exists.

        // Make selection/crop buttons self-exclusive via ButtonGroup.
        // The open-file button is NOT part of this group on purpose.
        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(this.selectionButton);
        buttonGroup.add(this.cropButton);

        // Add buttons to toolbar
        this.toolbar.add(this.selectionButton);
        this.toolbar.add(this.cropButton);

        // Visual divider between the toggleable editing tools and the
        // out-of-band open-file action. addSeparator() renders a fixed-size
        // gap (no toggle button, no click handler) which is exactly the
        // "non-toggleable separator" the request calls for.
        this.toolbar.addSeparator();
        this.toolbar.add(this.openFileButton);

        // Add action listeners
        this.selectionButton.addActionListener(e -> {
            if (this.selectionButton.isSelected())
            {
                setToolMode(ToolMode.SELECTION);
            }
        });

        this.cropButton.addActionListener(e -> {
            if (this.cropButton.isSelected())
            {
                setToolMode(ToolMode.CROP);
            }
        });

        this.openFileButton.addActionListener(e -> openCurrentMediaFileWithSystemDefault());

        // Add components to this panel
        this.add(this.toolbar, BorderLayout.NORTH);
        this.add(this.displayPanel, BorderLayout.CENTER);
    }

    /**
     * Sets the media object to display. Also refreshes the enabled state
     * of the open-file button based on whether the new model actually has
     * a usable file on disk.
     */
    public void setModel(MediaObject mediaObject)
    {
        this.displayPanel.setModel(mediaObject);
        this.openFileButton.setEnabled(hasOpenableFile(mediaObject));
    }

    /**
     * Returns {@code true} when the given media object has a FILE entry
     * that resolves to an existing file on disk. Used to decide whether
     * the open-file button should be clickable.
     */
    private static boolean hasOpenableFile(MediaObject mediaObject)
    {
        if (mediaObject == null)
            return false;
        File file = mediaObject.getMediaFile();
        return file != null && file.exists();
    }

    /**
     * Opens the current media object's file with the OS-registered default
     * application for its type (e.g. the default image viewer for a .jpg,
     * the default PDF reader for a .pdf, and so on). Surfaces failures as
     * an error dialog instead of throwing, since the toolbar is the
     * primary call site and the user must be told why nothing happened.
     */
    private void openCurrentMediaFileWithSystemDefault()
    {
        MediaObject model = this.displayPanel.getModel();
        if (model == null)
            return;

        File file = model.getMediaFile();
        if (file == null || !file.exists())
        {
            JOptionPane.showMessageDialog(
                    this,
                    "The media object has no associated file on disk.",
                    "Open file",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (!Desktop.isDesktopSupported())
        {
            JOptionPane.showMessageDialog(
                    this,
                    "This platform does not support opening files via the desktop integration.",
                    "Open file",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        Desktop desktop = Desktop.getDesktop();
        if (!desktop.isSupported(Desktop.Action.OPEN))
        {
            JOptionPane.showMessageDialog(
                    this,
                    "Opening files with the system default tool is not supported here.",
                    "Open file",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        try
        {
            desktop.open(file);
        }
        catch (IOException | IllegalArgumentException ex)
        {
            JOptionPane.showMessageDialog(
                    this,
                    "Could not open \"" + file.getAbsolutePath() + "\".\n\n" + ex.getMessage(),
                    "Open file",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Sets the listener for rectangle clicks.
     */
    public void setMediaObjectDisplayListener(MediaObjectDisplayListener listener)
    {
        this.displayPanel.setMediaObjectDisplayListener(listener);
    }

    /**
     * Returns the underlying display panel.
     */
    public MediaObjectDisplayPanel getDisplayPanel()
    {
        return this.displayPanel;
    }

    /**
     * Sets the current tool mode. Updates both the toolbar state and
     * the underlying display panel.
     */
    public void setToolMode(ToolMode mode)
    {
        this.currentMode = mode;

        // Update toolbar buttons
        this.selectionButton.setSelected(mode == ToolMode.SELECTION);
        this.cropButton.setSelected(mode == ToolMode.CROP);

        // Update the display panel's tool mode
        this.displayPanel.setToolMode(mode);
    }

    /**
     * Returns the current tool mode.
     */
    public ToolMode getToolMode()
    {
        return this.currentMode;
    }
}
