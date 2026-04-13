package iamd.gedcom.format;

import java.io.PrintStream;
import java.util.ArrayList;

import iamd.gedcom.datamodel.Document;

class GenericGedComNode extends GedComNode
{
    final public String data;
    
    public ArrayList<GedComNode> nodes = new ArrayList<GedComNode>();

    public GenericGedComNode(String gedCode, Document document)
    {
        this(gedCode, document, null);
    }
    
    public GenericGedComNode(String gedCode, Document document, String data)
    {
        super(gedCode, document);
        
        this.data = data;
    }

    @Override
    public String getData()
    {
        return this.data;
    }
    
    @Override
    public GedComNode setGEDNode(String gedCode, String rest) throws GedComParseException
    {
        GedComNode node = super.setGEDNode(gedCode, rest);
        
        if (node != null)
            return node;

        GenericGedComNode gedNode = new GenericGedComNode(gedCode, this.getDocument(), rest);

        this.nodes.add(gedNode);
        
        return gedNode;
    }

    public void print(PrintStream out, int depth)
    {
        super.print(out, depth);
        
        for (GedComNode node : this.nodes)
            node.print(out, depth + 1);
    }

    public void printReference(PrintStream out, String referenceKey, int depth)
    {
    }
}
