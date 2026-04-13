package iamd.gedcom.datamodel;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.TreeMap;

import iamd.gedcom.datamodel.Individual.FamilyChildRelationship;
import iamd.gedcom.datamodel.Individual.Sex;
import iamd.gedcom.format.GedComNode;
import iamd.gedcom.format.GedComNode.GEDNodeIgnore;
import iamd.gedcom.format.GedComParseException;
import iamd.gedcom.format.IdentifiedGedComNode;

@GEDNodeIgnore(propertyToIgnore = "HUSB", writeAfter = "")
@GEDNodeIgnore(propertyToIgnore = "WIFE", writeAfter = "")
@GEDNodeIgnore(propertyToIgnore = "CHIL", writeAfter = "DIV")
@GEDNodeIgnore(propertyToIgnore = "MARC")
@GEDNodeIgnore(propertyToIgnore = "SLGS")
@GEDNodeIgnore(propertyToIgnore = "SOUR")
@GEDNodeIgnore(propertyToIgnore = "EVEN")
public class Family extends IdentifiedGedComNode
{
    private ArrayList<Individual> spouses = new ArrayList<>();

    @GEDNodeAttribute
    public Event MARR;

    @GEDNodeAttribute
    public Event DIV;
    
    @GEDNodeAttribute
    public LongText NOTE;

    @GEDNodeAttribute
    public Event CHAN;

    public Family(String gedCode, Document document)
    {
        super(gedCode, document);
    }
    
    public Family(Document document)
    {
        super("FAM", document);
    }
    
    static private int numChildren = 0;
    
    @Override
    public GedComNode setGEDNode(String gedCode, String rest) throws GedComParseException
    {
        if (gedCode.equals("CHIL"))
        {
            Individual individual = (Individual) this.getDocument().getObjectById(rest);
            
            for (FamilyChildRelationship familyChild : individual.FAMC)
                familyChild.siblingNumber = numChildren ++;
            
            return null;
        }
        else if (gedCode.equals("HUSB") || gedCode.equals("WIFE"))
        {
            Individual individual = (Individual) this.getDocument().getObjectById(rest);
            
            this.spouses.add(individual);
            
            return null;
        }
        else
        {
            return super.setGEDNode(gedCode, rest);
        }
    }
    
    @Override
    protected void writeIgnoredProperty(PrintStream out, int depth, String propertyToIgnore)
    {
        if (propertyToIgnore.equals("HUSB"))
        {
            for (Individual individual : this.spouses)
                if (individual.SEX != Sex.F)
                    out.println(depth + " HUSB " + this.getDocument().getID(individual));
        }
        else if (propertyToIgnore.equals("WIFE"))
        {
            for (Individual individual : this.spouses)
                if (individual.SEX == Sex.F)
                    out.println(depth + " WIFE " + this.getDocument().getID(individual));
        }
        else if (propertyToIgnore.equals("CHIL"))
        {
            for (Individual individual : this.getChildren())
                out.println(depth + " CHIL " + this.getDocument().getID(individual));
        }
    }

    public Collection<Individual> getChildren()
    {
        TreeMap<Integer,Individual> children = new TreeMap<Integer,Individual>();
        
        for (Individual individual : this.getDocument().listIndividuals())
        {
            FamilyChildRelationship familyChild = individual.getFamilyChild(this);

            if (familyChild != null)
            {
                if (children.containsKey(familyChild.siblingNumber))
                    throw new AssertionError("Two siblings with the same sibling number!");
                
                children.put(familyChild.siblingNumber, individual);
            }
        }
        
        return Collections.unmodifiableCollection(children.values());
    }

    public boolean setIndividualOlderBrother(Individual individual)
    {
        Individual[] siblings = getChildren().toArray(new Individual[0]);
        
        for (int i = 1; i < siblings.length; i ++)
        {
            if (siblings[i] == individual)
            {
                swapBrothers(individual, siblings[i - 1]);
                return true;
            }
        }

        return false;
    }

    public boolean setIndividualYoungerBrother(Individual individual)
    {
        Individual[] siblings = getChildren().toArray(new Individual[0]);
        
        for (int i = 0; i < siblings.length - 1; i ++)
        {
            if (siblings[i] == individual)
            {
                swapBrothers(individual, siblings[i + 1]);
                return true;
            }
        }

        return false;
    }
    
    public void swapBrothers(Individual bro1, Individual bro2)
    {
        FamilyChildRelationship fam1 = bro1.getFamilyChild(this);
        
        if (fam1 == null)
            return;
        
        FamilyChildRelationship fam2 = bro2.getFamilyChild(this);
        
        if (fam2 == null)
            return;
        
        int tmp = fam1.siblingNumber;
        fam1.siblingNumber = fam2.siblingNumber;
        fam2.siblingNumber = tmp;
    }

    public void removeChild(Individual individual)
    {
        FamilyChildRelationship familyChild = individual.getFamilyChild(this);
        
        if (familyChild != null)
            individual.FAMC.remove(familyChild);
        
        this.getDocument().removeEmptyFamilies();
    }

    public void addChild(Individual individual)
    {
        if (individual.FAMC == null)
            individual.FAMC = new ArrayList<>();
        
        FamilyChildRelationship familyChild = new FamilyChildRelationship(this.getDocument(), this);
        
        familyChild.siblingNumber = numChildren ++;
        
        individual.FAMC.add(familyChild);
    }

    public Individual getSpouse(Individual individual)
    {
        if (individual == this.getSpouse1())
            return this.getSpouse2();
        
        if (individual == this.getSpouse2())
            return this.getSpouse1();
        
        return null;
    }

    public void addSpouse(Individual individual)
    {
        this.spouses.add(individual);
    }
    
    public void removeSpouse(Individual individual)
    {
        this.spouses.remove(individual);
        
        this.getDocument().removeEmptyFamilies();
    }
    
    public Individual getSpouse1()
    {
        return this.spouses.size() > 0 ? this.spouses.get(0) : null;
    }

    public Individual getSpouse2()
    {
        return this.spouses.size() > 1 ? this.spouses.get(1) : null;
    }

    public String spousesToString(Individual individual)
    {
        String myString = "";
        
        Individual spouse1 = this.getSpouse1();
        Individual spouse2 = this.getSpouse2();
        
        if (spouse1 != null && individual != spouse1)
            myString += 
                    Individual.Sex.toCharSymbol(spouse1.SEX) + " " +
                    (individual == spouse1 ? "<strong>" : "") + spouse1.NAME.name + 
                    (individual == spouse1 ? "</strong>" : "");
        
        if (spouse1 != null && individual != spouse1 &&
            spouse2 != null && individual != spouse2)
        {
            myString += " y ";
        }
        
        if (spouse2 != null && individual != spouse2)
            myString += 
                    Individual.Sex.toCharSymbol(spouse2.SEX) + " " +
                    (individual == spouse2 ? "<strong>" : "") + spouse2.NAME.name + 
                    (individual == spouse2 ? "</strong>" : "");
        
        return myString;
    }

    public String getSpouseNames()
    {
        String myString = "";
        
        Individual spouse1 = this.getSpouse1();
        Individual spouse2 = this.getSpouse2();
        
        if (spouse1 != null)
            myString += spouse1.getName();
        
        if (spouse1 != null && spouse2 != null)
            myString += " & ";
        
        if (spouse2 != null)
            myString += spouse2.getName();
        
        return myString;
    }


    @Override
    public String createIdentifier()
    {
        return this.getSpouseNames();
    }

    public Individual getMember()
    {
        if (!this.spouses.isEmpty())
            return this.spouses.get(0);
        
        Collection<Individual> children = this.getChildren();
        
        if (!children.isEmpty())
            return children.iterator().next();
        
        return null;
    }

    public void fireAttributeChanged()
    {
        if (this.CHAN == null)
            this.CHAN = new Event("CHAN", this.getDocument());
        
        this.CHAN.DATE = DateTime.now(this.getDocument());
    }
}
