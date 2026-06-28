package iamd.gedcom.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.reflect.Field;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import iamd.gedcom.datamodel.Document;
import iamd.gedcom.datamodel.Individual;
import iamd.gedcom.datamodel.MediaObject;
import iamd.gedcom.datamodel.MediaObjectReference;
import iamd.gedcom.ui.editors.LongTextEditor;
import iamd.rsrc.Resources;
import iamd.ui.AttributeEditorListener;
import iamd.ui.BorderListPanelGenerator;
import iamd.ui.RowPanelList;
import iamd.ui.RowPanelListListener;
import iamd.ui.TextLineEditor;

@SuppressWarnings("serial")
public class MediaObjectEditorPanel extends EditorPanel
{
    final private JFrame frame;
    
    final private ArrayList<FamilySelectionListener> familySelectionListeners       = new ArrayList<FamilySelectionListener>();
    final private ArrayList<GedComModifiedListener>  gedcomModifiedListeners        = new ArrayList<GedComModifiedListener>();
    final private ArrayList<Runnable>               mediaObjectChangedListeners    = new ArrayList<Runnable>();
    
    final private JPanel infoPanel = new JPanel(new BorderLayout());

    final private TextLineEditor titleEditor = new TextLineEditor();
    final private TextLineEditor formEditor  = new TextLineEditor();
    final private TextLineEditor fileEditor  = new TextLineEditor();
    final private LongTextEditor noteEditor  = new LongTextEditor(4);

    // Holds the value of MediaObject.FILE as it was before the most recent
    // user edit, so the rename logic can locate the actual file on disk even
    // after the AttributeBinder has already updated the FILE attribute.
    private final AtomicReference<String> previousFileRef = new AtomicReference<String>(null);

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

        // Listen for edits on the title / format editors so the document is
        // marked as modified (CHAN attribute is updated on save).
        AttributeEditorListener<String> attributeEditorListener = new AttributeEditorListener<String>()
        {
            @Override
            public void attributeModified(Object editingObject, java.lang.reflect.Field editingField, String value)
            {
                if (MediaObjectEditorPanel.this.mediaObject == null)
                    return;

                for (GedComModifiedListener listener : MediaObjectEditorPanel.this.gedcomModifiedListeners)
                    listener.attributeModified(MediaObjectEditorPanel.this.mediaObject);

                MediaObjectEditorPanel.this.invalidate();
                MediaObjectEditorPanel.this.updateUI();
                MediaObjectEditorPanel.this.repaint();
            }
        };

        this.titleEditor.addAttributeEditionListener(attributeEditorListener);
        this.formEditor .addAttributeEditionListener(attributeEditorListener);

        // Listener that handles edits on the FILE editor. When the user
        // confirms a new file path, this listener:
        //  1. Attempts to rename the actual file on disk (resolving relative
        //     paths against the Gedcom document directory).
        //  2. On success, fires the usual "document modified" listeners so the
        //     change is persisted on save.
        //  3. On failure, reverts the FILE attribute to its previous value,
        //     refreshes the editor display, and shows the user a descriptive
        //     error message.
        // The AttributeBinder has already set FILE to the new value before
        // this listener runs, so the rename is performed against the value
        // previously cached in previousFileRef.
        AttributeEditorListener<String> fileEditorListener = new AttributeEditorListener<String>()
        {
            @Override
            public void attributeModified(Object editingObject, Field editingField, String value)
            {
                MediaObject mo = MediaObjectEditorPanel.this.mediaObject;
                if (mo == null || editingObject != mo)
                    return;

                String newValue = value;
                String oldValue = MediaObjectEditorPanel.this.previousFileRef.get();

                String renameError = mo.renameMediaFile(oldValue, newValue);

                if (renameError == null)
                {
                    // Success: keep the new value and notify listeners.
                    MediaObjectEditorPanel.this.previousFileRef.set(newValue);

                    for (GedComModifiedListener listener : MediaObjectEditorPanel.this.gedcomModifiedListeners)
                        listener.attributeModified(mo);

                    // Refresh the display panel so the (possibly renamed) file
                    // is reloaded from its new location.
                    MediaObjectEditorPanel.this.fireMediaObjectChanged();
                }
                else
                {
                    // Failure: revert FILE, reset the editor display to the
                    // previous relative path and inform the user.
                    mo.FILE = oldValue;
                    MediaObjectEditorPanel.this.previousFileRef.set(oldValue);

                    String oldDisplay = mo.getRelativeFilePath();
                    if (oldDisplay == null)
                        oldDisplay = oldValue != null ? oldValue : "";
                    MediaObjectEditorPanel.this.fileEditor.initializeValue(oldDisplay);

                    String keptMessage = MessageFormat.format(
                            Messages.getString("MediaObjectEditorPanel.fileRenameKept"),
                            oldDisplay);
                    String errorTitle = Messages.getString("MediaObjectEditorPanel.fileRenameErrorTitle");
                    String errorHeader = Messages.getString("MediaObjectEditorPanel.fileRenameError");

                    JOptionPane.showMessageDialog(
                            MediaObjectEditorPanel.this.frame,
                            errorHeader + "\n" + renameError + "\n\n" + keptMessage,
                            errorTitle,
                            JOptionPane.ERROR_MESSAGE);
                }

                MediaObjectEditorPanel.this.invalidate();
                MediaObjectEditorPanel.this.updateUI();
                MediaObjectEditorPanel.this.repaint();
            }
        };
        this.fileEditor.addAttributeEditionListener(fileEditorListener);

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
        
        // Add media object info - editable title, format and file path. The
        // file path editor behaves specially: editing the field attempts to
        // rename the actual file on disk and reverts to the previous value
        // if the rename fails (see the file editor listener installed in the
        // constructor).
        this.titleEditor.bindValue(mediaObject, "TITL");
        this.formEditor .bindValue(mediaObject, "FORM");

        // Bind the file editor and override the displayed text with the
        // relative path so the user sees the same value they used to, but
        // can edit it freely. The previousFileRef is updated to the current
        // (raw) FILE attribute so the rename logic can locate the source
        // file on disk.
        this.fileEditor.bindValue(mediaObject, "FILE");
        String initialRelativePath = mediaObject.getRelativeFilePath();
        this.fileEditor.initializeValue(
                initialRelativePath != null ? initialRelativePath : "");
        this.previousFileRef.set(mediaObject.FILE);

        this.noteEditor.setDocument(mediaObject.getDocument());
        this.noteEditor.bindValue(mediaObject, "NOTE");
        infoGenerator.add(createTopBorder(newJLabel(Messages.getString("MediaObjectEditorPanel.title"))));
        infoGenerator.add(createTopBorder(this.titleEditor));

        infoGenerator.add(createTopBorder(newJLabel(Messages.getString("MediaObjectEditorPanel.format"))));
        infoGenerator.add(createTopBorder(this.formEditor));

        infoGenerator.add(createTopBorder(newJLabel(Messages.getString("MediaObjectEditorPanel.file"))));
        infoGenerator.add(createTopBorder(this.fileEditor));
        infoGenerator.add(createTopBorder(newJLabel(Messages.getString("MediaObjectEditorPanel.note"))));
        infoGenerator.add(createTopBorder(this.noteEditor));
        
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
        removeMediaObjectIcon.setOpaque(true);
        JLabel removeMediaObjectLabel = new JLabel(Messages.getString("MediaObjectEditorPanel.remove")); //$NON-NLS-1$
        removeMediaObjectLabel.setOpaque(true);

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
