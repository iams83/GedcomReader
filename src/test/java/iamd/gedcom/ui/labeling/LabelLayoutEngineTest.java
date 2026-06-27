package iamd.gedcom.ui.labeling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for LabelLayoutEngine. The tests use a tiny stub
 * LabelMetrics so they do not need a real font or graphics context,
 * and assert the geometric invariants the engine is meant to
 * uphold (collision-free placement, nudge rightward, and
 * termination).
 */

class LabelLayoutEngineTest
{
    private static final int INNER_MARGIN = 2;

    private static final int LABEL_WIDTH = 100;
    private static final int LABEL_HEIGHT = 10;
    private static final int PADDING = 2;
    private static final int ASCENT = 6;

    private static final LabelMetrics STUB_METRICS = new LabelMetrics()
    {
        @Override
        public Rectangle measure(String text)
        {
            return new Rectangle(0, 0, LABEL_WIDTH, LABEL_HEIGHT);
        }

        @Override
        public int textX(Rectangle background)
        {
            return background.x + PADDING;
        }

        @Override
        public int textY(Rectangle background)
        {
            return background.y + PADDING + ASCENT;
        }
    };

    private static LabeledItem item(String text, double x, double y)
    {
        return new LabeledItem()
        {
            @Override public String getLabelText() { return text; }
            @Override public double getAnchorX() { return x; }
            @Override public double getAnchorY() { return y; }
        };
    }
@Test
    void emptyInputProducesEmptyOutput()
    {
        var engine = new LabelLayoutEngine(STUB_METRICS);
        var placed = engine.layout(Arrays.<LabeledItem>asList(), new AffineTransform());
        assertTrue(placed.isEmpty());
    }

    @Test
    void itemsWithoutLabelAreSkipped()
    {
        var engine = new LabelLayoutEngine(STUB_METRICS);
        var placed = engine.layout(
            Arrays.asList(item(null, 0, 0), item("", 0, 0), item("ok", 0, 0)),
            new AffineTransform());
        assertEquals(1, placed.size());
        assertEquals("ok", placed.get(0).getItem().getLabelText());
    }

    @Test
    void nonOverlappingLabelsKeepTheirPreferredX()
    {
        var engine = new LabelLayoutEngine(STUB_METRICS);
        // Anchors spaced by more than the label width (100).
        // Each label should stay at its preferred x.
        var items = Arrays.asList(
            item("A", 0, 0),
            item("B", 300, 0),
            item("C", 600, 0));
        var placed = engine.layout(items, new AffineTransform());

        assertEquals(3, placed.size());
        assertEquals(0, placed.get(0).getPlacement().getBounds().x);
        assertEquals(300, placed.get(1).getPlacement().getBounds().x);
        assertEquals(600, placed.get(2).getPlacement().getBounds().x);
    }

    @Test
    void overlappingLabelsAreShiftedRight()
    {
        var engine = new LabelLayoutEngine(STUB_METRICS);
        var items = Arrays.asList(
            item("A", 0, 0),
            item("B", 0, 0),
            item("C", 0, 0));
        var placed = engine.layout(items, new AffineTransform());
        assertEquals(0, placed.get(0).getPlacement().getBounds().x);
        assertEquals(100, placed.get(1).getPlacement().getBounds().x);
        assertEquals(200, placed.get(2).getPlacement().getBounds().x);
    }

    @Test
    void textCoordinatesTrackTheBackground()
    {
        var engine = new LabelLayoutEngine(STUB_METRICS);
        var placed = engine.layout(
            Arrays.asList(item("A", 0, 0), item("B", 0, 0)),
            new AffineTransform());
        assertEquals(PADDING, placed.get(0).getPlacement().getTextX());
        assertEquals(100 + PADDING, placed.get(1).getPlacement().getTextX());
        assertEquals(INNER_MARGIN + PADDING + ASCENT,
            placed.get(0).getPlacement().getTextY());
    }

    @Test
    void sceneTransformIsAppliedToAnchor()
    {
        var engine = new LabelLayoutEngine(STUB_METRICS);
        var placed = engine.layout(
            Arrays.asList(item("A", 10, 0)),
            AffineTransform.getScaleInstance(2.0, 2.0));
        assertEquals(20, placed.get(0).getPlacement().getBounds().x);
    }

    @Test
    void pathologicalInputStillTerminates()
    {
        var engine = new LabelLayoutEngine(STUB_METRICS);
        var items = new ArrayList<LabeledItem>();
        for (int i = 0; i < 20; i++) items.add(item("L" + i, 0, 0));
        var placed = engine.layout(items, new AffineTransform());
        assertEquals(20, placed.size());
        for (var p : placed)
        {
            assertNotNull(p.getPlacement().getBounds());
            assertFalse(p.getPlacement().getBounds().isEmpty());
        }
    }

    @Test
    void engineReturnsPlacedLabelsInInputOrder()
    {
        var engine = new LabelLayoutEngine(STUB_METRICS);
        var items = Arrays.asList(
            item("first", 0, 0),
            item("second", 0, 0),
            item("third", 0, 0));
        var placed = engine.layout(items, new AffineTransform());
        List<String> names = placed.stream()
            .map(p -> p.getItem().getLabelText())
            .collect(Collectors.toList());
        assertEquals(Arrays.asList("first", "second", "third"), names);
    }
}
