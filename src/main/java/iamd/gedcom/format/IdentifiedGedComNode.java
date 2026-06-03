package iamd.gedcom.format;

import iamd.gedcom.datamodel.Document;

abstract public class IdentifiedGedComNode extends GedComNode implements Comparable<IdentifiedGedComNode>
{
    private static int counter = 0;
    
    final public String INSTANCEID = String.valueOf(++ counter);
    
    protected IdentifiedGedComNode(String gedCode, Document context)
    {
        super(gedCode, context);
    }

    abstract public String createIdentifier();

    @Override
    public int compareTo(IdentifiedGedComNode other)
    {
        return this.INSTANCEID.compareTo(other.INSTANCEID);
    }
}
