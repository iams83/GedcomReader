package iamd.gedcom.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
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
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.JOptionPane;

import iamd.gedcom.datamodel.Document;
import iamd.gedcom.datamodel.ExifOrientationUtil;
import iamd.gedcom.datamodel.Individual;
import iamd.gedcom.datamodel.MediaObject;
import iamd.gedcom.datamodel.MediaObject.MediaType;
import iamd.gedcom.datamodel.MediaObjectReference;
import iamd.gedcom.format.GedComContext;
import iamd.ui.BorderListPanelGenerator;
import iamd.ui.ComboBoxEditor;
import iamd.ui.FilePathEditor;
import iamd.ui.RowPanelList;
import iamd.ui.RowPanelListListener;
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

    final private JLabel errorLabel = new JLabel();
    private JButton createButton;

    final private PreviewImagePanel newFilePreviewImagePanel = new PreviewImagePanel();

    final private boolean allowNew;

    private boolean accepted = false;

    private Individual selectedIndividual = null;

    /**
     * Reference to the MediaObject currently being created in the dialog, if any.
     * Used by {@link #autofillFromFile(String)} to also write the derived FORM,
     * TITL and TYPE values back to the underlying model (the
     * AttributeEditor's {@code initializeValue} only refreshes the UI and does
     * not persist anything).
     */
    private MediaObject pendingNewMediaObject = null;

    // Index of the tabs in {@link #tabbedPane} (only valid when allowNew is true).
    static final private int TAB_SELECT_EXISTING = 0;
    static final private int TAB_ADD_NEW = 1;
    static final private int TAB_BULK_IMPORT = 2;

    /**
     * Table model backing the bulk-import preview. Each row contains the
     * absolute file path, the detected format, the auto-generated title and
     * the detected {@link MediaType}.
     */
    final private DefaultTableModel bulkImportTableModel = new DefaultTableModel();

    final private JTable bulkImportTable = new JTable(this.bulkImportTableModel)
    {
        @Override
        public boolean isCellEditable(int row, int col)
        {
            return false;
        }
    };

    /** Status / info label below the bulk-import table. */
    final private JLabel bulkImportStatusLabel = new JLabel();

    /** FileChooser used to pick multiple files for bulk import. */
    final private JFileChooser bulkImportFileChooser = new JFileChooser();

    /**
     * Button that performs the actual bulk import and dismisses the dialog.
     * It is enabled only when the bulk-import selection has at least one
     * non-duplicate file.
     */
    private JButton bulkImportButton;

    /**
     * MediaObjects that have been created via the bulk-import tab during the
     * current invocation of {@link #getSelectedMediaObject()}.
     */
    private final List<MediaObject> importedMediaObjects = new ArrayList<>();

    public Individual getSelectedIndividual()
    {
        return this.selectedIndividual;
    }

    public MediaObjectDialog(JFrame frame, String title, Document document, boolean allowNew)
    {
        super(frame, title);

        this.allowNew = allowNew;

        this.setModal(true);

        this.setLocationByPlatform(true);
        this.setSize(1200, 1200);

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

            final PreviewImagePanel previewImagePanel = new PreviewImagePanel();
            previewImagePanel.setBorder(new LineBorder(Color.black, 1));

            final RowPanelList<IndividualRowPanel> individualsRowPanelList = new RowPanelList<>(Messages.getString("MediaObjectDialog.individuals"),
                                                                                                new ArrayList<IndividualRowPanel>(), null); //$NON-NLS-1$
            final JSplitPane imagePreviewPanel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, individualsRowPanelList, previewImagePanel);
            imagePreviewPanel.setResizeWeight(0.0);
            imagePreviewPanel.setDividerLocation(200);

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
                        MediaObject mediaObject = MediaObjectDialog.this.findMediaObjectByDisplayPath(selectedFile);
                        if (mediaObject != null)
                        {
                            // Find all references to this media object
                            TreeMap<Individual, MediaObjectReference> refs = new TreeMap<>();
                            ArrayList<Individual> individuals = new ArrayList<>();
                            for (Individual ind : MediaObjectDialog.this.document.listIndividuals())
                            {
                                for (MediaObjectReference ref : ind.OBJE)
                                {
                                    if (ref.mediaObject == mediaObject)
                                    {
                                        refs.put(ind, ref);
                                        individuals.add(ind);
                                    }
                                }
                            }
                            previewImagePanel.setImage(mediaObject.getImage(), refs);

                            // Recreate the individuals panel list with updated individuals
                            Collection<IndividualRowPanel> individualRowPanels = GedComRowPanelList.getIndividualRowPanelList(individuals, false, false);
                            RowPanelList<IndividualRowPanel> newIndividualsList = new RowPanelList<>(Messages.getString("MediaObjectDialog.individuals"), individualRowPanels, null); //$NON-NLS-1$
                            newIndividualsList.addRowPanelListListener(new RowPanelListListener<IndividualRowPanel>()
                            {
                                @Override
                                public void rowPanelClicked(IndividualRowPanel rowPanel)
                                {
                                    MediaObjectDialog.this.selectedIndividual = rowPanel.getIndividual();
                                    MediaObjectDialog.this.accepted = true;
                                    MediaObjectDialog.this.setVisible(false);
                                }

                                @Override
                                public void rowPanelMovedUp(IndividualRowPanel rowPanel) {}

                                @Override
                                public void rowPanelMovedDown(IndividualRowPanel rowPanel) {}

                                @Override
                                public void rowPanelDeleted(IndividualRowPanel rowPanel) {}

                                @Override
                                public void rowPanelNew() {}
                            });
                            imagePreviewPanel.setLeftComponent(newIndividualsList);
                            return;
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

            JPanel topPanel = topPanelGenerator.extractPanel(imagePreviewPanel);

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
            JPanel newIndividualPanel = createNewMediaObjectPanel(acceptActionListener, rejectActionListener);
            JPanel bulkImportPanel = createBulkImportPanel(acceptActionListener, rejectActionListener);

            this.tabbedPane.addTab(Messages.getString("SelectorDialog.selectexistingmediaobject"), existingIndividualPanel); //$NON-NLS-1$
            this.tabbedPane.addTab(Messages.getString("SelectorDialog.addmediaobject"), newIndividualPanel); //$NON-NLS-1$
            this.tabbedPane.addTab(Messages.getString("SelectorDialog.bulkimportmediaobject"), bulkImportPanel); //$NON-NLS-1$

            // Open the dialog on the "Select existing media object" tab so the
            // most common workflow (re-using a previously imported media object)
            // is just one click away.
            this.tabbedPane.setSelectedIndex(TAB_SELECT_EXISTING);

            this.add(this.tabbedPane);
        }
        else
        {
            this.add(existingIndividualPanel);
        }

        this.document = document;
    }

    /**
     * Build the panel displayed in the "Add media object" tab. Extracted from
     * the constructor to keep it readable now that the dialog has three tabs.
     */
    private JPanel createNewMediaObjectPanel(ActionListener acceptActionListener, ActionListener rejectActionListener)
    {
        JPanel newIndividualPanel = new JPanel(new BorderLayout());
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

        // Add error label
        this.errorLabel.setForeground(Color.red);
        this.errorLabel.setVisible(false);

        this.createButton = new JButton(Messages.getString("SelectorDialog.create")); //$NON-NLS-1$
        this.createButton.addActionListener(acceptActionListener);

        JButton cancelButton = new JButton(Messages.getString("SelectorDialog.cancel")); //$NON-NLS-1$
        cancelButton.addActionListener(rejectActionListener);

        BorderListPanelGenerator bottomPanelGenerator = new BorderListPanelGenerator(BorderLayout.EAST);
        bottomPanelGenerator.add(this.createButton);
        bottomPanelGenerator.add(cancelButton);

        JPanel bottomPanel = bottomPanelGenerator.extractPanel(this.errorLabel);
        bottomPanel.setBorder(new EmptyBorder(20, 0, 0, 0));

        newIndividualPanel.add(globalPanelGenerator.extractPanel(newFilePreviewImagePanel));
        newIndividualPanel.add(bottomPanel, BorderLayout.SOUTH);
        return newIndividualPanel;
    }

    /**
     * Build the panel displayed in the "Import media objects in bulk" tab.
     * It allows the user to pick several files at once and previews how they
     * will be turned into {@link MediaObject} entries before performing the
     * actual import.
     */
    private JPanel createBulkImportPanel(ActionListener acceptActionListener, ActionListener rejectActionListener)
    {
        JPanel bulkImportPanel = new JPanel(new BorderLayout());
        bulkImportPanel.setBorder(new EmptyBorder(5, 5, 5, 5));

        // Configure the file chooser: any file (we validate extensions in code),
        // directories may not be selected and multiple files can be picked.
        this.bulkImportFileChooser.setMultiSelectionEnabled(true);
        this.bulkImportFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        this.bulkImportFileChooser.setAcceptAllFileFilterUsed(true);
        // Restrict to extensions the application can classify, but allow "All
        // files" too in case the user wants to import something exotic.
        this.bulkImportFileChooser.setFileFilter(new FileNameExtensionFilter(
                Messages.getString("SelectorDialog.importFiles"),
                MediaObject.MediaType.collectSupportedExtensions()));

        JButton chooseFilesButton = new JButton(Messages.getString("SelectorDialog.choosefiles")); //$NON-NLS-1$
        chooseFilesButton.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                onChooseBulkImportFiles();
            }
        });

        BorderListPanelGenerator topPanelGenerator = new BorderListPanelGenerator(BorderLayout.NORTH);
        topPanelGenerator.add(chooseFilesButton);

        this.bulkImportTableModel.addColumn(Messages.getString("SelectorDialog.table_file")); //$NON-NLS-1$
        this.bulkImportTableModel.addColumn(Messages.getString("SelectorDialog.table_format")); //$NON-NLS-1$
        this.bulkImportTableModel.addColumn(Messages.getString("SelectorDialog.table_title")); //$NON-NLS-1$
        this.bulkImportTableModel.addColumn(Messages.getString("SelectorDialog.table_type")); //$NON-NLS-1$

        JScrollPane bulkImportTableScrollPane = new JScrollPane(this.bulkImportTable);
        bulkImportTableScrollPane.setPreferredSize(new Dimension(0, 200));

        topPanelGenerator.add(bulkImportTableScrollPane);

        this.bulkImportStatusLabel.setText(""); //$NON-NLS-1$

        this.bulkImportButton = new JButton(Messages.getString("SelectorDialog.import")); //$NON-NLS-1$
        // Uses the supplied accept listener so that closing the dialog is
        // handled in a single place. We still perform the import before the
        // dialog hides itself (see {@link #performBulkImport()}).
        this.bulkImportButton.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                performBulkImport();
                acceptActionListener.actionPerformed(e);
            }
        });
        this.bulkImportButton.setEnabled(false);

        JButton cancelButton = new JButton(Messages.getString("SelectorDialog.cancel")); //$NON-NLS-1$
        cancelButton.addActionListener(rejectActionListener);

        BorderListPanelGenerator bottomPanelGenerator = new BorderListPanelGenerator(BorderLayout.EAST);
        bottomPanelGenerator.add(this.bulkImportButton);
        bottomPanelGenerator.add(cancelButton);

        JPanel bottomPanel = bottomPanelGenerator.extractPanel(this.bulkImportStatusLabel);
        bottomPanel.setBorder(new EmptyBorder(20, 0, 0, 0));

        bulkImportPanel.add(topPanelGenerator.extractPanel(), BorderLayout.CENTER);
        bulkImportPanel.add(bottomPanel, BorderLayout.SOUTH);
        return bulkImportPanel;
    }

    /**
     * Open the multi-select file chooser and, if the user accepts, refresh
     * the bulk-import preview table with the chosen files.
     */
    private void onChooseBulkImportFiles()
    {
        File gedcomFile = this.document.getFile();
        if (gedcomFile != null && gedcomFile.getParentFile() != null)
        {
            this.bulkImportFileChooser.setCurrentDirectory(gedcomFile.getParentFile());
        }

        int result = this.bulkImportFileChooser.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION)
            return;

        File[] selectedFiles = this.bulkImportFileChooser.getSelectedFiles();
        if (selectedFiles == null || selectedFiles.length == 0)
            return;

        populateBulkImportTable(selectedFiles);
    }

    /**
     * Replace the bulk-import preview rows with one row per selected file.
     * Updates the import-button enabled state and the status label.
     */
    private void populateBulkImportTable(File[] files)
    {
        this.bulkImportTableModel.setRowCount(0);

        int duplicatesInDocument = 0;
        Set<String> seenInSelection = new HashSet<>();

        for (File file : files)
        {
            String absolutePath = file.getAbsolutePath();
            String fileName = file.getName();
            int lastDot = fileName.lastIndexOf('.');
            String extension = (lastDot > 0) ? fileName.substring(lastDot + 1).toLowerCase() : ""; //$NON-NLS-1$
            String title = (lastDot > 0) ? fileName.substring(0, lastDot) : fileName;
            MediaType mediaType = extension.isEmpty() ? null : MediaType.getMediaTypeForExtension(extension);
            String typeLabel = mediaType != null ? mediaType.name() : ""; //$NON-NLS-1$

            String duplicateMarker = ""; //$NON-NLS-1$
            if (!seenInSelection.add(absolutePath))
            {
                duplicateMarker = Messages.getString("MediaObjectDialog.duplicate_in_bulk_selection"); //$NON-NLS-1$
            }
            else if (checkForDuplicateFile(absolutePath))
            {
                duplicateMarker = Messages.getString("MediaObjectDialog.duplicate_file_error"); //$NON-NLS-1$
                duplicatesInDocument++;
            }

            this.bulkImportTableModel.addRow(new Object[] {
                absolutePath,
                extension,
                title,
                typeLabel,
                duplicateMarker
            });
        }

        int importableCount = this.bulkImportTableModel.getRowCount() - duplicatesInDocument;
        // Duplicates within the selection are still rows but neither would
        // ever become a real media object; treat them as already filtered.
        if (this.bulkImportTableModel.getRowCount() > 0)
        {
            long uniqueInSelection = java.util.Arrays.stream(files)
                    .map(File::getAbsolutePath)
                    .distinct()
                    .count();
            importableCount = (int) (uniqueInSelection - duplicatesInDocument);
        }

        if (importableCount < 0)
            importableCount = 0;

        if (this.bulkImportButton != null)
            this.bulkImportButton.setEnabled(importableCount > 0);

        String status;
        if (this.bulkImportTableModel.getRowCount() == 0)
        {
            status = Messages.getString("MediaObjectDialog.no_files_selected"); //$NON-NLS-1$
        }
        else if (duplicatesInDocument > 0)
        {
            status = MessageFormat.format(
                    Messages.getString("MediaObjectDialog.duplicate_skipped"), //$NON-NLS-1$
                    Integer.valueOf(duplicatesInDocument));
        }
        else
        {
            status = MessageFormat.format(
                    Messages.getString("MediaObjectDialog.import_completed"), //$NON-NLS-1$
                    Integer.valueOf(importableCount));
        }
        this.bulkImportStatusLabel.setText(status);
    }

    /**
     * Create one {@link MediaObject} per unique, non-duplicate file in the
     * bulk-import preview table and add it to the document. The created
     * objects are also pushed into {@link #importedMediaObjects} so callers
     * can pick them up via {@link #getImportedMediaObjects()}.
     */
    private void performBulkImport()
    {
        this.importedMediaObjects.clear();

        Set<String> alreadyImported = new HashSet<>();
        for (int row = 0; row < this.bulkImportTableModel.getRowCount(); row++)
        {
            String absolutePath = (String) this.bulkImportTableModel.getValueAt(row, 0);
            if (absolutePath == null || absolutePath.isEmpty())
                continue;
            if (!alreadyImported.add(absolutePath))
                continue;
            if (checkForDuplicateFile(absolutePath))
                continue;

            String fileName = new File(absolutePath).getName();
            int lastDot = fileName.lastIndexOf('.');
            String extension = (lastDot > 0) ? fileName.substring(lastDot + 1).toLowerCase() : null;
            String title = (lastDot > 0) ? fileName.substring(0, lastDot) : fileName;
            MediaType mediaType = extension == null ? null : MediaType.getMediaTypeForExtension(extension);

            MediaObject mediaObject = new MediaObject(this.document);
            mediaObject.FILE = absolutePath;
            mediaObject.FORM = extension;
            mediaObject.TITL = title;
            mediaObject.TYPE = mediaType;

            this.document.addNewMediaObject(mediaObject);
            this.importedMediaObjects.add(mediaObject);
        }
    }

    /**
     * Return the value displayed in the "file" column of the existing-media-object
     * table. Mirrors the logic in {@link MediaObject#getRelativeFilePath()} so
     * that paths located inside the GED file's directory are shown relative to
     * that directory (as is already done elsewhere in the UI), while paths
     * outside it or when no GED file is associated fall back to the raw value
     * stored in {@link MediaObject#FILE}.
     */
    private String getDisplayPath(MediaObject mediaObject)
    {
        if (mediaObject == null)
            return "";

        String relativePath = mediaObject.getRelativeFilePath();
        if (relativePath != null)
            return relativePath;

        return mediaObject.FILE != null ? mediaObject.FILE : "";
    }

    /**
     * Locate the {@link MediaObject} whose display path (see
     * {@link #getDisplayPath(MediaObject)}) matches the supplied value.
     */
    private MediaObject findMediaObjectByDisplayPath(String displayPath)
    {
        if (displayPath == null || displayPath.isEmpty())
            return null;

        for (MediaObject mediaObject : this.document.listMediaObjects())
        {
            if (displayPath.equals(getDisplayPath(mediaObject)))
                return mediaObject;
        }
        return null;
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
                String displayPath = getDisplayPath(mediaObject);
                if (currentFile != null && currentFile.equals(displayPath))
                    newCurrentIndex = this.tableModel.getRowCount();

                this.tableModel.addRow(new Object[] {
                    displayPath,
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

    private boolean checkForDuplicateFile(String filePath)
    {
        for (MediaObject mediaObject : this.document.listMediaObjects())
        {
            if (mediaObject.FILE != null && mediaObject.FILE.equals(filePath))
            {
                return true;
            }
        }
        return false;
    }

    private void updateCreateButtonState(String filePath)
    {
        if (filePath == null || filePath.isEmpty())
        {
            // No file selected, clear error and enable button
            this.errorLabel.setVisible(false);
            if (this.createButton != null)
            {
                this.createButton.setEnabled(true);
            }
            return;
        }

        if (checkForDuplicateFile(filePath))
        {
            // Duplicate found, disable button and show error
            this.errorLabel.setText(Messages.getString("MediaObjectDialog.duplicate_file_error")); //$NON-NLS-1$
            this.errorLabel.setVisible(true);
            if (this.createButton != null)
            {
                this.createButton.setEnabled(false);
            }
        }
        else
        {
            // No duplicate, clear error and enable button
            this.errorLabel.setVisible(false);
            if (this.createButton != null)
            {
                this.createButton.setEnabled(true);
            }
        }
    }

    private void autofillFromFile(final String filePath)
    {
        if (filePath == null || filePath.isEmpty())
            return;

        // Check for duplicate before autofilling
        updateCreateButtonState(filePath);

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

        // Update the title field display
        SwingUtilities.invokeLater(() -> {
            this.titl.initializeValue(title);

            try
            {
                BufferedImage image = ExifOrientationUtil.read(new File(filePath));

                this.newFilePreviewImagePanel.setImage(image);
            }
            catch (IOException e)
            {
                // Do nothing, just don't show a preview
            	this.newFilePreviewImagePanel.setImage(null);
            }
        });

        // Determine extension (and media type) up-front. Use final variables
        // so they can be captured by the SwingUtilities.invokeLater lambdas.
        final String extension;
        final MediaType mediaType;
        if (lastDot > 0)
        {
            extension = fileName.substring(lastDot + 1).toLowerCase();
            mediaType = MediaType.getMediaTypeForExtension(extension);
        }
        else
        {
            extension = null;
            mediaType = null;
        }

        // Update the form/type field display
        SwingUtilities.invokeLater(() -> {
            if (extension != null)
            {
                this.form.initializeValue(extension);
            }
            this.type.initializeValue(mediaType);
        });

        // Persist the derived values on the MediaObject bound to the editors.
        // initializeValue only refreshes the UI; we have to write to the model
        // explicitly so that FORM/TITL/TYPE are actually set when the user
        // presses "Create".
        if (this.pendingNewMediaObject != null)
        {
            this.pendingNewMediaObject.TITL = title;
            this.pendingNewMediaObject.FORM = extension;
            this.pendingNewMediaObject.TYPE = mediaType;
        }

        // Request focus on the title field to auto-confirm and hide the file field
        SwingUtilities.invokeLater(() -> {
            this.titl.getComponent().requestFocusInWindow();
        });
    }

    /**
     * Show the dialog, let the user pick or create a media object, and
     * return the chosen one. Returns {@code null} when the user cancels the
     * dialog <em>or</em> when the bulk-import tab was used (callers should
     * use {@link #getImportedMediaObjects()} in that case).
     */
    public MediaObject getSelectedMediaObject()
    {
        this.accepted = false;
        // Make sure no leftovers from a previous invocation leak into this one.
        this.importedMediaObjects.clear();
        this.bulkImportTableModel.setRowCount(0);
        if (this.bulkImportButton != null)
            this.bulkImportButton.setEnabled(false);
        this.bulkImportStatusLabel.setText(""); //$NON-NLS-1$
        if (this.bulkImportFileChooser != null)
            this.bulkImportFileChooser.setSelectedFiles(new File[0]);

        MediaObject newMediaObject = new MediaObject(this.document);
        // Keep a reference for autofillFromFile so it can persist FORM/TITL/TYPE
        // directly on this instance. Cleared when the dialog returns.
        this.pendingNewMediaObject = newMediaObject;

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

        // Dialog is closing - release the reference.
        this.pendingNewMediaObject = null;

        if (!this.accepted)
            return null;

        // If the bulk-import tab was used, the resulting MediaObjects are
        // already accessible via getImportedMediaObjects(). Return null from
        // this method so that legacy callers don't accidentally treat the
        // list as a single object.
        if (this.allowNew && this.tabbedPane.getSelectedIndex() == TAB_BULK_IMPORT)
            return null;

        if (!this.allowNew || this.tabbedPane.getSelectedIndex() == TAB_SELECT_EXISTING)
        {
            int selectedRow = this.table.getSelectedRow();

            // Find the MediaObject matching the selected FILE value. The
            // displayed value in the table is the path relative to the GED
            // file's directory when applicable (see getDisplayPath), so we
            // resolve it back to a MediaObject using the same helper used by
            // the row selection listener.
            String selectedFile = (String) this.tableModel.getValueAt(selectedRow, 0);
            MediaObject selectedMediaObject = findMediaObjectByDisplayPath(selectedFile);
            if (selectedMediaObject != null)
                return selectedMediaObject;

            // Fallback: match against the raw FILE value for backward
            // compatibility (e.g. when nothing was selected but a value is
            // still present in the model).
            for (MediaObject mo : this.document.listMediaObjects())
            {
                if (selectedFile != null && selectedFile.equals(mo.FILE))
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

    /**
     * Return a snapshot of the media objects that were imported through the
     * bulk-import tab during the last call to {@link #getSelectedMediaObject()}.
     * The returned list is empty when:
     * <ul>
     *   <li>the dialog has not been shown yet,</li>
     *   <li>the user cancelled the dialog, or</li>
     *   <li>the user used one of the single-object tabs.</li>
     * </ul>
     * Callers that use the bulk-import tab should call this method
     * <em>after</em> {@link #getSelectedMediaObject()} to retrieve the
     * freshly created objects.
     */
    public List<MediaObject> getImportedMediaObjects()
    {
        return new ArrayList<>(this.importedMediaObjects);
    }

    // Read-only image panel for displaying pictures. Inherits all drawing
    // logic from MediaObjectDisplayPanel so the dialog's previews stay
    // visually identical to the main display and pick up any future
    // improvements (new colors, label layout, ...) automatically.
    private class PreviewImagePanel extends MediaObjectDisplayPanel
    {
        public PreviewImagePanel()
        {
            super();
        }

        public void setImage(BufferedImage image)
        {
            setImage(image, null);
        }

        public void setImage(BufferedImage image,
                             TreeMap<Individual, MediaObjectReference> references)
        {
            setImageAndReferences(image, references);
        }
    }


}
