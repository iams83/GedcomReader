package iamd.gedcom.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;

import iamd.gedcom.datamodel.Document;
import iamd.gedcom.datamodel.Head;
import iamd.gedcom.datamodel.Head.GedComFormat;
import iamd.gedcom.datamodel.Head.Source;
import iamd.gedcom.datamodel.Submitter;
import iamd.gedcom.datamodel.Submitter.SubmitterName;
import iamd.gedcom.ui.editors.LongTextEditor;
import iamd.ui.BorderListPanelGenerator;
import iamd.ui.TextLineEditor;

@SuppressWarnings("serial")
public class MetadataEditorDialog extends JDialog
{
    static final private class ReadOnlyTextField extends JTextField
    {
        public ReadOnlyTextField()
        {
            this.setEditable(false);
            this.setFont(this.getFont().deriveFont(Font.BOLD));
        }
    }
    
    final private ReadOnlyTextField filePath     = new ReadOnlyTextField();
    final private ReadOnlyTextField gedcVers     = new ReadOnlyTextField();
    final private ReadOnlyTextField gedcForm     = new ReadOnlyTextField();
    final private ReadOnlyTextField fileEncoding = new ReadOnlyTextField();
    final private ReadOnlyTextField fileDate     = new ReadOnlyTextField();

    final private TextLineEditor sourceNameLine = new TextLineEditor();
    final private TextLineEditor sourceName     = new TextLineEditor();
    final private TextLineEditor sourceVers     = new TextLineEditor();
    final private TextLineEditor sourceCorp     = new TextLineEditor();
    final private TextLineEditor dest           = new TextLineEditor();

    final private TextLineEditor submName       = new TextLineEditor();
    final private TextLineEditor submSurn       = new TextLineEditor();
    final private LongTextEditor submAddr       = new LongTextEditor(4);
    final private TextLineEditor submPhon       = new TextLineEditor();
    final private TextLineEditor submEmail      = new TextLineEditor();
    final private LongTextEditor submComm       = new LongTextEditor(4);
    final private LongTextEditor submNote       = new LongTextEditor(4);

    public MetadataEditorDialog(JFrame parent)
    {
        super(parent);
        
        this.setTitle(MainWindow.TITLE);
        this.setModal(true);
        this.setSize(new Dimension(300, 600));
        this.setLayout(new BorderLayout());

        BorderListPanelGenerator gedFormatPanel = new BorderListPanelGenerator(BorderLayout.NORTH);
        gedFormatPanel.setBackground(this.getBackground());
        gedFormatPanel.add(createTopBorder(newJLabel(Messages.getString("MetadataEditorDialog.filePath")))); //$NON-NLS-1$
        gedFormatPanel.add(this.filePath);
        gedFormatPanel.add(createTopBorder(newJLabel(Messages.getString("MetadataEditorDialog.gedcVers")))); //$NON-NLS-1$
        gedFormatPanel.add(this.gedcVers);
        gedFormatPanel.add(createTopBorder(newJLabel(Messages.getString("MetadataEditorDialog.gedcForm")))); //$NON-NLS-1$
        gedFormatPanel.add(this.gedcForm);
        gedFormatPanel.add(createTopBorder(newJLabel(Messages.getString("MetadataEditorDialog.fileEncoding")))); //$NON-NLS-1$
        gedFormatPanel.add(this.fileEncoding);
        gedFormatPanel.add(createTopBorder(newJLabel(Messages.getString("MetadataEditorDialog.fileDate")))); //$NON-NLS-1$
        gedFormatPanel.add(this.fileDate);
        
        JLabel sourceNameLabel, destLabel;
        gedFormatPanel.add(sourceNameLabel = (newJLabel(Messages.getString("MetadataEditorDialog.sourceNameLine")))); //$NON-NLS-1$
        gedFormatPanel.add(this.sourceNameLine);
        gedFormatPanel.add(createTopBorder(newJLabel(Messages.getString("MetadataEditorDialog.sourceName")))); //$NON-NLS-1$
        gedFormatPanel.add(this.sourceName);
        gedFormatPanel.add(createTopBorder(newJLabel(Messages.getString("MetadataEditorDialog.sourceVers")))); //$NON-NLS-1$
        gedFormatPanel.add(this.sourceVers);
        gedFormatPanel.add(createTopBorder(newJLabel(Messages.getString("MetadataEditorDialog.sourceCorp")))); //$NON-NLS-1$
        gedFormatPanel.add(this.sourceCorp);
        gedFormatPanel.add(destLabel = (newJLabel(Messages.getString("MetadataEditorDialog.dest")))); //$NON-NLS-1$
        gedFormatPanel.add(this.dest);

        sourceNameLabel.setBorder(new EmptyBorder(20, 0, 0, 0));
        sourceNameLabel.setBackground(new Color(0, 0, 0, 0));
        
        destLabel.setBorder(new EmptyBorder(20, 0, 0, 0));
        destLabel.setBackground(new Color(0, 0, 0, 0));

        BorderListPanelGenerator submitterPanel = new BorderListPanelGenerator(BorderLayout.NORTH);
        submitterPanel.setBackground(this.getBackground());
        submitterPanel.add(createTopBorder(newJLabel(Messages.getString("MetadataEditorDialog.submName")))); //$NON-NLS-1$
        submitterPanel.add(this.submName);
        submitterPanel.add(createTopBorder(newJLabel(Messages.getString("MetadataEditorDialog.submSurname")))); //$NON-NLS-1$
        submitterPanel.add(this.submSurn);
        submitterPanel.add(createTopBorder(newJLabel(Messages.getString("MetadataEditorDialog.submAddress")))); //$NON-NLS-1$
        submitterPanel.add(this.submAddr);
        submitterPanel.add(createTopBorder(newJLabel(Messages.getString("MetadataEditorDialog.submPhone")))); //$NON-NLS-1$
        submitterPanel.add(this.submPhon);
        submitterPanel.add(createTopBorder(newJLabel(Messages.getString("MetadataEditorDialog.submEmail")))); //$NON-NLS-1$
        submitterPanel.add(this.submEmail);
        submitterPanel.add(createTopBorder(newJLabel(Messages.getString("MetadataEditorDialog.submComm")))); //$NON-NLS-1$
        submitterPanel.add(this.submComm);
        submitterPanel.add(createTopBorder(newJLabel(Messages.getString("MetadataEditorDialog.submNote")))); //$NON-NLS-1$
        submitterPanel.add(this.submNote);
        
        JPanel submitterPanel0 = new JPanel(new BorderLayout());
        submitterPanel0.setBackground(this.getBackground());
        submitterPanel0.add(submitterPanel.extractPanel());

        JTabbedPane tabbedPane = new JTabbedPane();

        tabbedPane.add(gedFormatPanel.extractPanel(), Messages.getString("MetadataEditorDialog.fileFormat"));
        tabbedPane.add(submitterPanel0, Messages.getString("MetadataEditorDialog.submitter"));
        
        tabbedPane.setBackground(this.getBackground());
        tabbedPane.setBorder(new EmptyBorder(2, 2, 2, 2));
        this.add(tabbedPane);
    }

    protected JLabel newJLabel(String string)
    {
        JLabel label = new JLabel(string);
        label.setBackground(new Color(0, 0, 0, 0));
        return label;
    }

    protected JTextField newReadonlyJTextField(String string)
    {
        JTextField textField = new JTextField(string);
        textField.setBackground(new Color(0, 0, 0, 0));
        textField.setBorder(new EtchedBorder(EtchedBorder.LOWERED));
        textField.setEditable(false);
        return textField;
    }

    protected JComponent createTopBorder(JComponent component)
    {
        component.setBorder(new EmptyBorder(6, 0, 0, 0));
        component.setBackground(new Color(0, 0, 0, 0));
        return component;
    }

    public void display(Document model)
    {
        this.submComm.setDocument(model);
        this.submAddr.setDocument(model);
        this.submNote.setDocument(model);
        
        
        if (model.getFile() == null)
            this.setTitle(Messages.getString("MetadataEditorDialog.properties"));
        else
            this.setTitle(Messages.getString("MetadataEditorDialog.propertiesFor") + model.getFile().getName());
            
        this.setLocationRelativeTo(this.getParent());
        
        if (model.HEAD == null)
            model.HEAD = new Head(model);
        
        if (model.HEAD.GEDC == null)
            model.HEAD.GEDC = new GedComFormat(model);
        
        String headDate = null;
        
        if (model.HEAD.DATE != null)
            headDate = model.HEAD.DATE.dateToString(true);
        
        if (model.HEAD.SOUR == null)
            model.HEAD.SOUR = new Source(model);
        
        if (model.SUBM == null)
            model.SUBM = new Submitter(model);

        if (model.SUBM.NAME == null)
            model.SUBM.NAME = new SubmitterName(model);
        
        this.filePath    .setText(model.getFile() != null ? "" : model.getFile().getAbsolutePath());
        this.gedcVers    .setText(model.HEAD.GEDC.VERS);
        this.gedcForm    .setText(model.HEAD.GEDC.FORM);
        this.fileEncoding.setText(model.HEAD.CHAR);
        this.fileDate    .setText(headDate);
        
        this.sourceNameLine.bindValue(model.HEAD.SOUR, "sourceName");
        this.sourceName    .bindValue(model.HEAD.SOUR, "NAME");
        this.sourceVers    .bindValue(model.HEAD.SOUR, "VERS");
        this.sourceCorp    .bindValue(model.HEAD.SOUR, "CORP");
        this.sourceCorp    .bindValue(model.HEAD, "DEST");
        
        this.submName .bindValue(model.SUBM.NAME, "name");
        this.submSurn .bindValue(model.SUBM.NAME, "surname");
        this.submAddr .bindValue(model.SUBM, "ADDR");
        this.submPhon .bindValue(model.SUBM, "PHON");
        this.submEmail.bindValue(model.SUBM, "EMAIL");
        this.submComm .bindValue(model.SUBM, "COMM");
        this.submNote .bindValue(model.SUBM, "NOTE");

        this.setVisible(true);

        if (model.SUBM.NAME != null && model.SUBM.NAME.isEmpty())
            model.SUBM.NAME = null;

        if (model.SUBM.PHON != null && model.SUBM.PHON.isEmpty())
            model.SUBM.PHON = null;

        if (model.SUBM.EMAIL != null && model.SUBM.EMAIL.isEmpty())
            model.SUBM.EMAIL = null;

        if (model.SUBM != null && model.SUBM.isEmpty())
            model.SUBM = null;
        
        if (model.HEAD.GEDC != null && model.HEAD.GEDC.isEmpty())
            model.HEAD.GEDC = null;
        
        if (model.HEAD != null && model.HEAD.isEmpty())
            model.HEAD = null;
        
    }

}
