package iamd.gedcom.format;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import iamd.gedcom.datamodel.Document;

abstract public class GedComNode
{
    private static final String DOCUMENT_GED = "document.ged";

    final static public GedComNode Null = new GedComNode(null, null) {};
    
    final static public GedComNode IgnoreNode = new GedComNode(null, null) {};
    
    @Retention(RetentionPolicy.RUNTIME)
    public @interface GEDNodeAttribute
    {

    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface GEDNodeReference
    {

    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface GEDNodeList
    {
        Class<? extends GedComNode> value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface GEDNodeReferenceList
    {
    }
    
    @Retention(RetentionPolicy.RUNTIME)
    public @interface GEDNodeIgnores
    {
        GEDNodeIgnore[] value();
    }
    
    @Repeatable(GEDNodeIgnores.class)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface GEDNodeIgnore
    {
        String propertyToIgnore();
        
        String writeAfter() default "";
    }

    final public String gedCode;
    
    final private Document document;
    
    protected GedComNode(String gedCode, Document context)
    {
        this.gedCode = gedCode;
        this.document = context;
    }

    @Override
    public String toString()
    {
        String s = "";
        
        String id = this.getContext().getID(this);
        
        if (id != null)
            s += id + " ";

        s += this.gedCode;
        
        String data = this.getData();

        if (data != null)
            s += " " + data;
        
        return s;
    }
    
    public GedComContext getContext()
    {
        return this.document != null ? this.document : (GedComContext) this;
    }

    public Document getDocument()
    {
        return this.document != null ? this.document : (Document) this;
    }

    protected String getData()
    {
        return null;
    }

    public void print(PrintStream out, int depth)
    {
        if (depth >= 0)
            out.println(depth + " " + this.toString());
        
        try
        {
            printIgnoredFields(out, depth, "");
            
            for (Field field : this.getClass().getDeclaredFields())
            {
                if (field.isSynthetic() || Modifier.isStatic(field.getModifiers()) || Modifier.isFinal(field.getModifiers()))
                    continue;
                
                field.setAccessible(true);
                
                Object value = field.get(this);

                if (value != null)
                {
                    if (field.isAnnotationPresent(GEDNodeAttribute.class))
                    {
                        if (field.getType().isEnum() || field.getType().equals(String.class))
                        {
                            out.println((depth + 1) + " " + field.getName() + " " + value);
                        }
                        else if (value instanceof GedComNode)
                        {
                            ((GedComNode) value).print(out, depth + 1);
                        }
                        else if (field.getType().isPrimitive())
                        {
                            out.println((depth + 1) + " " + field.getName() + " " + value);
                        }
                    }
    
                    else if (field.isAnnotationPresent(GEDNodeReference.class))
                    {
                        if (value instanceof GedComNode)
                        {
                            out.println((depth + 1) + " " + field.getName() + " " + this.getContext().getID((GedComNode) value));
                        }
                    }

                    else if (field.isAnnotationPresent(GEDNodeReferenceList.class))
                    {
                        ArrayList<?> nodes = ((ArrayList<?>) value);
                        
                        for (Object node : nodes)
                        {
                            if (node instanceof GedComNode)
                                out.println((depth + 1) + " " + field.getName() + " " + this.getContext().getID((GedComNode) node));
                        }
                    }
                    
                    else if (field.isAnnotationPresent(GEDNodeList.class))
                    {
                        for (Object node : (ArrayList<?>) value)
                        {
                            if (node instanceof GedComNode)
                                ((GedComNode) node).print(out, depth + 1);
                        }
                    }
                }
                
                printIgnoredFields(out, depth, field.getName());
            }
        }
        catch(IllegalAccessException e)
        {
            throw new RuntimeException(e);
        }
    }

    private void printIgnoredFields(PrintStream out, int depth, String nextField)
    {
        while (nextField != null)
        {
            String currentField = nextField;
            
            nextField = null;
            
            for (GEDNodeIgnore ignore : this.getClass().getAnnotationsByType(GEDNodeIgnore.class))
            {
                if (ignore.writeAfter().equals(currentField))
                {
                    this.writeIgnoredProperty(out, depth + 1, ignore.propertyToIgnore());
                    
                    nextField = ignore.propertyToIgnore();
                }
            }
        }
    }

    protected void writeIgnoredProperty(PrintStream out, int depth, String propertyToIgnore)
    {
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public GedComNode setGEDNode(String gedCode, String rest) throws GedComParseException
    {
        try
        {
            try
            {
                Field field = this.getClass().getDeclaredField(gedCode);
            
                field.setAccessible(true);
                
                if (field.isAnnotationPresent(GEDNodeAttribute.class))
                {
                    if (field.getType().equals(String.class))
                    {
                        field.set(this, rest);
                        
                        return GedComNode.Null;
                    }
                    else if (field.getType().isEnum())
                    {
                        if (rest == null)
                            return GedComNode.Null;

                        for (Object c : field.getType().getEnumConstants())
                        {
                            if (c.toString().equals(rest))
                            {
                                field.set(this, c);
                                
                                return GedComNode.Null;
                            }
                        }
                        
                        if (rest.equals("U"))
                            return GedComNode.Null;
                        
                        throw new GedComParseException("Could not map " + gedCode + " " + rest + " in " + this.getClass().getName());
                    }
                    else if (field.getType().isPrimitive())
                    {
                        if (rest == null)
                        {
                            field.set(this, 0);
                        }
                        else
                        {
                            String trimmedRest = rest.trim();
                            if (field.getType().equals(int.class))
                                field.set(this, Integer.parseInt(trimmedRest));
                            else if (field.getType().equals(long.class))
                                field.set(this, Long.parseLong(trimmedRest));
                            else if (field.getType().equals(double.class))
                                field.set(this, Double.parseDouble(trimmedRest));
                            else if (field.getType().equals(float.class))
                                field.set(this, Float.parseFloat(trimmedRest));
                            else if (field.getType().equals(boolean.class))
                                field.set(this, Boolean.parseBoolean(trimmedRest));
                            else if (field.getType().equals(char.class))
                                field.set(this, trimmedRest.length() > 0 ? trimmedRest.charAt(0) : '\0');
                            else if (field.getType().equals(short.class))
                                field.set(this, Short.parseShort(trimmedRest));
                            else if (field.getType().equals(byte.class))
                                field.set(this, Byte.parseByte(trimmedRest));
                        }
                        
                        return GedComNode.Null;
                    }
                    else
                    {
                        Class<? extends GedComNode> type = (Class<? extends GedComNode>) field.getType();
                        
                        GedComNode gedNode = newInstance(type, gedCode, rest, field);
                        
                        field.set(this, gedNode);
                    
                        return gedNode;
                    }
                }
                else if (field.isAnnotationPresent(GEDNodeReference.class))
                {
                    if (rest == null || !rest.startsWith("@") || !rest.endsWith("@"))
                    {
                        throw new GedComParseException("Invalid identifier: " + rest + ".");
                    }
                    
                    this.getContext().addReference(field, this, rest, false);
                    
                    return GedComNode.Null;
                }
                else if (field.isAnnotationPresent(GEDNodeReferenceList.class))
                {
                    if (rest == null || !rest.startsWith("@") || !rest.endsWith("@"))
                    {
                        throw new GedComParseException("Invalid identifier: " + rest + ".");
                    }
                    
                    this.getContext().addReference(field, this, rest, true);
                    
                    return GedComNode.Null;
                }
                else if (field.isAnnotationPresent(GEDNodeList.class))
                {
                    Class<? extends GedComNode> type = field.getAnnotation(GEDNodeList.class).value();
                    
                    GedComNode gedNode = newInstance(type, gedCode, rest, field);
                    
                    ArrayList list = ((ArrayList) field.get(this));
                    
                    if (list == null)
                        throw new GedComParseException("List was not initialized " + this.getClass().getSimpleName() + "." + gedCode + ".");
                    
                    list.add(gedNode);
                    
                    return gedNode;
                }
                else
                {
                    throw new GedComParseException("Could not map " + this.getClass().getSimpleName() + "." + gedCode);
                }
            }
            catch(NoSuchFieldException e)
            {
                for (GEDNodeIgnore ignore : this.getClass().getAnnotationsByType(GEDNodeIgnore.class))
                {
                    if (ignore.propertyToIgnore().equals(gedCode))
                        return GedComNode.IgnoreNode;
                }
                
                throw new GedComParseException("Attribute not found " + this.getClass().getSimpleName() + "." + gedCode);
            }
        }
        catch (InvocationTargetException e)
        {
            throw new GedComParseException(e.getCause());
        }
        catch (InstantiationException | IllegalAccessException
                | IllegalArgumentException
                | NoSuchMethodException | SecurityException e)
        {
            throw new GedComParseException(e);
        }
    }

    private GedComNode newInstance(Class<? extends GedComNode> type, String gedCode, String data,
            Field field) throws InstantiationException, IllegalAccessException,
            InvocationTargetException, NoSuchMethodException
    {
        if (data == null)
            return type.getConstructor(String.class, Document.class).newInstance(gedCode, this.getContext());
        else
            return type.getConstructor(String.class, Document.class, String.class).newInstance(gedCode, this.getContext(), data);
    }
    
    public static class LocalizedGedComNode
    {
        final public GedComNode node;
        final public int lineCount;
        final public String line;
        
        public LocalizedGedComNode(GedComNode node, int lineCount, String line)
        {
            this.node = node;
            this.lineCount = lineCount;
            this.line = line;
        }
    }

    private static Document readDocument(Document document, InputStream inputStream) throws IOException, LocalizedGedComParseException
    {
        LinkedList<LocalizedGedComNode> nodeStack = new LinkedList<>();
        
        nodeStack.add(new LocalizedGedComNode(document, 0, ""));
        
        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));

        br.mark(4);
        
        if ('\ufeff' != br.read())
            br.reset(); // not the BOM marker
        
        String originalLine;
        
        int lineCount = 0;
        
        while ((originalLine = br.readLine()) != null)
        {
            lineCount ++;
            
            try
            {
                String line = originalLine.trim();
                
                if (!line.isEmpty())
                {
                    int indexOfWhitespace0 = line.indexOf(' ');
                    
                    if (indexOfWhitespace0 == -1)
                        throw new GedComParseException("Error reading depth number: " + line);
                
                    int depth;
                    
                    try
                    {
                        depth = Integer.parseInt(line.substring(0, indexOfWhitespace0));
                    }
                    catch(NumberFormatException e)
                    {
                        throw new GedComParseException("Error reading depth number: " + line);
                    }
                    
                    int indexOfWhitespace1 = line.indexOf(' ', indexOfWhitespace0 + 1);
                    
                    String token;
                    
                    if (indexOfWhitespace1 == -1)
                        token = line.substring(indexOfWhitespace0 + 1);
                    else
                        token = line.substring(indexOfWhitespace0 + 1, indexOfWhitespace1);
                    
                    String id = null, gedCode = token;
                    
                    if (token.startsWith("_"))
                        continue;
                    
                    if (token.startsWith("@"))
                    {
                        id = token;
                        
                        int indexOfWhitespace2 = line.indexOf(' ', indexOfWhitespace1 + 1);

                        if (indexOfWhitespace2 == -1)
                            gedCode = line.substring(indexOfWhitespace1 + 1);
                        else
                            gedCode = line.substring(indexOfWhitespace1 + 1, indexOfWhitespace2);
                        
                        indexOfWhitespace1 = indexOfWhitespace2;
                    }
                    
                    String rest = null;
                    
                    if (indexOfWhitespace1 != -1)
                        rest = line.substring(indexOfWhitespace1 + 1);
                    
                    while (nodeStack.size() > depth + 1)
                        nodeStack.remove(depth + 1);
                    
                    GedComNode lastNode = nodeStack.getLast().node;
                    
                    GedComNode gedNode = null;
                    
                    if (lastNode == null || lastNode == GedComNode.Null || lastNode == IgnoreNode)
                        gedNode = IgnoreNode;
                    else
                        gedNode = lastNode.setGEDNode(gedCode, rest);

                    if (gedNode != IgnoreNode)
                    {
                        if ((id != null) != (gedNode != null && gedNode instanceof IdentifiedGedComNode))
                            throw new GedComParseException("Unexpected GedNode identifier: " + gedCode + " id=" + id);
                        
                        if (id != null)
                            document.addIdentifiedObject(id, (IdentifiedGedComNode) gedNode);
                    }
                        
                    nodeStack.addLast(new LocalizedGedComNode(gedNode, lineCount, line));
                }
            }
            catch(GedComParseException e)
            {
                nodeStack.removeFirst();
                
                Collections.reverse(nodeStack);
                
                throw new LocalizedGedComParseException(e, nodeStack, lineCount, originalLine);
            }
        }
        
        try
        {
            document.solveReferences();
        }
        catch (GedComParseException e)
        {
            throw new LocalizedGedComParseException(e, nodeStack, 0, "<unknown>");
        }

        document.removeEmptyFamilies();

        return document;
    }

    public static Document readDocument(File inputFile) throws IOException, LocalizedGedComParseException
    {
        Document document = new Document(inputFile);
        
        if (inputFile.getName().toLowerCase().endsWith(".zip"))
        {
            try (ZipInputStream zis = new ZipInputStream(new FileInputStream(inputFile)))
            {
                ZipEntry entry = null;
                
                while ((entry = zis.getNextEntry()) != null)
                {
                    if (DOCUMENT_GED.equals(entry.getName()))
                    {
                        return readDocument(document, zis);
                    }
                }
            }
            
            throw new IOException("Malformed zip file");
        }
        else if (inputFile.getName().toLowerCase().endsWith(".ged"))
        {
            try (InputStream inputStream = new FileInputStream(inputFile))
            {
                return readDocument(document, inputStream);
            }
        }
        else
        {
            throw new IOException("Unexpected file extension");
        }
    }
    
    public static void writeDocument(Document model, File outputFile) throws IOException
    {
        if (outputFile.getName().toLowerCase().endsWith(".zip"))
        {
            try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(outputFile)))
            {
                ZipEntry e = new ZipEntry(new ZipEntry(DOCUMENT_GED));
                
                zos.putNextEntry(e);
                
                model.print(new PrintStream(zos, true, "UTF-8"), -1); //$NON-NLS-1$
            }
        }
        else if (outputFile.getName().toLowerCase().endsWith(".ged"))
        {
            try (FileOutputStream fw = new FileOutputStream(outputFile))
            {
                model.print(new PrintStream(fw, true, "UTF-8"), -1); //$NON-NLS-1$
            }
        }
        else
        {
            throw new IOException("Unexpected file extension");
        }
    }
}
