package iamd.gedcom.ui;

import java.awt.Color;
import java.awt.Font;

import iamd.gedcom.datamodel.Event;
import iamd.gedcom.datamodel.Individual;
import iamd.gedcom.datamodel.Individual.Sex;
import iamd.ui.ChartRect;

public class GedComChartRect extends ChartRect implements GedComChartElement
{
    private Individual individual;
    
    private Color color;
    
    public GedComChartRect(double x, double y, double width, double height, Individual individual)
    {
        super(x, y, width, height, 
                Sex.toCharSymbol(individual.SEX), individual.NAME.getShortName() + " " + //$NON-NLS-1$
                (individual.DEAT != null && individual.DEAT.happened != iamd.gedcom.datamodel.Bool.N ? Event.DEATH_SYMBOL : "")); //$NON-NLS-1$

        this.individual = individual;
    }
    
    public void setFont(Font font)
    {
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
}
