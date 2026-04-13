package iamd.gedcom.ui;

import iamd.gedcom.datamodel.Individual;
import iamd.ui.ObjectRowPanel;

@SuppressWarnings("serial")
public class IndividualRowPanel extends ObjectRowPanel<IndividualRowPanel>
{
    final private Individual individual;
    
    public IndividualRowPanel(Individual individual, boolean reorderButtons, boolean deleteButton)
    {
        super(reorderButtons, deleteButton);
        
        this.individual = individual;
        
        String myString = "<html>";

        myString += "<p>" + Individual.Sex.toCharSymbol(individual.SEX) + " <strong>" + individual.getName() + "</strong></p>";
        
        myString += "</html>";

        this.setText(myString);
    }

    public Individual getIndividual()
    {
        return this.individual;
    }
}
