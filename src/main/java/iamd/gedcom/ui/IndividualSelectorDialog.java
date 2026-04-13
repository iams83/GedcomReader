package iamd.gedcom.ui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import iamd.gedcom.datamodel.Document;
import iamd.gedcom.datamodel.Family;
import iamd.gedcom.datamodel.Individual;
import iamd.gedcom.datamodel.Individual.FamilyChildRelationship;
import iamd.gedcom.datamodel.Individual.Sex;
import iamd.ui.BorderListPanelGenerator;
import iamd.ui.ComboBoxEditor;
import iamd.ui.TextLineEditor;

@SuppressWarnings("serial")
public class IndividualSelectorDialog extends JDialog
{
    static class IndividualRow
    {
        final public Individual individual;
        
        public IndividualRow(Individual individual)
        {
            this.individual = individual;
        }
        
        @Override
        public String toString()
        {
            String s = Sex.toCharSymbol(this.individual.SEX) + " " + this.individual.getName(); //$NON-NLS-1$
            
            return s;
        }
    }
    
    final private Document document;

    final private DefaultTableModel tableModel = new DefaultTableModel();
    
    final private JTable table = new JTable(this.tableModel)
    {
        @Override
        public boolean isCellEditable(int row, int col)
        {
            return false;
        }
    };

    TextLineEditor      name       = new TextLineEditor();
    TextLineEditor      surname    = new TextLineEditor();
    TextLineEditor      nick       = new TextLineEditor();
    ComboBoxEditor<Sex> sex        = new ComboBoxEditor<Sex>(new Sex[] { null, Sex.F, Sex.M })
    {
        @Override
        protected String valueTypeToString(Sex s)
        {
            if (s == null)
                return " ?"; //$NON-NLS-1$
            
            return s.symbol + (s == Sex.F ? Messages.getString("SelectorDialog.female") : Messages.getString("SelectorDialog.male")); //$NON-NLS-1$ //$NON-NLS-2$
        }
    };
    
    final private JTabbedPane tabbedPane = new JTabbedPane();
    
    final private boolean allowNew;
    
    private boolean accepted = false;
    
    public IndividualSelectorDialog(JFrame frame, String title, Document document, boolean allowNew)
    {
        super(frame, title);
        
        this.allowNew = allowNew;
        
        this.setModal(true);
        
        this.setLocationByPlatform(true);
        this.setSize(600, 400);

        ActionListener acceptActionListener = new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                IndividualSelectorDialog.this.accepted = true;
                IndividualSelectorDialog.this.setVisible(false);
            }
        };
        
        ActionListener rejectActionListener = new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                IndividualSelectorDialog.this.accepted = false;
                IndividualSelectorDialog.this.setVisible(false);
            }
        };
        
        JPanel existingIndividualPanel = new JPanel(new BorderLayout());
        {
            existingIndividualPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
            
            this.table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            this.table.setColumnSelectionAllowed(false);
            
            this.tableModel.addColumn(Messages.getString("SelectorDialog.table_name")); //$NON-NLS-1$
            this.tableModel.addColumn(Messages.getString("SelectorDialog.table_parents")); //$NON-NLS-1$
            this.tableModel.addColumn(Messages.getString("SelectorDialog.table_spouses")); //$NON-NLS-1$
            this.tableModel.addColumn(Messages.getString("SelectorDialog.table_children")); //$NON-NLS-1$

            JTextField searchTextField = new JTextField(20);
            searchTextField.getDocument().addDocumentListener(new DocumentListener()
            {
                @Override
                public void removeUpdate(DocumentEvent e)
                {
                    IndividualSelectorDialog.this.filterBy(searchTextField.getText());
                }
                
                @Override
                public void insertUpdate(DocumentEvent e)
                {
                    IndividualSelectorDialog.this.filterBy(searchTextField.getText());
                }
                
                @Override
                public void changedUpdate(DocumentEvent e)
                {
                    IndividualSelectorDialog.this.filterBy(searchTextField.getText());
                }
            });
            
            BorderListPanelGenerator topPanelGenerator = new BorderListPanelGenerator(BorderLayout.EAST);
            topPanelGenerator.add(searchTextField);
            topPanelGenerator.add(new JLabel(Messages.getString("SelectorDialog.filter"))); //$NON-NLS-1$
            
            JPanel topPanel = topPanelGenerator.extractPanel();
            topPanel.setBorder(new EmptyBorder(0, 0, 10, 0));
            
            JButton selectionButton = new JButton(Messages.getString("SelectorDialog.select")); //$NON-NLS-1$
            selectionButton.addActionListener(acceptActionListener);
            
            JButton cancelButton = new JButton(Messages.getString("SelectorDialog.cancel")); //$NON-NLS-1$
            cancelButton.addActionListener(rejectActionListener);
            
            BorderListPanelGenerator bottomPanelGenerator = new BorderListPanelGenerator(BorderLayout.EAST);
            bottomPanelGenerator.add(selectionButton);
            bottomPanelGenerator.add(cancelButton);
            
            JPanel bottomPanel = bottomPanelGenerator.extractPanel();
            bottomPanel.setBorder(new EmptyBorder(20, 0, 0, 0));
    
            existingIndividualPanel.add(topPanel, BorderLayout.NORTH);
            existingIndividualPanel.add(new JScrollPane(this.table));
            existingIndividualPanel.add(bottomPanel, BorderLayout.SOUTH);
        }

        if (allowNew)
        {
            JPanel newIndividualPanel = new JPanel(new BorderLayout());
            {
                newIndividualPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
                
                BorderListPanelGenerator globalPanelGenerator = new BorderListPanelGenerator(BorderLayout.NORTH);
    
                JLabel sexLabel = new JLabel(Messages.getString("SelectorDialog.genre")); //$NON-NLS-1$
                sexLabel.setPreferredSize(this.sex.getPreferredSize());
                
                JPanel topPanel1 = new JPanel(new BorderLayout());
                topPanel1.add(new JLabel(Messages.getString("SelectorDialog.name"))); //$NON-NLS-1$
                topPanel1.add(sexLabel, BorderLayout.EAST);
                
                JPanel topPanel2 = new JPanel(new BorderLayout());
                topPanel2.add(this.name);
                topPanel2.add(this.sex, BorderLayout.EAST);
                
                globalPanelGenerator.add(topPanel1);
                globalPanelGenerator.add(topPanel2);
                globalPanelGenerator.add(new JLabel(Messages.getString("SelectorDialog.surname")));             //$NON-NLS-1$
                globalPanelGenerator.add(this.surname);
                globalPanelGenerator.add(new JLabel(Messages.getString("SelectorDialog.alias")));                 //$NON-NLS-1$
                globalPanelGenerator.add(this.nick);
                globalPanelGenerator.setBackground(this.getBackground());
    
                JButton selectionButton = new JButton(Messages.getString("SelectorDialog.create")); //$NON-NLS-1$
                selectionButton.addActionListener(acceptActionListener);
                
                JButton cancelButton = new JButton(Messages.getString("SelectorDialog.cancel")); //$NON-NLS-1$
                cancelButton.addActionListener(rejectActionListener);
                
                BorderListPanelGenerator bottomPanelGenerator = new BorderListPanelGenerator(BorderLayout.EAST);
                bottomPanelGenerator.add(selectionButton);
                bottomPanelGenerator.add(cancelButton);
                
                JPanel bottomPanel = bottomPanelGenerator.extractPanel();
                bottomPanel.setBorder(new EmptyBorder(20, 0, 0, 0));
        
                newIndividualPanel.add(globalPanelGenerator.extractPanel());
                newIndividualPanel.add(bottomPanel, BorderLayout.SOUTH);
            }
            
            this.tabbedPane.addTab(Messages.getString("SelectorDialog.addperson"), newIndividualPanel); //$NON-NLS-1$
            this.tabbedPane.addTab(Messages.getString("SelectorDialog.selectexistingperson"), existingIndividualPanel); //$NON-NLS-1$
            
            this.add(this.tabbedPane);
        }
        else
        {
            this.add(existingIndividualPanel);
        }
                
        this.document = document;
    }
    
    protected void filterBy(String text)
    {
        int selectedRow = this.table.getSelectedRow();
        
        Individual currentIndividual = selectedRow < 0 || selectedRow >= this.tableModel.getRowCount() ? null : 
            ((IndividualRow) this.tableModel.getValueAt(selectedRow, 0)).individual;
        
        this.tableModel.setRowCount(0);
        
        int newCurrentIndex = -1;
        
        for (Individual individual : this.document.listIndividuals())
        {
            String parents = ""; //$NON-NLS-1$

            for (FamilyChildRelationship parentFamily : individual.getParentFamilies())
            {
                if (parents.isEmpty())
                    parents = parentFamily.family.spousesToString(null);
                else
                    parents += "; " + parentFamily.family.spousesToString(null); //$NON-NLS-1$
            }
            
            String spouses = ""; //$NON-NLS-1$
            
            String children = ""; //$NON-NLS-1$

            for (Family ownFamily : individual.getFamilies())
            {
                Individual spouse = ownFamily.getSpouse(individual);
                
                if (spouse != null)
                {
                    if (spouses.isEmpty())
                        spouses = Individual.Sex.toCharSymbol(spouse.SEX) + " " + spouse.getName(); //$NON-NLS-1$
                    else
                        spouses += ", " + Individual.Sex.toCharSymbol(spouse.SEX) + " " + spouse.getName(); //$NON-NLS-1$ //$NON-NLS-2$
                }
                
                Collection<Individual> familyChildren = ownFamily.getChildren();
                
                if (!familyChildren.isEmpty())
                {
                    String familyChildrenStr = ""; //$NON-NLS-1$
                    
                    for (Individual child : familyChildren)
                    {
                        if (familyChildrenStr.isEmpty())
                            familyChildrenStr = Individual.Sex.toCharSymbol(child.SEX) + " " + child.NAME.name; //$NON-NLS-1$
                        else
                            familyChildrenStr += ", " + Individual.Sex.toCharSymbol(child.SEX) + " " + child.NAME.name; //$NON-NLS-1$ //$NON-NLS-2$
                    }
                    
                    if (children.isEmpty())
                        children = familyChildrenStr;
                    else
                        children += "; " + familyChildrenStr; //$NON-NLS-1$
                }
            }
            
            if (individual.getName()
                    .toUpperCase()
                    .replaceAll("[���]", "A") //$NON-NLS-1$ //$NON-NLS-2$
                    .replaceAll("[���]", "E") //$NON-NLS-1$ //$NON-NLS-2$
                    .replaceAll("[���]", "I") //$NON-NLS-1$ //$NON-NLS-2$
                    .replaceAll("[���]", "O") //$NON-NLS-1$ //$NON-NLS-2$
                    .replaceAll("[���]", "U") //$NON-NLS-1$ //$NON-NLS-2$
                .contains(text
                    .toUpperCase()
                    .replaceAll("[���]", "A") //$NON-NLS-1$ //$NON-NLS-2$
                    .replaceAll("[���]", "E") //$NON-NLS-1$ //$NON-NLS-2$
                    .replaceAll("[���]", "I") //$NON-NLS-1$ //$NON-NLS-2$
                    .replaceAll("[���]", "O") //$NON-NLS-1$ //$NON-NLS-2$
                    .replaceAll("[���]", "U"))) //$NON-NLS-1$ //$NON-NLS-2$
            {
                if (individual == currentIndividual)
                    newCurrentIndex = this.tableModel.getRowCount(); 

                this.tableModel.addRow(new Object[] { new IndividualRow(individual), parents, spouses, children });
            }
        }
        
        if (newCurrentIndex != -1)
        {
            this.table.getSelectionModel().setSelectionInterval(newCurrentIndex, newCurrentIndex);
        }
    }

    public Individual getSelectedIndividual()
    {
        this.accepted = false;
        
        Individual newIndividual = new Individual(this.document);

        this.name      .bindValue(newIndividual.NAME, "name"); //$NON-NLS-1$
        this.sex       .bindValue(newIndividual, "SEX"); //$NON-NLS-1$
        this.surname   .bindValue(newIndividual.NAME, "surname"); //$NON-NLS-1$
        this.nick      .bindValue(newIndividual.NAME, "nick"); //$NON-NLS-1$
        
        this.filterBy(""); //$NON-NLS-1$

        this.table.getSelectionModel().setSelectionInterval(0, 0);
        
        this.setVisible(true);
        
        if (!this.accepted)
            return null;
        
        if (!this.allowNew || this.tabbedPane.getSelectedIndex() == 1)
        {        
            int selectedRow = this.table.getSelectedRow();
            
            return ((IndividualRow) this.tableModel.getValueAt(selectedRow, 0)).individual;
        }
        else
        {
            this.document.addNewIndividual(newIndividual);

            return newIndividual;
        }
    }
}
