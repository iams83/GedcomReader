package iamd.gedcom.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;

import org.w3c.dom.events.MouseEvent;

import iamd.gedcom.datamodel.Document;
import iamd.gedcom.datamodel.MediaObject;
import iamd.gedcom.datamodel.MediaObject.MediaType;
import iamd.gedcom.format.GedComContext;
import iamd.ui.BorderListPanelGenerator;
import iamd.ui.ComboBoxEditor;
import iamd.ui.FilePathEditor;
import iamd.ui.GraphicsPanel;
import iamd.ui.GraphicsPanel.PanelMovement;
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

    FilePathEditor       file = new FilePathEditor();
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

    final private PreviewImagePanel newFilePreviewImagePanel = new PreviewImagePanel();
    
    final private boolean allowNew;
    
    private boolean accepted = false;
    
    public MediaObjectDialog(JFrame frame, String title, Document document, boolean allowNew)
    {
        super(frame, title);
        
        this.allowNew = allowNew;
        
        this.setModal(true);
        
        this.setLocationByPlatform(true);
        this.setSize(600, 600);

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
            
            PreviewImagePanel previewImagePanel = new PreviewImagePanel();
            previewImagePanel.setBorder(new LineBorder(Color.black, 1));

            this.table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            this.table.setColumnSelectionAllowed(false);

            this.table.getSelectionModel().addListSelectionListener(new ListSelectionListener()
            {
                @Override
                public void valueChanged(ListSelectionEvent e)
                {
                    if (!e.getValueIsAdjusting())
                    {
                        int selectedRow = MediaObjectDialog.this.table.getSelectedRow();
                        if (selectedRow >= 0 && selectedRow < MediaObjectDialog.this.tableModel.getRowCount())
                        {
                            String selectedFile = (String) MediaObjectDialog.this.tableModel.getValueAt(selectedRow, 0);
                            if (selectedFile != null && !selectedFile.isEmpty())
                            {
                                for (MediaObject mediaObject : MediaObjectDialog.this.document.listMediaObjects())
                                {
                                    if (selectedFile.equals(mediaObject.FILE))
                                    {
                                        previewImagePanel.setImage(mediaObject.getImage());
                                        return;
                                    }
                                }
                            }
                        }
                        previewImagePanel.setImage(null);
                    }
                }
            });
            
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
            
            BorderListPanelGenerator searchPanelGenerator = new BorderListPanelGenerator(BorderLayout.EAST);
            searchPanelGenerator.add(searchTextField);

            JScrollPane tableScrollPane = new JScrollPane(this.table);
            tableScrollPane.setPreferredSize(new Dimension(0, 150));
            
            BorderListPanelGenerator topPanelGenerator = new BorderListPanelGenerator(BorderLayout.NORTH);
            topPanelGenerator.add(searchPanelGenerator.extractPanel(new JLabel(Messages.getString("SelectorDialog.filter")))); //$NON-NLS-1$
            topPanelGenerator.add(tableScrollPane);
            
            JPanel topPanel = topPanelGenerator.extractPanel(previewImagePanel);
            
            JButton selectionButton = new JButton(Messages.getString("SelectorDialog.select")); //$NON-NLS-1$
            selectionButton.addActionListener(acceptActionListener);
            
            JButton cancelButton = new JButton(Messages.getString("SelectorDialog.cancel")); //$NON-NLS-1$
            cancelButton.addActionListener(rejectActionListener);
            
            BorderListPanelGenerator bottomPanelGenerator = new BorderListPanelGenerator(BorderLayout.EAST);
            bottomPanelGenerator.add(selectionButton);
            bottomPanelGenerator.add(cancelButton);
            
            JPanel bottomPanel = bottomPanelGenerator.extractPanel();
            bottomPanel.setBorder(new EmptyBorder(20, 0, 0, 0));
    
            existingIndividualPanel.add(topPanel, BorderLayout.CENTER);
            existingIndividualPanel.add(bottomPanel, BorderLayout.SOUTH);
        }

        if (allowNew)
        {
            JPanel newIndividualPanel = new JPanel(new BorderLayout());
            {
                newIndividualPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
                
                // Create file path panel
                JPanel topPanel2 = new JPanel(new BorderLayout());
                topPanel2.add(this.file);
                topPanel2.add(this.form, BorderLayout.EAST);
                
                BorderListPanelGenerator globalPanelGenerator = new BorderListPanelGenerator(BorderLayout.NORTH);
    
                JLabel formLabel = new JLabel(Messages.getString("SelectorDialog.format")); //$NON-NLS-1$
                formLabel.setPreferredSize(this.form.getPreferredSize());
                
                JPanel topPanel1 = new JPanel(new BorderLayout());
                topPanel1.add(new JLabel(Messages.getString("SelectorDialog.file"))); //$NON-NLS-1$
                topPanel1.add(formLabel, BorderLayout.EAST);
                
                globalPanelGenerator.add(topPanel1);
                globalPanelGenerator.add(topPanel2);
                globalPanelGenerator.add(new JLabel(Messages.getString("SelectorDialog.title")));             //$NON-NLS-1$
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
        
                newIndividualPanel.add(globalPanelGenerator.extractPanel(newFilePreviewImagePanel));
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
        
        // Get current selected FILE value
        String currentFile = selectedRow < 0 || selectedRow >= this.tableModel.getRowCount() ? null : 
            (String) this.tableModel.getValueAt(selectedRow, 0);
        
        this.tableModel.setRowCount(0);
        
        int newCurrentIndex = -1;
        
        for (MediaObject mediaObject : this.document.listMediaObjects())
        {
            if (mediaObject.TITL != null &&
                GedComContext.normalizeID(mediaObject.TITL).contains(GedComContext.normalizeID(text)))
            {
                if (currentFile != null && currentFile.equals(mediaObject.FILE))
                    newCurrentIndex = this.tableModel.getRowCount(); 

                this.tableModel.addRow(new Object[] { 
                    mediaObject.FILE != null ? mediaObject.FILE : "",
                    mediaObject.FORM != null ? mediaObject.FORM : "",
                    mediaObject.TITL != null ? mediaObject.TITL : "",
                    mediaObject.TYPE != null ? mediaObject.TYPE.name() : ""
                });
            }
        }
        
        if (newCurrentIndex != -1)
        {
            this.table.getSelectionModel().setSelectionInterval(newCurrentIndex, newCurrentIndex);
        }
    }
    
    private void autofillFromFile(final String filePath)
    {
        if (filePath == null || filePath.isEmpty())
            return;
        
        // Extract filename from path
        String fileName = filePath;
        int lastSeparator = Math.max(filePath.lastIndexOf('/'), filePath.lastIndexOf('\\'));
        if (lastSeparator >= 0)
        {
            fileName = filePath.substring(lastSeparator + 1);
        }
        
        int lastDot = fileName.lastIndexOf('.');
        
        // Set title from filename (without extension)
        String title = (lastDot > 0) ? fileName.substring(0, lastDot) : fileName;
        // Update the title field display and binding
        SwingUtilities.invokeLater(() -> {
            this.titl.initializeValue(title);

            try
            {
                BufferedImage image = ImageIO.read(new File(filePath));

                this.newFilePreviewImagePanel.setImage(image);
            }
            catch (IOException e)
            {
                // Do nothing, just don't show a preview
            	this.newFilePreviewImagePanel.setImage(null);
            }
        });
        
        // Set format and type from extension
        if (lastDot > 0)
        {
            String extension = fileName.substring(lastDot + 1).toLowerCase();
            
            // Update the form field display and binding
            SwingUtilities.invokeLater(() -> {
                this.form.initializeValue(extension);
            });
            
            // Update type based on extension
            MediaType mediaType = MediaType.getMediaTypeForExtension(extension);
            SwingUtilities.invokeLater(() -> {
                this.type.initializeValue(mediaType);
            });
        }
        
        // Request focus on the title field to auto-confirm and hide the file field
        SwingUtilities.invokeLater(() -> {
            this.titl.getComponent().requestFocusInWindow();
        });
    }

    public MediaObject getSelectedMediaObject()
    {
        this.accepted = false;
        
        MediaObject newMediaObject = new MediaObject(this.document);

        File gedcomFile = this.document.getFile();
        if (gedcomFile != null)
        {
            this.file.setDefaultDirectory(gedcomFile.getParentFile());
        }
        
        this.file   .bindValue(newMediaObject, "FILE"); //$NON-NLS-1$
        this.form   .bindValue(newMediaObject, "FORM"); //$NON-NLS-1$
        this.titl   .bindValue(newMediaObject, "TITL"); //$NON-NLS-1$
        this.type   .bindValue(newMediaObject, "TYPE"); //$NON-NLS-1$
        
        this.filterBy(""); //$NON-NLS-1$
        
        // Add FileSelectionListener to the file path editor to autofill when file is selected via dialog
        this.file.addFileSelectionListener(new FilePathEditor.FileSelectionListener()
        {
            @Override
            public void fileSelected(File selectedFile)
            {
                if (selectedFile != null)
                {
                    autofillFromFile(selectedFile.getAbsolutePath());
                }
            }
        });
        
        this.setVisible(true);
        
        if (!this.accepted)
            return null;
        
        if (!this.allowNew || this.tabbedPane.getSelectedIndex() == 1)
        {        
            int selectedRow = this.table.getSelectedRow();
            
            // Find the MediaObject matching the selected FILE value
            String selectedFile = (String) this.tableModel.getValueAt(selectedRow, 0);
            for (MediaObject mo : this.document.listMediaObjects())
            {
                if (selectedFile.equals(mo.FILE))
                    return mo;
            }
            return null;
        }
        else
        {
            this.document.addNewMediaObject(newMediaObject);

            return newMediaObject;
        }
    }
        
    // Image panel for displaying pictures
    private class PreviewImagePanel extends GraphicsPanel
    {
        private BufferedImage image;
        
        public PreviewImagePanel()
        {
            super(PanelMovement.PANNING_AND_SCALING, Reverse.NO);
        }

        public void setImage(BufferedImage image)
        {
            this.image = image;

            if (image != null)
                this.initializeBoundingBox(new Rectangle2D.Double(0, 0, image.getWidth(), image.getHeight()));
            else
                this.initializeBoundingBox(new Rectangle2D.Double(0, 0, 100, 100));
        }
 
        @Override
        protected void paint(Graphics2D g2, AffineTransform tx2, Dimension size)
        {
            g2.setColor(Color.white);
            g2.fillRect(0, 0, getWidth(), getHeight());

            if (this.image != null)
                g2.drawImage(this.image, tx2, this);
            else
                g2.drawString("Unable to load image", 20, 30);
        }
    }

}
