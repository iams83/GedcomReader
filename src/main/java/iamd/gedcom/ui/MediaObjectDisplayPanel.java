package iamd.gedcom.ui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
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
import iamd.gedcom.ui.labeling.LabelLayoutEngine;
import iamd.gedcom.ui.labeling.LabelMetrics;
import iamd.gedcom.ui.labeling.LabeledItem;
import iamd.gedcom.ui.labeling.PlacedLabel;

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
    private List<ReferenceLabelItem> labelItems = new ArrayList<>();
    private MediaObjectDisplayListener listener;
    
    // Tool mode and crop state
    private ToolMode toolMode = ToolMode.SELECTION;
    private IndividualReference activeCropReference; // The individual whose crop is being edited
    private Point2D cropDragStart;
    private Point2D cropDragCurrent;
    private boolean cropDragging = false;
    
    // Single source of truth for each individual's color. Used for the
    // rectangle border, the label background, and (with a lower alpha) the
    // semi-transparent fill of the in-progress crop drag rectangle.
    private static final int[] RECTANGLE_BORDER_COLORS = {
        0xFF0000FF, // Blue
        0xFFFF0000, // Red
        0xFF00B000, // Green (muted so it reads on a white background)
        0xFFB0B000, // Yellow (muted olive)
        0xFFFF00FF, // Magenta
        0xFF00B0B0, // Cyan (muted teal)
        0xFFFF8000, // Orange
        0xFF8000FF, // Purple
        0xFF00B060, // Light Green (muted for readability)
        0xFFFF0080, // Pink
    };
    
    // Alpha (0–255) used for the semi-transparent fill of the in-progress
    // crop drag rectangle, derived from the opaque RECTANGLE_BORDER_COLORS
    // by replacing the top byte.
    private static final int DRAG_FILL_ALPHA = 0x80;
    
    /** Returns the semi-transparent fill color for the drag rectangle. */
    private static int dragFillRgb(int borderRgb)
    {
        return (borderRgb & 0x00FFFFFF) | (DRAG_FILL_ALPHA << 24);
    }
    
    // Border stroke widths (in pixels) used when drawing rectangles.
    // The inactive stroke is also reused for the in-progress crop rectangle
    // because it shares the same visual weight.
    private static final float ACTIVE_BORDER_STROKE_WIDTH = 4.0f;
    private static final float INACTIVE_BORDER_STROKE_WIDTH = 2.0f;
    
    // Font used to draw each individual's name label.
    private static final Font LABEL_FONT = new Font("Arial", Font.BOLD, 12);
    
    // Spacing (in pixels) used when positioning a label relative to the
    // top-left corner of its CROP rectangle. LABEL_BACKGROUND_PADDING also
    // drives the inner text X offset: when the background adds
    // LABEL_BACKGROUND_PADDING/2 pixels of padding on each side, the text
    // starts at LABEL_INNER_MARGIN + LABEL_BACKGROUND_PADDING/2 from the
    // rectangle edge.
    private static final int LABEL_BACKGROUND_PADDING = 6;
    
    // Position and color of placeholder text shown when no image is available.
    private static final int PLACEHOLDER_TEXT_X = 20;
    private static final int PLACEHOLDER_TEXT_Y = 30;
    private static final Color PLACEHOLDER_TEXT_COLOR = Color.WHITE;
    
    // Color used to fill the empty panel background.
    private static final Color PANEL_BACKGROUND_COLOR = Color.DARK_GRAY;
    
    // Minimum size (in pixels) of a crop drag before it is considered a real
    // crop operation (smaller drags are treated as accidental clicks).
    private static final int MIN_CROP_DRAG_SIZE = 2;
    
    // Default size (in pixels) used when a media object has no image to size
    // the placeholder against.
    private static final double DEFAULT_MEDIA_WIDTH  = 128;
    private static final double DEFAULT_MEDIA_HEIGHT = 128;
    
    /**
     * Stateless layout engine used to keep labels from overlapping.
     * Re-evaluated on every paint so it stays correct under zoom and pan.
     */
    private final LabelLayoutEngine labelLayoutEngine =
            new LabelLayoutEngine(new PanelLabelMetrics());

    /**
     * Adapts the panel's current font metrics to the layout engine.
     * Implemented as an inner class so it can read getFont() live at
     * layout time (the panel's font may be changed later).
     */
    private class PanelLabelMetrics implements LabelMetrics
    {
        @Override
        public Rectangle measure(String text)
        {
            FontMetrics fm = getFontMetrics(LABEL_FONT);
            int textWidth = fm.stringWidth(text);
            int textAscent = fm.getAscent();
            int textDescent = fm.getDescent();
            int bgWidth = textWidth + LABEL_BACKGROUND_PADDING;
            int bgHeight = textAscent + textDescent + LABEL_BACKGROUND_PADDING;
            return new Rectangle(0, 0, bgWidth, bgHeight);
        }

        @Override
        public int textX(Rectangle background)
        {
            return background.x + LABEL_BACKGROUND_PADDING / 2;
        }

        @Override
        public int textY(Rectangle background)
        {
            FontMetrics fm = getFontMetrics(LABEL_FONT);
            return background.y + LABEL_BACKGROUND_PADDING / 2 + fm.getAscent();
        }
    }

    /**
     * Adapts an IndividualReference plus its scene-space rectangle to the
     * engine's LabeledItem contract. Created fresh for each paint because
     * the rectangle can change between frames (zoom, pan, new CROP).
     */
    private static class ReferenceLabelItem implements LabeledItem
    {
        final IndividualReference reference;
        final Rectangle2D.Double sceneRect;

        ReferenceLabelItem(IndividualReference reference, Rectangle2D.Double sceneRect)
        {
            this.reference = reference;
            this.sceneRect = sceneRect;
        }

        @Override public String getLabelText() { return this.reference.individual.getName(); }
        @Override public double getAnchorX() { return this.sceneRect.getX(); }
        @Override public double getAnchorY() { return this.sceneRect.getY(); }
    }

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
                        if (width > MIN_CROP_DRAG_SIZE && height > MIN_CROP_DRAG_SIZE)
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
                            this.individualReferences.add(new IndividualReference(ind, ref, colorIndex % RECTANGLE_BORDER_COLORS.length));
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
            initializeBoundingBox(new Rectangle2D.Double(
                0, 0, DEFAULT_MEDIA_WIDTH, DEFAULT_MEDIA_HEIGHT));
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
    
    /**
     * Sets the image to display and the individuals (with their references)
     * whose CROP rectangles and labels should be drawn on top of it.
     * Intended for read-only preview subclasses (e.g. the one used in
     * {@link MediaObjectDialog}) that don't have a backing {@link MediaObject}
     * to pull the data from; both panels then share exactly the same drawing
     * logic, colors, fonts and layout.
     */
    protected void setImageAndReferences(BufferedImage image,
                                         Map<Individual, MediaObjectReference> references)
    {
        this.image = image;
        this.individualReferences.clear();
        this.activeCropReference = null;
        this.cropDragging = false;
        
        if (references != null)
        {
            int colorIndex = 0;
            for (Map.Entry<Individual, MediaObjectReference> entry : references.entrySet())
            {
                this.individualReferences.add(new IndividualReference(
                    entry.getKey(), entry.getValue(),
                    colorIndex % RECTANGLE_BORDER_COLORS.length));
                colorIndex++;
            }
        }
        
        if (image != null)
        {
            initializeBoundingBox(new Rectangle2D.Double(0, 0, image.getWidth(), image.getHeight()));
        }
        else
        {
            initializeBoundingBox(new Rectangle2D.Double(0, 0, DEFAULT_MEDIA_WIDTH, DEFAULT_MEDIA_HEIGHT));
        }
        
        repaint();
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
        
        if (width > MIN_CROP_DRAG_SIZE && height > MIN_CROP_DRAG_SIZE)
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
        double width = (this.image != null) ? this.image.getWidth() : DEFAULT_MEDIA_WIDTH;
        double height = (this.image != null) ? this.image.getHeight() : DEFAULT_MEDIA_HEIGHT;
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
        g2.setColor(PANEL_BACKGROUND_COLOR);
        g2.fillRect(0, 0, size.width, size.height);
        
        // Pick the best image to draw: a directly-set image (used by
        // read-only previews like MediaObjectDialog.PreviewImagePanel) wins;
        // otherwise we fall back to the icon associated with the bound
        // MediaObject for non-image media types.
        if (this.image != null)
        {
            g2.drawImage(this.image, tx2, this);
        }
        else if (this.mediaObject != null)
        {
            javax.swing.ImageIcon icon = this.mediaObject.getIconType();
            if (icon != null && icon.getImage() != null)
            {
                int iconWidth = icon.getIconWidth();
                int iconHeight = icon.getIconHeight();
                
                AffineTransform iconTx = new AffineTransform(tx2);
                iconTx.translate((DEFAULT_MEDIA_WIDTH - iconWidth) / 2.0, (DEFAULT_MEDIA_HEIGHT - iconHeight) / 2.0);
                
                g2.drawImage(icon.getImage(), iconTx, this);
            }
            else
            {
                g2.setColor(PLACEHOLDER_TEXT_COLOR);
                g2.drawString("Unable to display media", PLACEHOLDER_TEXT_X, PLACEHOLDER_TEXT_Y);
                return;
            }
        }
        else
        {
            g2.setColor(PLACEHOLDER_TEXT_COLOR);
            g2.drawString("No media object selected", PLACEHOLDER_TEXT_X, PLACEHOLDER_TEXT_Y);
            return;
        }
        
        java.awt.Font labelOriginalFont = g2.getFont();
        g2.setFont(LABEL_FONT);
        
        // Draw rectangles for all individuals
        for (IndividualReference ir : this.individualReferences)
        {
            Rectangle2D.Double rect = getRectangleForReference(ir);
            
            Shape transformedRect = tx2.createTransformedShape(rect);
            
            // Highlight the active crop reference with a thicker border
            boolean isActive = (ir == this.activeCropReference);
            
            // Draw border
            g2.setColor(new Color(RECTANGLE_BORDER_COLORS[ir.colorIndex], true));
            g2.setStroke(new java.awt.BasicStroke(isActive ? ACTIVE_BORDER_STROKE_WIDTH : INACTIVE_BORDER_STROKE_WIDTH));
            g2.draw(transformedRect);
            
            // Defer drawing until the layout engine has resolved
            // collisions across every collected label.
            String label = ir.individual.getName();
            if (label != null && !label.isEmpty())
            {
                this.labelItems.add(new ReferenceLabelItem(ir, rect));
            }
        }
        
        
        // Resolve label collisions across all individuals via the layout
        // engine, then draw the resulting placements. The engine anchors
        // each label to the scene-space top-left of its rectangle; if two
        // labels overlap, the second one is nudged to the right.

        List<PlacedLabel<ReferenceLabelItem>> placedLabels =
            this.labelLayoutEngine.layout(this.labelItems, tx2);
        this.labelItems.clear();
        for (PlacedLabel<ReferenceLabelItem> placed : placedLabels)
        {
            IndividualReference ir = placed.getItem().reference;
            Rectangle bg = placed.getPlacement().getBounds();
            g2.setColor(new Color(RECTANGLE_BORDER_COLORS[ir.colorIndex]));
            g2.fillRect(bg.x, bg.y, bg.width, bg.height);
            g2.setColor(Color.WHITE);
            g2.drawString(ir.individual.getName(),
                          placed.getPlacement().getTextX(),
                          placed.getPlacement().getTextY());
        }
        g2.setFont(labelOriginalFont);
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
            
            // Use the active reference's color for the drag rectangle: a
            // semi-transparent fill (so the image shows through) with an
            // opaque border matching the regular rectangle outline.
            int colorIndex = this.activeCropReference.colorIndex;
            int borderRgb = RECTANGLE_BORDER_COLORS[colorIndex];
            g2.setColor(new Color(dragFillRgb(borderRgb), true));
            g2.fill(dragRect);
            
            g2.setColor(new Color(borderRgb));
            g2.setStroke(new java.awt.BasicStroke(INACTIVE_BORDER_STROKE_WIDTH));
            g2.draw(dragRect);
        }
    }
}
