package iamd.gedcom.ui;

import java.awt.Color;

import iamd.gedcom.datamodel.Individual;

import iamd.ui.ChartElement;

public interface GedComChartElement extends ChartElement
{
    public Individual getIndividual();

    public Color getColor();

    public void setFillingColor(Color color);

    public void setColor(Color color);
}
