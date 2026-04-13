package iamd.gedcom.datamodel;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import iamd.gedcom.format.GedComContext;
import iamd.gedcom.format.GedComNode.GEDNodeIgnore;
import iamd.gedcom.format.IdentifiedGedComNode;

@GEDNodeIgnore(propertyToIgnore = "SUBM", writeAfter = "HEAD")
@GEDNodeIgnore(propertyToIgnore = "TRLR", writeAfter = "OBJE")
@GEDNodeIgnore(propertyToIgnore = "CSTA")
@GEDNodeIgnore(propertyToIgnore = "REPO")
@GEDNodeIgnore(propertyToIgnore = "SOUR")
@GEDNodeIgnore(propertyToIgnore = "NOTE")
public class Document extends GedComContext
{
    @GEDNodeAttribute
    public Head HEAD;
    
    @GEDNodeAttribute
    public Submitter SUBM;
    
    @GEDNodeList(Individual.class)
    private ArrayList<Individual> INDI = new ArrayList<Individual>();
    
    @GEDNodeList(Family.class)
    private ArrayList<Family> FAM = new ArrayList<Family>();

    @GEDNodeList(MediaObject.class)
    private ArrayList<MediaObject> OBJE = new ArrayList<MediaObject>();

    private File file;
    
    public Document(File file)
    {
        this.file = file;
    }

    public void forceFile(File file)
    {
        this.file = file;
    }
    
    public void resetHead(File file)
    {
        this.file = file;
        
        this.HEAD = new Head(this.getDocument());
    }

    @Override
    protected void writeIgnoredProperty(PrintStream out, int depth, String propertyToIgnore)
    {
        if (propertyToIgnore.equals("TRLR"))
        {
            out.println(depth + " TRLR");
        }
    }

    public File getFile()
    {
        return this.file;
    }

    public Collection<Individual> listIndividuals()
    {
        return Collections.unmodifiableCollection(this.INDI);
    }

    public Collection<Family> listFamilies()
    {
        return Collections.unmodifiableCollection(this.FAM);
    }

    public Collection<MediaObject> listMediaObjects()
    {
        return Collections.unmodifiableCollection(this.OBJE);
    }

    public Family addNewFamily()
    {
        Family newFamily = new Family(this);
        
        this.addIdentifiedObject(newFamily);
        
        this.FAM.add(newFamily);
        
        return newFamily;
    }

    public Individual addNewIndividual()
    {
        Individual individual = new Individual(this);
        
        return this.addNewIndividual(individual);
    }

    public Individual addNewIndividual(Individual newIndividual)
    {
        this.addIdentifiedObject(newIndividual);
        
        this.INDI.add(newIndividual);
        
        return newIndividual;
    }

    public MediaObject addNewMediaObject(MediaObject newMediaObject)
    {
        this.addIdentifiedObject(newMediaObject);
        
        this.OBJE.add(newMediaObject);
        
        return newMediaObject;
    }

    public void removeEmptyFamilies()
    {
        for (Map.Entry<String, IdentifiedGedComNode> entry : 
            new TreeMap<String,IdentifiedGedComNode>(this.idMap).entrySet())
        {
            if (entry.getValue() instanceof Family)
            {
                Family family = (Family) entry.getValue();
                
                int numMembers = family.getChildren().size();
                
                if (family.getSpouse1() != null)
                    numMembers ++;
                
                if (family.getSpouse2() != null)
                    numMembers ++;
    
                if (numMembers <= 1)
                {
                    this.idMap.remove(entry.getKey());
                    
                    this.FAM.remove(family);
                    
                    for (Individual child : family.getChildren())
                        family.removeChild(child);
                }
            }
        }
    }

    public void removeIndividual(Individual individual)
    {
        if (this.INDI.size() > 1)
        {
            for (Map.Entry<String, IdentifiedGedComNode> entry : 
                new TreeMap<String,IdentifiedGedComNode>(this.idMap).entrySet())
            {
                if (entry.getValue() == individual)
                {
                    this.idMap.remove(entry.getKey());
                    
                    this.INDI.remove(individual);
                    
                    for (Family family : new ArrayList<Family>(this.FAM))
                        family.removeSpouse(individual);
                    
                    this.removeEmptyFamilies();
                }
            }
        }
    }

    public void swapFamilies(Family family1, Family family2)
    {
        int indexOfFamily1 = this.FAM.indexOf(family1);
        int indexOfFamily2 = this.FAM.indexOf(family2);
        
        this.FAM.set(indexOfFamily1, family2);
        this.FAM.set(indexOfFamily2, family1);
    }
}
