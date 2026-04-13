package iamd.gedcom.datamodel;

import iamd.gedcom.format.GedComNode;
import iamd.gedcom.format.GedComNode.GEDNodeIgnore;

@GEDNodeIgnore(propertyToIgnore = "TYPE")
@GEDNodeIgnore(propertyToIgnore = "CAUS")
@GEDNodeIgnore(propertyToIgnore = "SOUR")
@GEDNodeIgnore(propertyToIgnore = "NOTE")
@GEDNodeIgnore(propertyToIgnore = "QUAY")
public class Event extends GedComNode
{
    public static final String DEATH_SYMBOL = "\u271D";

    public Bool happened;
    
    @GEDNodeAttribute
    public DateTime DATE;

    @GEDNodeAttribute
    public String PLAC;

    public Event(String gedCode, Document document)
    {
        super(gedCode, document);
        
        this.happened = null;
    }

    public Event(String gedCode, Document document, String data)
    {
        super(gedCode, document);
        
        this.happened = data == null ? null : Bool.valueOf(data);
    }
    
    @Override
    public String getData()
    {
        return this.DATE != null || this.PLAC != null ? null : 
                    (this.happened != null ? this.happened.name() : null);
    }
}
