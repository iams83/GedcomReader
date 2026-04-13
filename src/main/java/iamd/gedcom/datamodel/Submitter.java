package iamd.gedcom.datamodel;

import iamd.gedcom.format.GedComNode;
import iamd.gedcom.format.IdentifiedGedComNode;

public class Submitter extends IdentifiedGedComNode
{
    static public class SubmitterName extends GedComNode
    {
        public String name, surname;
        
        public SubmitterName(String gedCode,  Document document, String data)
        {
            super(gedCode, document);
            
            if (data == null)
                throw new AssertionError("Unexpected rest after NAME: " + data);
            
            if (data.endsWith("/"))
            {
                int indexOfSlash = data.indexOf("/");
                
                this.name = data.substring(0, indexOfSlash).trim();
                this.surname = data.substring(indexOfSlash + 1, data.length() - 1).trim();
            }
            else
            {
                this.name = data.trim();
                this.surname = null;
            }
        }
        
        public SubmitterName(Document document)
        {
            super("NAME", document);
            
            this.name = null;
            this.surname = null;
        }
        
        @Override
        public String getData()
        {
            return this.name + 
                    (this.surname != null ? " /" + this.surname + "/" : "");
        }

        public boolean isEmpty()
        {
            return (this.name == null || this.name.isEmpty()) &&
                   (this.surname == null || this.surname.isEmpty());
        }
    }

    @GEDNodeAttribute
    public SubmitterName NAME;

    @GEDNodeAttribute
    public LongText ADDR;

    @GEDNodeAttribute
    public String PHON;

    @GEDNodeAttribute
    public String EMAIL;

    @GEDNodeAttribute
    public LongText COMM;

    @GEDNodeAttribute
    public LongText NOTE;

    public Submitter(String gedCode, Document document)
    {
        super(gedCode, document);
    }

    public Submitter(Document document)
    {
        super("SUBM", document);
    }

    @Override
    public String createIdentifier()
    {
        return "SUBM";
    }

    public boolean isEmpty()
    {
        return (this.NAME == null || this.NAME.isEmpty()) &&
               (this.ADDR == null || this.ADDR.isEmpty()) &&
               (this.PHON == null || this.PHON.isEmpty()) &&
               (this.EMAIL== null || this.EMAIL.isEmpty()) &&
               (this.COMM == null || this.COMM.isEmpty()) &&
               (this.NOTE == null || this.NOTE.isEmpty());
    }
}
