package iamd.gedcom.rsrc;

import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

public class Resources
{
    public static final ImageIcon LavandaIcon, LavandaIconMini;

    public static final ImageIcon PictureIcon, AudioIcon, VideoIcon, DocumentIcon, MediaIcon;

    public static final ImageIcon ExploreIcon, CropImageIcon, OpenFileIcon;

    static
    {
        ImageIcon lavandaIcon0 = null, lavandaIconMini0 = null;

        ImageIcon pictureIcon0 = null, audioIcon0 = null, videoIcon0 = null, documentIcon0 = null, mediaIcon0 = null;

        ImageIcon exploreIcon0 = null, cropImageIcon0 = null, openFileIcon0 = null;

        try
        {
            lavandaIcon0 = new ImageIcon(ImageIO.read(Resources.class.getResourceAsStream("Lavanda.png"))); //$NON-NLS-1$
            
            lavandaIconMini0 = new ImageIcon(ImageIO.read(Resources.class.getResourceAsStream("LavandaMini.png"))); //$NON-NLS-1$

            pictureIcon0 = new ImageIcon(ImageIO.read(Resources.class.getResourceAsStream("picture_icon.png"))); //$NON-NLS-1$
            
            audioIcon0 = new ImageIcon(ImageIO.read(Resources.class.getResourceAsStream("audio_icon.png"))); //$NON-NLS-1$
            
            videoIcon0 = new ImageIcon(ImageIO.read(Resources.class.getResourceAsStream("video_icon.png"))); //$NON-NLS-1$
            
            documentIcon0 = new ImageIcon(ImageIO.read(Resources.class.getResourceAsStream("document_icon.png"))); //$NON-NLS-1$
            
            mediaIcon0 = new ImageIcon(ImageIO.read(Resources.class.getResourceAsStream("media_icon.png"))); //$NON-NLS-1$

            exploreIcon0 = new ImageIcon(ImageIO.read(Resources.class.getResourceAsStream("explore.png"))); //$NON-NLS-1$

            cropImageIcon0 = new ImageIcon(ImageIO.read(Resources.class.getResourceAsStream("crop.png"))); //$NON-NLS-1$

            openFileIcon0 = new ImageIcon(ImageIO.read(Resources.class.getResourceAsStream("openfile.png"))); //$NON-NLS-1$
        }
        catch(IOException | IllegalArgumentException e)
        {
            throw new AssertionError(e);
        }
        finally
        {
            LavandaIcon = lavandaIcon0;
            
            LavandaIconMini = lavandaIconMini0;

            PictureIcon = pictureIcon0;

            AudioIcon = audioIcon0;

            VideoIcon = videoIcon0;

            DocumentIcon = documentIcon0;
        
            MediaIcon = mediaIcon0;

            ExploreIcon = exploreIcon0;

            CropImageIcon = cropImageIcon0;

            OpenFileIcon = openFileIcon0;
        }
    }

}
