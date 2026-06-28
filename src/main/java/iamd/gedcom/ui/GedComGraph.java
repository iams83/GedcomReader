package iamd.gedcom.ui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.TreeSet;

import javax.swing.JPanel;
import javax.swing.Timer;

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
                
            if (object == null)
            {
                for (FamilySelectionListener listener : GedComGraph.this.graphListeners)
                    listener.nothingHovered();
            }
            else if (object instanceof Individual)
            {
                for (FamilySelectionListener listener : GedComGraph.this.graphListeners)
                    listener.individualHovered((Individual) object);
            }
            else if (object instanceof Family)
            {
                for (FamilySelectionListener listener : GedComGraph.this.graphListeners)
                    listener.familyHovered((Family) object);
            }
            else
            {
                for (FamilySelectionListener listener : GedComGraph.this.graphListeners)
                    listener.nothingHovered();
            }
            
            this.mouseMoved = true;
        }

        @Override
        public void mouseDragged(MouseEvent e)
        {
            this.mouseMoved = true;
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
        }
    };
    
    final private ArrayList<FamilySelectionListener> graphListeners = new ArrayList<>();

    final private Graph graph = new MultiGraph("FamilyTree");

    final private String stylesheet;

    private Node dummyNode;
    
    private Document model = null;
    private Individual selectedIndividual = null;
    private Family     selectedFamily = null;
    
    public GedComGraph()
    {
        this.setLayout(new BorderLayout());
        
        Viewer viewer = new Viewer(this.graph, Viewer.ThreadingModel.GRAPH_IN_ANOTHER_THREAD);
        
        ViewPanel view = viewer.addDefaultView(false);
        
        this.add(view);
        
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

    public void exportAsImage(File selectedImage)
    {
        this.graph.addAttribute("ui.screenshot", selectedImage.getPath());
    }

}
