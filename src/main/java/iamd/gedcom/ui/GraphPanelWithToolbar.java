package iamd.gedcom.ui;

import java.awt.BorderLayout;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;

import iamd.gedcom.datamodel.Individual;
import iamd.ui.ChartPanelListener;

/**
 * A wrapper panel for GedComGraph that adds a toggleable, self-exclusive
 * toolbar at the top, similar to ChartPanelWithToolbar. This way, the
 * user always has access to switch between chart types and the graph view
 * regardless of which main panel is currently showing.
 */
@SuppressWarnings("serial")
public class GraphPanelWithToolbar extends JPanel
{
    /**
     * Listener interface for toolbar actions. The listener is notified
     * when the user clicks any toolbar button.
     */
    public interface GraphToolbarListener
    {
        void onSwitchToGraph();
        void onChartTypeSelectedFromToolbar(GedComChartPanel.ChartType type);
    }
    
    private final GedComGraph graphPanel;
    private final JToolBar toolbar;
    
    private final JToggleButton descendantButton;
    private final JToggleButton ancestorButton;
    private final JToggleButton twoWayButton;
    private final JToggleButton graphButton;
    
    private GraphToolbarListener listener;
    
    public GraphPanelWithToolbar()
    {
        super(new BorderLayout());
        
        // Create the underlying graph panel
        this.graphPanel = new GedComGraph();
        
        // Create the toolbar
        this.toolbar = new JToolBar();
        this.toolbar.setFloatable(false);
        this.toolbar.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        
        // Create toggle buttons (same as in ChartPanelWithToolbar)
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
        this.graphButton.setSelected(true); // Default selection in graph view
        
        // Make buttons self-exclusive via ButtonGroup
        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(this.descendantButton);
        buttonGroup.add(this.ancestorButton);
        buttonGroup.add(this.twoWayButton);
        buttonGroup.add(this.graphButton);
        
        // Add buttons to toolbar
        this.toolbar.add(this.descendantButton);
        this.toolbar.add(this.ancestorButton);
        this.toolbar.add(this.twoWayButton);
        this.toolbar.add(Box.createHorizontalStrut(10));
        this.toolbar.add(this.graphButton);
        
        // Add action listeners
        this.descendantButton.addActionListener(e -> {
            if (this.descendantButton.isSelected() && this.listener != null)
            {
                this.listener.onChartTypeSelectedFromToolbar(GedComChartPanel.ChartType.DescendantChart);
            }
        });
        
        this.ancestorButton.addActionListener(e -> {
            if (this.ancestorButton.isSelected() && this.listener != null)
            {
                this.listener.onChartTypeSelectedFromToolbar(GedComChartPanel.ChartType.ParentChart);
            }
        });
        
        this.twoWayButton.addActionListener(e -> {
            if (this.twoWayButton.isSelected() && this.listener != null)
            {
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
        this.add(this.toolbar, BorderLayout.NORTH);
        this.add(this.graphPanel, BorderLayout.CENTER);
    }
    
    public void setGraphToolbarListener(GraphToolbarListener listener)
    {
        this.listener = listener;
    }
    
    /**
     * Returns the underlying graph panel.
     */
    public GedComGraph getGraphPanel()
    {
        return this.graphPanel;
    }
    
    /**
     * Sets the model on the underlying graph panel.
     */
    public void setModel(Individual individual)
    {
        this.graphPanel.setModel(individual);
    }
    
    /**
     * Sets the document model on the underlying graph panel.
     */
    public void setModel(iamd.gedcom.datamodel.Document document)
    {
        this.graphPanel.setModel(document);
    }
    
    /**
     * Sets the family model on the underlying graph panel.
     */
    public void setModel(iamd.gedcom.datamodel.Family family)
    {
        this.graphPanel.setModel(family);
    }
    
    /**
     * Reloads the model on the underlying graph panel.
     */
    public void reloadModel()
    {
        this.graphPanel.reloadModel();
    }
    
    /**
     * Adds a family selection listener.
     */
    public void addFamilySelectionListener(FamilySelectionListener listener)
    {
        this.graphPanel.addFamilySelectionListener(listener);
    }
    
    /**
     * Returns the selected individual in the graph panel.
     */
    public Individual getSelectedIndividual()
    {
        return this.graphPanel.getSelectedIndividual();
    }
    
    /**
     * Returns the selected family in the graph panel.
     */
    public iamd.gedcom.datamodel.Family getSelectedFamily()
    {
        return this.graphPanel.getSelectedFamily();
    }
    
    /**
     * Sets the tool tip text.
     */
    public void setToolTipText(String text)
    {
        this.graphPanel.setToolTipText(text);
    }
    
    /**
     * Exports the graph as an image.
     */
    public void exportAsImage(java.io.File file)
    {
        this.graphPanel.exportAsImage(file);
    }
}
