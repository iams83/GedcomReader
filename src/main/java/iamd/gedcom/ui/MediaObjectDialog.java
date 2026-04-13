package iamd.gedcom.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

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
import iamd.gedcom.datamodel.MediaObject;
import iamd.gedcom.datamodel.MediaObject.MediaType;
import iamd.ui.BorderListPanelGenerator;
import iamd.ui.ComboBoxEditor;
import iamd.ui.TextLineEditor;

@SuppressWarnings("serial")
public class MediaObjectDialog extends JDialog
{
    static class MediaObjectRow
    {
        final public MediaObject mediaObject;
        
        public MediaObjectRow(MediaObject mediaObject)
        {
            this.mediaObject = mediaObject;
        }
        
        @Override
        public String toString()
        {
            String s = this.mediaObject.FILE; //$NON-NLS-1$
            
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

    TextLineEditor       file = new TextLineEditor();
    TextLineEditor       form = new TextLineEditor();
    TextLineEditor       titl = new TextLineEditor();
    ComboBoxEditor<MediaType> type  = new ComboBoxEditor<MediaType>(
            new MediaType[] { null, MediaType.Picture, MediaType.Audio, MediaType.Video, MediaType.Document })
    {
        @Override
        protected String valueTypeToString(MediaType type)
        {
            if (type == null)
                return " ?"; //$NON-NLS-1$
            
            return Messages.getString("SelectorDialog." + type.name().toLowerCase()); //$NON-NLS-1$
        }
    };
    
    final private JTabbedPane tabbedPane = new JTabbedPane();
    
    final private boolean allowNew;
    
    private boolean accepted = false;
    
    public MediaObjectDialog(JFrame frame, String title, Document document, boolean allowNew)
    {
        super(frame, title);
        
        this.allowNew = allowNew;
        
        this.setModal(true);
        
        this.setLocationByPlatform(true);
        this.setSize(600, 400);

        this.form.setPreferredSize(new Dimension(100, this.form.getPreferredSize().height));
        
        ActionListener acceptActionListener = new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                MediaObjectDialog.this.accepted = true;
                MediaObjectDialog.this.setVisible(false);
            }
        };
        
        ActionListener rejectActionListener = new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                MediaObjectDialog.this.accepted = false;
                MediaObjectDialog.this.setVisible(false);
            }
        };
        
        JPanel existingIndividualPanel = new JPanel(new BorderLayout());
        {
            existingIndividualPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
            
            this.table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            this.table.setColumnSelectionAllowed(false);
            
            this.tableModel.addColumn(Messages.getString("SelectorDialog.table_file")); //$NON-NLS-1$
            this.tableModel.addColumn(Messages.getString("SelectorDialog.table_format")); //$NON-NLS-1$
            this.tableModel.addColumn(Messages.getString("SelectorDialog.table_title")); //$NON-NLS-1$
            this.tableModel.addColumn(Messages.getString("SelectorDialog.table_type")); //$NON-NLS-1$

            JTextField searchTextField = new JTextField(20);
            searchTextField.getDocument().addDocumentListener(new DocumentListener()
            {
                @Override
                public void removeUpdate(DocumentEvent e)
                {
                    MediaObjectDialog.this.filterBy(searchTextField.getText());
                }
                
                @Override
                public void insertUpdate(DocumentEvent e)
                {
                    MediaObjectDialog.this.filterBy(searchTextField.getText());
                }
                
                @Override
                public void changedUpdate(DocumentEvent e)
                {
                    MediaObjectDialog.this.filterBy(searchTextField.getText());
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
    
                JLabel formLabel = new JLabel(Messages.getString("SelectorDialog.form")); //$NON-NLS-1$
                formLabel.setPreferredSize(this.form.getPreferredSize());
                
                JPanel topPanel1 = new JPanel(new BorderLayout());
                topPanel1.add(new JLabel(Messages.getString("SelectorDialog.file"))); //$NON-NLS-1$
                topPanel1.add(formLabel, BorderLayout.EAST);
                
                JPanel topPanel2 = new JPanel(new BorderLayout());
                topPanel2.add(this.file);
                topPanel2.add(this.form, BorderLayout.EAST);
                
                globalPanelGenerator.add(topPanel1);
                globalPanelGenerator.add(topPanel2);
                globalPanelGenerator.add(new JLabel(Messages.getString("SelectorDialog.titl")));             //$NON-NLS-1$
                globalPanelGenerator.add(this.titl);
                globalPanelGenerator.add(new JLabel(Messages.getString("SelectorDialog.type")));                 //$NON-NLS-1$
                globalPanelGenerator.add(this.type);
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
            
            this.tabbedPane.addTab(Messages.getString("SelectorDialog.addmediaobject"), newIndividualPanel); //$NON-NLS-1$
            this.tabbedPane.addTab(Messages.getString("SelectorDialog.selectexistingmediaobject"), existingIndividualPanel); //$NON-NLS-1$
            
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
        
        MediaObject currentMediaObject = selectedRow < 0 || selectedRow >= this.tableModel.getRowCount() ? null : 
            ((MediaObjectRow) this.tableModel.getValueAt(selectedRow, 0)).mediaObject;
        
        this.tableModel.setRowCount(0);
        
        int newCurrentIndex = -1;
        
        for (MediaObject mediaObject : this.document.listMediaObjects())
        {
            if (mediaObject.TITL != null &&
                mediaObject.TITL
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
                if (mediaObject == currentMediaObject)
                    newCurrentIndex = this.tableModel.getRowCount(); 

                this.tableModel.addRow(new Object[] { new MediaObjectRow(mediaObject) });
            }
        }
        
        if (newCurrentIndex != -1)
        {
            this.table.getSelectionModel().setSelectionInterval(newCurrentIndex, newCurrentIndex);
        }
    }

    public MediaObject getSelectedMediaObject()
    {
        this.accepted = false;
        
        MediaObject newMediaObject = new MediaObject(this.document);

        this.file   .bindValue(newMediaObject, "FILE"); //$NON-NLS-1$
        this.form   .bindValue(newMediaObject, "FORM"); //$NON-NLS-1$
        this.titl   .bindValue(newMediaObject, "TITL"); //$NON-NLS-1$
        this.type   .bindValue(newMediaObject, "TYPE"); //$NON-NLS-1$
        
        this.filterBy(""); //$NON-NLS-1$

        this.table.getSelectionModel().setSelectionInterval(0, 0);
        
        this.setVisible(true);
        
        if (!this.accepted)
            return null;
        
        if (!this.allowNew || this.tabbedPane.getSelectedIndex() == 1)
        {        
            int selectedRow = this.table.getSelectedRow();
            
            return ((MediaObjectRow) this.tableModel.getValueAt(selectedRow, 0)).mediaObject;
        }
        else
        {
            this.document.addNewMediaObject(newMediaObject);

            return newMediaObject;
        }
    }
}
