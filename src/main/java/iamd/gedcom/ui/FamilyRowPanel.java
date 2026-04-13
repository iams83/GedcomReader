package iamd.gedcom.ui;

import java.util.Collection;

import iamd.gedcom.datamodel.Family;
import iamd.gedcom.datamodel.Individual;
import iamd.ui.ObjectRowPanel;

@SuppressWarnings("serial")
public class FamilyRowPanel extends ObjectRowPanel<FamilyRowPanel>
{
    final private Family family;
    
    public FamilyRowPanel(Family family, Individual individual, String parents, String children, 
            boolean reorderButtons, boolean deleteButton)
    {
        super(reorderButtons, deleteButton);
        
        this.family = family;
        
        String myString = "<html>";

        myString += "<p>" + parents + ": " + family.spousesToString(individual) + "</p>";
        
        Collection<Individual> childrenList = family.getChildren();
        
        if (!childrenList.isEmpty())
        {
            myString += "<p>" + children + ": ";
            
            boolean first = true;
            
            for (Individual child : childrenList)
            {
                if (first)
                    first = false;
                else
                    myString += ", ";

                myString += 
                        Individual.Sex.toCharSymbol(child.SEX) + " " +
                        (individual == child ? "<strong>" : "") + child.NAME.name +
                        (individual == child ? "</strong>" : "");
            }
            
            myString += "</p>";
        }
        
        myString += "</html>";
    
        this.setText(myString);
    }

    public Family getFamily()
    {
        return this.family;
    }
}
