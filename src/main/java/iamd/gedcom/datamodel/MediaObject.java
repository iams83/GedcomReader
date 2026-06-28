package iamd.gedcom.datamodel;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

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

        /**
         * Return a sorted array of every file extension that
         * {@link #getMediaTypeForExtension(String)} can classify. Useful for
         * configuring file pickers that should only offer known media types.
         */
        public static String[] collectSupportedExtensions()
        {
            String[] extensions = EXTENSION_TO_TYPE.keySet().toArray(new String[0]);
            java.util.Arrays.sort(extensions);
            return extensions;
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
    
    @GEDNodeAttribute
    public LongText NOTE;

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

    public void remove()
    {
        this.getDocument().removeMediaObject(this);
    }

    public File getMediaFile()
    {
        if (this.FILE == null)
        {
            return null;
        }
        
        File mediaFile = new File(this.FILE);
        if (mediaFile.isAbsolute())
        {
            return mediaFile;
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
        
        return new File(gedcomDir, this.FILE);
    }

    public ImageIcon getIconType()
    {
        if (this.TYPE != null)
        {
            return this.TYPE.getIcon();
        }
        else
        {
            return Resources.MediaIcon;
        }
    }
    
    public BufferedImage getImage()
    {
        File mediaFile = this.getMediaFile();
        if (mediaFile == null || !mediaFile.exists())
        {
            return null;
        }

        try
        {
            // Use ExifOrientationUtil.read so the image is rotated /
            // mirrored according to its EXIF orientation tag. Many
            // photos taken on phones and cameras have a non-default
            // orientation tag and would otherwise appear rotated in
            // the application.
            return ExifOrientationUtil.read(mediaFile);
        }
        catch (IOException e)
        {
            System.err.println("Failed to load image from file: " + mediaFile.getAbsolutePath());
            e.printStackTrace(System.err);
            return null;
        }
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
        if (this.TITL != null)
            return this.TITL;

        String relativePath = this.getRelativeFilePath();
        if (relativePath != null)
            return relativePath;

        return "Media Object";
    }

    /**
     * Resolves a FILE-style path (either absolute or relative to the Gedcom
     * document directory) into an absolute {@link File}. Returns {@code null}
     * if {@code filePath} is {@code null}.
     */
    public File resolveFile(String filePath)
    {
        if (filePath == null)
        {
            return null;
        }

        File file = new File(filePath);
        if (file.isAbsolute())
        {
            return file;
        }

        File gedcomFile = this.getDocument().getFile();
        if (gedcomFile == null)
        {
            return new File(filePath);
        }

        File gedcomDir = gedcomFile.getParentFile();
        if (gedcomDir == null)
        {
            return new File(filePath);
        }

        return new File(gedcomDir, filePath);
    }

    /**
     * Attempts to rename the actual file referenced by this media object from
     * {@code oldPath} to {@code newPath}. Both paths may be relative (in
     * which case they are resolved against the Gedcom document directory) or
     * absolute.
     *
     * <p>The {@link #FILE} attribute of this object is <strong>not</strong>
     * modified by this method. It is the caller's responsibility to update
     * the attribute on success and to keep the previous value on failure.</p>
     *
     * @param oldPath The previous file path (the one currently stored as
     *                {@link #FILE}). May be {@code null} only when there is
     *                no previous file.
     * @param newPath The new file path (relative or absolute).
     * @return {@code null} when the rename succeeded (or when both paths
     *         resolve to the same file), or a human-readable error message
     *         describing why the rename failed.
     */
    public String renameMediaFile(String oldPath, String newPath)
    {
        if (oldPath == null)
        {
            return "The current file path is not set.";
        }

        File oldFile = this.resolveFile(oldPath);
        File newFile = this.resolveFile(newPath);

        if (oldFile == null || newFile == null)
        {
            return "The new file path is invalid.";
        }

        // No-op: both paths point to the same file on disk.
        try
        {
            if (oldFile.getCanonicalFile().equals(newFile.getCanonicalFile()))
            {
                return null;
            }
        }
        catch (IOException e)
        {
            // If canonical paths cannot be resolved, fall back to a plain
            // comparison; the rename may still be attempted.
            if (oldFile.equals(newFile))
            {
                return null;
            }
        }

        if (!oldFile.exists())
        {
            return "Source file does not exist: " + oldFile.getAbsolutePath();
        }

        if (newFile.exists())
        {
            return "Destination file already exists: " + newFile.getAbsolutePath();
        }

        // Make sure the parent directory of the destination exists so the
        // rename has a chance to succeed when the user is moving the file
        // to a new folder.
        File newParent = newFile.getParentFile();
        if (newParent != null && !newParent.exists())
        {
            if (!newParent.mkdirs())
            {
                return "Could not create destination directory: " + newParent.getAbsolutePath();
            }
        }

        if (!oldFile.renameTo(newFile))
        {
            return "Failed to rename file from \"" + oldFile.getAbsolutePath()
                    + "\" to \"" + newFile.getAbsolutePath() + "\".";
        }

        return null;
    }
}
