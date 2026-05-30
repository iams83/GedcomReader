package iamd.gedcom.datamodel;

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
}
