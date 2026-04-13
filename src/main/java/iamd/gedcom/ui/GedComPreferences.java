package iamd.gedcom.ui;

import java.awt.Font;
import java.util.prefs.Preferences;

public class GedComPreferences
{
    final private Preferences prefs = Preferences.userNodeForPackage(iamd.gedcom.datamodel.Document.class);
    static final String LAST_INDIVIDUAL = "last_individual"; //$NON-NLS-1$
    static final String LAST_FILE_PREF_NAME = "last_file"; //$NON-NLS-1$
    static final String FONT_NAME = "font_name"; //$NON-NLS-1$
    static final String FONT_SIZE = "font_size"; //$NON-NLS-1$
    static final String FONT_STYLE = "font_weigth"; //$NON-NLS-1$
    
    public String getLastOpenedFileName()
    {
        return this.prefs.get(LAST_FILE_PREF_NAME, null);
    }

    public void putCurrentFileName(String absolutePath)
    {
        this.prefs.put(LAST_FILE_PREF_NAME, absolutePath);        
    }

    public String getLastIndividual()
    {
        return this.prefs.get(LAST_INDIVIDUAL, null);
    }

    public void putCurrentIndividual(String id)
    {
        this.prefs.put(LAST_INDIVIDUAL, id);
    }

    public void putCurrentFont(Font selectedFont)
    {
        this.prefs.put(FONT_NAME, selectedFont.getName());
        this.prefs.put(FONT_SIZE, String.valueOf(selectedFont.getSize()));
        this.prefs.put(FONT_STYLE, String.valueOf(selectedFont.getStyle()));
    }
    
    public Font getLastFont()
    {
        try
        {
            String family = this.prefs.get(FONT_NAME, null);
            int size = Integer.parseInt(this.prefs.get(FONT_SIZE, ""));
            int style = Integer.parseInt(this.prefs.get(FONT_STYLE, ""));
            
            return new Font(family, style, size);
        }
        catch(Throwable e)
        {
            return null;
        }
    }
    

}
