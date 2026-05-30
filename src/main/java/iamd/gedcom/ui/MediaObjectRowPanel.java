package iamd.gedcom.ui;

import iamd.gedcom.datamodel.Individual;
import iamd.gedcom.datamodel.MediaObjectReference;
import iamd.ui.ObjectRowPanel;

@SuppressWarnings("serial")
public class MediaObjectRowPanel extends ObjectRowPanel<MediaObjectRowPanel>
{
    final private MediaObjectReference mediaObjectRef;
    
    public MediaObjectRowPanel(MediaObjectReference mediaObjectRef, Individual individual, String parents, String children, 
            boolean reorderButtons, boolean deleteButton)
    {
        super(reorderButtons, deleteButton);
        
        this.mediaObjectRef = mediaObjectRef;
        
        String myString = "<html>";

        myString += "<p>" + mediaObjectRef.mediaObject.getDisplayLabel() + "</p>";
        
        this.setText(myString);
    }

    public MediaObjectReference getMediaObjectReference()
    {
        return this.mediaObjectRef;
    }
}
