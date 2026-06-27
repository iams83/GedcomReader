package iamd.gedcom.ui.labeling;

import java.awt.Rectangle;

/**
 * One candidate placement for a label, expressed entirely in viewport
 * (i.e. screen) coordinates so that collision checks are independent of
 * the scene transform.
 *
 * <p>This is an immutable value object produced by a {@link LabelLayoutEngine}
 * when it considers where a particular label could be drawn. The engine may
 * generate several of these per label while searching for a collision-free
 * spot, but only one is ultimately surfaced to the caller as a
 * {@link PlacedLabel}.</p>
 *
 * <p>The {@link Rectangle} covers the visible label background (the colored
 * pill that hosts the text). The text baseline is {@link #textX},
 * {@link #textY} in the same coordinate space; clients usually don't need
 * those separately, but they are kept here so the engine can move the
 * rectangle and the text together.</p>
 */
public final class LabelPlacement
{
    private final Rectangle bounds;
    private final int textX;
    private final int textY;

    public LabelPlacement(Rectangle bounds, int textX, int textY)
    {
        this.bounds = bounds;
        this.textX = textX;
        this.textY = textY;
    }

    /** Rectangle (in viewport coordinates) that the label background occupies. */
    public Rectangle getBounds()
    {
        return this.bounds;
    }

    /** X coordinate of the text baseline, in viewport coordinates. */
    public int getTextX()
    {
        return this.textX;
    }

    /** Y coordinate of the text baseline, in viewport coordinates. */
    public int getTextY()
    {
        return this.textY;
    }

    /**
     * Return a new placement shifted {@code dx} pixels to the right. The
     * text coordinates shift by the same amount so the label stays
     * internally consistent.
     */
    public LabelPlacement shiftedBy(int dx)
    {
        return new LabelPlacement(
            new Rectangle(this.bounds.x + dx, this.bounds.y,
                         this.bounds.width, this.bounds.height),
            this.textX + dx,
            this.textY);
    }
}