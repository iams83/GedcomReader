package iamd.gedcom.datamodel;

import java.awt.image.BufferedImage;

import iamd.gedcom.format.GedComNode;
import iamd.gedcom.format.GedComParseException;

public class MediaObjectReference extends GedComNode
{
    public MediaObject mediaObject;
    
    @GEDNodeAttribute
    public Crop CROP;

    public MediaObjectReference(String gedCode, Document document)
    {
        super(gedCode, document);
    }

    public MediaObjectReference(String gedCode, Document document, String data) throws GedComParseException
    {
        super(gedCode, document);
        
        try
        {
            document.addReference(this.getClass().getDeclaredField("mediaObject"), this, data, false);
        }
        catch (NoSuchFieldException | SecurityException e)
        {
            throw new GedComParseException(e);
        }
    }

    public MediaObjectReference(Document document, MediaObject mediaObject)
    {
        this("OBJE", document);
        this.mediaObject = mediaObject;
    }
    
    @Override
    protected String getData()
    {
        return this.getContext().getID(this.mediaObject);
    }
    
    /**
     * Returns a cropped version of the referenced image based on the CROP attribute.
     * If no crop is defined or the media object is not a picture, returns the full image.
     * 
     * @return A BufferedImage cropped according to the CROP values, or null if the image cannot be loaded
     */
    public BufferedImage getCroppedImage()
    {
        if (this.mediaObject == null)
        {
            return null;
        }
        
        BufferedImage originalImage = this.mediaObject.getImage();
        if (originalImage == null)
        {
            return null;
        }

        // If no crop defined, return the full image
        if (this.CROP == null)
        {
            return originalImage;
        }
        
        int cropX = this.CROP.LEFT;
        int cropY = this.CROP.TOP;
        int cropWidth = this.CROP.WIDTH;
        int cropHeight = this.CROP.HEIGHT;
        
        // If crop dimensions are invalid (zero or negative), return the full image
        if (cropWidth <= 0 || cropHeight <= 0)
        {
            return originalImage;
        }
        
        // Clamp crop bounds to image dimensions
        int imageWidth = originalImage.getWidth();
        int imageHeight = originalImage.getHeight();
        
        // Ensure crop coordinates are within image bounds
        if (cropX < 0) cropX = 0;
        if (cropY < 0) cropY = 0;
        
        // Ensure crop doesn't exceed image dimensions
        if (cropX >= imageWidth || cropY >= imageHeight)
        {
            return originalImage;
        }
        
        // Clamp width and height
        int maxWidth = imageWidth - cropX;
        int maxHeight = imageHeight - cropY;
        cropWidth = Math.min(cropWidth, maxWidth);
        cropHeight = Math.min(cropHeight, maxHeight);
        
        // Return the cropped sub-image
        return originalImage.getSubimage(cropX, cropY, cropWidth, cropHeight);
    }
}
