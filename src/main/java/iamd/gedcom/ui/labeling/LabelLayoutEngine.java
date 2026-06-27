package iamd.gedcom.ui.labeling;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Stateless helper that turns a list of {@link LabeledItem}s into a
 * collision-free list of {@link PlacedLabel}s.
 *
 * <h2>Algorithm</h2>
 * <ol>
 *   <li>For each item in input order, project its anchor point from scene
 *       coordinates to viewport coordinates using the supplied
 *       {@link AffineTransform}.</li>
 *   <li>Build a {@link LabelPlacement} for it using the configured
 *       {@link LabelMetrics}.</li>
 *   <li>Compare it against every label already placed. If there is an
 *       overlap, shift the candidate to the right by the width of the
 *       colliding label and try again, up to {@link #MAX_NUDGE_ATTEMPTS}
 *       times. This guarantees termination even for pathological inputs.</li>
 * </ol>
 *
 * <p>The engine is deliberately stateless: callers instantiate one and
 * invoke {@link #layout(List, AffineTransform)} once per paint. There is
 * no caching between paints; the layout is so cheap (a handful of
 * rectangle checks per label) that it's not worth invalidating on
 * zoom/pan.</p>
 */
public class LabelLayoutEngine
{
    /**
     * Maximum number of rightward nudges per label. Prevents an infinite
     * loop if two labels are forced to chase each other across the
     * viewport. After this many nudges the label keeps its last safe
     * position, accepting the remaining overlap rather than spinning.
     */
    public static final int MAX_NUDGE_ATTEMPTS = 256;

    /** Vertical gap between a label and the rectangle it labels
     *  (label rendered below the rectangle). */
    private final int innerMargin;
    /** Vertical gap between a label and the rectangle it labels
     *  (label rendered above the rectangle). */
    private final int outerMargin;
    /** Extra horizontal gap kept between non-overlapping labels so they
     *  don't visually touch. Set to 0 for a perfectly tight layout. */
    private final int horizontalGap;
    private final LabelMetrics metrics;

    public LabelLayoutEngine(LabelMetrics metrics)
    {
        this(metrics, 2, 2, 0);
    }

    public LabelLayoutEngine(LabelMetrics metrics,
                             int innerMargin,
                             int outerMargin,
                             int horizontalGap)
    {
        this.metrics = metrics;
        this.innerMargin = innerMargin;
        this.outerMargin = outerMargin;
        this.horizontalGap = horizontalGap;
    }

    /**
     * Compute a collision-free placement for each item.
     *
     * @param items   the items to label, in the order they should be
     *                placed. Items with {@code null} or empty label text
     *                are skipped.
     * @param sceneToViewport transform that maps scene coordinates into
     *                viewport coordinates (typically the same one passed
     *                to the renderer).
     * @return a list of {@link PlacedLabel}s in the same order as the
     *         input, skipping items without a label.
     */
    public <T extends LabeledItem> List<PlacedLabel<T>> layout(List<T> items,
                                                              AffineTransform sceneToViewport)
    {
        List<PlacedLabel<T>> placed = new ArrayList<>(items.size());

        placed.sort(new Comparator<PlacedLabel<T>>() {
            @Override
            public int compare(PlacedLabel<T> o1, PlacedLabel<T> o2) {
                int diffX = Double.compare(o1.getPlacement().getBounds().x, o2.getPlacement().getBounds().x);

                if (diffX != 0)
                    return diffX;

                int diffY = Double.compare(o1.getPlacement().getBounds().y, o2.getPlacement().getBounds().y);

                if (diffY != 0)
                    return diffY;

                return o1.getItem().getLabelText().compareTo(o2.getItem().getLabelText());
            }
        });

        for (T item : items)
        {
            String text = item.getLabelText();
            if (text == null || text.isEmpty())
                continue;

            // Convert the anchor point from scene to viewport coordinates.
            Point2D anchor = sceneToViewport.transform(
                new Point2D.Double(item.getAnchorX(), item.getAnchorY()), null);

            // Build the candidate placement at its preferred location.
            LabelPlacement candidate = buildPlacement(text, anchor);

            // Push it to the right until it no longer overlaps any
            // already-placed label.
            candidate = resolveCollisions(candidate, placed);

            placed.add(new PlacedLabel<>(item, candidate));
        }

        return placed;
    }

    /**
     * Build the initial {@link LabelPlacement} for a label, anchored
     * above or below the scene rectangle depending on its {@code y}
     * coordinate: when the anchor is at the top of the scene (y == 0)
     * the label sits <em>below</em> the anchor, otherwise it sits
     * <em>above</em>.
     *
     * <p>This mirrors the existing behavior of
     * {@code MediaObjectDisplayPanel}: labels whose top-left point is
     * at (0, 0) (no CROP or CROP starting at the image origin) go inside
     * the rectangle; all other labels sit just above the rectangle.</p>
     */
    private LabelPlacement buildPlacement(String text, Point2D anchor)
    {
        Rectangle bg = new Rectangle(this.metrics.measure(text));
        int bgX;
        int bgY;

        if (anchor.getY() <= 0.0)
        {
            // Anchor at (or near) the top of the image: render the label
            // just below the anchor (inside the rectangle).
            bgX = (int) Math.round(anchor.getX());
            bgY = (int) Math.round(anchor.getY() + this.innerMargin);
        }
        else
        {
            // Otherwise: render the label just above the anchor, so the
            // label sits outside (above) the rectangle.
            bgX = (int) Math.round(anchor.getX());
            bgY = (int) Math.round(anchor.getY() - this.outerMargin - bg.height);
        }

        bg.setLocation(bgX, bgY);
        int textX = this.metrics.textX(bg);
        int textY = this.metrics.textY(bg);
        return new LabelPlacement(bg, textX, textY);
    }

    /**
     * Shift {@code candidate} to the right until its background does not
     * overlap any already-placed label. If a maximum number of attempts
     * is reached, the candidate is left at its last position; this
     * guarantees the algorithm always terminates.
     */
    private <T extends LabeledItem> LabelPlacement resolveCollisions(
            LabelPlacement candidate, List<PlacedLabel<T>> alreadyPlaced)
    {
        for (int attempt = 0; attempt < MAX_NUDGE_ATTEMPTS; attempt++)
        {
            LabelPlacement colliding = findFirstCollision(candidate, alreadyPlaced);
            if (colliding == null)
                return candidate;

            // Shift right by the colliding label's width (plus an optional
            // small gap) so the new label starts where the old one ends.
            int shift = colliding.getBounds().width + this.horizontalGap;
            candidate = candidate.shiftedBy(shift + colliding.getBounds().x - candidate.getBounds().x);
        }
        return candidate;
    }

    /**
     * Linear scan over already-placed labels; returns the first one whose
     * background intersects the candidate's background, or {@code null}
     * if there is no collision.
     */
    private <T extends LabeledItem> LabelPlacement findFirstCollision(
            LabelPlacement candidate, List<PlacedLabel<T>> alreadyPlaced)
    {
        Rectangle candidateBounds = candidate.getBounds();
        for (PlacedLabel<T> existing : alreadyPlaced)
        {
            if (existing.getPlacement().getBounds().intersects(candidateBounds))
                return existing.getPlacement();
        }
        return null;
    }
}