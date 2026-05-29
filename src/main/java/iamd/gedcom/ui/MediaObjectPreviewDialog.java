package iamd.gedcom.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import iamd.gedcom.datamodel.MediaObject;
import iamd.gedcom.datamodel.MediaObject.MediaType;
import iamd.ui.GraphicsPanel;

@SuppressWarnings("serial")
public class MediaObjectPreviewDialog extends JDialog
{
    private final MediaObject mediaObject;
    
    private ImagePanel imagePanel;
    private ImageIconPanel iconPanel;

    int cropX = 0, cropY = 0, cropWidth = 0, cropHeight = 0;

    JTextField cropXField = new JTextField("0"), 
               cropYField = new JTextField("0"), 
               cropWidthField = new JTextField("0"), 
               cropHeightField = new JTextField("0");
    
    public MediaObjectPreviewDialog(JFrame parent, MediaObject mediaObject)
    {
        super(parent, "Media Object Preview", true);
        
        this.mediaObject = mediaObject;
        
        this.setSize(800, 600);
        this.setLocationByPlatform(true);
        
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel fieldsPanel = new JPanel(new java.awt.GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new java.awt.Insets(2, 2, 2, 2);
        gbc.fill = GridBagConstraints.BOTH;
        
        final float FIELD_PANEL_SPLIT = 0.7f;

        {
            gbc.gridx = 0; gbc.weightx = FIELD_PANEL_SPLIT;
            JPanel leftFieldsPanel = new JPanel(new java.awt.GridBagLayout());
            GridBagConstraints gbcInner = new GridBagConstraints();
            gbcInner.insets = new java.awt.Insets(2, 2, 2, 2);
            gbcInner.fill = GridBagConstraints.HORIZONTAL;

            final float LABEL_WEIGHT = 0.2f;
            
            gbcInner.gridx = 0; gbcInner.weightx = LABEL_WEIGHT;
            leftFieldsPanel.add(new JLabel("File:"), gbcInner);
            gbcInner.gridx = 1; gbcInner.weightx = 1.0f - LABEL_WEIGHT;
            leftFieldsPanel.add(readOnlyJTextField(mediaObject.getRelativeFilePath() != null ? mediaObject.getRelativeFilePath() : (mediaObject.FILE != null ? mediaObject.FILE : "")), gbcInner);

            gbcInner.gridx = 0; gbcInner.weightx = LABEL_WEIGHT;
            leftFieldsPanel.add(new JLabel("Format:"), gbcInner);
            gbcInner.gridx = 1; gbcInner.weightx = 1.0f - LABEL_WEIGHT;
            leftFieldsPanel.add(readOnlyJTextField(mediaObject.FORM != null ? mediaObject.FORM : ""), gbcInner);
            
            gbcInner.gridx = 0; gbcInner.weightx = LABEL_WEIGHT;
            leftFieldsPanel.add(new JLabel("Title:"), gbcInner);
            gbcInner.gridx = 1; gbcInner.weightx = 1.0f - LABEL_WEIGHT;
            leftFieldsPanel.add(readOnlyJTextField(mediaObject.TITL != null ? mediaObject.TITL : ""), gbcInner);
            
            gbcInner.gridx = 0; gbcInner.weightx = LABEL_WEIGHT;
            leftFieldsPanel.add(new JLabel("Type:"), gbcInner);
            gbcInner.gridx = 1; gbcInner.weightx = 1.0f - LABEL_WEIGHT;
            String typeStr = "";
            if (mediaObject.TYPE != null)
                typeStr = Messages.getString("SelectorDialog." + mediaObject.TYPE.name().toLowerCase());
            leftFieldsPanel.add(readOnlyJTextField(typeStr), gbcInner);
            
            fieldsPanel.add(leftFieldsPanel, gbc);
        }
        
        if (mediaObject.TYPE == MediaType.Picture)
        {
            gbc.gridx = 1; gbc.weightx = 1.0f - FIELD_PANEL_SPLIT;
            JPanel rightFieldsPanel = new JPanel(new java.awt.GridBagLayout());
            GridBagConstraints gbcInner = new GridBagConstraints();
            gbcInner.insets = new java.awt.Insets(2, 2, 2, 2);
            gbcInner.fill = GridBagConstraints.HORIZONTAL;

            final float LABEL_WEIGHT = 0.5f;
            
            gbcInner.gridx = 0; gbcInner.weightx = LABEL_WEIGHT;
            rightFieldsPanel.add(new JLabel("Crop X:"), gbcInner);
            gbcInner.gridx = 1; gbcInner.weightx = 1.0f - LABEL_WEIGHT;
            rightFieldsPanel.add(cropXField, gbcInner);
            
            gbcInner.gridx = 0; gbcInner.weightx = LABEL_WEIGHT;
            rightFieldsPanel.add(new JLabel("Crop Y:"), gbcInner);
            gbcInner.gridx = 1; gbcInner.weightx = 1.0f - LABEL_WEIGHT;
            rightFieldsPanel.add(cropYField, gbcInner);
            
            gbcInner.gridx = 0; gbcInner.weightx = LABEL_WEIGHT;
            rightFieldsPanel.add(new JLabel("Crop Width:"), gbcInner);
            gbcInner.gridx = 1; gbcInner.weightx = 1.0f - LABEL_WEIGHT;
            rightFieldsPanel.add(cropWidthField, gbcInner);
            
            gbcInner.gridx = 0; gbcInner.weightx = LABEL_WEIGHT;
            rightFieldsPanel.add(new JLabel("Crop Height:"), gbcInner);
            gbcInner.gridx = 1; gbcInner.weightx = 1.0f - LABEL_WEIGHT;
            rightFieldsPanel.add(cropHeightField, gbcInner);
            
            fieldsPanel.add(rightFieldsPanel, gbc);
        }
        mainPanel.add(fieldsPanel, BorderLayout.NORTH);
        
        // Create preview panel
        {
            JPanel previewPanel = new JPanel(new BorderLayout());
            previewPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
            
            MediaType mediaType = mediaObject.TYPE;
            
            // Determine the actual media type based on extension if not set
            if (mediaType == null && mediaObject.FORM != null)
            {
                mediaType = MediaType.getMediaTypeForExtension(mediaObject.FORM.toLowerCase());
            }
            
            if (mediaType == MediaType.Picture)
            {
                // Create image panel
                this.imagePanel = new ImagePanel();
                
                File mediaFile = mediaObject.getMediaFile();
                if (mediaFile != null && mediaFile.exists())
                {
                    SwingUtilities.invokeLater(() -> {
                        this.imagePanel.loadImage(mediaFile);
                    });
                }
                
                previewPanel.add(this.imagePanel, BorderLayout.CENTER);
                
                // Add click to open
                this.imagePanel.addMouseListener(new MouseAdapter()
                {
                    @Override
                    public void mouseClicked(MouseEvent e)
                    {
                        openFileWithSystem();
                    }
                });
                this.imagePanel.setCursor(new Cursor(Cursor.HAND_CURSOR));
            }
            else
            {
                // Create icon panel for non-image media
                this.iconPanel = new ImageIconPanel(mediaType);
                previewPanel.add(this.iconPanel, BorderLayout.CENTER);
                
                // Add click to open
                this.iconPanel.addMouseListener(new MouseAdapter()
                {
                    @Override
                    public void mouseClicked(MouseEvent e)
                    {
                        openFileWithSystem();
                    }
                });
                this.iconPanel.setCursor(new Cursor(Cursor.HAND_CURSOR));
            }
        
            mainPanel.add(previewPanel, BorderLayout.CENTER);
        }
        
        {
            JPanel buttonPanel = new JPanel();
            
            JButton openButton = new JButton("Open with System");
            openButton.addActionListener(e -> openFileWithSystem());
            buttonPanel.add(openButton);
            
            JButton closeButton = new JButton("Close");
            closeButton.addActionListener(e -> setVisible(false));
            buttonPanel.add(closeButton);
            
            mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        }
        
        this.add(mainPanel);
    }

    private JTextField readOnlyJTextField(String text)
    {
        JTextField textField = new JTextField(text);
        textField.setEditable(false);
        return textField;
    }
    
    private void openFileWithSystem()
    {
        try
        {
            File mediaFile = this.mediaObject.getMediaFile();
            if (mediaFile != null && mediaFile.exists())
            {
                java.awt.Desktop.getDesktop().open(mediaFile);
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
    
    // Image panel for displaying pictures
    private class ImagePanel extends GraphicsPanel
    {
        private BufferedImage image;
        
        public ImagePanel()
        {
            super(PanelMovement.PANNING_AND_SCALING, Reverse.NO);
        }

        public void loadImage(File file)
        {
            try
            {
                this.image = ImageIO.read(file);
                if (this.image != null)
                {
                    initializeBoundingBox(new java.awt.geom.Rectangle2D.Double(
                        0, 0, this.image.getWidth(), this.image.getHeight()));
                }
            }
            catch (IOException e)
            {
                this.image = null;
                e.printStackTrace();
            }
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
    
    // Icon panel for non-image media types
    private class ImageIconPanel extends JPanel
    {
        private static final long serialVersionUID = 1L;
        
        public ImageIconPanel(MediaType mediaType)
        {
            this.setLayout(new BorderLayout());
            
            JLabel iconLabel = new JLabel(getIconForMediaType(mediaType), JLabel.CENTER);
            this.add(iconLabel, BorderLayout.CENTER);
            
            JLabel hintLabel = new JLabel("Click to open with system default application", JLabel.CENTER);
            this.add(hintLabel, BorderLayout.SOUTH);
        }
        
        private String getIconForMediaType(MediaType mediaType)
        {
            if (mediaType == MediaType.Audio)
            {
                return "[Audio File]";
            }
            else if (mediaType == MediaType.Video)
            {
                return "[Video File]";
            }
            else if (mediaType == MediaType.Document)
            {
                return "[Document File]";
            }
            return "[Media File]";
        }
    }
}
