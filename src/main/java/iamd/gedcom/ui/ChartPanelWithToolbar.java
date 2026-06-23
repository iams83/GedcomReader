package iamd.gedcom.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;

import iamd.gedcom.datamodel.Family;
import iamd.gedcom.datamodel.Individual;
import iamd.ui.ChartPanelListener;

/**
 * A wrapper panel for GedComChartPanel that adds a toggleable, self-exclusive
 * toolbar at the top to switch between the different chart types and the
 * graph view.
 */
@SuppressWarnings("serial")
public class ChartPanelWithToolbar extends JPanel
{
    /**
     * Listener interface for toolbar actions.
     * The listener is notified when the user clicks the graph button
     * (which switches to a different main panel) and when the user
     * switches between chart types via the toolbar (so the menu can sync).
     */
    public interface ChartToolbarListener
    {
        void onSwitchToGraph();
        void onChartTypeSelectedFromToolbar(GedComChartPanel.ChartType type);
    }
    
    private final GedComChartPanel chartPanel;
    private final JToolBar toolbar;
    
    private final JToggleButton descendantButton;
    private final JToggleButton ancestorButton;
    private final JToggleButton twoWayButton;
    private final JToggleButton graphButton;
    
    private ChartToolbarListener listener;
    
    public ChartPanelWithToolbar()
    {
        super(new java.awt.BorderLayout());
        
        // Create the underlying chart panel
        this.chartPanel = new GedComChartPanel();
        
        // Create the toolbar
        this.toolbar = new JToolBar();
        this.toolbar.setFloatable(false);
        this.toolbar.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        
        // Create toggle buttons
        this.descendantButton = new JToggleButton("\u2193"); // Down arrow
        this.descendantButton.setToolTipText("Descendant chart");
        this.descendantButton.setFocusable(false);
        
        this.ancestorButton = new JToggleButton("\u2191"); // Up arrow
        this.ancestorButton.setToolTipText("Ancestor (Parent) chart");
        this.ancestorButton.setFocusable(false);
        
        this.twoWayButton = new JToggleButton("\u2195"); // Up-down arrow
        this.twoWayButton.setToolTipText("Two-way chart");
        this.twoWayButton.setFocusable(false);
        
        this.graphButton = new JToggleButton("\u29B9"); // Circled plus (graph-like)
        this.graphButton.setToolTipText("Dynamic graph");
        this.graphButton.setFocusable(false);
        
        // Make buttons self-exclusive via ButtonGroup
        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(this.descendantButton);
        buttonGroup.add(this.ancestorButton);
        buttonGroup.add(this.twoWayButton);
        buttonGroup.add(this.graphButton);
        
        // Set initial selection based on current chart type
        syncButtonWithChartType();
        
        // Add buttons to toolbar
        this.toolbar.add(this.descendantButton);
        this.toolbar.add(this.ancestorButton);
        this.toolbar.add(this.twoWayButton);
        this.toolbar.add(Box.createHorizontalStrut(10));
        this.toolbar.add(this.graphButton);
        
        // Add action listeners
        this.descendantButton.addActionListener(e -> {
            if (this.descendantButton.isSelected())
            {
                this.chartPanel.setChartType(GedComChartPanel.ChartType.DescendantChart);
                if (this.listener != null)
                    this.listener.onChartTypeSelectedFromToolbar(GedComChartPanel.ChartType.DescendantChart);
            }
        });
        
        this.ancestorButton.addActionListener(e -> {
            if (this.ancestorButton.isSelected())
            {
                this.chartPanel.setChartType(GedComChartPanel.ChartType.ParentChart);
                if (this.listener != null)
                    this.listener.onChartTypeSelectedFromToolbar(GedComChartPanel.ChartType.ParentChart);
            }
        });
        
        this.twoWayButton.addActionListener(e -> {
            if (this.twoWayButton.isSelected())
            {
                this.chartPanel.setChartType(GedComChartPanel.ChartType.TwoWayChart);
                if (this.listener != null)
                    this.listener.onChartTypeSelectedFromToolbar(GedComChartPanel.ChartType.TwoWayChart);
            }
        });
        
        this.graphButton.addActionListener(e -> {
            if (this.graphButton.isSelected() && this.listener != null)
            {
                this.listener.onSwitchToGraph();
            }
        });
        
        // Add components to this panel
        this.add(this.toolbar, java.awt.BorderLayout.NORTH);
        this.add(this.chartPanel, java.awt.BorderLayout.CENTER);
    }
    
    public void setChartToolbarListener(ChartToolbarListener listener)
    {
        this.listener = listener;
    }
    
    /**
     * Returns the underlying chart panel for delegation purposes.
     */
    public GedComChartPanel getChartPanel()
    {
        return this.chartPanel;
    }
    
    /**
     * Delegates to the underlying chart panel.
     */
    public void setModel(Individual individual)
    {
        this.chartPanel.setModel(individual);
    }
    
    /**
     * Delegates to the underlying chart panel.
     */
    public void setModel(Family family)
    {
        this.chartPanel.setModel(family);
    }
    
    /**
     * Sets the chart type and updates the corresponding toolbar button.
     * Note: This does NOT fire the chartTypeSelectedFromToolbar listener,
     * because it's typically called in response to a menu selection.
     */
    public void setChartType(GedComChartPanel.ChartType type)
    {
        this.chartPanel.setChartType(type);
        syncButtonWithChartType();
    }
    
    /**
     * Returns the current chart type.
     */
    public GedComChartPanel.ChartType getChartType()
    {
        return this.chartPanel.getType();
    }
    
    /**
     * Returns the current chart type (alias for getChartType for compatibility).
     */
    public GedComChartPanel.ChartType getType()
    {
        return this.chartPanel.getType();
    }
    
    /**
     * Reloads the model on the underlying chart panel.
     */
    public void reloadModel()
    {
        this.chartPanel.reloadModel();
    }
    
    /**
     * Selects the graph button in the toolbar (used to sync the toolbar
     * state with the menu when the user switches to graph from elsewhere).
     */
    public void selectGraphButton(boolean selected)
    {
        this.graphButton.setSelected(selected);
    }
    
    // Delegate methods to the underlying chart panel
    
    public void addChartPanelListener(ChartPanelListener<GedComChartElement> listener)
    {
        this.chartPanel.addChartPanelListener(listener);
    }
    
    public GedComChartElement getArcAt(java.awt.Point point)
    {
        return this.chartPanel.getArcAt(point);
    }
    
    public void initializeBoundingBox()
    {
        this.chartPanel.initializeBoundingBox();
    }
    
    public Rectangle2D getBounds2D()
    {
        return this.chartPanel.getBounds2D();
    }
    
    public AffineTransform getTransform()
    {
        return this.chartPanel.getTransform();
    }
    
    public AffineTransform initializeTransformation(Rectangle2D bounds, Dimension size)
    {
        return this.chartPanel.initializeTransformation(bounds, size);
    }
    
    public void setBackground(Color color)
    {
        if (this.chartPanel != null)
            this.chartPanel.setBackground(color);
        else
            super.setBackground(color);
    }
    
    public Color getBackground()
    {
        if (this.chartPanel != null)
            return this.chartPanel.getBackground();
        return super.getBackground();
    }
    
    public void setFont(Font font)
    {
        if (this.chartPanel != null)
            this.chartPanel.setFont(font);
        else
            super.setFont(font);
    }
    
    public Font getFont()
    {
        if (this.chartPanel != null)
            return this.chartPanel.getFont();
        return super.getFont();
    }
    
    public void setToolTipText(String text)
    {
        if (this.chartPanel != null)
            this.chartPanel.setToolTipText(text);
        else
            super.setToolTipText(text);
    }
    
    public void paint(Graphics2D g2, AffineTransform tx, Dimension size)
    {
        this.chartPanel.paint(g2, tx, size);
    }
    
    @Override
    public void paint(Graphics g)
    {
        super.paint(g);
    }
    
    /**
     * Updates the toolbar buttons to reflect the current chart type.
     */
    private void syncButtonWithChartType()
    {
        GedComChartPanel.ChartType type = this.chartPanel.getType();
        
        this.descendantButton.setSelected(type == GedComChartPanel.ChartType.DescendantChart);
        this.ancestorButton.setSelected(type == GedComChartPanel.ChartType.ParentChart);
        this.twoWayButton.setSelected(type == GedComChartPanel.ChartType.TwoWayChart);
        // The graph button is not auto-synced - it's only set by the user clicking it
    }
}
