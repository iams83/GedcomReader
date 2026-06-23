package iamd.gedcom.ui;

import java.awt.Image;
import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;
import javax.swing.JLabel;

import iamd.gedcom.datamodel.Individual;
import iamd.gedcom.datamodel.MediaObject.MediaType;
import iamd.gedcom.datamodel.MediaObjectReference;
import iamd.gedcom.rsrc.Resources;
import iamd.ui.ObjectRowPanel;

@SuppressWarnings("serial")
public class MediaObjectRowPanel extends ObjectRowPanel<MediaObjectRowPanel>
{
    final private MediaObjectReference mediaObjectRef;
    
    final private JLabel panelIcon;

    public MediaObjectRowPanel(MediaObjectReference mediaObjectRef, Individual individual, String parents, String children, 
            boolean reorderButtons, boolean deleteButton)
    {
        super(reorderButtons, deleteButton);
        
        this.mediaObjectRef = mediaObjectRef;
        
        String myString = "<html>";

        myString += "<p>" + (mediaObjectRef.mediaObject != null ? mediaObjectRef.mediaObject.getDisplayLabel() : "Not found") + "</p>";
        
        this.setText(myString);

        this.panelIcon = new JLabel(mediaObjectRef.mediaObject != null ? mediaObjectRef.mediaObject.getIconType() : Resources.MediaIcon);
        this.addComponent(this.panelIcon);

        this.refresh();
    }


    private ImageIcon getThumbnailIcon(Image image)
    {
        if (image == null)
            return Resources.MediaIcon;

        // Create a thumbnail icon (e.g., 100x100 pixels)
        int thumbnailHeight = 100;
        int thumbnailWidth = thumbnailHeight * image.getWidth(null) / image.getHeight(null);
        BufferedImage thumbnail = new BufferedImage(thumbnailWidth, thumbnailHeight, BufferedImage.TYPE_INT_ARGB);
        thumbnail.getGraphics().drawImage(image, 0, 0, thumbnailWidth, thumbnailHeight, null);
        return new ImageIcon(thumbnail);
    }

    public void refresh()
    {
        if (mediaObjectRef.mediaObject == null)
            this.panelIcon.setIcon(Resources.MediaIcon);
        else if (mediaObjectRef.mediaObject.TYPE == MediaType.Picture)
            this.panelIcon.setIcon(getThumbnailIcon(mediaObjectRef.getCroppedImage()));
        else
            this.panelIcon.setIcon(getThumbnailIcon(mediaObjectRef.mediaObject.getIconType().getImage()));
    }

    public MediaObjectReference getMediaObjectReference()
    {
        return this.mediaObjectRef;
    }
}
