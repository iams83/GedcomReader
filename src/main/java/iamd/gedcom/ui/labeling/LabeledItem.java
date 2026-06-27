package iamd.gedcom.ui.labeling;

/**
 * Marker interface implemented by anything the label layout engine can
 * position. The engine does not need to know about {@code MediaObject},
 * {@code Individual} or any domain type — it only needs the label text
 * and the rectangle (in scene coordinates) that the label should
 * initially attach to.
 *
 * <p>This decoupling keeps the layout engine reusable from any panel that
 * wants collision-free labels (e.g. charts, graphs, photo displays) and
 * makes it trivial to unit-test.</p>
 */
public interface LabeledItem
{
    /**
     * The text to render for this item. May be {@code null} or empty, in
     * which case the engine should skip placing a label for it.
     */
    String getLabelText();

    /**
     * X coordinate (scene space) of the top-left point that the label
     * should initially attach to.
     */
    double getAnchorX();

    /**
     * Y coordinate (scene space) of the top-left point that the label
     * should initially attach to.
     */
    double getAnchorY();
}