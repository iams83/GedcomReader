package iamd.gedcom.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.lang.reflect.Field;
import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import iamd.gedcom.datamodel.Family;
import iamd.gedcom.datamodel.Individual;
import iamd.gedcom.ui.editors.EventEditor;
import iamd.gedcom.ui.editors.LongTextEditor;
import iamd.ui.AttributeEditorListener;
import iamd.ui.BorderListPanelGenerator;
import iamd.ui.RowPanelList;
import iamd.ui.RowPanelListListener;

@SuppressWarnings("serial")
public class FamilyEditorPanel extends EditorPanel
{
    final private JFrame frame;
    
    final private ArrayList<FamilySelectionListener> familySelectionListeners = new ArrayList<FamilySelectionListener>();
    final private ArrayList<GedComModifiedListener> attributeEditorListeners = new ArrayList<GedComModifiedListener>();
    
    final private EventEditor marriage = new EventEditor(Messages.getString("FamilyEditorPanel.marriage"), "MARR", true, this); //$NON-NLS-1$ //$NON-NLS-2$
    final private EventEditor divorce  = new EventEditor(Messages.getString("FamilyEditorPanel.divorce"),  "DIV",   true,  this); //$NON-NLS-1$ //$NON-NLS-2$

    final private JPanel spousesInfoPanel  = new JPanel(new BorderLayout());
    final private JPanel childrenInfoPanel = new JPanel(new BorderLayout());
    
    final private LongTextEditor note      = new LongTextEditor(4);
    
    private Family family;

    public FamilyEditorPanel(JFrame frame)
    {
        this.frame = frame;
        
        this.setLayout(new BorderLayout());
        
        BorderListPanelGenerator globalPanelGenerator = new BorderListPanelGenerator(BorderLayout.NORTH);
        
        globalPanelGenerator.add(this.spousesInfoPanel);

        globalPanelGenerator.add(createTopBorder(this.marriage));
        globalPanelGenerator.add(createTopBorder(this.divorce));
        globalPanelGenerator.add(createTopBorder(this.childrenInfoPanel));
        globalPanelGenerator.add(createTopBorder(newJLabel(Messages.getString("FamilyEditorPanel.note"))));
        globalPanelGenerator.add(this.note); //$NON-NLS-1$
        
        JPanel panel = globalPanelGenerator.extractPanel();
        panel.setBorder(new EmptyBorder(2, 2, 2, 2));
        panel.setBackground(this.getBackground());
        panel.setPreferredSize(new Dimension(300, panel.getPreferredSize().height));
        
        this.add(panel);
        
        AttributeEditorListener attributeEditorListener = new AttributeEditorListener()
        {
            @Override
            public void attributeModified(Object editingObject, Field editingField, Object value)
            {
                for (GedComModifiedListener listener : FamilyEditorPanel.this.attributeEditorListeners)
                    listener.attributeModified(FamilyEditorPanel.this.family);
                
                FamilyEditorPanel.this.invalidate();
                FamilyEditorPanel.this.updateUI();
                FamilyEditorPanel.this.repaint();
            }
        };
        
        this.marriage.addAttributeEditionListener(attributeEditorListener);
        this.divorce .addAttributeEditionListener(attributeEditorListener);
        this.note    .addAttributeEditionListener(attributeEditorListener);

        this.addComponentListener(new ComponentAdapter()
        {
            @Override
            public void componentShown(ComponentEvent e)
            {
                FamilyEditorPanel.this.invalidate();
                FamilyEditorPanel.this.updateUI();
                FamilyEditorPanel.this.repaint();
            }

            @Override
            public void componentResized(ComponentEvent e)
            {
                FamilyEditorPanel.this.invalidate();
                FamilyEditorPanel.this.updateUI();
                FamilyEditorPanel.this.repaint();
            }
        });
    }

    public void addAttributeEditorListener(GedComModifiedListener listener)
    {
        this.attributeEditorListeners.add(listener);
    }

    public void addFamilySelectionListener(FamilySelectionListener listener)
    {
        this.familySelectionListeners.add(listener);
    }

    public void setModel(Family family)
    {
        this.family = family;
        
        this.marriage.setDocument(family.getDocument());
        this.divorce .setDocument(family.getDocument());
        this.note    .setDocument(family.getDocument());
        
        this.marriage     .bindValue(family, "MARR"); //$NON-NLS-1$
        this.divorce      .bindValue(family, "DIV");  //$NON-NLS-1$
        this.note         .bindValue(family, "NOTE"); //$NON-NLS-1$
        
        Individual spouse1 = family.getSpouse1();
        Individual spouse2 = family.getSpouse2();

        ArrayList<Individual> spousesList = new ArrayList<>();
        
        if (spouse1 != null)
            spousesList.add(spouse1);
        
        if (spouse2 != null)
            spousesList.add(spouse2);
        
        RowPanelList<IndividualRowPanel> spousesRowPanelList = 
                new RowPanelList<IndividualRowPanel>(Messages.getString("FamilyEditorPanel.spouses"),  //$NON-NLS-1$
                        GedComRowPanelList.getIndividualRowPanelList(spousesList, false, true), spousesList.size() == 2 ? null : Messages.getString("FamilyEditorPanel.addspouse")); //$NON-NLS-1$
        
        spousesRowPanelList.addRowPanelListListener(new RowPanelListListener<IndividualRowPanel>()
        {
            @Override
            public void rowPanelClicked(IndividualRowPanel rowPanel)
            {
                for (FamilySelectionListener listener : FamilyEditorPanel.this.familySelectionListeners)
                    listener.individualClicked(rowPanel.getIndividual());
            }

            @Override
            public void rowPanelMovedUp(IndividualRowPanel rowPanel)
            {
                throw new AssertionError("This code should never be reached!"); //$NON-NLS-1$
            }

            @Override
            public void rowPanelMovedDown(IndividualRowPanel rowPanel)
            {
                throw new AssertionError("This code should never be reached!"); //$NON-NLS-1$
            }

            @Override
            public void rowPanelDeleted(IndividualRowPanel rowPanel)
            {
                family.removeSpouse(rowPanel.getIndividual());

                FamilyEditorPanel.this.setModel(family);

                for (GedComModifiedListener listener : FamilyEditorPanel.this.attributeEditorListeners)
                    listener.attributeModified(rowPanel.getIndividual());
            }

            @Override
            public void rowPanelNew()
            {
                IndividualSelectorDialog individualSelector = new IndividualSelectorDialog(
                        FamilyEditorPanel.this.frame, 
                        Messages.getString("FamilyEditorPanel.newspouse") + family.getSpouseNames() + "...", //$NON-NLS-1$ //$NON-NLS-2$
                        family.getDocument(), true);
                
                Individual newIndividual = individualSelector.getSelectedIndividual();

                if (newIndividual != null)
                {
                    family.addSpouse(newIndividual);

                    for (GedComModifiedListener listener : FamilyEditorPanel.this.attributeEditorListeners)
                        listener.attributeModified(family);
                    
                    for (FamilySelectionListener listener : FamilyEditorPanel.this.familySelectionListeners)
                        listener.individualClicked(newIndividual);
                }
            }
        });

        this.spousesInfoPanel.removeAll();
        this.spousesInfoPanel.add(spousesRowPanelList);

        RowPanelList<IndividualRowPanel> individualRowPanelList = 
                new RowPanelList<IndividualRowPanel>(Messages.getString("FamilyEditorPanel.children"),  //$NON-NLS-1$
                        GedComRowPanelList.getIndividualRowPanelList(family.getChildren(), true, true), Messages.getString("FamilyEditorPanel.addchild")); //$NON-NLS-1$
        
        individualRowPanelList.addRowPanelListListener(new RowPanelListListener<IndividualRowPanel>()
        {
            @Override
            public void rowPanelClicked(IndividualRowPanel rowPanel)
            {
                for (FamilySelectionListener listener : FamilyEditorPanel.this.familySelectionListeners)
                    listener.individualClicked(rowPanel.getIndividual());
            }

            @Override
            public void rowPanelMovedUp(IndividualRowPanel rowPanel)
            {
                family.setIndividualOlderBrother(rowPanel.getIndividual());
                
                FamilyEditorPanel.this.setModel(family);

                for (GedComModifiedListener listener : FamilyEditorPanel.this.attributeEditorListeners)
                    listener.attributeModified(family);
            }

            @Override
            public void rowPanelMovedDown(IndividualRowPanel rowPanel)
            {
                family.setIndividualYoungerBrother(rowPanel.getIndividual());
                
                FamilyEditorPanel.this.setModel(family);

                for (GedComModifiedListener listener : FamilyEditorPanel.this.attributeEditorListeners)
                    listener.attributeModified(family);
            }

            @Override
            public void rowPanelDeleted(IndividualRowPanel rowPanel)
            {
                family.removeChild(rowPanel.getIndividual());

                FamilyEditorPanel.this.setModel(family);

                for (GedComModifiedListener listener : FamilyEditorPanel.this.attributeEditorListeners)
                    listener.attributeModified(family);
            }

            @Override
            public void rowPanelNew()
            {
                IndividualSelectorDialog individualSelector = new IndividualSelectorDialog(
                        FamilyEditorPanel.this.frame, 
                        Messages.getString("FamilyEditorPanel.newchild") + family.getSpouseNames() + "...",  //$NON-NLS-1$ //$NON-NLS-2$
                        family.getDocument(), true);
                
                Individual newIndividual = individualSelector.getSelectedIndividual();

                if (newIndividual != null)
                {
                    family.addChild(newIndividual);
                    
                    for (GedComModifiedListener listener : FamilyEditorPanel.this.attributeEditorListeners)
                        listener.attributeModified(family);
                    
                    for (FamilySelectionListener listener : FamilyEditorPanel.this.familySelectionListeners)
                        listener.individualClicked(newIndividual);
                }
            }
        });
        
        this.childrenInfoPanel.removeAll();
        this.childrenInfoPanel.add(createTopBorder(individualRowPanelList));
        
        this.invalidate();
        this.updateUI();
    }
}
