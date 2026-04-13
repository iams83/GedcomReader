package iamd.gedcom.datamodel;

import java.io.File;
import java.io.PrintStream;
import java.util.GregorianCalendar;

import iamd.gedcom.format.GedComNode;
import iamd.gedcom.format.GedComNode.GEDNodeIgnore;
import iamd.gedcom.ui.MainWindow;

@GEDNodeIgnore(propertyToIgnore = "LANG")
@GEDNodeIgnore(propertyToIgnore = "SUBM", writeAfter = "DATE")
@GEDNodeIgnore(propertyToIgnore = "FILE", writeAfter = "SUBM")
@GEDNodeIgnore(propertyToIgnore = "COPR", writeAfter = "FILE")
@GEDNodeIgnore(propertyToIgnore = "DATE")
@GEDNodeIgnore(propertyToIgnore = "TIME")
public class Head extends GedComNode
{
    static public class Source extends GedComNode
    {
        public String sourceName;
        
        @GEDNodeAttribute
        public String VERS;

        @GEDNodeAttribute
        public String CORP;

        @GEDNodeAttribute
        public String NAME;

        public Source(String gedCode, Document document, String data)
        {
            super(gedCode, document);
            
            this.sourceName = data;
        }
        
        public Source(Document document)
        {
            super("SOUR", document);

            this.sourceName = MainWindow.APP_NAME;
            
            this.VERS = MainWindow.APP_NAME + "-" + MainWindow.VERSION;
            
            this.CORP = null;
            
            this.NAME = null;
        }

        @Override
        public String getData()
        {
            return this.sourceName;
        }

        public boolean isEmpty()
        {
            return (this.sourceName == null || this.sourceName.isEmpty()) &&
                    (this.VERS == null || this.VERS.isEmpty()) &&
                    (this.CORP == null || this.CORP.isEmpty()) &&
                    (this.NAME == null || this.NAME.isEmpty());
        }
    }

    static public class GedComFormat extends GedComNode
    {
        @GEDNodeAttribute
        public String VERS;
        
        @GEDNodeAttribute
        public String FORM;
        
        public GedComFormat(String gedCode, Document document)
        {
            super(gedCode, document);
        }

        public GedComFormat(Document document)
        {
            super("GEDC", document);
            
            this.VERS = "5.5.1";
            
            this.FORM = null;
        }

        public boolean isEmpty()
        {
            return (this.VERS == null || this.VERS.isEmpty()) &&
                    (this.FORM == null || this.FORM.isEmpty());
        }
    }
    
    @GEDNodeAttribute
    public Source SOUR;

    @GEDNodeAttribute
    public String DEST;

    @GEDNodeAttribute
    public DateTime DATE;
    
    @GEDNodeAttribute
    public GedComFormat GEDC;
    
    @GEDNodeAttribute
    public String CHAR;
    
    public Head(String gedCode, Document document)
    {
        super(gedCode, document);
    }

    public Head(Document document)
    {
        super("HEAD", document);
        
        this.SOUR = new Source(document);
        
        this.DEST = null;
        
        this.DATE = DateTime.now(document);
        
        this.GEDC = new GedComFormat(document);
        
        this.CHAR = "UTF-8";
    }

    @Override
    protected void writeIgnoredProperty(PrintStream out, int depth, String propertyToIgnore)
    {
        if (propertyToIgnore.equals("FILE"))
        {
            File file = this.getDocument().getFile();
            
            if (file != null)
                out.println(depth + " FILE " + file);
        }

        if (propertyToIgnore.equals("COPR"))
        {
            Submitter subm = this.getDocument().SUBM;

            if (subm != null && subm.NAME != null)
            {
                out.println(depth + " COPR Copyright (c) " + 
                        new GregorianCalendar().get(GregorianCalendar.YEAR) + " " +
                        subm.NAME.getData() + ".");
            }
        }

        if (propertyToIgnore.equals("SUBM"))
        {
            Submitter subm = this.getDocument().SUBM;

            if (subm != null)
                out.println(depth + " SUBM " + this.getDocument().getID(subm));
        }
    }

    public boolean isEmpty()
    {
        return (this.SOUR == null || this.SOUR.isEmpty()) &&
                (this.DEST == null || this.DEST.isEmpty()) &&
                (this.DATE == null || this.DATE.isEmpty()) &&
                (this.GEDC == null || this.GEDC.isEmpty()) &&
                (this.CHAR == null || this.CHAR.isEmpty());
    }
}
