package iamd.gedcom.ui;

import java.util.ArrayList;
import java.util.Collection;

import iamd.gedcom.datamodel.Family;
import iamd.gedcom.datamodel.Individual;
import iamd.gedcom.datamodel.Individual.FamilyChildRelationship;
import iamd.gedcom.datamodel.MediaObject;
import iamd.gedcom.ui.FamilyRowPanel;
import iamd.gedcom.ui.IndividualRowPanel;

public class GedComRowPanelList
{
    static public Collection<IndividualRowPanel> getIndividualRowPanelList(Collection<Individual> individuals, 
            boolean reorderButtons, boolean deleteButton)
    {
        ArrayList<IndividualRowPanel> list = new ArrayList<IndividualRowPanel>();
        
        for (Individual child : individuals)
            list.add(new IndividualRowPanel(child, reorderButtons, deleteButton));
            
        return list;
    }

    static public Collection<MediaObjectRowPanel> getMediaObjectRowPanelList(Collection<MediaObject> mediaObjects, Individual individual, String parents, String children, 
            boolean reorderButtons, boolean deleteButton)
    {
        ArrayList<MediaObjectRowPanel> list = new ArrayList<MediaObjectRowPanel>();
        
        for (MediaObject mediaObject : mediaObjects)
            list.add(new MediaObjectRowPanel(mediaObject, individual, parents, children, reorderButtons, deleteButton));
            
        return list;
    }

    static public Collection<FamilyRowPanel> getFamilyRowPanelList(Collection<Family> families, Individual individual, String parents, String children, 
            boolean reorderButtons, boolean deleteButton)
    {
        ArrayList<FamilyRowPanel> list = new ArrayList<FamilyRowPanel>();
        
        for (Family family : families)
            list.add(new FamilyRowPanel(family, individual, parents, children, reorderButtons, deleteButton));
            
        return list;
    }

    static public Collection<FamilyRowPanel> getFamilyChildRowPanelList(Collection<FamilyChildRelationship> families, Individual individual, String parents, String children, 
            boolean reorderButtons, boolean deleteButton)
    {
        ArrayList<FamilyRowPanel> list = new ArrayList<FamilyRowPanel>();
        
        for (FamilyChildRelationship familyChild : families)
            list.add(new FamilyRowPanel(familyChild.family, individual, parents, children, reorderButtons, deleteButton));
            
        return list;
    }


}
