package iamd.gedcom.ui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Shape;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import iamd.gedcom.datamodel.Crop;
import iamd.gedcom.datamodel.Individual;
import iamd.gedcom.datamodel.MediaObject;
import iamd.gedcom.datamodel.MediaObjectReference;
import iamd.gedcom.ui.MediaObjectPanelWithToolbar.ToolMode;
import iamd.ui.GraphicsPanel;

@SuppressWarnings("serial")
public class MediaObjectDisplayPanel extends GraphicsPanel
{
    public interface MediaObjectDisplayListener
    {
        void rectangleClicked(Individual individual, MediaObjectReference ref);
    }
    
    private MediaObject mediaObject;
    private BufferedImage image;
    private List<IndividualReference> individualReferences = new ArrayList<>();
    private MediaObjectDisplayListener listener;
    
    // Tool mode and crop state
    private ToolMode toolMode = ToolMode.SELECTION;
    private IndividualReference activeCropReference; // The individual whose crop is being edited
    private Point2D cropDragStart;
    private Point2D cropDragCurrent;
    private boolean cropDragging = false;
    
    private static final int[] RECTANGLE_COLORS = {
        0x800000FF, // Blue
        0x80FF0000, // Red
        0x8000FF00, // Green
        0x80FFFF00, // Yellow
        0x80FF00FF, // Magenta
        0x8000FFFF, // Cyan
        0x80FF8000, // Orange
        0x808000FF, // Purple
        0x8000FF80, // Light Green
        0x80FF0080, // Pink
    };
    
    private static final int[] RECTANGLE_BORDER_COLORS = {
        0xFF0000FF, // Blue
        0xFFFF0000, // Red
        0xFF00FF00, // Green
        0xFFFFFF00, // Yellow
        0xFFFF00FF, // Magenta
        0xFF00FFFF, // Cyan
        0xFFFF8000, // Orange
        0xFF8000FF, // Purple
        0xFF00FF80, // Light Green
        0xFFFF0080, // Pink
    };
    
    private static class IndividualReference
    {
        public final Individual individual;
        public final MediaObjectReference ref;
        public final int colorIndex;
        
        public IndividualReference(Individual individual, MediaObjectReference ref, int colorIndex)
        {
            this.individual = individual;
            this.ref = ref;
            this.colorIndex = colorIndex;
        }
    }
    
    public MediaObjectDisplayPanel()
    {
        super(PanelMovement.PANNING_AND_SCALING, Reverse.NO);
        
        final MediaObjectDisplayPanel self = this;
        
        addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                handleClick(e.getPoint());
            }
            
            @Override
            public void mouseMoved(MouseEvent e)
            {
                updateTooltip(e.getPoint());
            }
            
            @Override
            public void mousePressed(MouseEvent e)
            {
                if (self.toolMode == ToolMode.CROP && self.image != null)
                {
                    Point2D scenePoint = self.mapToScene(e.getPoint());
                    if (scenePoint == null)
                        return;
                    
                    // In crop mode, mouse press starts a crop drag.
                    // The crop is applied to the active reference (which is
                    // set when the mouse was first pressed inside a rectangle).
                    self.cropDragStart = scenePoint;
                    self.cropDragCurrent = scenePoint;
                    self.cropDragging = true;
                    self.repaint();
                }
            }
            
            @Override
            public void mouseReleased(MouseEvent e)
            {
                if (self.toolMode == ToolMode.CROP && self.cropDragging)
                {
                    Point2D releasePoint = self.mapToScene(e.getPoint());
                    Point2D startPoint = self.cropDragStart;
                    
                    if (releasePoint != null && startPoint != null)
                    {
                        double width = Math.abs(releasePoint.getX() - startPoint.getX());
                        double height = Math.abs(releasePoint.getY() - startPoint.getY());
                        
                        // Only apply a new crop if the user actually dragged
                        // (a meaningful distance)
                        if (width > 2 && height > 2)
                        {
                            // Find the active reference. The user must have
                            // clicked inside a rectangle to start cropping,
                            // otherwise there's no active reference.
                            if (self.activeCropReference != null)
                            {
                                self.applyCropToActiveReference(releasePoint);
                            }
                        }
                    }
                    self.cropDragging = false;
                    self.cropDragStart = null;
                    self.cropDragCurrent = null;
                    // Keep the active reference selected so the user can
                    // continue cropping the same individual
                    self.repaint();
                }
            }
        });
        
        addMouseMotionListener(new MouseMotionAdapter()
        {
            @Override
            public void mouseDragged(MouseEvent e)
            {
                if (self.toolMode == ToolMode.CROP && self.cropDragging)
                {
                    Point2D scenePoint = self.mapToScene(e.getPoint());
                    if (scenePoint != null)
                    {
                        // If we haven't selected an active reference yet, try
                        // to find one based on the current mouse position
                        if (self.activeCropReference == null)
                        {
                            for (IndividualReference ir : self.individualReferences)
                            {
                                Rectangle2D.Double rect = self.getRectangleForReference(ir);
                                if (rect.contains(scenePoint))
                                {
                                    self.activeCropReference = ir;
                                    self.cropDragStart = scenePoint;
                                    break;
                                }
                            }
                        }
                        
                        self.cropDragCurrent = scenePoint;
                        self.repaint();
                    }
                }
            }
        });
    }
    
    public void setMediaObjectDisplayListener(MediaObjectDisplayListener listener)
    {
        this.listener = listener;
    }
    
    /**
     * Sets the current tool mode (selection or crop).
     * Resets the crop drag state when switching modes.
     * Also enables/disables panning and scaling on the underlying GraphicsPanel,
     * because panning and scaling consume mouse events needed for the crop tool.
     */
    public void setToolMode(ToolMode mode)
    {
        this.toolMode = mode;
        this.cropDragging = false;
        this.cropDragStart = null;
        this.cropDragCurrent = null;
        this.activeCropReference = null;
        
        // Update cursor based on tool mode
        if (mode == ToolMode.CROP && image != null)
        {
            setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
        }
        else
        {
            setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        }
        
        // Enable/disable panning and scaling based on the tool mode
        try
        {
            java.lang.reflect.Method setPanningEnabled = this.getClass().getSuperclass().getMethod("setPanningEnabled", boolean.class);
            java.lang.reflect.Method setScalingEnabled = this.getClass().getSuperclass().getMethod("setScalingEnabled", boolean.class);
            setPanningEnabled.invoke(this, mode == ToolMode.SELECTION);
            setScalingEnabled.invoke(this, mode == ToolMode.SELECTION);
        }
        catch (Exception ex)
        {
            // Methods not available
        }
        
        repaint();
    }
    
    public ToolMode getToolMode()
    {
        return this.toolMode;
    }
    
    public void setModel(MediaObject mediaObject)
    {
        this.mediaObject = mediaObject;
        this.individualReferences.clear();
        this.activeCropReference = null;
        this.cropDragging = false;
        
        if (mediaObject == null)
        {
            this.image = null;
            this.repaint();
            return;
        }
        
        // Load image
        this.image = mediaObject.getImage();
        
        // Find all individuals with references to this media object
        if (mediaObject.getDocument() != null)
        {
            int colorIndex = 0;
            Map<Individual, Boolean> seenIndividuals = new HashMap<>();
            
            for (Individual ind : mediaObject.getDocument().listIndividuals())
            {
                for (MediaObjectReference ref : ind.OBJE)
                {
                    if (ref.mediaObject == mediaObject)
                    {
                        // Only add if we haven't seen this individual yet
                        if (!seenIndividuals.containsKey(ind))
                        {
                            seenIndividuals.put(ind, true);
                            this.individualReferences.add(new IndividualReference(ind, ref, colorIndex % RECTANGLE_COLORS.length));
                            colorIndex++;
                        }
                        break;
                    }
                }
            }
        }
        
        // Initialize bounding box
        if (this.image != null)
        {
            initializeBoundingBox(new Rectangle2D.Double(0, 0, this.image.getWidth(), this.image.getHeight()));
        }
        else
        {
            // For non-image media, use a default size
            initializeBoundingBox(new Rectangle2D.Double(0, 0, 400, 300));
        }
        
        // Update cursor and panning/scaling based on current tool mode
        if (this.toolMode == ToolMode.CROP)
        {
            setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
            
            // Disable panning/scaling in crop mode
            try
            {
                java.lang.reflect.Method setPanningEnabled = this.getClass().getSuperclass().getMethod("setPanningEnabled", boolean.class);
                java.lang.reflect.Method setScalingEnabled = this.getClass().getSuperclass().getMethod("setScalingEnabled", boolean.class);
                setPanningEnabled.invoke(this, false);
                setScalingEnabled.invoke(this, false);
            }
            catch (Exception ex)
            {
                // Methods not available
            }
        }
    }
    
    private void handleClick(Point screenPoint)
    {
        if (this.image == null || this.individualReferences.isEmpty())
            return;
        
        Point2D scenePoint = mapToScene(screenPoint);
        if (scenePoint == null)
            return;
        
        // In selection mode, click navigates to the individual.
        // In crop mode, click selects the individual to crop.
        if (this.toolMode != ToolMode.SELECTION)
        {
            // In crop mode, find which rectangle was clicked and select it
            for (IndividualReference ir : this.individualReferences)
            {
                Rectangle2D.Double rect = getRectangleForReference(ir);
                
                if (rect.contains(scenePoint))
                {
                    this.activeCropReference = ir;
                    this.repaint();
                    return;
                }
            }
            // If no rectangle was clicked in crop mode, deselect
            this.activeCropReference = null;
            this.repaint();
            return;
        }
        
        // Selection mode: click navigates to the individual
        for (IndividualReference ir : this.individualReferences)
        {
            Rectangle2D.Double rect = getRectangleForReference(ir);
            
            if (rect.contains(scenePoint))
            {
                if (this.listener != null)
                {
                    this.listener.rectangleClicked(ir.individual, ir.ref);
                }
                return;
            }
        }
    }
    
    /**
     * Applies the current crop drag rectangle to the active individual's
     * MediaObjectReference. Called when the user releases the mouse after
     * dragging in crop mode.
     */
    private void applyCropToActiveReference(Point2D releasePoint)
    {
        if (this.image == null || this.activeCropReference == null)
            return;
        
        MediaObjectReference ref = this.activeCropReference.ref;
        
        double x = Math.min(cropDragStart.getX(), releasePoint.getX());
        double y = Math.min(cropDragStart.getY(), releasePoint.getY());
        double width = Math.abs(releasePoint.getX() - cropDragStart.getX());
        double height = Math.abs(releasePoint.getY() - cropDragStart.getY());
        
        if (width > 2 && height > 2)
        {
            int cropX = (int) Math.max(0, x);
            int cropY = (int) Math.max(0, y);
            int cropWidth = (int) Math.min(width, image.getWidth() - cropX);
            int cropHeight = (int) Math.min(height, image.getHeight() - cropY);
            
            if (cropX <= 0 && cropY <= 0 && cropWidth >= image.getWidth() && cropHeight >= image.getHeight())
            {
                // If the crop rectangle covers the entire image, clear the crop
                ref.CROP = null;
            }
            else
            {
                if (ref.CROP == null)
                {
                    ref.CROP = new Crop(ref.getDocument());
                }
                ref.CROP.LEFT = cropX;
                ref.CROP.TOP = cropY;
                ref.CROP.WIDTH = cropWidth;
                ref.CROP.HEIGHT = cropHeight;
            }
        }
    }
    
    private Rectangle2D.Double getRectangleForReference(IndividualReference ir)
    {
        Crop crop = ir.ref.CROP;
        if (crop != null && crop.WIDTH > 0 && crop.HEIGHT > 0)
        {
            return new Rectangle2D.Double(crop.LEFT, crop.TOP, crop.WIDTH, crop.HEIGHT);
        }
        
        // No CROP: use the full image bounds
        double width = (this.image != null) ? this.image.getWidth() : 400;
        double height = (this.image != null) ? this.image.getHeight() : 300;
        return new Rectangle2D.Double(0, 0, width, height);
    }
    
    private void updateTooltip(Point screenPoint)
    {
        if (this.image == null && this.mediaObject == null)
        {
            setToolTipText(null);
            return;
        }
        
        Point2D scenePoint = mapToScene(screenPoint);
        if (scenePoint == null)
        {
            setToolTipText(null);
            return;
        }
        
        // Find which rectangle is under the mouse
        for (IndividualReference ir : this.individualReferences)
        {
            Rectangle2D.Double rect = getRectangleForReference(ir);
            
            if (rect.contains(scenePoint))
            {
                if (toolMode == ToolMode.CROP)
                {
                    String prefix = (ir == this.activeCropReference) ? "[Selected] " : "";
                    setToolTipText(prefix + "Drag to crop for: " + ir.individual.getName());
                }
                else
                {
                    setToolTipText(ir.individual.getName());
                }
                setCursor(new Cursor(Cursor.HAND_CURSOR));
                return;
            }
        }
        
        setToolTipText(null);
        setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
    }
    
    @Override
    protected void paint(Graphics2D g2, AffineTransform tx2, Dimension size)
    {
        g2.setColor(Color.DARK_GRAY);
        g2.fillRect(0, 0, size.width, size.height);
        
        if (this.mediaObject == null)
        {
            g2.setColor(Color.WHITE);
            g2.drawString("No media object selected", 20, 30);
            return;
        }
        
        // Draw the media object image or icon
        if (this.image != null)
        {
            g2.drawImage(this.image, tx2, this);
        }
        else
        {
            // Draw placeholder for non-image media
            javax.swing.ImageIcon icon = this.mediaObject.getIconType();
            if (icon != null && icon.getImage() != null)
            {
                int iconWidth = icon.getIconWidth();
                int iconHeight = icon.getIconHeight();
                
                AffineTransform iconTx = new AffineTransform(tx2);
                iconTx.translate(-iconWidth / 2.0, -iconHeight / 2.0);
                
                g2.drawImage(icon.getImage(), iconTx, this);
            }
            else
            {
                g2.setColor(Color.WHITE);
                g2.drawString("Unable to display media", 20, 30);
            }
        }
        
        // Draw rectangles for all individuals
        for (IndividualReference ir : this.individualReferences)
        {
            Rectangle2D.Double rect = getRectangleForReference(ir);
            
            Shape transformedRect = tx2.createTransformedShape(rect);
            
            // Highlight the active crop reference with a thicker border
            boolean isActive = (ir == this.activeCropReference);
            
            // Draw border
            g2.setColor(new Color(RECTANGLE_BORDER_COLORS[ir.colorIndex], true));
            g2.setStroke(new java.awt.BasicStroke(isActive ? 4 : 2));
            g2.draw(transformedRect);
            
            // Draw label
            String label = ir.individual.getName();
            if (label != null && !label.isEmpty())
            {
                java.awt.Font originalFont = g2.getFont();
                g2.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 12));
                
                java.awt.FontMetrics fm = g2.getFontMetrics();
                int textWidth = fm.stringWidth(label);
                int textHeight = fm.getHeight();
                
                // Position label at top-left of rectangle (in screen coordinates)
                Point2D labelPos = tx2.transform(
                    new Point2D.Double(rect.getX(), rect.getY()), null);
                
                // Draw background for label
                g2.setColor(new Color(255, 255, 255, 200));
                g2.fillRect((int)labelPos.getX() - 2, (int)labelPos.getY() - textHeight - 2, 
                           textWidth + 4, textHeight + 4);
                
                // Draw label text
                g2.setColor(new Color(RECTANGLE_BORDER_COLORS[ir.colorIndex], true));
                g2.drawString(label, (int)labelPos.getX(), (int)labelPos.getY() - 2);
                
                g2.setFont(originalFont);
            }
        }
        
        // Draw the in-progress crop rectangle if currently dragging.
        // Use the active reference's color so it matches the individual being cropped.
        if (this.toolMode == ToolMode.CROP && this.cropDragging 
            && this.cropDragStart != null && this.cropDragCurrent != null
            && this.image != null && this.activeCropReference != null)
        {
            double x = Math.min(cropDragStart.getX(), cropDragCurrent.getX());
            double y = Math.min(cropDragStart.getY(), cropDragCurrent.getY());
            double width = Math.abs(cropDragCurrent.getX() - cropDragStart.getX());
            double height = Math.abs(cropDragCurrent.getY() - cropDragStart.getY());
            
            Shape dragRect = tx2.createTransformedShape(new Rectangle2D.Double(x, y, width, height));
            
            // Use the active reference's color for the drag rectangle
            int colorIndex = this.activeCropReference.colorIndex;
            g2.setColor(new Color(RECTANGLE_COLORS[colorIndex], true));
            g2.fill(dragRect);
            
            g2.setColor(new Color(RECTANGLE_BORDER_COLORS[colorIndex], true));
            g2.setStroke(new java.awt.BasicStroke(2));
            g2.draw(dragRect);
        }
    }
}
