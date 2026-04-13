package iamd.gedcom.datamodel;

import iamd.gedcom.format.IdentifiedGedComNode;

public class MediaObject extends IdentifiedGedComNode
{
    public enum MediaType
    {
        Picture, Audio, Video, Document
    }
    
    @GEDNodeAttribute
    public String FILE, FORM;
    
    @GEDNodeAttribute
    public MediaType TYPE;
    
    @GEDNodeAttribute
    public String TITL;

    public MediaObject(String gedCode, Document context)
    {
        super(gedCode, context);
    }

    public MediaObject(Document document)
    {
        this("OBJE", document);
    }

    @Override
    public String createIdentifier()
    {
        return "MEDIA" + hashCode();
    }
}
