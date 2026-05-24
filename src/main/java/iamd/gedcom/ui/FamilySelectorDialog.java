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
import iamd.gedcom.datamodel.Individual.Sex;
import iamd.gedcom.format.GedComContext;
import iamd.ui.BorderListPanelGenerator;
import iamd.ui.ComboBoxEditor;
import iamd.ui.TextLineEditor;

@SuppressWarnings("serial")
public class FamilySelectorDialog extends JDialog
{
    static class FamilyRow
    {
        final public Family family;
        
        public FamilyRow(Family family)
        {
            this.family = family;
        }
        
        @Override
        public String toString()
        {
            Individual spouse1 = this.family.getSpouse1();
            
            if (spouse1 != null)
                return Sex.toCharSymbol(spouse1.SEX) + " " + spouse1.getName();
            
            return "";
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
    
    public FamilySelectorDialog(JFrame frame, String title, Document document, boolean allowNew)
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
                FamilySelectorDialog.this.accepted = true;
                FamilySelectorDialog.this.setVisible(false);
            }
        };
        
        ActionListener rejectActionListener = new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                FamilySelectorDialog.this.accepted = false;
                FamilySelectorDialog.this.setVisible(false);
            }
        };
        
        JPanel existingIndividualPanel = new JPanel(new BorderLayout());
        {
            existingIndividualPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
            
            this.table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            this.table.setColumnSelectionAllowed(false);
            
            this.tableModel.addColumn(Messages.getString("SelectorDialog.table_spouse1")); //$NON-NLS-1$
            this.tableModel.addColumn(Messages.getString("SelectorDialog.table_spouse2")); //$NON-NLS-1$
            this.tableModel.addColumn(Messages.getString("SelectorDialog.table_children")); //$NON-NLS-1$

            JTextField searchTextField = new JTextField(20);
            searchTextField.getDocument().addDocumentListener(new DocumentListener()
            {
                @Override
                public void removeUpdate(DocumentEvent e)
                {
                    FamilySelectorDialog.this.filterBy(searchTextField.getText());
                }
                
                @Override
                public void insertUpdate(DocumentEvent e)
                {
                    FamilySelectorDialog.this.filterBy(searchTextField.getText());
                }
                
                @Override
                public void changedUpdate(DocumentEvent e)
                {
                    FamilySelectorDialog.this.filterBy(searchTextField.getText());
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
            
            this.tabbedPane.addTab(Messages.getString("SelectorDialog.addfamily"), newIndividualPanel); //$NON-NLS-1$
            this.tabbedPane.addTab(Messages.getString("SelectorDialog.selectexistingfamily"), existingIndividualPanel); //$NON-NLS-1$
            
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
        
        Family currentFamily = selectedRow < 0 || selectedRow >= this.tableModel.getRowCount() ? null : 
            ((FamilyRow) this.tableModel.getValueAt(selectedRow, 0)).family;
        
        this.tableModel.setRowCount(0);
        
        int newCurrentIndex = -1;
        
        for (Family family : this.document.listFamilies())
        {
            Individual spouse2 = family.getSpouse2();
            
            String spouse2Name = "";
            
            if (spouse2 != null)
                spouse2Name = Sex.toCharSymbol(spouse2.SEX) + " " + spouse2.getName();
            
            String children = ""; //$NON-NLS-1$

            Collection<Individual> familyChildren = family.getChildren();
            
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
                
                children = familyChildrenStr;
            }
            
            if (GedComContext.normalizeID(family.getSpouseNames()).contains(GedComContext.normalizeID(text)))
            {
                if (family == currentFamily)
                    newCurrentIndex = this.tableModel.getRowCount(); 

                this.tableModel.addRow(new Object[] { new FamilyRow(family), spouse2Name, children });
            }
        }
        
        if (newCurrentIndex != -1)
        {
            this.table.getSelectionModel().setSelectionInterval(newCurrentIndex, newCurrentIndex);
        }
    }

    public Family getSelectedFamily()
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
            
            return ((FamilyRow) this.tableModel.getValueAt(selectedRow, 0)).family;
        }
        else
        {
            Family family = document.addNewFamily();

            family.addSpouse(newIndividual);

            this.document.addNewIndividual(newIndividual);

            return family;
        }
    }
}
