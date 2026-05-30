package iamd.gedcom.datamodel;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import iamd.gedcom.format.GedComNode;
import iamd.gedcom.format.GedComNode.GEDNodeIgnore;
import iamd.gedcom.format.GedComParseException;
import iamd.gedcom.format.IdentifiedGedComNode;

@GEDNodeIgnore(propertyToIgnore = "FAMS", writeAfter = "FAMC")
@GEDNodeIgnore(propertyToIgnore = "BAPM")
@GEDNodeIgnore(propertyToIgnore = "BURI")
@GEDNodeIgnore(propertyToIgnore = "TITL")
@GEDNodeIgnore(propertyToIgnore = "CHR")
@GEDNodeIgnore(propertyToIgnore = "HIST")
@GEDNodeIgnore(propertyToIgnore = "RELI")
@GEDNodeIgnore(propertyToIgnore = "AFN")
@GEDNodeIgnore(propertyToIgnore = "BAPL")
@GEDNodeIgnore(propertyToIgnore = "ENDL")
@GEDNodeIgnore(propertyToIgnore = "SLGC")
@GEDNodeIgnore(propertyToIgnore = "REFN")
@GEDNodeIgnore(propertyToIgnore = "ASSO")
@GEDNodeIgnore(propertyToIgnore = "RESI")
@GEDNodeIgnore(propertyToIgnore = "DSCR")
@GEDNodeIgnore(propertyToIgnore = "SOUR")
@GEDNodeIgnore(propertyToIgnore = "EVEN")
public class Individual extends IdentifiedGedComNode
{
    static public enum Sex
    {
        M("\u2642"), F("\u2640");
        
        final public String symbol;
        
        Sex(String symbol)
        {
            this.symbol = symbol;
        }

        static public String toCharSymbol(Sex sex)
        {
            return sex == null ? "" : sex.symbol;
        }
    }

    static public class Name extends GedComNode
    {
        public String name, surname, nick;
        
        public Name(String gedCode,  Document document)
        {
            this(gedCode, document, "");
        }
        
        public Name(String gedCode,  Document document, String data)
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
        
        @Override
        public String getData()
        {
            return this.name + (this.surname != null ? " /" + this.surname + "/" : "");
        }

        @Override
        public void print(PrintStream out, int depth)
        {
            super.print(out, depth);
            
            out.println((depth + 1) + " GIVN " + this.name);
            
            if (this.surname != null && !this.surname.isEmpty())
                out.println((depth + 1) + " SURN " + this.surname);
            
            if (this.nick != null && !this.nick.isEmpty())
                out.println((depth + 1) + " NICK " + this.nick);
        }

        @Override
        public GedComNode setGEDNode(String gedCode, String data) throws GedComParseException
        {
            if ("NICK".equals(gedCode))
                this.nick = data;
            
            if ("GIVN".equals(gedCode) && !this.name.equals(data))
            {
              //  throw new GedComParseException("Unexpected GIVN value '" + data + "' (expected: '" + this.name + ")'");
            }

            if ("SURN".equals(gedCode) && (this.surname == null || !this.surname.equals(data)))
            {
                if (this.surname != null)
                    this.name = this.name.trim() + " (" + this.surname + ")";
                
                this.surname = data;
            }

            return null;
        }

        public String createIdentifier()
        {
            String s = "";
            
            if (this.name != null)
                s = this.name;
            
            if (this.name != null && this.surname != null)
                s += " ";
            
            if (this.surname != null)
                s += this.surname;
            
            return s;
        }

        public String getShortName()
        {
            if (this.nick != null && !this.nick.isEmpty())
                return "'" + this.nick + "'";
            
            if (this.name != null && !this.name.isEmpty())
                return this.name;
            
            if (this.surname != null && !this.surname.isEmpty())
                return "/" + this.surname + "/";
            
            return "\uFFFD"; 
        }
    }

    public enum PedigreeLinkageType
    {
        adopted, birth, foster, sealing
    }
    
    static public class FamilyChildRelationship extends GedComNode
    {
        public Family family;
        
        @GEDNodeAttribute
        public PedigreeLinkageType PEDI;
        
        Integer siblingNumber = 0;

        public FamilyChildRelationship(String gedCode, Document document, String data) throws GedComParseException
        {
            super(gedCode, document);
            
            try
            {
                document.addReference(this.getClass().getDeclaredField("family"), this, data, false);
            }
            catch (NoSuchFieldException | SecurityException e)
            {
                throw new GedComParseException(e);
            }
        }
        
        public FamilyChildRelationship(Document document, Family family)
        {
            super("FAMC", document);
            
            this.family = family;
        }
        
        @Override
        public String getData()
        {
            return this.getContext().getID(this.family);
        }
    }

    @GEDNodeAttribute
    public Name NAME;

    @GEDNodeAttribute
    public Sex SEX;

    @GEDNodeAttribute
    public Event BIRT;

    @GEDNodeAttribute
    public Event DEAT;

    @GEDNodeAttribute
    public String OCCU;

    @GEDNodeAttribute
    public String EDUC;

    @GEDNodeAttribute
    public String HEAL;

    @GEDNodeAttribute
    public LongText NOTE;

    @GEDNodeList(FamilyChildRelationship.class)
    public ArrayList<FamilyChildRelationship> FAMC = new ArrayList<FamilyChildRelationship>();
    
    @GEDNodeAttribute
    public Event CHAN;
    
    @GEDNodeList(MediaObjectReference.class)
    public ArrayList<MediaObjectReference> OBJE = new ArrayList<MediaObjectReference>();

    public Individual(String gedCode, Document document)
    {
        super(gedCode, document);
    }

    public Individual(Document document)
    {
        this("INDI", document);
        
        this.NAME = new Name("NAME", document, "");
    }

    @Override
    public GedComNode setGEDNode(String gedCode, String data) throws GedComParseException
    {
        if ("ALIA".equals(gedCode)) //$NON-NLS-1$
        {
            if (this.NAME != null)
            {
                this.NAME.nick = data;
                
                return GedComNode.Null;
            }
            else
            {
                throw new GedComParseException("ALIA found but no name defined yet");
            }
        }
        
        return super.setGEDNode(gedCode, data);
    }
    
    @Override
    protected void writeIgnoredProperty(PrintStream out, int depth, String propertyToIgnore)
    {
        if (propertyToIgnore.equals("FAMS"))
        {
            for (Family family : this.getFamilies())
                out.println(depth + " FAMS " + this.getContext().getID(family));
        }
    }
    
    public Collection<Family> getFamilies()
    {
        ArrayList<Family> families = new ArrayList<Family>();
        
        for (Family family : this.getDocument().listFamilies())
        {
            if (family.getSpouse1() == this || 
                family.getSpouse2() == this)
                families.add(family);
        }
        
        return Collections.unmodifiableCollection(families);
    }

    public List<FamilyChildRelationship> getParentFamilies()
    {
        if (this.FAMC != null)
            return Collections.unmodifiableList(this.FAMC);
        
        return Collections.unmodifiableList(new ArrayList<FamilyChildRelationship>());
    }

    public String getName()
    {
        if (this.NAME == null)
            return "\uFFFD"; 
        
        return this.NAME.getShortName();
    }

    public FamilyChildRelationship getFamilyChild(Family family)
    {
        if (this.FAMC != null)
        {
            for (FamilyChildRelationship familyChild : this.FAMC)
                if (familyChild.family == family)
                    return familyChild;
        }
        
        return null;
    }

    @Override
    public String createIdentifier()
    {
        if (this.NAME != null)
            return this.NAME.createIdentifier();
        
        return null;
    }

    public void remove()
    {
        this.getDocument().removeIndividual(this);
    }

    public void setIndividualOlderFamily(Family givenFamily)
    {
        Family olderFamily = null;
        
        for (Family family : this.getDocument().listFamilies())
        {
            if (family.getSpouse1() == this || 
                family.getSpouse2() == this)
            {
                if (family == givenFamily)
                    break;
                
                olderFamily = family;
            }
        }
        
        if (olderFamily != null)
            this.getDocument().swapFamilies(olderFamily, givenFamily);
    }

    public void setIndividualYoungerFamily(Family givenFamily)
    {
        boolean foundGivenFamily = false;
        
        for (Family family : this.getDocument().listFamilies())
        {
            if (family.getSpouse1() == this || 
                family.getSpouse2() == this)
            {
                if (family == givenFamily)
                {
                    foundGivenFamily = true;
                }
                else if (foundGivenFamily)
                {
                    this.getDocument().swapFamilies(family, givenFamily);
                    return;
                }
            }
        }
        
    }

    public void fireAttributeChanged()
    {
        if (this.CHAN == null)
            this.CHAN = new Event("CHAN", this.getDocument());
        
        this.CHAN.DATE = DateTime.now(this.getDocument());
    }

    public void setIndividualOlderMediaObject(MediaObjectReference mediaObjectRef)
    {
        int i = this.OBJE.indexOf(mediaObjectRef);
        
        if (i > 0)
        {
            this.OBJE.remove(i);
            this.OBJE.add(i - 1, mediaObjectRef);
        }
    }

    public void setIndividualYoungerMediaObject(MediaObjectReference mediaObjectRef)
    {
        int i = this.OBJE.indexOf(mediaObjectRef);
        
        if (i < this.OBJE.size() - 1)
        {
            this.OBJE.remove(i);
            this.OBJE.add(i + 1, mediaObjectRef);
        }
    }

    public void removeMediaObject(MediaObjectReference mediaObjectRef)
    {
        this.OBJE.remove(mediaObjectRef);
    }

    public void addNewMediaObjectReference(MediaObjectReference mediaObjectRef)
    {
        this.OBJE.add(mediaObjectRef);
    }
}
