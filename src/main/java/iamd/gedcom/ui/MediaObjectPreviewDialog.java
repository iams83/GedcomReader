package iamd.gedcom.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.Shape;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import iamd.gedcom.datamodel.Crop;
import iamd.gedcom.datamodel.MediaObject;
import iamd.gedcom.datamodel.MediaObject.MediaType;
import iamd.gedcom.datamodel.MediaObjectReference;
import iamd.gedcom.rsrc.Resources;
import iamd.ui.GraphicsPanel;

@SuppressWarnings("serial")
public class MediaObjectPreviewDialog extends JDialog
{
    private final MediaObject mediaObject;
    
    int cropX = 0, cropY = 0, cropWidth = 0, cropHeight = 0;
    
    JTextField cropXField = new JTextField("0"), 
    cropYField = new JTextField("0"), 
    cropWidthField = new JTextField("0"), 
    cropHeightField = new JTextField("0");
    
    private CropImagePanel imagePanel;
    
    private boolean cropTool = false;
    
    public MediaObjectPreviewDialog(JFrame parent, MediaObjectReference mediaObjectRef)
    {
        super(parent, "Media Object Preview", true);
        
        this.mediaObject = mediaObjectRef.mediaObject;

        if (mediaObjectRef.CROP != null)
        {
            this.cropX = mediaObjectRef.CROP.LEFT;
            this.cropY = mediaObjectRef.CROP.TOP;
            this.cropWidth = mediaObjectRef.CROP.WIDTH;
            this.cropHeight = mediaObjectRef.CROP.HEIGHT;

            updateCropFields();
        }
        
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
            
            if (mediaObject.TYPE == MediaType.Picture)
            {
                JToggleButton exploreButton = new JToggleButton(Resources.ExploreIcon);
                JToggleButton cropImageButton = new JToggleButton(Resources.CropImageIcon);

                exploreButton.addActionListener(e -> {
                    cropTool = false;
                    updateCursor();
                    // Enable/disable panning and scaling based on crop tool state
                    imagePanel.setPanningEnabled(true);
                    imagePanel.setScalingEnabled(true);
                    cropImageButton.setSelected(false);
                });
                
                cropImageButton.addActionListener(e -> {
                    cropTool = true;
                    updateCursor();
                    // Enable/disable panning and scaling based on crop tool state
                    imagePanel.setPanningEnabled(false);
                    imagePanel.setScalingEnabled(false);
                    exploreButton.setSelected(false);
                });
                
                JPanel toolPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
                toolPanel.add(exploreButton);
                toolPanel.add(cropImageButton);
                
                previewPanel.add(toolPanel, BorderLayout.NORTH);
                
                // Create image panel
                this.imagePanel = new CropImagePanel();
                
                BufferedImage mediaImage = mediaObject.getImage();
                if (mediaImage != null)
                {
                    SwingUtilities.invokeLater(() -> {
                        this.imagePanel.setImage(mediaImage);
                    });
                }
                
                // Add click to open (only when crop tool is not active)
                this.imagePanel.addMouseListener(new MouseAdapter()
                {
                    @Override
                    public void mouseClicked(MouseEvent e)
                    {
                        if (!cropTool)
                        {
                            openFileWithSystem();
                        }
                    }
                });
                this.imagePanel.setCursor(new Cursor(Cursor.HAND_CURSOR));

                JPanel imageContainerPanel = new JPanel(new BorderLayout());
                imageContainerPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
                imageContainerPanel.add(this.imagePanel);

                previewPanel.add(imageContainerPanel, BorderLayout.CENTER);
                
            }
            else
            {
                // Create icon panel for non-image media
                ImageIconPanel iconPanel = new ImageIconPanel(mediaObject.TYPE);
                previewPanel.add(iconPanel, BorderLayout.CENTER);
                
                // Add click to open
                iconPanel.addMouseListener(new MouseAdapter()
                {
                    @Override
                    public void mouseClicked(MouseEvent e)
                    {
                        openFileWithSystem();
                    }
                });
                iconPanel.setCursor(new Cursor(Cursor.HAND_CURSOR));
            }
            
            mainPanel.add(previewPanel, BorderLayout.CENTER);
        }
        
        {
            JPanel buttonPanel = new JPanel();
            
            JButton openButton = new JButton("Open with System");
            openButton.addActionListener(e -> openFileWithSystem());
            buttonPanel.add(openButton);
            
            JButton saveChangesButton = new JButton("Save Changes");
            saveChangesButton.addActionListener(e -> {
                if (mediaObject.TYPE == MediaType.Picture && cropWidth > 0 && cropHeight > 0)
                {
                    mediaObjectRef.CROP = new Crop("CROP", mediaObjectRef.getDocument());
                    mediaObjectRef.CROP.LEFT = cropX;
                    mediaObjectRef.CROP.TOP = cropY;
                    mediaObjectRef.CROP.WIDTH = cropWidth;
                    mediaObjectRef.CROP.HEIGHT = cropHeight;
                }
                else
                {
                    mediaObjectRef.CROP = null; // Clear crop if no valid crop area
                }
                
                setVisible(false);
            });
            buttonPanel.add(saveChangesButton);
            
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
    
    private void updateCursor()
    {
        if (this.imagePanel != null)
        {
            if (cropTool)
            {
                this.imagePanel.setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
            }
            else
            {
                this.imagePanel.setCursor(new Cursor(Cursor.HAND_CURSOR));
            }
        }
    }
    
    private void updateCropFields()
    {
        cropXField.setText(String.valueOf(cropX));
        cropYField.setText(String.valueOf(cropY));
        cropWidthField.setText(String.valueOf(cropWidth));
        cropHeightField.setText(String.valueOf(cropHeight));
    }
    
    // Image panel for displaying pictures
    private class CropImagePanel extends GraphicsPanel
    {
        private BufferedImage image;
        
        private Point2D dragStart;
        private Point2D dragCurrent;
        private boolean isDragging = false;
        
        public CropImagePanel()
        {
            super(PanelMovement.PANNING_AND_SCALING, Reverse.NO);
            
            addMouseListener(new MouseAdapter()
            {
                @Override
                public void mousePressed(MouseEvent e)
                {
                    if (cropTool && image != null)
                    {
                        // Crop mode - capture start position
                        dragStart = mapToScene(e.getPoint());
                        dragCurrent = new Point2D.Double(dragStart.getX(), dragStart.getY());
                        isDragging = true;
                    }
                }
                
                @Override
                public void mouseReleased(MouseEvent e)
                {
                    if (cropTool && image != null && isDragging)
                    {
                        Point2D releasePoint = mapToScene(e.getPoint());
                        
                        // Calculate crop rectangle
                        double x = Math.min(dragStart.getX(), releasePoint.getX());
                        double y = Math.min(dragStart.getY(), releasePoint.getY());
                        double width = Math.abs(releasePoint.getX() - dragStart.getX());
                        double height = Math.abs(releasePoint.getY() - dragStart.getY());
                        
                        // Only update if the rectangle has some size
                        if (width > 2 && height > 2)
                        {
                            // Update crop values in image coordinates
                            cropX = (int) Math.max(0, x);
                            cropY = (int) Math.max(0, y);
                            cropWidth = (int) Math.min(width, image.getWidth() - cropX);
                            cropHeight = (int) Math.min(height, image.getHeight() - cropY);
                            
                            if (cropX <= 0 && cropY <= 0 && cropWidth >= image.getWidth() && cropHeight >= image.getHeight())
                            {
                                // If the crop rectangle covers the entire image, reset to no cropping
                                cropX = 0;
                                cropY = 0;
                                cropWidth = 0;
                                cropHeight = 0;
                            }

                            // Update text fields
                            updateCropFields();
                        }
                        
                        isDragging = false;
                        repaint();
                    }
                }
            });
            
            addMouseMotionListener(new MouseMotionAdapter()
            {
                @Override
                public void mouseDragged(MouseEvent e)
                {
                    if (cropTool && image != null && isDragging)
                    {
                        dragCurrent = mapToScene(e.getPoint());
                        repaint();
                    }
                }
            });
        }

        public void setImage(BufferedImage image)
        {
            this.image = image;

            this.initializeBoundingBox(new Rectangle2D.Double(0, 0, image.getWidth(), image.getHeight()));
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
            
            Shape rect = null;

            // Draw crop rectangle if dragging
            if (cropTool && isDragging && dragStart != null && dragCurrent != null)
            {
                double x = Math.min(dragStart.getX(), dragCurrent.getX());
                double y = Math.min(dragStart.getY(), dragCurrent.getY());
                double width = Math.abs(dragCurrent.getX() - dragStart.getX());
                double height = Math.abs(dragCurrent.getY() - dragStart.getY());

                rect = tx2.createTransformedShape(new Rectangle2D.Double(x, y, width, height));
            }
            else if (cropWidth > 0 && cropHeight > 0)
            {
                rect = tx2.createTransformedShape(new Rectangle2D.Double(cropX, cropY, cropWidth, cropHeight));
            }

            if (rect != null)
            {
                g2.setColor(new Color(0, 0, 255, 100));
                g2.fill(rect);
                
                g2.setColor(Color.BLUE);
                g2.setStroke(new java.awt.BasicStroke(2));
                g2.draw(rect);
            }
        }
    }
    
    // Icon panel for non-image media types
    private class ImageIconPanel extends JPanel
    {
        private static final long serialVersionUID = 1L;
        
        public ImageIconPanel(MediaType mediaType)
        {
            this.setLayout(new BorderLayout());
            
            JLabel iconLabel = new JLabel(mediaType != null ? mediaType.getIcon() : null, JLabel.CENTER);
            this.add(iconLabel, BorderLayout.CENTER);
        }
    }
}
