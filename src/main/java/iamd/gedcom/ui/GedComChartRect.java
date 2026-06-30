package iamd.gedcom.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.lang.reflect.Field;

import iamd.gedcom.datamodel.Event;
import iamd.gedcom.datamodel.Individual;
import iamd.gedcom.datamodel.Individual.Sex;
import iamd.ui.ChartRect;

public class GedComChartRect extends ChartRect implements GedComChartElement
{
    /**
     * ChartRect keeps text1/text2/font1/font2 private and exposes no setters.
     * To truncate the displayed name with an ellipsis at paint time (when the
     * real transform/scaleX is known), we mutate the parent's text2 via
     * reflection just for the duration of one paint() call.
     */
    private static final Field CHART_RECT_TEXT2;

    static
    {
        try
        {
            CHART_RECT_TEXT2 = ChartRect.class.getDeclaredField("text2");
            CHART_RECT_TEXT2.setAccessible(true);
        }
        catch (NoSuchFieldException e)
        {
            throw new ExceptionInInitializerError(e);
        }
    }

    private final Individual individual;

    private Color color;

    /**
     * Full, un-truncated text2 (name + optional death symbol). Kept so we can
     * re-truncate on every paint() against the current graphics transform.
     */
    private final String fullText2;

    /**
     * The font assigned via setFont(); used as font2 by ChartRect for text2.
     * Tracked here because ChartRect exposes setFont2 but no getter.
     */
    private Font labelFont;

    public GedComChartRect(double x, double y, double width, double height, Individual individual)
    {
        this(x, y, width, height, individual, null);
    }

    public GedComChartRect(double x, double y, double width, double height, Individual individual, Font textFont)
    {
        super(x, y, width, height,
                Sex.toCharSymbol(individual.SEX),
                buildLabelText(individual, textFont));

        this.individual = individual;
        this.fullText2 = fullNameText(individual);
    }

    private static String fullNameText(Individual individual)
    {
        String name = individual.NAME.getShortName();
        boolean dead = individual.DEAT != null && individual.DEAT.happened != iamd.gedcom.datamodel.Bool.N;
        return dead ? name + " " + Event.DEATH_SYMBOL : name;
    }

    private static String buildLabelText(Individual individual, Font textFont)
    {
        // Default fallback if no font was supplied: behave exactly as before.
        if (textFont == null)
            return fullNameText(individual);

        // We can't know the actual transform at construction time, so just
        // return the full text. The real truncation happens in paint() using
        // the live graphics state.
        return fullNameText(individual);
    }

    public void setFont(Font font)
    {
        this.labelFont = font;
        this.setFont1(font.deriveFont(Font.BOLD));
        this.setFont2(font);
    }

    public Individual getIndividual()
    {
        return this.individual;
    }

    public void setColor(Color color)
    {
        this.color = color;

        this.setFillingColor(color);
    }

    public Color getColor()
    {
        return this.color;
    }

    @Override
    public void paint(Graphics2D g, AffineTransform transform)
    {
        Font font2 = this.labelFont;
        if (this.fullText2 == null || font2 == null)
        {
            super.paint(g, transform);
            return;
        }

        FontMetrics fm = g.getFontMetrics(font2);
        double scaleX = Math.max(0.0001, Math.abs(transform.getScaleX()));
        double maxPixelWidth = Math.max(0.0, this.getWidth() * scaleX - TEXT_PADDING_PIXELS);

        String truncated = truncateToWidth(this.fullText2, fm, maxPixelWidth);
        String previous;
        try
        {
            previous = (String) CHART_RECT_TEXT2.get(this);
        }
        catch (IllegalAccessException e)
        {
            super.paint(g, transform);
            return;
        }

        boolean mutated = !truncated.equals(previous);
        if (mutated)
        {
            try
            {
                CHART_RECT_TEXT2.set(this, truncated);
            }
            catch (IllegalAccessException e)
            {
                super.paint(g, transform);
                return;
            }
        }

        try
        {
            super.paint(g, transform);
        }
        finally
        {
            if (mutated)
            {
                try
                {
                    CHART_RECT_TEXT2.set(this, previous);
                }
                catch (IllegalAccessException ignored)
                {
                    // Best-effort restore; ignore if reflection fails here.
                }
            }
        }
    }

    private static String truncateToWidth(String text, FontMetrics fm, double maxWidth)
    {
        if (text == null || text.isEmpty() || fm == null)
            return text;

        if (maxWidth <= 0)
            return "...";

        if (fm.stringWidth(text) <= maxWidth)
            return text;

        final String ellipsis = "...";
        String trimmed = text;
        while (trimmed.length() > 1)
        {
            trimmed = trimmed.substring(0, trimmed.length() - 1).trim();
            String candidate = trimmed + ellipsis;
            if (fm.stringWidth(candidate) <= maxWidth)
                return candidate;
        }

        return ellipsis;
    }

    private static final double TEXT_PADDING_PIXELS = 4.0;
}