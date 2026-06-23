package iamd.gedcom.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import iamd.gedcom.datamodel.Document;
import iamd.gedcom.datamodel.Individual;
import iamd.gedcom.datamodel.MediaObject;
import iamd.gedcom.datamodel.MediaObjectReference;
import iamd.rsrc.Resources;
import iamd.ui.BorderListPanelGenerator;
import iamd.ui.RowPanelList;
import iamd.ui.RowPanelListListener;

@SuppressWarnings("serial")
public class MediaObjectEditorPanel extends EditorPanel
{
    final private JFrame frame;
    
    final private ArrayList<FamilySelectionListener> familySelectionListeners       = new ArrayList<FamilySelectionListener>();
    final private ArrayList<GedComModifiedListener>  gedcomModifiedListeners        = new ArrayList<GedComModifiedListener>();
    final private ArrayList<Runnable>               mediaObjectChangedListeners    = new ArrayList<Runnable>();
    
    final private JPanel infoPanel = new JPanel(new BorderLayout());
    
    private MediaObject mediaObject;

    public MediaObjectEditorPanel(JFrame frame)
    {
        this.frame = frame;
        
        this.setLayout(new BorderLayout());
        
        BorderListPanelGenerator globalPanelGenerator = new BorderListPanelGenerator(BorderLayout.NORTH);
        
        globalPanelGenerator.add(this.infoPanel);
        
        JPanel panel = globalPanelGenerator.extractPanel();
        panel.setBorder(new EmptyBorder(2, 2, 2, 2));
        panel.setBackground(this.getBackground());
        panel.setPreferredSize(new Dimension(300, panel.getPreferredSize().height));
        
        this.add(panel);

        this.addComponentListener(new ComponentAdapter()
        {
            @Override
            public void componentShown(ComponentEvent e)
            {
                MediaObjectEditorPanel.this.invalidate();
                MediaObjectEditorPanel.this.updateUI();
                MediaObjectEditorPanel.this.repaint();
            }

            @Override
            public void componentResized(ComponentEvent e)
            {
                MediaObjectEditorPanel.this.invalidate();
                MediaObjectEditorPanel.this.updateUI();
                MediaObjectEditorPanel.this.repaint();
            }
        });
    }

    public void addFamilySelectionListener(FamilySelectionListener listener)
    {
        this.familySelectionListeners.add(listener);
    }

    public void addAttributeEditorListener(GedComModifiedListener listener)
    {
        this.gedcomModifiedListeners.add(listener);
    }

    /**
     * Registers a callback that is invoked whenever the panel modifies the
     * links between individuals and the displayed media object (unlink or
     * link). Used so that other panels (e.g. the rectangle overlay in the
     * MediaObjectDisplayPanel) can refresh their view.
     */
    public void addMediaObjectChangedListener(Runnable listener)
    {
        this.mediaObjectChangedListeners.add(listener);
    }

    private void fireMediaObjectChanged()
    {
        for (Runnable listener : this.mediaObjectChangedListeners)
            listener.run();
    }

    public MediaObject getMediaObject()
    {
        return this.mediaObject;
    }

    public void setModel(MediaObject mediaObject)
    {
        this.mediaObject = mediaObject;
        
        this.infoPanel.removeAll();
        
        if (mediaObject == null)
        {
            this.invalidate();
            this.updateUI();
            return;
        }
        
        // Find all individuals that have references to this media object
        Document document = mediaObject.getDocument();
        ArrayList<Individual> linkedIndividuals = new ArrayList<>();
        Map<Individual, MediaObjectReference> individualRefs = new HashMap<>();
        
        for (Individual ind : document.listIndividuals())
        {
            for (MediaObjectReference ref : ind.OBJE)
            {
                if (ref.mediaObject == mediaObject)
                {
                    linkedIndividuals.add(ind);
                    individualRefs.put(ind, ref);
                    break;
                }
            }
        }
        
        BorderListPanelGenerator infoGenerator = new BorderListPanelGenerator(BorderLayout.NORTH);
        
        // Add media object info
        infoGenerator.add(createTopBorder(newJLabel("Media Object")));
        infoGenerator.add(createTopBorder(newReadonlyJTextField(mediaObject.getDisplayLabel())));
        
        // Add list of linked individuals. Enable delete (unlink) buttons on each row,
        // and offer an "add" button at the lower part of the panel to link new individuals.
        RowPanelList<IndividualRowPanel> individualsRowPanelList = 
                new RowPanelList<IndividualRowPanel>(Messages.getString("MediaObjectEditorPanel.linkedIndividuals"),
                        GedComRowPanelList.getIndividualRowPanelList(linkedIndividuals, false, true),
                        Messages.getString("MediaObjectEditorPanel.addIndividual"));
        
        individualsRowPanelList.addRowPanelListListener(new RowPanelListListener<IndividualRowPanel>()
        {
            @Override
            public void rowPanelClicked(IndividualRowPanel rowPanel)
            {
                for (FamilySelectionListener listener : MediaObjectEditorPanel.this.familySelectionListeners)
                    listener.individualClicked(rowPanel.getIndividual());
            }

            @Override
            public void rowPanelMovedUp(IndividualRowPanel rowPanel)
            {
                throw new AssertionError("This code should never be reached!");
            }

            @Override
            public void rowPanelMovedDown(IndividualRowPanel rowPanel)
            {
                throw new AssertionError("This code should never be reached!");
            }

            @Override
            public void rowPanelDeleted(IndividualRowPanel rowPanel)
            {
                // Unlink the selected individual from this media object
                Individual individual = rowPanel.getIndividual();
                MediaObjectReference ref = individualRefs.get(individual);
                
                if (ref != null)
                {
                    individual.removeMediaObject(ref);

                    MediaObjectEditorPanel.this.fireMediaObjectChanged();

                    MediaObjectEditorPanel.this.setModel(mediaObject);

                    for (GedComModifiedListener listener : MediaObjectEditorPanel.this.gedcomModifiedListeners)
                        listener.attributeModified(individual);
                }
            }

            @Override
            public void rowPanelNew()
            {
                // Link a new (or existing) individual to this media object
                Document document = mediaObject.getDocument();
                
                IndividualSelectorDialog individualSelector = new IndividualSelectorDialog(
                        MediaObjectEditorPanel.this.frame, 
                        Messages.getString("MediaObjectEditorPanel.addIndividual") + " - " + mediaObject.getDisplayLabel() + "...", //$NON-NLS-1$ //$NON-NLS-2$
                        document, true);
                
                Individual newIndividual = individualSelector.getSelectedIndividual();

                if (newIndividual != null)
                {
                    // Skip if this individual is already linked to the media object
                    boolean alreadyLinked = false;
                    for (MediaObjectReference existingRef : newIndividual.OBJE)
                    {
                        if (existingRef.mediaObject == mediaObject)
                        {
                            alreadyLinked = true;
                            break;
                        }
                    }
                    
                    if (!alreadyLinked)
                    {
                        MediaObjectReference newMediaObjectRef = new MediaObjectReference(document, mediaObject);
                        newIndividual.addNewMediaObjectReference(newMediaObjectRef);

                        MediaObjectEditorPanel.this.fireMediaObjectChanged();

                        MediaObjectEditorPanel.this.setModel(mediaObject);

                        for (GedComModifiedListener listener : MediaObjectEditorPanel.this.gedcomModifiedListeners)
                            listener.attributeModified(newIndividual);

                        for (FamilySelectionListener listener : MediaObjectEditorPanel.this.familySelectionListeners)
                            listener.individualClicked(newIndividual);
                    }
                }
            }
        });
        
        infoGenerator.add(createTopBorder(individualsRowPanelList));
        
        // Add a "Delete this media object" entry, mirroring the pattern used in
        // IndividualEditorPanel. The click removes the media object from the
        // document (and any references from individuals), then refreshes the
        // display panel and navigates back to the first individual.
        JLabel removeMediaObjectIcon = new JLabel(Resources.DeleteDisabledIcon);
        JLabel removeMediaObjectLabel = new JLabel(Messages.getString("MediaObjectEditorPanel.remove")); //$NON-NLS-1$

        MouseAdapter deleteMouseListener = new MouseAdapter()
        {
            @Override
            public void mouseEntered(MouseEvent e)
            {
                removeMediaObjectLabel.setForeground(Color.RED);
                removeMediaObjectIcon.setIcon(Resources.DeleteIcon);
            }

            @Override
            public void mouseExited(MouseEvent e)
            {
                removeMediaObjectLabel.setForeground(Color.BLACK);
                removeMediaObjectIcon.setIcon(Resources.DeleteDisabledIcon);
            }

            @Override
            public void mouseReleased(MouseEvent e)
            {
                MediaObject objectToDelete = MediaObjectEditorPanel.this.mediaObject;
                
                if (objectToDelete == null)
                    return;
                
                // Collect the individuals that currently reference this media object
                // before removing them, so we can fire attributeModified for each
                // of them afterwards.
                Document doc = objectToDelete.getDocument();
                ArrayList<Individual> affectedIndividuals = new ArrayList<>();
                for (Individual ind : doc.listIndividuals())
                {
                    for (MediaObjectReference ref : ind.OBJE)
                    {
                        if (ref.mediaObject == objectToDelete)
                        {
                            affectedIndividuals.add(ind);
                            break;
                        }
                    }
                }
                
                // Remove the media object from the document. This also removes
                // every MediaObjectReference pointing to it from each individual.
                objectToDelete.remove();
                
                // Clear the panel model BEFORE notifying listeners so that
                // getMediaObject() returns null to the mediaObjectChangedListener
                // and the display panel is reset accordingly.
                MediaObjectEditorPanel.this.setModel(null);
                
                MediaObjectEditorPanel.this.fireMediaObjectChanged();
                
                // Fire attributeModified for every affected individual so the
                // document is marked as modified and the affected individuals
                // get their CHAN attribute updated.
                for (Individual ind : affectedIndividuals)
                {
                    for (GedComModifiedListener listener : MediaObjectEditorPanel.this.gedcomModifiedListeners)
                        listener.attributeModified(ind);
                }
                
                // Navigate back to the first individual in the document, if any.
                // This avoids leaving the user stranded on an empty media object
                // editor panel after the deletion.
                java.util.Iterator<Individual> it = doc.listIndividuals().iterator();
                if (it.hasNext())
                {
                    Individual anyIndividual = it.next();
                    for (FamilySelectionListener listener : MediaObjectEditorPanel.this.familySelectionListeners)
                        listener.individualClicked(anyIndividual);
                }
                
                MediaObjectEditorPanel.this.invalidate();
                MediaObjectEditorPanel.this.updateUI();
                MediaObjectEditorPanel.this.repaint();
            }
        };

        removeMediaObjectIcon.addMouseListener(deleteMouseListener);
        removeMediaObjectLabel.addMouseListener(deleteMouseListener);
        
        JPanel removeMediaObjectPanel = new JPanel(new BorderLayout());
        removeMediaObjectPanel.add(removeMediaObjectLabel);
        removeMediaObjectPanel.add(removeMediaObjectIcon, BorderLayout.EAST);
        removeMediaObjectPanel.setBorder(new EmptyBorder(20, 0, 0, 0));
        
        infoGenerator.add(createTopBorder(removeMediaObjectPanel));
        
        JPanel infoContentPanel = infoGenerator.extractPanel();
        infoContentPanel.setBackground(this.getBackground());
        
        this.infoPanel.add(infoContentPanel);
        
        this.invalidate();
        this.updateUI();
    }
}
