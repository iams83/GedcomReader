package iamd.gedcom.format;

import java.util.Collection;
import java.util.Collections;

import iamd.gedcom.format.GedComNode.LocalizedGedComNode;

@SuppressWarnings("serial")
public class LocalizedGedComParseException extends Exception
{
    final public Collection<LocalizedGedComNode> nodeStack;
    
    final public int lineCount;
    final public String line;
    
    public LocalizedGedComParseException(GedComParseException e, Collection<LocalizedGedComNode> nodeStack, int lineCount, String line)
    {
        super(e.getMessage());
        
        this.nodeStack = Collections.unmodifiableCollection(nodeStack);
        this.lineCount = lineCount;
        this.line      = line;
    }

}
