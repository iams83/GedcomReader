package iamd.gedcom.ui;

import iamd.gedcom.datamodel.Family;
import iamd.gedcom.datamodel.Individual;

public interface FamilySelectionListener
{
    void familyHovered(Family family);

    void individualHovered(Individual individual);

    void familyClicked(Family family);

    void individualClicked(Individual individual);

    void nothingHovered();
}
