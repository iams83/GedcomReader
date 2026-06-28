package iamd.gedcom.ui;

import java.awt.BorderLayout;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;

import iamd.gedcom.datamodel.MediaObject;
import iamd.gedcom.rsrc.Resources;
import iamd.gedcom.ui.MediaObjectDisplayPanel.MediaObjectDisplayListener;

/**
 * A wrapper panel for MediaObjectDisplayPanel that adds a toggleable,
 * self-exclusive toolbar at the top to switch between selection and crop tools.
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
        
        // Make buttons self-exclusive via ButtonGroup
        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(this.selectionButton);
        buttonGroup.add(this.cropButton);
        
        // Add buttons to toolbar
        this.toolbar.add(this.selectionButton);
        this.toolbar.add(this.cropButton);
        
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
        
        // Add components to this panel
        this.add(this.toolbar, BorderLayout.NORTH);
        this.add(this.displayPanel, BorderLayout.CENTER);
    }
    
    /**
     * Sets the media object to display.
     */
    public void setModel(MediaObject mediaObject)
    {
        this.displayPanel.setModel(mediaObject);
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
