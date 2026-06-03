package iamd.gedcom.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import iamd.gedcom.datamodel.Document;
import iamd.gedcom.datamodel.Family;
import iamd.gedcom.datamodel.Individual;
import iamd.gedcom.datamodel.MediaObject;
import iamd.gedcom.datamodel.MediaObject.MediaType;
import iamd.gedcom.datamodel.MediaObjectReference;
import iamd.gedcom.ui.editors.EventEditor;
import iamd.gedcom.ui.editors.LongTextEditor;
import iamd.gedcom.ui.editors.SexEditor;
import iamd.rsrc.Resources;
import iamd.ui.AttributeEditorListener;
import iamd.ui.BorderListPanelGenerator;
import iamd.ui.RowPanelList;
import iamd.ui.RowPanelListListener;
import iamd.ui.ScrollablePanel;
import iamd.ui.TextLineEditor;
import net.iharder.dnd.FileDrop;
import iamd.ui.ScrollablePanel.ScrollableSizeHint;

@SuppressWarnings("serial")
public class IndividualEditorPanel extends EditorPanel
{
    final private JFrame frame;
    
    final private ArrayList<FamilySelectionListener> familySelectionListeners = new ArrayList<FamilySelectionListener>();
    final private ArrayList<GedComModifiedListener>  gedcomModifiedListeners = new ArrayList<GedComModifiedListener>();
    
    final private TextLineEditor      name       = new TextLineEditor();
    final private TextLineEditor      surname    = new TextLineEditor();
    final private TextLineEditor      nick       = new TextLineEditor();
    final private SexEditor           sex        = new SexEditor();
    final private EventEditor         birth      = new EventEditor(Messages.getString("IndividualEditorPanel.birth"), "BIRT", false, this); //$NON-NLS-1$ //$NON-NLS-2$
    final private EventEditor         death      = new EventEditor(Messages.getString("IndividualEditorPanel.death"), "DEAT", true, this); //$NON-NLS-1$ //$NON-NLS-2$
    final private TextLineEditor      education  = new TextLineEditor();
    final private TextLineEditor      occupation = new TextLineEditor();
    final private TextLineEditor      health     = new TextLineEditor();
    final private LongTextEditor      note       = new LongTextEditor(4);
    
    final private JPanel parentFamilyInfoPanel = new JPanel(new BorderLayout());
    final private JPanel ownFamilyInfoPanel = new JPanel(new BorderLayout());
    final private JPanel mediaInfoPanel = new JPanel(new BorderLayout());
    final private ArrayList<JScrollPane> scrollPanes = new ArrayList<>();
    
    private Individual individual;
    
    @SuppressWarnings("unchecked")
    public IndividualEditorPanel(JFrame frame)
    {
        this.frame = frame;
        this.setLayout(new BorderLayout());

        @SuppressWarnings("rawtypes")
        AttributeEditorListener attributeEditorListener = new AttributeEditorListener()
        {
            @Override
            public void attributeModified(Object editingObject, Field editingField, Object value)
            {
                for (GedComModifiedListener listener : IndividualEditorPanel.this.gedcomModifiedListeners)
                    listener.attributeModified(IndividualEditorPanel.this.individual);
                
                IndividualEditorPanel.this.invalidate();
                IndividualEditorPanel.this.updateUI();
                IndividualEditorPanel.this.repaint();
            }
        };
        
        BorderListPanelGenerator globalPanelGenerator = new BorderListPanelGenerator(BorderLayout.NORTH);

        JLabel sexLabel = newJLabel(Messages.getString("IndividualEditorPanel.genre")); //$NON-NLS-1$
        sexLabel.setPreferredSize(this.sex.getPreferredSize());
        
        JPanel topPanel1 = new JPanel(new BorderLayout());
        topPanel1.add(newJLabel(Messages.getString("IndividualEditorPanel.name"))); //$NON-NLS-1$
        topPanel1.add(sexLabel, BorderLayout.EAST);
        
        JPanel topPanel2 = new JPanel(new BorderLayout());
        topPanel2.add(this.name);
        topPanel2.add(this.sex, BorderLayout.EAST);
        
        globalPanelGenerator.add(topPanel1);
        globalPanelGenerator.add(topPanel2);
        globalPanelGenerator.add(createTopBorder(newJLabel(Messages.getString("IndividualEditorPanel.surname")))); //$NON-NLS-1$
        globalPanelGenerator.add(this.surname);
        globalPanelGenerator.add(createTopBorder(newJLabel(Messages.getString("IndividualEditorPanel.alias")))); //$NON-NLS-1$
        globalPanelGenerator.add(this.nick);
        globalPanelGenerator.setBackground(this.getBackground());
        
        JLabel removeIndividualIcon = new JLabel(Resources.DeleteDisabledIcon);
        JLabel removeIndividualLabel = new JLabel(Messages.getString("IndividualEditorPanel.remove")); //$NON-NLS-1$

        MouseAdapter mouseListener = new MouseAdapter()
        {
            @Override
            public void mouseEntered(MouseEvent e)
            {
                removeIndividualLabel.setForeground(Color.RED);
                removeIndividualIcon.setIcon(Resources.DeleteIcon);
            }

            @Override
            public void mouseExited(MouseEvent e)
            {
                removeIndividualLabel.setForeground(Color.BLACK);
                removeIndividualIcon.setIcon(Resources.DeleteDisabledIcon);
            }

            @Override
            public void mouseReleased(MouseEvent e)
            {
                IndividualEditorPanel.this.individual.remove();
                
                Individual anyIndividual = IndividualEditorPanel.this.individual.getDocument().listIndividuals().iterator().next();
                
                for (GedComModifiedListener listener : IndividualEditorPanel.this.gedcomModifiedListeners)
                    listener.attributeModified(anyIndividual);
                
                for (FamilySelectionListener listener : IndividualEditorPanel.this.familySelectionListeners)
                    listener.individualClicked(anyIndividual);
                
                IndividualEditorPanel.this.invalidate();
                IndividualEditorPanel.this.updateUI();
                IndividualEditorPanel.this.repaint();
            }
        };
        
        removeIndividualIcon.addMouseListener(mouseListener);
        removeIndividualLabel.addMouseListener(mouseListener);
        
        JPanel removeIndividualPanel = new JPanel(new BorderLayout());
        removeIndividualPanel.add(removeIndividualLabel);
        removeIndividualPanel.add(removeIndividualIcon, BorderLayout.EAST);
        removeIndividualPanel.setBorder(new EmptyBorder(20, 0, 0, 0));
        
        BorderListPanelGenerator personalInfoGenerator = new BorderListPanelGenerator(BorderLayout.NORTH);
        personalInfoGenerator.add(this.birth);
        personalInfoGenerator.add(createTopBorder(this.death));
        personalInfoGenerator.add(createTopBorder(newJLabel(Messages.getString("IndividualEditorPanel.education"))));   personalInfoGenerator.add(this.education); //$NON-NLS-1$
        personalInfoGenerator.add(createTopBorder(newJLabel(Messages.getString("IndividualEditorPanel.occupation"))));  personalInfoGenerator.add(this.occupation); //$NON-NLS-1$
        personalInfoGenerator.add(createTopBorder(newJLabel(Messages.getString("IndividualEditorPanel.health"))));      personalInfoGenerator.add(this.health); //$NON-NLS-1$
        personalInfoGenerator.add(this.parentFamilyInfoPanel);
        personalInfoGenerator.add(this.ownFamilyInfoPanel);
        personalInfoGenerator.add(createTopBorder(newJLabel(Messages.getString("IndividualEditorPanel.note"))));        personalInfoGenerator.add(this.note); //$NON-NLS-1$
        personalInfoGenerator.add(removeIndividualPanel);
        JPanel personalInfoPanel = personalInfoGenerator.extractPanel();
        personalInfoPanel.setBackground(this.getBackground());
        personalInfoPanel.setBorder(new EmptyBorder(0, 5, 5, 5));
        
        ScrollablePanel personalInfoScrollablePane = new ScrollablePanel(new BorderLayout());
        personalInfoScrollablePane.setScrollableWidth(ScrollableSizeHint.FIT);
        personalInfoScrollablePane.add(personalInfoPanel);
        
        JScrollPane personalInfoScrollPane = new JScrollPane(personalInfoScrollablePane, 
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        
        BorderListPanelGenerator mediaInfoGenerator = new BorderListPanelGenerator(BorderLayout.NORTH);
        mediaInfoGenerator.add(this.mediaInfoPanel);

        JPanel mediaInfoPanel = mediaInfoGenerator.extractPanel();
        mediaInfoPanel.setBackground(this.getBackground());
        mediaInfoPanel.setBorder(new EmptyBorder(0, 5, 5, 5));
        
        ScrollablePanel mediaInfoScrollablePane = new ScrollablePanel(new BorderLayout());
        mediaInfoScrollablePane.setScrollableWidth(ScrollableSizeHint.FIT);
        mediaInfoScrollablePane.add(mediaInfoPanel);
        
        JScrollPane mediaInfoScrollPane = new JScrollPane(mediaInfoScrollablePane, 
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        
        this.scrollPanes.add(personalInfoScrollPane);
        this.scrollPanes.add(mediaInfoScrollPane);
        
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Personal", personalInfoScrollPane);
        tabbedPane.addTab("Media", mediaInfoScrollPane);
        
        JPanel panel = globalPanelGenerator.extractPanel(createTopBorder(tabbedPane));
        panel.setBackground(this.getBackground());
        panel.setBorder(new EmptyBorder(2, 2, 2, 2));
        panel.setPreferredSize(new Dimension(300, panel.getPreferredSize().height));
        
        this.add(panel);

        this.name      .addAttributeEditionListener(attributeEditorListener);
        this.sex       .addAttributeEditionListener(attributeEditorListener);
        this.surname   .addAttributeEditionListener(attributeEditorListener);
        this.nick      .addAttributeEditionListener(attributeEditorListener);
        this.birth     .addAttributeEditionListener(attributeEditorListener);
        this.death     .addAttributeEditionListener(attributeEditorListener);
        this.education .addAttributeEditionListener(attributeEditorListener);
        this.occupation.addAttributeEditionListener(attributeEditorListener);
        this.health    .addAttributeEditionListener(attributeEditorListener);
        this.note      .addAttributeEditionListener(attributeEditorListener);
        
        this.addComponentListener(new ComponentAdapter()
        {
            @Override
            public void componentShown(ComponentEvent e)
            {
                IndividualEditorPanel.this.invalidate();
                IndividualEditorPanel.this.updateUI();
                IndividualEditorPanel.this.repaint();
            }
            
            @Override
            public void componentResized(ComponentEvent e)
            {
                IndividualEditorPanel.this.invalidate();
                IndividualEditorPanel.this.updateUI();
                IndividualEditorPanel.this.repaint();
            }
        });
    }
    
    public void paint(Graphics g)
    {
        g.clearRect(0, 0, getWidth(), getHeight());
        
        super.paint(g);
    }

    public void addAttributeEditorListener(GedComModifiedListener listener)
    {
        this.gedcomModifiedListeners.add(listener);
    }

    public void addFamilySelectionListener(FamilySelectionListener listener)
    {
        this.familySelectionListeners.add(listener);
    }

    public void setModel(Individual individual)
    {
        this.individual = individual;
        
        this.birth.setDocument(individual.getDocument());
        this.death.setDocument(individual.getDocument());
        this.note .setDocument(individual.getDocument());
        
        this.name      .bindValue(individual.NAME, "name"); //$NON-NLS-1$
        this.sex       .bindValue(individual, "SEX"); //$NON-NLS-1$
        this.surname   .bindValue(individual.NAME, "surname"); //$NON-NLS-1$
        this.nick      .bindValue(individual.NAME, "nick"); //$NON-NLS-1$
        
        this.birth     .bindValue(individual, "BIRT"); //$NON-NLS-1$
        this.death     .bindValue(individual, "DEAT");
        this.education .bindValue(individual, "EDUC"); //$NON-NLS-1$
        this.occupation.bindValue(individual, "OCCU"); //$NON-NLS-1$
        this.health    .bindValue(individual, "HEAL"); //$NON-NLS-1$
        this.note      .bindValue(individual, "NOTE");

        RowPanelList<FamilyRowPanel> parentFamilyRowPanelList = 
                new RowPanelList<FamilyRowPanel>(Messages.getString("IndividualEditorPanel.parentfamily"),  //$NON-NLS-1$
                        GedComRowPanelList.getFamilyChildRowPanelList(individual.FAMC, individual, 
                                Messages.getString("IndividualEditorPanel.parents"), Messages.getString("IndividualEditorPanel.siblings"), false, true), Messages.getString("IndividualEditorPanel.addparent")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        
        parentFamilyRowPanelList.addRowPanelListListener(new RowPanelListListener<FamilyRowPanel>()
        {
            @Override
            public void rowPanelClicked(FamilyRowPanel rowPanel)
            {
                for (FamilySelectionListener listener : IndividualEditorPanel.this.familySelectionListeners)
                    listener.individualClicked(rowPanel.getFamily().getMember());
                
                for (FamilySelectionListener listener : IndividualEditorPanel.this.familySelectionListeners)
                    listener.familyClicked(rowPanel.getFamily());
            }

            @Override
            public void rowPanelMovedUp(FamilyRowPanel rowPanel)
            {
                throw new AssertionError("This code should never be reached!");
            }

            @Override
            public void rowPanelMovedDown(FamilyRowPanel rowPanel)
            {
                throw new AssertionError("This code should never be reached!"); //$NON-NLS-1$
            }

            @Override
            public void rowPanelDeleted(FamilyRowPanel rowPanel)
            {
                rowPanel.getFamily().removeChild(individual);

                IndividualEditorPanel.this.setModel(individual);

                for (GedComModifiedListener listener : IndividualEditorPanel.this.gedcomModifiedListeners)
                    listener.attributeModified(individual);

                for (GedComModifiedListener listener : IndividualEditorPanel.this.gedcomModifiedListeners)
                    listener.attributeModified(rowPanel.getFamily());
            }

            @Override
            public void rowPanelNew()
            {
                Document document = individual.getDocument();
                
                FamilySelectorDialog individualSelector = new FamilySelectorDialog(
                        IndividualEditorPanel.this.frame, 
                        Messages.getString("IndividualEditorPanel.newparent") + individual.getName() + "...",  //$NON-NLS-1$ //$NON-NLS-2$
                        document, true);
                
                Family family = individualSelector.getSelectedFamily();

                if (family != null)
                {
                    family.addChild(individual);

                    for (FamilySelectionListener listener : IndividualEditorPanel.this.familySelectionListeners)
                        listener.familyClicked(family);

                    for (GedComModifiedListener listener : IndividualEditorPanel.this.gedcomModifiedListeners)
                        listener.attributeModified(individual);

                    for (GedComModifiedListener listener : IndividualEditorPanel.this.gedcomModifiedListeners)
                        listener.attributeModified(family);
                }
            }
        });
        
        this.parentFamilyInfoPanel.removeAll();
        this.parentFamilyInfoPanel.add(createTopBorder(parentFamilyRowPanelList));
        
        RowPanelList<FamilyRowPanel> ownFamilyRowPanelList = 
                new RowPanelList<FamilyRowPanel>(Messages.getString("IndividualEditorPanel.ownfamily"),  //$NON-NLS-1$
                        GedComRowPanelList.getFamilyRowPanelList(individual.getFamilies(), individual, 
                                Messages.getString("IndividualEditorPanel.spouse"), Messages.getString("IndividualEditorPanel.children"), true, true), Messages.getString("IndividualEditorPanel.addspouse")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        
        ownFamilyRowPanelList.addRowPanelListListener(new RowPanelListListener<FamilyRowPanel>()
        {
            @Override
            public void rowPanelClicked(FamilyRowPanel rowPanel)
            {
                for (FamilySelectionListener listener : IndividualEditorPanel.this.familySelectionListeners)
                    listener.familyClicked(rowPanel.getFamily());
            }

            @Override
            public void rowPanelMovedUp(FamilyRowPanel rowPanel)
            {
                individual.setIndividualOlderFamily(rowPanel.getFamily());
                
                IndividualEditorPanel.this.setModel(individual);

                for (GedComModifiedListener listener : IndividualEditorPanel.this.gedcomModifiedListeners)
                    listener.attributeModified(individual);
            }

            @Override
            public void rowPanelMovedDown(FamilyRowPanel rowPanel)
            {
                individual.setIndividualYoungerFamily(rowPanel.getFamily());
                
                IndividualEditorPanel.this.setModel(individual);

                for (GedComModifiedListener listener : IndividualEditorPanel.this.gedcomModifiedListeners)
                    listener.attributeModified(individual);
            }

            @Override
            public void rowPanelDeleted(FamilyRowPanel rowPanel)
            {
                rowPanel.getFamily().removeSpouse(individual);

                IndividualEditorPanel.this.setModel(individual);

                for (GedComModifiedListener listener : IndividualEditorPanel.this.gedcomModifiedListeners)
                    listener.attributeModified(individual);
            }

            @Override
            public void rowPanelNew()
            {
                Document document = individual.getDocument();
                
                IndividualSelectorDialog individualSelector = new IndividualSelectorDialog(
                        IndividualEditorPanel.this.frame, 
                        Messages.getString("IndividualEditorPanel.newspouse") + individual.getName() + "...",  //$NON-NLS-1$ //$NON-NLS-2$
                        document, true);
                
                Individual newIndividual = individualSelector.getSelectedIndividual();

                if (newIndividual != null)
                {
                    Family family = document.addNewFamily();

                    family.addSpouse(individual);
                    family.addSpouse(newIndividual);

                    for (FamilySelectionListener listener : IndividualEditorPanel.this.familySelectionListeners)
                        listener.familyClicked(family);

                    for (GedComModifiedListener listener : IndividualEditorPanel.this.gedcomModifiedListeners)
                        listener.attributeModified(family);
                }
            }
        });
        
        this.ownFamilyInfoPanel.removeAll();
        this.ownFamilyInfoPanel.add(createTopBorder(ownFamilyRowPanelList));

        RowPanelList<MediaObjectRowPanel> mediaPanelList = 
                new RowPanelList<MediaObjectRowPanel>(Messages.getString("IndividualEditorPanel.mediaobjects"),  //$NON-NLS-1$
                        GedComRowPanelList.getMediaObjectRowPanelList(individual.OBJE, individual, 
                                Messages.getString("IndividualEditorPanel.mediaobject"), Messages.getString("IndividualEditorPanel.children"), true, true), Messages.getString("IndividualEditorPanel.addMediaObject")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        
        new FileDrop(mediaPanelList, new FileDrop.Listener()
        {
            @Override
            public void filesDropped(java.io.File[] files)
            {
                for (java.io.File file : files)
                {
                    MediaObject newMediaObject = new MediaObject(individual.getDocument());
                    newMediaObject.FILE = file.getAbsolutePath();
                                
                    // Extract filename from path
                    String fileName = file.getName();
                    int lastSeparator = Math.max(file.getAbsolutePath().lastIndexOf('/'), file.getAbsolutePath().lastIndexOf('\\'));
                    if (lastSeparator >= 0)
                    {
                        fileName = file.getAbsolutePath().substring(lastSeparator + 1);
                    }
                    
                    int lastDot = fileName.lastIndexOf('.');
                    
                    // Set title from filename (without extension)
                    newMediaObject.TITL = (lastDot > 0) ? fileName.substring(0, lastDot) : fileName;
                    // Set format and type from extension
                    if (lastDot > 0)
                    {
                        String extension = fileName.substring(lastDot + 1).toLowerCase();
                        
                        newMediaObject.FORM = extension;
                        newMediaObject.TYPE = MediaType.getMediaTypeForExtension(extension);
                    }
                    
                    MediaObjectReference newMediaObjectRef = new MediaObjectReference(individual.getDocument(), newMediaObject);
                    individual.addNewMediaObjectReference(newMediaObjectRef);

                    IndividualEditorPanel.this.setModel(individual);

                    for (GedComModifiedListener listener : IndividualEditorPanel.this.gedcomModifiedListeners)
                        listener.attributeModified(individual);
                }
            }
        });

        mediaPanelList.addRowPanelListListener(new RowPanelListListener<MediaObjectRowPanel>()
        {
            @Override
            public void rowPanelClicked(MediaObjectRowPanel rowPanel)
            {
                MediaObjectCropDialog cropDialog = new MediaObjectCropDialog(
                    IndividualEditorPanel.this.frame, rowPanel.getMediaObjectReference());

                cropDialog.setVisible(true);
                
                rowPanel.refresh();
            }

            @Override
            public void rowPanelMovedUp(MediaObjectRowPanel rowPanel)
            {
                individual.setIndividualOlderMediaObject(rowPanel.getMediaObjectReference());
                
                IndividualEditorPanel.this.setModel(individual);

                for (GedComModifiedListener listener : IndividualEditorPanel.this.gedcomModifiedListeners)
                    listener.attributeModified(individual);
            }

            @Override
            public void rowPanelMovedDown(MediaObjectRowPanel rowPanel)
            {
                individual.setIndividualYoungerMediaObject(rowPanel.getMediaObjectReference());
                
                IndividualEditorPanel.this.setModel(individual);

                for (GedComModifiedListener listener : IndividualEditorPanel.this.gedcomModifiedListeners)
                    listener.attributeModified(individual);
            }

            @Override
            public void rowPanelDeleted(MediaObjectRowPanel rowPanel)
            {
                individual.removeMediaObject(rowPanel.getMediaObjectReference());

                IndividualEditorPanel.this.setModel(individual);

                for (GedComModifiedListener listener : IndividualEditorPanel.this.gedcomModifiedListeners)
                    listener.attributeModified(individual);
            }

            @Override
            public void rowPanelNew()
            {
                Document document = individual.getDocument();
                
                MediaObjectDialog mediaObjectSelector = new MediaObjectDialog(
                        IndividualEditorPanel.this.frame, 
                        Messages.getString("IndividualEditorPanel.addmediaobject") + " - " + individual.getName() + "...",  //$NON-NLS-1$ //$NON-NLS-2$
                        document, true);
                
                MediaObject newMediaObject = mediaObjectSelector.getSelectedMediaObject();

                if (newMediaObject != null)
                {
                    MediaObjectReference newMediaObjectRef = new MediaObjectReference(document, newMediaObject);
                    individual.addNewMediaObjectReference(newMediaObjectRef);

                    IndividualEditorPanel.this.setModel(individual);

                    for (GedComModifiedListener listener : IndividualEditorPanel.this.gedcomModifiedListeners)
                        listener.attributeModified(individual);
                }
            }
        });
        
        this.mediaInfoPanel.removeAll();
        this.mediaInfoPanel.add(createTopBorder(mediaPanelList));
        
        for (JScrollPane scrollPane : this.scrollPanes)
            scrollPane.getVerticalScrollBar().setValue(0);
        
        this.invalidate();
        this.updateUI();
    }
    
    public void selectMediaObject(MediaObject mediaObject)
    {
        // Find the individual that has a reference to this media object
        Document document = this.individual.getDocument();
        for (Individual ind : document.listIndividuals())
        {
            for (MediaObjectReference ref : ind.OBJE)
            {
                if (ref.mediaObject == mediaObject)
                {
                    // Found the individual, set the model and switch to media tab
                    this.setModel(ind);
                    
                    // The media tab selection will show the media objects
                    return;
                }
            }
        }
        
        // If no individual owns this media object, just show it in the current individual
        // (this shouldn't normally happen)
        this.setModel(this.individual);
    }
}
