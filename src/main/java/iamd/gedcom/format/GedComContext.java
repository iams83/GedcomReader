package iamd.gedcom.format;

import java.lang.reflect.Field;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

public class GedComContext extends GedComNode
{
    public static class Reference
    {
        final private Field field;
        final private Object object;
        final private String id;
        final private boolean list;
        
        Reference(Field field, Object object, String id, boolean list)
        {
            this.field = field;
            this.object = object;
            this.id = id;
            this.list = list;
        }
        
        @SuppressWarnings({ "unchecked", "rawtypes" })
        public void set(TreeMap<String, IdentifiedGedComNode> idMap) throws GedComParseException
        {
            if (!this.list)
            {
                try
                {
                    this.field.set(this.object, idMap.get(this.id));
                }
                catch (NullPointerException | IllegalArgumentException | IllegalAccessException e)
                {
                    throw new GedComParseException("Could not assign reference " + this.id);
                }
            }
            else
            {
                try
                {
                    Object referencedObject = idMap.get(this.id);
                    if (referencedObject != null)
                        ((ArrayList) this.field.get(this.object)).add(referencedObject);
                }
                catch (NullPointerException | IllegalArgumentException | IllegalAccessException e)
                {
                    throw new GedComParseException("Could not assign reference " + this.id);
                }
            }
        }
    }
    
    public static class DelayedAssignment
    {
        final private Field field;
        final private String id;
        final private Object value;
        final private boolean list;
        
        DelayedAssignment(Field field, String id, Object value, boolean list)
        {
            this.field = field;
            this.id = id;
            this.value = value;
            this.list = list;
        }
        
        @SuppressWarnings({ "unchecked", "rawtypes" })
        public void set(TreeMap<String, IdentifiedGedComNode> idMap) throws GedComParseException
        {
            if (!this.list)
            {
                try
                {
                    this.field.set(idMap.get(this.id), this.value);
                }
                catch (NullPointerException | IllegalArgumentException | IllegalAccessException e)
                {
                    throw new GedComParseException("Could not assign reference " + this.id);
                }
            }
            else
            {
                try
                {
                    Object target = idMap.get(this.id);
                    if (target != null && this.value != null)
                        ((ArrayList) this.field.get(target)).add(this.value);
                }
                catch (NullPointerException | IllegalArgumentException | IllegalAccessException e)
                {
                    throw new GedComParseException("Could not assign reference " + this.id);
                }
            }
        }
    }
    
    protected final TreeMap<String,IdentifiedGedComNode> idMap = new TreeMap<String,IdentifiedGedComNode>();
    
    final private ArrayList<Reference> references = new ArrayList<Reference>();
    
    final private ArrayList<DelayedAssignment> assignments = new ArrayList<DelayedAssignment>();

    protected GedComContext()
    {
        super(null, null);
    }

    public String getID(GedComNode gedComNode)
    {
        for (Map.Entry<String,IdentifiedGedComNode> entry : this.idMap.entrySet())
        {
            if (entry.getValue() == gedComNode)
                return entry.getKey();
        }
        
        return null;
    }

    public IdentifiedGedComNode getObjectById(String id)
    {
        return this.idMap.get(id);
    }

    public void addReference(Field field, Object object, String id, boolean list)
    {
        this.references.add(new Reference(field, object, id, list));
    }

    public void addAssignment(Field field, String id, Object value, boolean list)
    {
        this.assignments.add(new DelayedAssignment(field, id, value, list));
    }

    public void addIdentifiedObject(String id, IdentifiedGedComNode gedNode)
    {
        this.idMap.put(id, gedNode);
    }

    public static String normalizeID(String input)
    {
        if (input == null) {
            return null;
        }

        // 1. Turn into upper case
        input = input.toUpperCase();
        
        // 2. Decompose characters (e.g., 'é' becomes 'e' + combining acute accent)
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        
        // 3. Use a regex pattern to match and remove all diacritical marks
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(normalized).replaceAll("");
    }
        
    public void addIdentifiedObject(IdentifiedGedComNode gedNode)
    {
        int i = 0;
        
        String key = gedNode.createIdentifier();
        
        if (key == null)
            key = "";
        
        else
        {
            key = normalizeID(key);

            String cleanKey = "";
            
            for (char c : key.toCharArray())
            {
                if (Character.isJavaIdentifierPart(c) ||
                    Character.isDigit(c))
                {
                    cleanKey += c;
                }
                else
                {
                    cleanKey += " ";
                }
            }

            key = cleanKey.trim().replaceAll("  +",  " ").replace(' ', '_');

            if (key.length() > 36)
                key = key.substring(0, 36).trim();
        }
        
        String id;

        if (!key.isEmpty() && !this.idMap.containsKey((id = "@" + key + "@")))
        {
            this.idMap.put(id, gedNode);
            
            return;
        }
        
        if (!key.isEmpty())
            key += "_";

        while (this.idMap.containsKey((id = "@" + key + i + "@")))
            i ++;
        
        this.idMap.put(id, gedNode);
    }
    
    public void solveReferences() throws GedComParseException
    {
        for (Reference reference : this.references)
            reference.set(this.idMap);
        
        for (DelayedAssignment assignment : this.assignments)
            assignment.set(this.idMap);
        
        TreeMap<String,Integer> typeCount = new TreeMap<String,Integer>();
        
        for (Reference reference : this.references)
        {
            if (typeCount.containsKey(reference.field.getName()))
                typeCount.put(reference.field.getName(), typeCount.get(reference.field.getName()) + 1);
            else
                typeCount.put(reference.field.getName(), 1);
        }
        
        this.references.clear();
        
        this.assignments.clear();
    }
    
    public void resetIdentifiers()
    {
        ArrayList<IdentifiedGedComNode> newIDMap = new ArrayList<>(this.idMap.values());
        
        this.idMap.clear();
        
        for (IdentifiedGedComNode gedNode : newIDMap)
            this.addIdentifiedObject(gedNode);
    }
}
