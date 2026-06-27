package iamd.gedcom.ui.labeling;

/**
 * The final position of one label after collision resolution. This is
 * the object a renderer should iterate over to actually draw labels.
 *
 * <p>The placement is in viewport coordinates (already transformed by
 * the scene), so the renderer does not need to know anything about
 * {@link java.awt.geom.AffineTransform}.</p>
 *
 * @param <T> the type of the underlying item being labeled; lets callers
 *            retrieve domain objects (e.g. {@code Individual}) without
 *            casting.
 */
public final class PlacedLabel<T extends LabeledItem>
{
    private final T item;
    private final LabelPlacement placement;

    public PlacedLabel(T item, LabelPlacement placement)
    {
        this.item = item;
        this.placement = placement;
    }

    /** The item this label belongs to. */
    public T getItem()
    {
        return this.item;
    }

    /** Where the label has been placed, in viewport coordinates. */
    public LabelPlacement getPlacement()
    {
        return this.placement;
    }
}