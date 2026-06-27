package iamd.gedcom.ui.labeling;

import java.awt.FontMetrics;
import java.awt.Rectangle;

/**
 * Tiny helper that turns a label string into the size of its visible
 * "pill" (background) plus the coordinates where the text baseline
 * should land inside that pill.
 *
 * <p>The engine is intentionally agnostic about fonts and padding so
 * different panels can plug in their own {@code LabelMetrics} without
 * the engine having to know about {@link java.awt.Font} or
 * {@link java.awt.FontMetrics} directly. This also makes the engine
 * straightforward to unit-test: tests can pass in a deterministic
 * {@code LabelMetrics} that always reports the same width/height.</p>
 */
public interface LabelMetrics
{
    /**
     * Measure a label string and return its size as a {@link Rectangle}.
     * The rectangle's {@code width}/{@code height} are the visible
     * background dimensions; {@code x}/{@code y} are not used by the
     * engine and may be zero.
     */
    Rectangle measure(String text);

    /**
     * Compute the text baseline coordinates inside a background rectangle
     * that was produced by {@link #measure(String)}. The engine uses
     * these values to render the label text precisely under the
     * background.
     */
    int textX(Rectangle background);
    int textY(Rectangle background);
}