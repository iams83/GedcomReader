package iamd.gedcom.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.TreeSet;

import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;

import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.MultiGraph;
import org.graphstream.ui.graphicGraph.GraphicElement;
import org.graphstream.ui.swingViewer.ViewPanel;
import org.graphstream.ui.view.Viewer;
import org.graphstream.ui.view.ViewerPipe;

import iamd.gedcom.datamodel.Document;
import iamd.gedcom.datamodel.Family;
import iamd.gedcom.datamodel.Individual;
import iamd.gedcom.datamodel.Individual.Sex;
import iamd.gedcom.format.IdentifiedGedComNode;


public class GedComGraph extends JPanel
{
    static
    {
        System.setProperty("org.graphstream.ui.renderer", "org.graphstream.ui.j2dviewer.J2DGraphRenderer");
    }

    /**
     * Colors used by the floating hover label to identify the kind of node
     * currently under the cursor. They mirror the {@code fill-color} defined
     * in {@code GedComGraph.css} so the label and the node stay visually
     * consistent.
     */
    private static final Color HOVER_LABEL_COLOR_MALE    = new Color(0x1E90FF); // #1E90FF
    private static final Color HOVER_LABEL_COLOR_FEMALE  = new Color(0xFF1493); // #FF1493
    private static final Color HOVER_LABEL_COLOR_UNKNOWN = new Color(0x888888); // #888
    private static final Color HOVER_LABEL_COLOR_FAMILY  = new Color(0x00AA00); // #00AA00

    /**
     * Font for the floating hover label. Same family and weight as the
     * labels drawn by {@link MediaObjectDisplayPanel} so the two panels
     * look consistent.
     */
    private static final Font HOVER_LABEL_FONT = new Font("Arial", Font.BOLD, 12);

    /** Padding around the hover label text, in pixels. */
    private static final int HOVER_LABEL_PADDING_X = 6;
    private static final int HOVER_LABEL_PADDING_Y = 3;

    /** Pixel offset of the hover label from the cursor. */
    private static final int HOVER_LABEL_CURSOR_OFFSET = 12;

    class GraphMouseListener extends MouseAdapter
    {
        final private ViewPanel view;

        private boolean mouseMoved = false;
        
        public GraphMouseListener(ViewPanel view)
        {
            this.view = view;
        }
        
        @Override
        public void mouseMoved(MouseEvent e)
        {
            GraphicElement node = this.view.findNodeOrSpriteAt(e.getX(), e.getY());

            IdentifiedGedComNode object = null;
            
            if (node != null)
                object = GedComGraph.this.model.getObjectById(node.getId());
            
            // Show / move / hide the floating label next to the hovered node.
            // The hover no longer goes through FamilySelectionListener, so the
            // shared status bar stays untouched when the user is on the graph.
            GedComGraph.this.updateHoverLabel(object, e.getX(), e.getY());
            
            this.mouseMoved = true;
        }

        @Override
        public void mouseDragged(MouseEvent e)
        {
            this.mouseMoved = true;
            // Hide the label while the user is panning the graph so it does
            // not appear stuck on the previously hovered node.
            GedComGraph.this.hideHoverLabel();
        }
        
        @Override
        public void mouseReleased(MouseEvent e)
        {
            if (this.mouseMoved)
                return;
            
            GraphicElement node = this.view.findNodeOrSpriteAt(e.getX(), e.getY());

            IdentifiedGedComNode object = null;
            
            if (node != null)
                object = GedComGraph.this.model.getObjectById(node.getId());
            
            if (object != null)
            {
                if (object instanceof Individual)
                {
                    for (FamilySelectionListener listener : GedComGraph.this.graphListeners)
                        listener.individualClicked((Individual) object);
                }
                else if (object instanceof Family)
                {
                    for (FamilySelectionListener listener : GedComGraph.this.graphListeners)
                        listener.familyClicked((Family) object);
                }
            }
        }
        
        @Override
        public void mousePressed(MouseEvent e)
        {
            this.mouseMoved = false;
            // Hide the label as soon as a drag could start, so the user does
            // not see the label pinned while panning the graph.
            GedComGraph.this.hideHoverLabel();
        }
        
        @Override
        public void mouseExited(MouseEvent e)
        {
            GedComGraph.this.hideHoverLabel();
        }
    };
    
    final private ArrayList<FamilySelectionListener> graphListeners = new ArrayList<>();

    final private Graph graph = new MultiGraph("FamilyTree");

    final private String stylesheet;

    private Node dummyNode;
    
    private Document model = null;
    private Individual selectedIndividual = null;
    private Family     selectedFamily = null;
    
    /**
     * Floating label that appears next to the hovered node, replacing the
     * old status-bar hover text. Styled like the labels drawn by
     * {@link MediaObjectDisplayPanel}: opaque colored background, white bold
     * text, small padding around.
     *
     * <p>{@link #contains(int, int)} is overridden so the label does not
     * absorb mouse events — the cursor passing over the label still
     * delivers {@code mouseMoved} events to the ViewPanel underneath.</p>
     */
    private final JLabel hoverLabel = new JLabel()
    {
        @Override
        public boolean contains(int x, int y)
        {
            return false;
        }
    };
    
    /**
     * Layered pane holding the graph view underneath and the hover label on
     * top. Using a layered pane lets the label float anywhere over the view
     * without being clipped by the ViewPanel's painting surface.
     */
    private final JLayeredPane layeredPane = new JLayeredPane();
    
    public GedComGraph()
    {
        super();
        this.setLayout(new BorderLayout());
        
        Viewer viewer = new Viewer(this.graph, Viewer.ThreadingModel.GRAPH_IN_ANOTHER_THREAD);
        
        ViewPanel view = viewer.addDefaultView(false);
        
        // Configure the floating hover label. Returning false from
        // contains() makes the label transparent to mouse hit-testing, so
        // mouse-moved events keep flowing to the ViewPanel underneath even
        // when the cursor happens to be over the label area.
        this.hoverLabel.setFont(HOVER_LABEL_FONT);
        this.hoverLabel.setForeground(Color.WHITE);
        this.hoverLabel.setOpaque(true);
        this.hoverLabel.setVisible(false);
        this.hoverLabel.setBorder(new EmptyBorder(HOVER_LABEL_PADDING_Y, HOVER_LABEL_PADDING_X,
                                                   HOVER_LABEL_PADDING_Y, HOVER_LABEL_PADDING_X));
        this.hoverLabel.setHorizontalAlignment(JLabel.LEFT);
        this.hoverLabel.setFocusable(false);
        
        // Stack the view in the default layer and the hover label on top in
        // the palette layer. JLayeredPane has no layout manager by default,
        // so the view's bounds are kept in sync manually on resize below.
        this.layeredPane.add(view, JLayeredPane.DEFAULT_LAYER);
        this.layeredPane.add(this.hoverLabel, JLayeredPane.PALETTE_LAYER);
        
        this.layeredPane.addComponentListener(new ComponentAdapter()
        {
            @Override
            public void componentResized(ComponentEvent e)
            {
                view.setBounds(0, 0, GedComGraph.this.layeredPane.getWidth(),
                               GedComGraph.this.layeredPane.getHeight());
            }
        });
        
        this.add(this.layeredPane, BorderLayout.CENTER);
        
        viewer.enableAutoLayout();
        
        ViewerPipe viewerPipe = viewer.newViewerPipe();
        
        GraphMouseListener mouseListener = new GraphMouseListener(view);
        
        view.addMouseMotionListener(mouseListener);

        view.addMouseListener(mouseListener);
        
        viewerPipe.addSink(this.graph);
        
        Timer t = new Timer(20, new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                viewerPipe.pump();

                    if (GedComGraph.this.dummyNode == null)
                    {
                        GedComGraph.this.dummyNode = GedComGraph.this.graph.addNode("T");
                        
                        GedComGraph.this.dummyNode.setAttribute("ui.class", "invisible");
                    }
                    else
                    {
                        GedComGraph.this.graph.removeNode(GedComGraph.this.dummyNode);
                    
                        GedComGraph.this.dummyNode = null;
                    }
                }
        });
        
        t.start();

        String stylesheet = "";
        
        try (BufferedReader br = new BufferedReader(new InputStreamReader(GedComGraph.class.getResourceAsStream("GedComGraph.css"))))
        {
            String line = null;
            
            while ((line = br.readLine()) != null)
                stylesheet += line + "\n";
        }
        catch (IOException e1)
        {
            throw new AssertionError(e1);
        }
        
        this.stylesheet = stylesheet;
    }
    
    TreeSet<String> currentNodes = new TreeSet<>();
    TreeSet<String> currentEdges = new TreeSet<>();

    public void setModel(Document model)
    {
        this.model = model;
        this.selectedIndividual = null;
        this.selectedFamily = null;
        
        this.graph.clear();
        this.currentNodes.clear();
        this.currentEdges.clear();
        this.dummyNode = null;
        
        this.graph.addAttribute("ui.quality");
        this.graph.addAttribute("ui.antialias");
        this.graph.addAttribute("ui.stylesheet", this.stylesheet);
        
        this.reloadModel();
    }
    
    public void setModel(Individual individual)
    {
        this.selectedIndividual = individual;
        
        this.selectedFamily = null;
        
        this.reloadModel();
    }

    public Individual getSelectedIndividual()
    {
        return this.selectedIndividual;
    }

    public void setModel(Family family)
    {
        this.selectedFamily = family;
        
        this.selectedIndividual = null;
        
        this.reloadModel();
    }
    
    public Family getSelectedFamily()
    {
        return this.selectedFamily;
    }
    
    public void reloadModel()
    {
        TreeSet<String> nodesToRemove = new TreeSet<>(this.currentNodes);
        TreeSet<String> edgesToRemove = new TreeSet<>(this.currentEdges);
        
        for (Individual individual : this.model.listIndividuals())
        {
            String indiId = this.model.getID(individual);

            Node indiNode;

            if (!this.currentNodes.contains(indiId))
            {
                indiNode = this.graph.addNode(indiId);
        
                this.currentNodes.add(indiId);
            }
            else
        {
                indiNode = this.graph.getNode(indiId); 
                
                nodesToRemove.remove(indiId);
            }

            indiNode.setAttribute("ui.class",
                    (individual == this.selectedIndividual ? "selected_" : "") +
                    (individual.SEX == null ? "unknown" : (individual.SEX == Sex.M ? "male" : "female")));
        }

        for (Family family : this.model.listFamilies())
            {
            String familyId = this.model.getID(family);

            Node familyNode;
            
            if (!this.currentNodes.contains(familyId))
                {
                familyNode = this.graph.addNode(familyId);
            
                this.currentNodes.add(familyId);
        }
        else
        {
                familyNode = this.graph.getNode(familyId);
                
            nodesToRemove.remove(familyId);
        }
        
            familyNode.setAttribute("ui.class", (this.selectedFamily == family ? "selected_" : "") + "family");
            
        Individual spouse1 = family.getSpouse1();
        
        if (spouse1 != null)
        {
                String spouseId = this.model.getID(spouse1);

                String edgeId = familyId + ":" + spouseId;
    
                Edge spouse1Edge;
                
                if (!this.currentEdges.contains(edgeId))
                {
                    spouse1Edge = this.graph.addEdge(edgeId, spouseId, familyId);
                    
                    this.currentEdges.add(edgeId);
                }
                else
                {
                    spouse1Edge = this.graph.getEdge(edgeId);
                    
                    edgesToRemove.remove(edgeId);
                }

                spouse1Edge.setAttribute("ui.class", "parent");
        }
        
        Individual spouse2 = family.getSpouse2();
        
        if (spouse2 != null)
        {
                String spouse2Id = this.model.getID(spouse2);
                
                String edgeId = familyId + ":" + spouse2Id;
    
                Edge spouse2Edge;
                
                if (!this.currentEdges.contains(edgeId))
                        {
                    spouse2Edge = this.graph.addEdge(edgeId, spouse2Id, familyId);
                    
                    this.currentEdges.add(edgeId);
                }
                else
                {
                    spouse2Edge = this.graph.getEdge(edgeId);
                    
                    edgesToRemove.remove(edgeId);
                }

                spouse2Edge.setAttribute("ui.class", "parent");
        }
        
        for (Individual child : family.getChildren())
        {
            String childId = this.model.getID(child);
            
            String edgeId = familyId + ":" + childId;
    
                Edge childEdge;
                
                if (!this.currentEdges.contains(edgeId))
                {
                    childEdge = this.graph.addEdge(edgeId, childId, familyId);
    
                    this.currentEdges.add(edgeId);
            }
            else
            {
                    childEdge = this.graph.getEdge(edgeId);
                    
                edgesToRemove.remove(edgeId);
            }
                    
                childEdge.setAttribute("ui.class", "child");
                }
        }
        
        for (String edgeToRemove : edgesToRemove)
        {
            this.graph.removeEdge(edgeToRemove);
            
            this.currentEdges.remove(edgeToRemove);
        }

        for (String nodeToRemove : nodesToRemove)
        {
            this.graph.removeNode(nodeToRemove);
            
            this.currentNodes.remove(nodeToRemove);
        }
    }

    public void addFamilySelectionListener(FamilySelectionListener graphListener)
    {
        this.graphListeners.add(graphListener);
    }

    /**
     * Shows, moves, recolors and retexts the floating hover label so it
     * sits next to the node currently under the cursor. If
     * {@code hoveredObject} is {@code null} (or not an Individual/Family),
     * the label is hidden.
     *
     * <p>The label background color matches the node's CSS fill color, so
     * hovering an Individual male lights the label blue, a female pink,
     * an unknown sex gray and a Family green — the same palette as in
     * {@code GedComGraph.css}.</p>
     */
    private void updateHoverLabel(IdentifiedGedComNode hoveredObject, int mouseX, int mouseY)
    {
        String text;
        Color color;
        
        if (hoveredObject instanceof Individual)
        {
            Individual individual = (Individual) hoveredObject;
            text = individual.getName();
            
            Sex sex = individual.SEX;
            if (sex == Sex.M)
                color = HOVER_LABEL_COLOR_MALE;
            else if (sex == Sex.F)
                color = HOVER_LABEL_COLOR_FEMALE;
            else
                color = HOVER_LABEL_COLOR_UNKNOWN;
        }
        else if (hoveredObject instanceof Family)
        {
            Family family = (Family) hoveredObject;
            text = "Family: " + family.getSpouseNames();
            color = HOVER_LABEL_COLOR_FAMILY;
        }
        else
        {
            this.hideHoverLabel();
            return;
        }
        
        this.hoverLabel.setText(text);
        this.hoverLabel.setBackground(color);
        
        // Match the label size to its new text so getX/getY math below is
        // based on the freshly measured bounds.
        java.awt.Dimension size = this.hoverLabel.getPreferredSize();
        this.hoverLabel.setSize(size);
        
        // Position the label next to the cursor by default...
        int labelX = mouseX + HOVER_LABEL_CURSOR_OFFSET;
        int labelY = mouseY + HOVER_LABEL_CURSOR_OFFSET;
        
        // ...but flip it to the upper-left if it would otherwise fall off
        // the right or bottom edge of the panel.
        if (labelX + size.width > this.layeredPane.getWidth())
            labelX = mouseX - size.width - 4;
        if (labelY + size.height > this.layeredPane.getHeight())
            labelY = mouseY - size.height - 4;
        if (labelX < 0)
            labelX = 0;
        if (labelY < 0)
            labelY = 0;
        
        this.hoverLabel.setLocation(labelX, labelY);
        this.hoverLabel.setVisible(true);
    }
    
    /**
     * Hides the floating hover label. Called when the cursor leaves the
     * graph or when the user starts dragging the view.
     */
    private void hideHoverLabel()
    {
        if (this.hoverLabel.isVisible())
            this.hoverLabel.setVisible(false);
    }

    public void exportAsImage(File selectedImage)
    {
        this.graph.addAttribute("ui.screenshot", selectedImage.getPath());
    }

}
