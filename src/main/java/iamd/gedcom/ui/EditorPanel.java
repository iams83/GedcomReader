package iamd.gedcom.ui;

import java.awt.Color;
import java.awt.Container;
import java.awt.Graphics;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;

@SuppressWarnings("serial")
public class EditorPanel extends JPanel
{
    /**
     * Creates a section label that paints itself in lock-step with its
     * container's current background.
     *
     * <p>The label is opaque but its background is filled with the
     * immediate parent's {@code background} color at paint time, so it
     * visually blends in with whatever container it ends up inside. This
     * avoids the noisy/banding artifacts you get with the previous
     * {@code setBackground(new Color(0, 0, 0, 0))} + {@code setOpaque(false)}
     * approach: there the label lets the parent's underlying paint show
     * through, which leaks gradients, patterns, or off-tone L&F colors
     * onto the label area and creates a visible "ring" around the text —
     * especially when the {@code UIManager} paints a different color
     * than the parent panel's expected backdrop.
     */
    protected JLabel newJLabel(String string)
    {
        return new ContainerBackgroundLabel(string);
    }

    /**
     * Read-only counterpart of {@link #newJLabel(String)}; same
     * container-tracking background fill.
     */
    protected JTextField newReadonlyJTextField(String string)
    {
        return new ContainerBackgroundTextField(string);
    }

    /**
     * Adds a top spacing border to a component and makes it non-opaque so
     * it does not paint any background of its own. Without an explicit
     * {@code setBackground(new Color(0, 0, 0, 0))}, the component's
     * background falls back to whatever the parent (or the L&F) provides —
     * so the component stays visually attached to its container across
     * Look &amp; Feel changes instead of leaving stray rectangles when the
     * {@code UIManager} swaps in a different default color.
     */
    protected JComponent createTopBorder(JComponent component)
    {
        component.setBorder(new EmptyBorder(6, 0, 0, 0));
        component.setOpaque(false);
        return component;
    }

    /**
     * Public factory so other panels in this package (which don't subclass
     * {@link EditorPanel}) can request the same container-tracking label
     * without duplicating the inner class.
     */
    public static JLabel newContainerBackgroundLabel(String text)
    {
        return new ContainerBackgroundLabel(text);
    }

    /**
     * A {@link JLabel} whose background is always painted with its
     * immediate parent's current background color. By re-querying the
     * parent at every paint, the label automatically tracks whatever the
     * surrounding container reports — including when the
     * {@code UIManager} changes the look-and-feel and the parent's color
     * updates underneath it.
     */
    private static class ContainerBackgroundLabel extends JLabel
    {
        ContainerBackgroundLabel(String text)
        {
            super(text);
            // Keep setOpaque(true) so the L&F doesn't suppress our
            // paintComponent and so the label always paints a solid,
            // opaque rectangle before its text — that's the whole point.
            setOpaque(true);
        }

        @Override
        protected void paintComponent(Graphics g)
        {
            Container parent = getParent();
            Color bg = (parent != null) ? parent.getBackground() : getBackground();
            g.setColor(bg);
            g.fillRect(0, 0, getWidth(), getHeight());
            super.paintComponent(g);
        }
    }

    /**
     * Read-only text field analog of {@link ContainerBackgroundLabel}.
     */
    private static class ContainerBackgroundTextField extends JTextField
    {
        ContainerBackgroundTextField(String text)
        {
            super(text);
            setEditable(false);
            setBorder(new EtchedBorder(EtchedBorder.LOWERED));
            setOpaque(true);
        }

        @Override
        protected void paintComponent(Graphics g)
        {
            Container parent = getParent();
            Color bg = (parent != null) ? parent.getBackground() : getBackground();
            g.setColor(bg);
            g.fillRect(0, 0, getWidth(), getHeight());
            super.paintComponent(g);
        }
    }

}
