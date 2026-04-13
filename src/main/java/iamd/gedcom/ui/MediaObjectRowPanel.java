package iamd.gedcom.ui;

import iamd.gedcom.datamodel.Individual;
import iamd.gedcom.datamodel.MediaObject;
import iamd.ui.ObjectRowPanel;

@SuppressWarnings("serial")
public class MediaObjectRowPanel extends ObjectRowPanel<MediaObjectRowPanel>
{
    final private MediaObject mediaObject;
    
    public MediaObjectRowPanel(MediaObject mediaObject, Individual individual, String parents, String children, 
            boolean reorderButtons, boolean deleteButton)
    {
        super(reorderButtons, deleteButton);
        
        this.mediaObject = mediaObject;
        
        String myString = "<html>";

        myString += "<p>" + mediaObject.toString() + "</p>";
        
        this.setText(myString);
    }

    public MediaObject getMediaObject()
    {
        return this.mediaObject;
    }
}
