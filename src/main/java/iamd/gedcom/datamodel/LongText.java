package iamd.gedcom.datamodel;

import java.io.PrintStream;

import iamd.gedcom.format.GedComNode;

public class LongText extends GedComNode
{
    private String str; 
    
    public LongText(String gedCode, Document context, String data)
    {
        super(gedCode, context);
        
        this.str = data.trim();
    }
    
    public LongText(String gedCode, Document context)
    {
        super(gedCode, context);
        
        this.str = "";
    }
    
    @Override
    public GedComNode setGEDNode(String gedCode, String data)
    {
        if ("CONT".equals(gedCode)) //$NON-NLS-1$
        {
            this.str += "\n" + data.trim();
        }
        
        return null;
    }
    
    @Override
    public String getData()
    {
        String[] lines = this.str.split("\n");
        
        return lines[0].trim();
    }

    @Override
    public void print(PrintStream out, int depth)
    {
        super.print(out, depth);
        
        String[] lines = this.str.split("\n");
        
        boolean first = true;
        
        for (String line : lines)
        {
            if (first)
                first = false;
            else
                out.println((depth + 1) + " CONT " + line); //$NON-NLS-1$
        }
    }

    public boolean isEmpty()
    {
        return this.str == null || this.str.length() == 0;
    }

    public String getText()
    {
        return this.str;
    }

}
