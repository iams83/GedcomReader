package iamd.gedcom.datamodel;

import iamd.gedcom.format.GedComNode;

public class Crop extends GedComNode
{
    public Crop(String gedCode, Document context)
    {
        super(gedCode, context);
    }

    public Crop(Document document)
    {
        this("CROP", document);
    }

    @GEDNodeAttribute
    public int TOP;

    @GEDNodeAttribute
    public int LEFT;

    @GEDNodeAttribute
    public int WIDTH;

    @GEDNodeAttribute
    public int HEIGHT;
}
