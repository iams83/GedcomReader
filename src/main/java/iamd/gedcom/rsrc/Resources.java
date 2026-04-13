package iamd.gedcom.rsrc;

import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

public class Resources
{
    public static final ImageIcon LavandaIcon, LavandaIconMini;

    static
    {
        ImageIcon lavandaIcon0 = null, lavandaIconMini0 = null;
        
        try
        {
            lavandaIcon0 = new ImageIcon(ImageIO.read(Resources.class.getResourceAsStream("Lavanda.png"))); //$NON-NLS-1$
            
            lavandaIconMini0 = new ImageIcon(ImageIO.read(Resources.class.getResourceAsStream("LavandaMini.png"))); //$NON-NLS-1$
        }
        catch(IOException | IllegalArgumentException e)
        {
            throw new AssertionError(e);
        }
        finally
        {
            LavandaIcon = lavandaIcon0;
            
            LavandaIconMini = lavandaIconMini0;
        }
    }

}
