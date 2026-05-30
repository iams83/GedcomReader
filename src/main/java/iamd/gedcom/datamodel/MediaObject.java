package iamd.gedcom.datamodel;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

import java.awt.image.BufferedImage;

import iamd.gedcom.format.IdentifiedGedComNode;
import iamd.gedcom.rsrc.Resources;

public class MediaObject extends IdentifiedGedComNode
{
    public enum MediaType
    {
        Picture, Audio, Video, Document;

        // File extension to MediaType mapping
        private static final Map<String, MediaType> EXTENSION_TO_TYPE = new HashMap<>();
        static
        {
            // Pictures
            EXTENSION_TO_TYPE.put("jpg", MediaType.Picture);
            EXTENSION_TO_TYPE.put("jpeg", MediaType.Picture);
            EXTENSION_TO_TYPE.put("png", MediaType.Picture);
            EXTENSION_TO_TYPE.put("gif", MediaType.Picture);
            EXTENSION_TO_TYPE.put("bmp", MediaType.Picture);
            EXTENSION_TO_TYPE.put("webp", MediaType.Picture);
            EXTENSION_TO_TYPE.put("svg", MediaType.Picture);
            EXTENSION_TO_TYPE.put("tiff", MediaType.Picture);
            EXTENSION_TO_TYPE.put("tif", MediaType.Picture);
            
            // Audio
            EXTENSION_TO_TYPE.put("mp3", MediaType.Audio);
            EXTENSION_TO_TYPE.put("wav", MediaType.Audio);
            EXTENSION_TO_TYPE.put("ogg", MediaType.Audio);
            EXTENSION_TO_TYPE.put("flac", MediaType.Audio);
            EXTENSION_TO_TYPE.put("aac", MediaType.Audio);
            EXTENSION_TO_TYPE.put("m4a", MediaType.Audio);
            EXTENSION_TO_TYPE.put("wma", MediaType.Audio);
            
            // Video
            EXTENSION_TO_TYPE.put("mp4", MediaType.Video);
            EXTENSION_TO_TYPE.put("avi", MediaType.Video);
            EXTENSION_TO_TYPE.put("mkv", MediaType.Video);
            EXTENSION_TO_TYPE.put("mov", MediaType.Video);
            EXTENSION_TO_TYPE.put("wmv", MediaType.Video);
            EXTENSION_TO_TYPE.put("flv", MediaType.Video);
            EXTENSION_TO_TYPE.put("webm", MediaType.Video);
            EXTENSION_TO_TYPE.put("mpeg", MediaType.Video);
            EXTENSION_TO_TYPE.put("mpg", MediaType.Video);
            
            // Documents
            EXTENSION_TO_TYPE.put("pdf", MediaType.Document);
            EXTENSION_TO_TYPE.put("doc", MediaType.Document);
            EXTENSION_TO_TYPE.put("docx", MediaType.Document);
            EXTENSION_TO_TYPE.put("txt", MediaType.Document);
            EXTENSION_TO_TYPE.put("rtf", MediaType.Document);
            EXTENSION_TO_TYPE.put("odt", MediaType.Document);
            EXTENSION_TO_TYPE.put("xls", MediaType.Document);
            EXTENSION_TO_TYPE.put("xlsx", MediaType.Document);
        }
        
        public static MediaType getMediaTypeForExtension(String extension)
        {
            return EXTENSION_TO_TYPE.get(extension);
        }

        public ImageIcon getIcon()
        {
            switch (this)
            {
                case Picture: return Resources.PictureIcon;
                case Audio: return Resources.AudioIcon;
                case Video: return Resources.VideoIcon;
                case Document: return Resources.DocumentIcon;
                default: return Resources.MediaIcon;
            }
        }
    }
    
    @GEDNodeAttribute
    public String FILE, FORM;
    
    @GEDNodeAttribute
    public MediaType TYPE;
    
    @GEDNodeAttribute
    public String TITL;

    public MediaObject(String gedCode, Document context)
    {
        super(gedCode, context);
    }

    public MediaObject(Document document)
    {
        this("OBJE", document);
    }

    @Override
    public String createIdentifier()
    {
        return "MEDIA" + hashCode();
    }

    public File getMediaFile()
    {
        if (this.FILE == null)
        {
            return null;
        }
        
        File gedcomFile = this.getDocument().getFile();
        if (gedcomFile == null)
        {
            return new File(this.FILE);
        }
        
        File gedcomDir = gedcomFile.getParentFile();
        if (gedcomDir == null)
        {
            return new File(this.FILE);
        }
        
        File mediaFile = new File(this.FILE);
        if (mediaFile.isAbsolute())
        {
            return mediaFile;
        }
        
        return new File(gedcomDir, this.FILE);
    }
    
    public BufferedImage getImage()
    {
        File mediaFile = this.getMediaFile();
        if (mediaFile == null || !mediaFile.exists())
        {
            return null;
        }
        
        BufferedImage originalImage;
        try
        {
            originalImage = ImageIO.read(mediaFile);
        }
        catch (IOException e)
        {
            return null;
        }
        
        if (originalImage == null)
        {
            return null;
        }
        return originalImage;
    }

    public String getRelativeFilePath()
    {
        if (this.FILE == null)
        {
            return null;
        }
        
        File gedcomFile = this.getDocument().getFile();
        if (gedcomFile == null)
        {
            return this.FILE;
        }
        
        File gedcomDir = gedcomFile.getParentFile();
        if (gedcomDir == null)
        {
            return this.FILE;
        }
        
        try
        {
            File mediaFile = new File(this.FILE);
            
            // If the FILE path is already absolute, check if it's under gedcomDir
            if (mediaFile.isAbsolute())
            {
                String mediaCanonical = mediaFile.getCanonicalPath();
                String gedcomCanonical = gedcomDir.getCanonicalPath();
                
                if (mediaCanonical.startsWith(gedcomCanonical + File.separator))
                {
                    return mediaCanonical.substring(gedcomCanonical.length() + File.separator.length());
                }
                return this.FILE;
            }
            
            // For relative paths, compute the actual relative path
            File absoluteMediaFile = new File(gedcomDir, this.FILE).getCanonicalFile();
            String mediaCanonical = absoluteMediaFile.getPath();
            String gedcomCanonical = gedcomDir.getCanonicalPath();
            
            if (mediaCanonical.startsWith(gedcomCanonical + File.separator))
            {
                return mediaCanonical.substring(gedcomCanonical.length() + File.separator.length());
            }
            else
            {
                // Media file is outside gedcom directory, return as-is
                return this.FILE;
            }
        }
        catch (Exception e)
        {
            return this.FILE;
        }
    }
    
    public String getDisplayLabel()
    {
        String result = "";
        
        if (this.TITL != null)
        {
            result += this.TITL;
        }
        
        String relativePath = this.getRelativeFilePath();
        if (relativePath != null)
        {
            if (result.length() > 0)
            {
                result += " - ";
            }
            
            result += relativePath;
        }
        
        if (result.isEmpty())
        {
            result = "Media Object";
        }
        
        return result;
    }
}
