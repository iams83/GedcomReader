package iamd.gedcom.ui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.Arrays;
import java.util.TreeSet;

import javax.imageio.ImageIO;
import javax.swing.ButtonGroup;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.apache.batik.dom.GenericDOMImplementation;
import org.apache.batik.svggen.SVGGraphics2D;
import org.w3c.dom.DOMImplementation;

import iamd.gedcom.datamodel.Bool;
import iamd.gedcom.datamodel.Document;
import iamd.gedcom.datamodel.Family;
import iamd.gedcom.datamodel.Individual;
import iamd.gedcom.datamodel.Individual.FamilyChildRelationship;
import iamd.gedcom.datamodel.Individual.Sex;
import iamd.gedcom.datamodel.MediaObject;
import iamd.gedcom.datamodel.MediaObjectReference;
import iamd.gedcom.format.GedComNode;
import iamd.gedcom.format.GedComNode.LocalizedGedComNode;
import iamd.gedcom.format.IdentifiedGedComNode;
import iamd.gedcom.format.LocalizedGedComParseException;
import iamd.gedcom.rsrc.Resources;
import iamd.ui.ChartPanelListener;
import iamd.ui.ErrorMessage;
import say.swing.JFontChooser;

@SuppressWarnings("serial")
public class MainWindow extends JFrame
{
    public static final String APP_NAME = Messages.getString("MainWindow.title"); //$NON-NLS-1$
    
    public static final String VERSION = "1.1.0"; //$NON-NLS-1$

    public static final String TITLE = APP_NAME + " " + VERSION; //$NON-NLS-1$
    
    enum LeftPanelCard
    {
        FAMILY_PANEL, INDIVIDUAL_PANEL, MEDIA_OBJECT_PANEL
    }
        
    enum MainPanelCard
    {
        GRAPH_PANEL, CHART_PANEL, MEDIA_OBJECT_PANEL
    }

    final private ChartPanelWithToolbar chartPanel = new ChartPanelWithToolbar();
    
    final private GraphPanelWithToolbar graphPanel = new GraphPanelWithToolbar();
    
    final private MetadataEditorDialog  metadataEditorDialog  = new MetadataEditorDialog(this);

    final private IndividualEditorPanel individualEditorPanel = new IndividualEditorPanel(this);
    
    final private FamilyEditorPanel     familyEditorPanel     = new FamilyEditorPanel(this);
    
    final private MediaObjectEditorPanel mediaObjectEditorPanel = new MediaObjectEditorPanel(this);
    
    final private MediaObjectPanelWithToolbar mediaObjectDisplayPanel = new MediaObjectPanelWithToolbar();
    
    final private CardLayout leftCardLayout = new CardLayout();
    
    final private JPanel leftStackPanel = new JPanel(this.leftCardLayout);
    
    final private CardLayout mainCardLayout = new CardLayout();
    
    final private JPanel mainStackPanel = new JPanel(this.mainCardLayout);
    
    final private JLabel statusLine = new JLabel();

    final private GedComPreferences prefs = new GedComPreferences();
    
    final private JFileChooser chooser = new JFileChooser();
    {
        this.chooser.setFileFilter(new FileNameExtensionFilter(Messages.getString("MainWindow.fileextname") + " (*.ged *.zip)", "ged", "zip")); //$NON-NLS-1$ //$NON-NLS-2$
    }
    
    private Document model = new Document(null); 
    
    private boolean unsavedChanges;

    private MainPanelCard selectedDiagram = MainPanelCard.CHART_PANEL;
    
    // Diagram menu items (kept as fields so the toolbar can sync with them)
    private JRadioButtonMenuItem ascendentChartMenuItem;
    private JRadioButtonMenuItem descendentChartMenuItem;
    private JRadioButtonMenuItem twoWayChartMenuItem;
    private JRadioButtonMenuItem dynamicGraphMenuItem;
    
    public MainWindow()
    {        
        this.setTitle(TITLE);
        this.setIconImage(Resources.LavandaIconMini.getImage());
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.setLocationByPlatform(true);

        this.leftStackPanel.add(this.individualEditorPanel, LeftPanelCard.INDIVIDUAL_PANEL.name());
        this.leftStackPanel.add(this.familyEditorPanel,     LeftPanelCard.FAMILY_PANEL.name());
        this.leftStackPanel.add(this.mediaObjectEditorPanel, LeftPanelCard.MEDIA_OBJECT_PANEL.name());
        
        this.mainStackPanel.add(this.chartPanel, MainPanelCard.CHART_PANEL.name());
        this.mainStackPanel.add(this.graphPanel, MainPanelCard.GRAPH_PANEL.name());
        this.mainStackPanel.add(this.mediaObjectDisplayPanel, MainPanelCard.MEDIA_OBJECT_PANEL.name());
        
        this.add(new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, this.leftStackPanel, this.mainStackPanel));
        this.add(this.statusLine, BorderLayout.SOUTH);
        this.setSize(1800, 1400);
        
        this.statusLine.setBorder(new EmptyBorder(2, 2, 2, 2));
        
        FamilySelectionListener familySelectionListener = new FamilySelectionListener()
        {
            @Override
            public void familyClicked(Family family)
            {
                MainWindow.this.selectFamily(family);
            }

            @Override
            public void individualClicked(Individual individual)
            {
                MainWindow.this.selectIndividual(individual);
            }

            @Override
            public void familyHovered(Family family)
            {
                String tooltipText = "Family: " + family.getSpouseNames();
                
                MainWindow.this.graphPanel.setToolTipText(tooltipText);
                MainWindow.this.chartPanel.setToolTipText(tooltipText);
                MainWindow.this.statusLine.setText(tooltipText);
            }

            @Override
            public void individualHovered(Individual individual)
            {
                String tooltipText = 
                        (Sex.toCharSymbol(individual.SEX) + " " +  //$NON-NLS-1$
                                individual.NAME.name + " " +  //$NON-NLS-1$
                                individual.NAME.surname + " " + //$NON-NLS-1$
                                (individual.DEAT != null && individual.DEAT.happened != Bool.N ? "\u271d" : "")).trim(); //$NON-NLS-1$ //$NON-NLS-2$
                
                MainWindow.this.graphPanel.setToolTipText(tooltipText);
                MainWindow.this.chartPanel.setToolTipText(tooltipText);
                MainWindow.this.statusLine.setText(tooltipText);
            }

            @Override
            public void nothingHovered()
            {
                MainWindow.this.graphPanel.setToolTipText("");
                MainWindow.this.chartPanel.setToolTipText("");
                MainWindow.this.statusLine.setText(" ");
            }
        };
        
        this.individualEditorPanel.addFamilySelectionListener(familySelectionListener);
        
        this.familyEditorPanel    .addFamilySelectionListener(familySelectionListener);
        
        this.mediaObjectEditorPanel.addFamilySelectionListener(familySelectionListener);
        
        this.mediaObjectDisplayPanel.setMediaObjectDisplayListener(new MediaObjectDisplayPanel.MediaObjectDisplayListener()
        {
            @Override
            public void rectangleClicked(Individual individual, MediaObjectReference ref)
            {
                MainWindow.this.selectIndividual(individual);
            }
        });
        
        // Register chart toolbar listener to handle the "switch to graph" action
        // and to sync the Diagram menu when a chart type is selected via the toolbar.
        this.chartPanel.setChartToolbarListener(new ChartPanelWithToolbar.ChartToolbarListener()
        {
            @Override
            public void onSwitchToGraph()
            {
                MainWindow.this.showGraphPanel();
            }
            
            @Override
            public void onChartTypeSelectedFromToolbar(GedComChartPanel.ChartType type)
            {
                // Sync the menu with the toolbar selection
                if (MainWindow.this.ascendentChartMenuItem != null)
                    MainWindow.this.ascendentChartMenuItem.setSelected(type == GedComChartPanel.ChartType.ParentChart);
                if (MainWindow.this.descendentChartMenuItem != null)
                    MainWindow.this.descendentChartMenuItem.setSelected(type == GedComChartPanel.ChartType.DescendantChart);
                if (MainWindow.this.twoWayChartMenuItem != null)
                    MainWindow.this.twoWayChartMenuItem.setSelected(type == GedComChartPanel.ChartType.TwoWayChart);
                if (MainWindow.this.dynamicGraphMenuItem != null)
                    MainWindow.this.dynamicGraphMenuItem.setSelected(false);
                
                // Make sure the chart panel is showing
                MainWindow.this.selectedDiagram = MainPanelCard.CHART_PANEL;
                MainWindow.this.mainCardLayout.show(MainWindow.this.mainStackPanel, MainPanelCard.CHART_PANEL.name());
            }
        });
        
        // Register graph toolbar listener to handle chart type selections
        // (switches to the chart view) and the "switch to graph" action.
        this.graphPanel.setGraphToolbarListener(new GraphPanelWithToolbar.GraphToolbarListener()
        {
            @Override
            public void onSwitchToGraph()
            {
                MainWindow.this.showGraphPanel();
            }
            
            @Override
            public void onChartTypeSelectedFromToolbar(GedComChartPanel.ChartType type)
            {
                // Switch to the chart panel with the selected type
                MainWindow.this.showChartPanel(type);
            }
        });
        
        this.addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosing(WindowEvent e)
            {
                if (!MainWindow.this.unsavedChanges || 
                    JOptionPane.showConfirmDialog(MainWindow.this, 
                            Messages.getString("MainWindow.confirmexitlosingchanges"), 
                            TITLE, JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
                {
                    MainWindow.this.dispose();
                    System.exit(0);
                }
            }
        });
        
        Font chartPanelFont = this.prefs.getLastFont();
        
        if (chartPanelFont != null)
            this.chartPanel.setFont(chartPanelFont);
        
        this.chartPanel.addChartPanelListener(new ChartPanelListener<GedComChartElement>()
        {
            @Override
            public void mouseEntered(MouseEvent e, GedComChartElement arc)
            {
                Individual individual = arc.getIndividual();
                
                familySelectionListener.individualHovered(individual);
            }

            @Override
            public void mouseExited(MouseEvent e, GedComChartElement arc)
            {
                if (MainWindow.this.chartPanel.getArcAt(e.getPoint()) == null)
                {
                    MainWindow.this.statusLine.setText(" "); //$NON-NLS-1$
                    MainWindow.this.chartPanel.setToolTipText(""); //$NON-NLS-1$
                }
            }

            @Override
            public void mouseMoved(MouseEvent e, GedComChartElement arc)
            {
            }

            @Override
            public void mouseClicked(MouseEvent e, GedComChartElement arc)
            {
                SwingUtilities.invokeLater(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        MainWindow.this.selectIndividual(arc.getIndividual());
                    }
                });
            }
        });
        
        this.graphPanel.addFamilySelectionListener(familySelectionListener);
        
        GedComModifiedListener attributeEditorListener = new GedComModifiedListener()
        {
            @Override
            public void attributeModified(IdentifiedGedComNode o)
            {
                if (o instanceof Individual)
                    ((Individual) o).fireAttributeChanged();
                
                else if (o instanceof Family)
                    ((Family) o).fireAttributeChanged();
                
                else
                    throw new AssertionError();
                
                MainWindow.this.unsavedChanges = true;
                
                MainWindow.this.updateTitle();
                
                MainWindow.this.chartPanel.reloadModel();
                
                MainWindow.this.graphPanel.reloadModel();
            }
        };
        
        this.individualEditorPanel.addAttributeEditorListener(attributeEditorListener);
        this.familyEditorPanel    .addAttributeEditorListener(attributeEditorListener);
        this.mediaObjectEditorPanel.addAttributeEditorListener(attributeEditorListener);
        
        // When an individual is unlinked from / linked to a media object via the
        // editor panel, refresh the media object's display panel so that the
        // rectangles drawn on top of the image are kept in sync.
        this.mediaObjectEditorPanel.addMediaObjectChangedListener(new Runnable()
        {
            @Override
            public void run()
            {
                MainWindow.this.mediaObjectDisplayPanel.setModel(
                        MainWindow.this.mediaObjectEditorPanel.getMediaObject());
            }
        });
        
        JMenuBar menubar = new JMenuBar();
        
        menubar.add(createFileMenu());
        menubar.add(createToolMenu());
        menubar.add(createDiagramMenu());
        menubar.add(createHelpMenu());
        
        this.setJMenuBar(menubar);

        this.setVisible(true);

        MainWindow.this.chartPanel.initializeBoundingBox();
        
        SwingUtilities.invokeLater(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    String lastOpenedFile = MainWindow.this.prefs.getLastOpenedFileName();
                    
                    if (lastOpenedFile != null)
                        MainWindow.this.openGedFile(new File(lastOpenedFile));
                }
                catch(Throwable e1)
                {
                    ErrorMessage.showErrorMessage(MainWindow.this, e1, TITLE);
                }
            }
        });
    }
    
    private JMenu createFileMenu()
    {
        JMenu fileMenu = new JMenu(Messages.getString("MainWindow.file")); //$NON-NLS-1$
        
        JMenuItem newFile    = new JMenuItem(Messages.getString("MainWindow.new")); //$NON-NLS-1$
        JMenuItem openFile   = new JMenuItem(Messages.getString("MainWindow.open")); //$NON-NLS-1$
        JMenuItem saveFile   = new JMenuItem(Messages.getString("MainWindow.save")); //$NON-NLS-1$
        JMenuItem saveFileAs = new JMenuItem(Messages.getString("MainWindow.saveas")); //$NON-NLS-1$
        JMenuItem exit       = new JMenuItem(Messages.getString("MainWindow.exit")); //$NON-NLS-1$
        
        String currentFileName = this.prefs.getLastOpenedFileName();
        
        if (currentFileName != null)
            this.chooser.setSelectedFile(new File(currentFileName));

        newFile.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {                
                if (!MainWindow.this.unsavedChanges || 
                        JOptionPane.showConfirmDialog(MainWindow.this, 
                                Messages.getString("MainWindow.confirmcontinuelosingchanges"), 
                                TITLE, JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
                {
                    MainWindow.this.newGedFile();
                }
            }
        });

        openFile.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                try
                {
                    if (MainWindow.this.chooser.showOpenDialog(MainWindow.this) == JFileChooser.APPROVE_OPTION)
                    {
                        if (!MainWindow.this.unsavedChanges || 
                                JOptionPane.showConfirmDialog(MainWindow.this, 
                                        Messages.getString("MainWindow.confirmcontinuelosingchanges"), 
                                        TITLE, JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
                        {
                            File selectedFile = MainWindow.this.chooser.getSelectedFile();
                            
                            MainWindow.this.openGedFile(selectedFile);
                        }
                    }
                }
                catch(Throwable e1)
                {
                    ErrorMessage.showErrorMessage(MainWindow.this, e1, TITLE);
                }
            }
        });
        
        ActionListener saveFileAction = new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                try
                {
                    if (e.getSource() == saveFileAs || MainWindow.this.model.getFile() == null)
                    {
                        if (MainWindow.this.chooser.showSaveDialog(MainWindow.this) == JFileChooser.APPROVE_OPTION)
                        {
                            File selectedFile = MainWindow.this.chooser.getSelectedFile();
    
                            MainWindow.this.saveGedFile(selectedFile);
                        }
                    }
                    else
                    {
                        MainWindow.this.saveGedFile(MainWindow.this.model.getFile());
                    }
                }
                catch(Throwable e1)
                {
                    ErrorMessage.showErrorMessage(MainWindow.this, e1, TITLE);
                }
            }
        };
        
        saveFile.addActionListener(saveFileAction);
        saveFileAs.addActionListener(saveFileAction);

        exit.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {                
                if (!MainWindow.this.unsavedChanges || 
                        JOptionPane.showConfirmDialog(MainWindow.this, 
                                Messages.getString("MainWindow.confirmcontinuelosingchanges"), 
                                TITLE, JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
                {
                    MainWindow.this.dispose();
                    
                    System.exit(0);
                }
            }
        });

        JMenuItem docProperties = new JMenuItem(Messages.getString("MainWindow.docproperties")); //$NON-NLS-1$
        
        docProperties.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                MainWindow.this.metadataEditorDialog.display(MainWindow.this.model);
            }
        });
        
        JMenuItem exportAs = new JMenuItem(Messages.getString("MainWindow.exportimage")); //$NON-NLS-1$
        
        exportAs.addActionListener(new ActionListener()
        {
            final private TreeSet<String> chartImageFormats = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
            
            final private JFileChooser chartExportationFileChooser = new JFileChooser();
            
            final private JFileChooser graphExportationFileChooser = new JFileChooser();

            {
                this.chartExportationFileChooser.addChoosableFileFilter(
                        new FileNameExtensionFilter(Messages.getString("MainWindow.svgfilename") + " SVG (*.svg)", "svg")); //$NON-NLS-1$ //$NON-NLS-2$
                
                this.chartImageFormats.addAll(Arrays.asList(ImageIO.getWriterFormatNames()));
                
                for (String imageFormat : this.chartImageFormats)
                {
                    String format = imageFormat.toLowerCase();
                    
                    if (format.equals("jpeg"))
                        continue;
                    
                    this.chartExportationFileChooser.addChoosableFileFilter(
                            new FileNameExtensionFilter(
                                    Messages.getString("MainWindow.imgfilename") + " " + format.toUpperCase() 
                                        + " (*." + format + ")", format)); //$NON-NLS-1$ //$NON-NLS-2$
                    
                    this.graphExportationFileChooser.addChoosableFileFilter(
                            new FileNameExtensionFilter(
                                    Messages.getString("MainWindow.imgfilename") + " " + format.toUpperCase() 
                                        + " (*." + format + ")", format)); //$NON-NLS-1$ //$NON-NLS-2$
                }
            }
                        @Override
            public void actionPerformed(ActionEvent e)
            {
                try
                {
                    if (MainWindow.this.selectedDiagram == MainPanelCard.CHART_PANEL)
                    {
                        if (this.chartExportationFileChooser.showSaveDialog(MainWindow.this) == JFileChooser.APPROVE_OPTION)
                        {
                            File selectedFile = this.chartExportationFileChooser.getSelectedFile();
        
                            String extension = selectedFile.getName().substring(selectedFile.getName().lastIndexOf(".") + 1).toLowerCase();
                        
							if (extension.equals("svg"))
							{
								exportAsSvg(selectedFile);
							}
							else if (this.chartImageFormats.contains(extension))
							{
								BufferedImage image = exportAsImage(!extension.equals("png") && !extension.equals("gif"));
                            
                                ImageIO.write(image, extension, selectedFile);
                            }
                        }
                    }
                    else
                    {
                        if (this.graphExportationFileChooser.showSaveDialog(MainWindow.this) == JFileChooser.APPROVE_OPTION)
                        {
                            File selectedFile = this.graphExportationFileChooser.getSelectedFile();

                            MainWindow.this.graphPanel.exportAsImage(selectedFile);
                        }
                    }
                }
                catch (Throwable e1)
                {
                    ErrorMessage.showErrorMessage(MainWindow.this, e1, TITLE);
                }
            }
        });
        
        fileMenu.add(newFile);
        fileMenu.add(openFile);
        fileMenu.addSeparator();
        fileMenu.add(saveFile);
        fileMenu.add(saveFileAs);
        fileMenu.addSeparator();
        fileMenu.addSeparator();
        fileMenu.add(docProperties);
        fileMenu.add(exportAs);
        fileMenu.addSeparator();
        fileMenu.add(exit);

        return fileMenu;
    }

    private JMenu createToolMenu()
    {
        JMenuItem searchPeople = new JMenuItem(Messages.getString("MainWindow.findpeople")); //$NON-NLS-1$
        
        searchPeople.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                IndividualSelectorDialog individualSelector = new IndividualSelectorDialog(
                        MainWindow.this, 
                        Messages.getString("MainWindow.findpeople"),  //$NON-NLS-1$
                        MainWindow.this.model, false);
                
                Individual individual = individualSelector.getSelectedIndividual();
                
                if (individual != null)
                    MainWindow.this.selectIndividual(individual);
            }
        });

        JMenuItem searchFamily = new JMenuItem(Messages.getString("MainWindow.findfamily")); //$NON-NLS-1$
        
        searchFamily.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                FamilySelectorDialog familySelector = new FamilySelectorDialog(
                        MainWindow.this, 
                        Messages.getString("MainWindow.findfamily"),  //$NON-NLS-1$
                        MainWindow.this.model, false);
                
                Family family = familySelector.getSelectedFamily();
                
                if (family != null)
                    MainWindow.this.selectFamily(family);
            }
        });
        
        JMenuItem diffUnsavedChanges = new JMenuItem(Messages.getString("MainWindow.diffUnsavedChanges"));
        
        diffUnsavedChanges.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                try
                {
                    String[] diffExeAlternatives = {
                        "C:\\Program Files\\WinMerge\\WinMergeU.exe",
                        "C:\\Program Files (x86)\\WinMerge\\WinMergeU.exe"
                    };
                    
                    File currentFile = MainWindow.this.model.getFile();
                    
                    File tmpFile = File.createTempFile("gedcom-", ".ged", new File(System.getProperty("java.io.tmpdir")));
                    
                    MainWindow.this.saveGedFile(tmpFile);

                    MainWindow.this.model.forceFile(currentFile);
                    
                    if (currentFile != null)
                    {
                        for (String diffExe : diffExeAlternatives)
                        {
                            if (new File(diffExe).exists())
                            {
                                Runtime.getRuntime().exec(new String[] { diffExe, MainWindow.this.model.getFile().getPath(), tmpFile.getPath() });
                                return;
                            }
                        }
                    }
                }
                catch (Throwable e1)
                {
                    ErrorMessage.showErrorMessage(MainWindow.this, e1, TITLE);
                } 
            }
        });

        JMenuItem cascadeRemoval = new JMenuItem(Messages.getString("MainWindow.cascadeRemoval"));
        
        cascadeRemoval.addActionListener(new ActionListener()
        {
            private TreeSet<String> individualsToRemove = new TreeSet<>();
            private TreeSet<String> familiesToRemove = new TreeSet<>();
            
            @Override
            public void actionPerformed(ActionEvent e)
            {
                try
                {
                    this.individualsToRemove.clear();
                    this.familiesToRemove.clear();
                    
                    Individual individual = MainWindow.this.graphPanel.getSelectedIndividual();
                    
                    if (individual != null)
                        individualCascadeRemoval(individual);
                    
                    Family family = MainWindow.this.graphPanel.getSelectedFamily();
                    
                    if (family != null)
                        individualCascadeRemoval(family.getMember());
                    
                    if (this.individualsToRemove.size() == MainWindow.this.model.listIndividuals().size())
                    {
                        JOptionPane.showMessageDialog(MainWindow.this, 
                                Messages.getString("MainWindow.cascadeRemoval.badInput"), TITLE, JOptionPane.OK_OPTION);
                        
                        return;
                    }
                    
                    for (String id : this.individualsToRemove)
                    {
                        Individual individualToRemove = (Individual) MainWindow.this.model.getObjectById(id);
                        
                        if (individualToRemove != null)
                        {
                            individualToRemove.remove();
                            
                            MainWindow.this.unsavedChanges = true;
                            
                            MainWindow.this.updateTitle();
                        }
                    }
    
                    Individual anyIndividual = MainWindow.this.model.listIndividuals().iterator().next();
                    
                    MainWindow.this.selectIndividual(anyIndividual);
    
                    MainWindow.this.graphPanel.reloadModel();
                }
                catch (Throwable e1)
                {
                    ErrorMessage.showErrorMessage(MainWindow.this, e1, TITLE);
                } 
            }

            private void individualCascadeRemoval(Individual individual)
            {
                String id = MainWindow.this.model.getID(individual);
                
                if (this.individualsToRemove.contains(id))
                    return;
                
                this.individualsToRemove.add(id);
                
                for (Family family : individual.getFamilies())
                    familyCascadeRemoval(family);

                for (FamilyChildRelationship family : individual.getParentFamilies())
                    familyCascadeRemoval(family.family);
            }

            private void familyCascadeRemoval(Family family)
            {
                String id = MainWindow.this.model.getID(family);
                
                if (this.familiesToRemove.contains(id))
                    return;
                
                this.familiesToRemove.add(id);
                
                Individual spouse1 = family.getSpouse1();
                
                if (spouse1 != null)
                    individualCascadeRemoval(spouse1);
                
                Individual spouse2 = family.getSpouse2();
                
                if (spouse2 != null)
                    individualCascadeRemoval(spouse2);
                
                for (Individual individual : family.getChildren())
                    individualCascadeRemoval(individual);
            }
        });
        
        JMenu advancedMenu = new JMenu(Messages.getString("MainWindow.advanced"));
        advancedMenu.add(diffUnsavedChanges);
        advancedMenu.add(cascadeRemoval);
        
        JMenuItem searchMedia = new JMenuItem(Messages.getString("MainWindow.findmediaobjects")); //$NON-NLS-1$
        
        searchMedia.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                MediaObjectDialog mediaObjectSelector = new MediaObjectDialog(
                        MainWindow.this, 
                        Messages.getString("MainWindow.findmediaobjects"),  //$NON-NLS-1$
                        MainWindow.this.model, true);
                
                MediaObject mediaObject = mediaObjectSelector.getSelectedMediaObject();
                
                if (mediaObject != null)
                    MainWindow.this.selectMediaObject(mediaObject);
            }
        });

        JMenu toolMenu = new JMenu(Messages.getString("MainWindow.tools")); //$NON-NLS-1$
        
        toolMenu.add(searchPeople);
        toolMenu.add(searchFamily);
        toolMenu.add(searchMedia);
        toolMenu.addSeparator();
        toolMenu.add(advancedMenu);
        
        return toolMenu;
    }
    
    private JMenu createDiagramMenu()
    {
        this.ascendentChartMenuItem  = new JRadioButtonMenuItem(Messages.getString("MainWindow.ascendingdiagram"),  //$NON-NLS-1$
                this.chartPanel.getType() == GedComChartPanel.ChartType.ParentChart);
        this.descendentChartMenuItem = new JRadioButtonMenuItem(Messages.getString("MainWindow.descendingdiagram"),  //$NON-NLS-1$
                this.chartPanel.getType() == GedComChartPanel.ChartType.DescendantChart);
        this.twoWayChartMenuItem     = new JRadioButtonMenuItem(Messages.getString("MainWindow.twowaysdiagram"),  //$NON-NLS-1$
                this.chartPanel.getType() == GedComChartPanel.ChartType.TwoWayChart);
        this.dynamicGraphMenuItem    = new JRadioButtonMenuItem(Messages.getString("MainWindow.treegraph"),  //$NON-NLS-1$
                this.selectedDiagram == MainPanelCard.GRAPH_PANEL);
        
        ButtonGroup buttonGroup = new ButtonGroup();
        
        buttonGroup.add(this.ascendentChartMenuItem);
        buttonGroup.add(this.descendentChartMenuItem);
        buttonGroup.add(this.twoWayChartMenuItem);
        buttonGroup.add(this.dynamicGraphMenuItem);
        
        this.ascendentChartMenuItem.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                MainWindow.this.showChartPanel(GedComChartPanel.ChartType.ParentChart);
            }
        });

        this.descendentChartMenuItem.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                MainWindow.this.showChartPanel(GedComChartPanel.ChartType.DescendantChart);
            }
        });
        
        this.twoWayChartMenuItem.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                MainWindow.this.showChartPanel(GedComChartPanel.ChartType.TwoWayChart);
            }
        });
        
        this.dynamicGraphMenuItem.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                MainWindow.this.showGraphPanel();
            }
        });
        
        JMenuItem font = new JMenuItem(Messages.getString("MainWindow.font")); //$NON-NLS-1$
        
        font.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                JFontChooser fontChooser = new JFontChooser();
                
                fontChooser.setSelectedFont(MainWindow.this.chartPanel.getFont());
                
                if (fontChooser.showDialog(MainWindow.this) == JFontChooser.OK_OPTION)
                {
                    MainWindow.this.prefs.putCurrentFont(fontChooser.getSelectedFont());
                    
                    MainWindow.this.chartPanel.setFont(fontChooser.getSelectedFont());
                }
            }
        });
        
        JMenu chartMenu = new JMenu(Messages.getString("MainWindow.diagram")); //$NON-NLS-1$
        
        chartMenu.add(this.descendentChartMenuItem);
        chartMenu.add(this.ascendentChartMenuItem);
        chartMenu.add(this.twoWayChartMenuItem);
        chartMenu.add(this.dynamicGraphMenuItem);
        chartMenu.addSeparator();
        chartMenu.add(font);
        
        return chartMenu;
    }
    
    /**
     * Shows the chart panel with the specified chart type. Updates both
     * the chart panel and the Diagram menu selection.
     */
    private void showChartPanel(GedComChartPanel.ChartType type)
    {
        this.mainCardLayout.show(this.mainStackPanel, 
                (this.selectedDiagram = MainPanelCard.CHART_PANEL).name());
        
        this.chartPanel.setChartType(type);
        
        // Sync the menu selection with the chart type
        if (this.ascendentChartMenuItem != null)
            this.ascendentChartMenuItem.setSelected(type == GedComChartPanel.ChartType.ParentChart);
        if (this.descendentChartMenuItem != null)
            this.descendentChartMenuItem.setSelected(type == GedComChartPanel.ChartType.DescendantChart);
        if (this.twoWayChartMenuItem != null)
            this.twoWayChartMenuItem.setSelected(type == GedComChartPanel.ChartType.TwoWayChart);
        if (this.dynamicGraphMenuItem != null)
            this.dynamicGraphMenuItem.setSelected(false);
    }
    
    /**
     * Shows the dynamic graph panel. Updates the Diagram menu selection.
     */
    private void showGraphPanel()
    {
        this.mainCardLayout.show(this.mainStackPanel, 
                (this.selectedDiagram = MainPanelCard.GRAPH_PANEL).name());
        
        // Sync the menu selection
        if (this.ascendentChartMenuItem != null)
            this.ascendentChartMenuItem.setSelected(false);
        if (this.descendentChartMenuItem != null)
            this.descendentChartMenuItem.setSelected(false);
        if (this.twoWayChartMenuItem != null)
            this.twoWayChartMenuItem.setSelected(false);
        if (this.dynamicGraphMenuItem != null)
            this.dynamicGraphMenuItem.setSelected(true);
        
        // Unselect all chart toolbar buttons (the graph button is handled
        // by the chartToolbarListener in the constructor)
        this.chartPanel.selectGraphButton(true);
    }
    
    public JMenu createHelpMenu()
    {
        JMenu helpMenu = new JMenu(Messages.getString("MainWindow.help")); //$NON-NLS-1$

        JMenuItem about = new JMenuItem(Messages.getString("MainWindow.about")); //$NON-NLS-1$
        
        about.setIcon(Resources.LavandaIconMini);
        
        about.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                JOptionPane jp = new JOptionPane(Messages.getString("MainWindow.about")); 
                jp.setMessageType(JOptionPane.INFORMATION_MESSAGE);
                jp.setMessage(
                        Messages.getString("MainWindow.title") + " " + MainWindow.VERSION + ", " +
                        Messages.getString("MainWindow.createdBy") + "\n" + //$NON-NLS-1$ //$NON-NLS-2$
                        Messages.getString("MainWindow.licensedUnder") + "\n");
                jp.setIcon(Resources.LavandaIcon);
                
                JDialog dialog = jp.createDialog(null, Messages.getString("MainWindow.about"));
                ((Frame)dialog.getParent()).setIconImage((Resources.LavandaIcon).getImage());  
                dialog.setResizable(true);
                dialog.pack();
                dialog.setVisible(true);
            }
        });

        helpMenu.add(about);
        
        return helpMenu;
    }
    

    private void setCurrentFile(File selectedFile)
    {
        if (selectedFile != null)
            this.prefs.putCurrentFileName(selectedFile.getAbsolutePath());
        
        this.updateTitle();
    }
    
    private void updateTitle()
    {
        String unsavedChangesSuffix = " " + (this.unsavedChanges ? "*" : "");
        
        if (this.model.getFile() != null)
        {
            this.setTitle(TITLE + " - " + this.model.getFile().getName() + unsavedChangesSuffix); //$NON-NLS-1$
        }
        else
        {
            this.setTitle(TITLE + unsavedChangesSuffix);
        }
        
    }
    
    public void setModel(Document model)
    {
        this.model = model;
        
        this.graphPanel.setModel(model);
        
        Individual individual;
        
        if (!model.listIndividuals().isEmpty())
        {
            String currentIndividual = this.prefs.getLastIndividual();
            
            Object object = null;
            
            if (currentIndividual != null)
                object = this.model.getObjectById(currentIndividual);
            
            if (object == null)
                individual = model.listIndividuals().iterator().next();

            else if (object instanceof Individual)
                individual = (Individual) object;
            
            else
                individual = model.listIndividuals().iterator().next();
        }
        else
        {
            individual = this.model.addNewIndividual();
        }
        
        this.selectIndividual(individual);
    }
    
    public void selectIndividual(Individual individual)
    {
        this.chartPanel.setModel(individual);
        
        this.graphPanel.setModel(individual);
        
        this.leftCardLayout.show(this.leftStackPanel, LeftPanelCard.INDIVIDUAL_PANEL.name());
        
        this.mainCardLayout.show(this.mainStackPanel, 
                (this.selectedDiagram = MainPanelCard.CHART_PANEL).name());
        
        this.individualEditorPanel.setModel(individual);
        
        if (individual != null)
        {
            String id = this.model.getID(individual);
            
            if (!id.equals("@0@")) //$NON-NLS-1$
                this.prefs.putCurrentIndividual(id);
        }
    }

    public void selectFamily(Family family)
    {
        this.chartPanel.setModel(family);
        
        this.graphPanel.setModel(family);
        
        this.leftCardLayout.show(this.leftStackPanel, LeftPanelCard.FAMILY_PANEL.name());
        
        this.familyEditorPanel.setModel(family);
        
        Individual individual = null;
        
        if (family != null)
            individual = family.getMember();
        
        if (individual != null)
        {
            String id = this.model.getID(individual);
            
            if (!id.equals("@0@")) //$NON-NLS-1$
                this.prefs.putCurrentIndividual(id);
        }
    }

    public void selectMediaObject(MediaObject mediaObject)
    {
        this.mediaObjectDisplayPanel.setModel(mediaObject);
        
        this.mediaObjectEditorPanel.setModel(mediaObject);
        
        this.mainCardLayout.show(this.mainStackPanel, MainPanelCard.MEDIA_OBJECT_PANEL.name());
        
        this.leftCardLayout.show(this.leftStackPanel, LeftPanelCard.MEDIA_OBJECT_PANEL.name());
    }

    public Document newGedFile()
    {
        Document newModel = new Document(null);
        
        this.setCurrentFile(null);
        
        this.setModel(newModel);

        this.unsavedChanges = false;
        
        this.updateTitle();

        return newModel;
    }
    
    public Document openGedFile(File selectedFile) throws IOException
    {
        try
        {
            this.unsavedChanges = false;
            
            Document newModel = GedComNode.readDocument(selectedFile);
            
            this.setModel(newModel);
            
            this.setCurrentFile(selectedFile);
            
            this.updateTitle();
            
            return newModel;
        }
        catch (LocalizedGedComParseException e1)
        {
            String message = e1.getMessage() + "\n";
            message += "\twhile reading " + selectedFile + "\n";
            message += "\tat line " + e1.lineCount + ": " + e1.line + "\n";
            
            for (LocalizedGedComNode node : e1.nodeStack)
                message += "\tat line " + node.lineCount + ": " + node.line + "\n";
            
            ErrorMessage.showErrorMessage(this, "Error parsing GEDCom file.", message, TITLE);
            
            MainWindow.this.setCurrentFile(null);
            
            return null;
        }
    }

    private void saveGedFile(File selectedFile) throws FileNotFoundException, IOException
    {
        this.model.resetHead(selectedFile);

        this.model.resetIdentifiers();
        
        GedComNode.writeDocument(this.model, selectedFile);

        this.unsavedChanges = false;
        
        this.updateTitle();
    }

    private void exportAsSvg(File outputFile) throws UnsupportedEncodingException, IOException
    {
        DOMImplementation domImpl = GenericDOMImplementation.getDOMImplementation();

        org.w3c.dom.Document document = domImpl.createDocument("http://www.w3.org/2000/svg", "svg", null);

        SVGGraphics2D svgGenerator = new SVGGraphics2D(document);
        
        Color prevBackgroundColor = MainWindow.this.chartPanel.getBackground();
        
        MainWindow.this.chartPanel.setBackground(new Color(0, 0, 0, 0));
        
        Rectangle2D bounds = MainWindow.this.chartPanel.getBounds2D();

        Dimension size = MainWindow.this.chartPanel.getTransform().createTransformedShape(bounds).getBounds().getSize();
        
        AffineTransform tx = MainWindow.this.chartPanel.initializeTransformation(bounds, size);
        
        final int BORDER = 10;
        
        tx.translate(BORDER, -BORDER);
        
        svgGenerator.setSVGCanvasSize(new Dimension(size.width + 2 * BORDER, size.height + 2 * BORDER));
        
        MainWindow.this.chartPanel.paint(svgGenerator, tx, size);
        
        MainWindow.this.chartPanel.setBackground(prevBackgroundColor);
        
        boolean useCSS = true;
        
        try (Writer out = new OutputStreamWriter(new FileOutputStream(outputFile), "UTF-8"))
        {
            svgGenerator.stream(out, useCSS);
        }
    }

    private BufferedImage exportAsImage(boolean fillBackground)
    {
        Color prevBackgroundColor = MainWindow.this.chartPanel.getBackground();
        
        Rectangle2D bounds = MainWindow.this.chartPanel.getBounds2D();
        
        Dimension size = MainWindow.this.chartPanel.getTransform().createTransformedShape(bounds).getBounds().getSize();
        
        AffineTransform tx = MainWindow.this.chartPanel.initializeTransformation(bounds, size);
        
        final int BORDER = 10;
        
        tx.translate(BORDER, -BORDER);
        
        BufferedImage image = new BufferedImage(
                size.width + 2 * BORDER, 
                size.height + 2 * BORDER, 
                fillBackground ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB);
        
        Graphics2D g2 = image.createGraphics();

        if (fillBackground)
        {
            g2.setColor(Color.white);
            
            g2.fillRect(0, 0, image.getWidth(), image.getHeight());
        }
        
        MainWindow.this.chartPanel.paint(g2, tx, size);
        
        MainWindow.this.chartPanel.setBackground(prevBackgroundColor);
        
        g2.dispose();
        
        return image;
    }
}
