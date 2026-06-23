package iamd.gedcom.datamodel;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

import javax.imageio.ImageIO;

/**
 * Utility class that handles JPEG/EXIF orientation metadata and falls
 * back to robust loading for JPEGs that the JDK's built-in reader
 * cannot decode.
 *
 * <p>Many JPEG images produced by cameras and phones embed an EXIF
 * "Orientation" tag (TIFF tag 0x0112) that describes how the raw pixel
 * data should be rotated or mirrored to be displayed correctly. Java's
 * {@link ImageIO#read} does not apply that transform, so an image with
 * a non-default orientation tag would otherwise appear rotated or
 * flipped.</p>
 *
 * <p>This utility reads the EXIF orientation tag from a JPEG file and
 * exposes helpers to load the image already rotated. It only parses the
 * minimum amount of the JPEG/EXIF/TIFF structure required to locate the
 * orientation tag, so it has no external dependencies for that part.</p>
 *
 * <p>A small subset of JPEGs - typically those exported by older
 * scanning software or photo editors - carry an ICC color profile in
 * an APP2 segment whose number of components does not match the actual
 * image data. The JDK's stock JPEG reader throws an
 * {@code IIOException} ("Numbers of source Raster bands and source
 * color space components do not match") on those files. As a fallback,
 * {@link #read(File)} strips the APP2 (ICC_PROFILE) segment and retries
 * so the image can still be displayed - the colors will be interpreted
 * using Java's default sRGB profile instead of the embedded one.</p>
 *
 * <p>The orientation values follow the EXIF specification:</p>
 * <pre>
 *   1 = Top-left     (0 degrees) - identity
 *   2 = Top-right    (mirror horizontal)
 *   3 = Bottom-right (rotate 180 degrees)
 *   4 = Bottom-left  (mirror vertical)
 *   5 = Left-top     (transpose: mirror H + rotate 270 CW)
 *   6 = Right-top    (rotate 90 CW)
 *   7 = Right-bottom (transverse: mirror H + rotate 90 CW)
 *   8 = Left-bottom  (rotate 270 CW)
 * </pre>
 */
public final class ExifOrientationUtil
{
    /** EXIF / TIFF tag number for the Orientation tag. */
    private static final int TAG_ORIENTATION = 0x0112;

    /** JPEG APP2 marker (where ICC_PROFILE segments live). */
    private static final int APP2_MARKER = 0xE2;

    /** JPEG APP1 marker (where the EXIF block lives). */
    private static final int APP1_MARKER = 0xE1;

    /** JPEG SOI marker - the very first two bytes of any JPEG file. */
    private static final int SOI_MARKER = 0xFFD8;

    /** Magic bytes that identify the start of EXIF data inside APP1. */
    private static final byte[] EXIF_HEADER = { 'E', 'x', 'i', 'f', 0x00, 0x00 };

    /**
     * Substring of the exception message that the JDK's built-in JPEG
     * reader produces when an APP2 (ICC_PROFILE) segment is inconsistent
     * with the image's actual color space. Used to trigger the fallback
     * that strips the APP2 segment and retries.
     */
    private static final String ICC_MISMATCH_HINT =
        "source Raster bands and source color space components do not match";

    private ExifOrientationUtil()
    {
        // Utility class - no instances.
    }

    /**
     * Reads the EXIF orientation value from a JPEG file.
     *
     * <p>Returns {@code 1} (the EXIF default, "no transform") if the
     * file has no EXIF data, no orientation tag, or cannot be parsed.
     * That keeps callers safe to apply the result unconditionally.</p>
     *
     * @param file the JPEG file to inspect
     * @return the orientation tag value (1-8), or 1 if it cannot be read
     */
    public static int readOrientation(File file)
    {
        if (file == null || !file.isFile())
        {
            return 1;
        }

        try (RandomAccessFile raf = new RandomAccessFile(file, "r"))
        {
            // JPEG files start with the SOI marker 0xFFD8.
            if (raf.length() < 4)
            {
                return 1;
            }

            int soi = raf.readUnsignedShort();
            if (soi != SOI_MARKER)
            {
                // Not a JPEG file (or not at the expected offset).
                return 1;
            }

            // Walk JPEG segments until we find an APP1 segment holding
            // EXIF data, or we reach the image data.
            while (raf.getFilePointer() < raf.length())
            {
                if (raf.getFilePointer() + 4 > raf.length())
                {
                    return 1;
                }

                int marker = raf.readUnsignedShort();
                int segmentLength = raf.readUnsignedShort();

                // APPn markers are 0xFFE0 .. 0xFFEF; APP1 (EXIF) is 0xFFE1.
                if ((marker & 0xFFF0) != 0xFFE0)
                {
                    // Not an APP segment. Stand-alone markers without
                    // payload (like SOI, EOI, RSTn) have a length of 0
                    // and no body. For other markers we just skip the
                    // segment body and keep scanning.
                    if (segmentLength <= 2)
                    {
                        return 1;
                    }
                    raf.skipBytes(segmentLength - 2);
                    continue;
                }

                if (marker != ((0xFF << 8) | APP1_MARKER))
                {
                    // Some other APP segment - skip it.
                    if (segmentLength <= 2)
                    {
                        return 1;
                    }
                    raf.skipBytes(segmentLength - 2);
                    continue;
                }

                // We have an APP1 segment. The first six bytes are the
                // EXIF header ("Exif\0\0"). Read the whole APP1 payload
                // into memory and let readExifOrientation parse it.
                int payloadLength = segmentLength - 2;
                if (payloadLength < EXIF_HEADER.length)
                {
                    return 1;
                }
                byte[] payload = new byte[payloadLength];
                raf.readFully(payload);

                if (!startsWith(payload, EXIF_HEADER))
                {
                    return 1;
                }

                // The TIFF header starts right after the EXIF header.
                int orientation = readExifOrientation(
                    payload, EXIF_HEADER.length, payloadLength - EXIF_HEADER.length);
                return clampOrientation(orientation);
            }
        }
        catch (IOException e)
        {
            // Any I/O problem means we treat the file as having no
            // orientation metadata.
            return 1;
        }

        return 1;
    }

    /**
     * Reads the orientation tag from an EXIF / TIFF block.
     *
     * @param data     the raw bytes of the TIFF block (after the EXIF header)
     * @param offset   offset inside {@code data} where the TIFF block starts
     * @param length   length of the TIFF block
     * @return the orientation tag value (1-8), or 1 if not found / invalid
     */
    private static int readExifOrientation(byte[] data, int offset, int length)
    {
        if (length < 8)
        {
            return 1;
        }

        // The first two bytes of the TIFF header are the byte order:
        // "II" (0x4949) = little-endian, "MM" (0x4D4D) = big-endian.
        ByteOrder order;
        if (data[offset] == (byte) 0x49 && data[offset + 1] == (byte) 0x49)
        {
            order = ByteOrder.LITTLE_ENDIAN;
        }
        else if (data[offset] == (byte) 0x4D && data[offset + 1] == (byte) 0x4D)
        {
            order = ByteOrder.BIG_ENDIAN;
        }
        else
        {
            return 1;
        }

        ByteBuffer buf = ByteBuffer.wrap(
            Arrays.copyOfRange(data, offset, offset + length)).order(order);

        // Skip the byte-order marker (2 bytes) and the magic 0x002A (2 bytes).
        buf.position(4);

        // Next 4 bytes are the offset (from the start of the TIFF header)
        // to the first Image File Directory (IFD0).
        if (buf.remaining() < 4)
        {
            return 1;
        }
        int ifd0Offset = buf.getInt();

        return readOrientationFromIfd(buf, ifd0Offset);
    }

    /**
     * Walks an IFD looking for the orientation tag.
     *
     * @param buf        byte buffer positioned over the TIFF block
     * @param ifdOffset  offset of the IFD from the start of the TIFF block
     * @return the orientation tag value (1-8), or 1 if not present
     */
    private static int readOrientationFromIfd(ByteBuffer buf, int ifdOffset)
    {
        if (ifdOffset < 0 || ifdOffset >= buf.capacity())
        {
            return 1;
        }

        // Reconfigure byte order for the original buffer view.
        ByteOrder order = buf.order();
        ByteBuffer ifdBuf = (ByteBuffer) buf.duplicate().position(0).order(order);

        if (ifdOffset + 2 > ifdBuf.capacity())
        {
            return 1;
        }

        ifdBuf.position(ifdOffset);
        int entryCount = ifdBuf.getShort() & 0xFFFF;

        for (int i = 0; i < entryCount; i++)
        {
            int entryPos = ifdOffset + 2 + (i * 12);
            if (entryPos + 12 > ifdBuf.capacity())
            {
                return 1;
            }

            ifdBuf.position(entryPos);
            int tag = ifdBuf.getShort() & 0xFFFF;
            // int type = ifdBuf.getShort() & 0xFFFF; // not needed
            // int count = ifdBuf.getInt();           // not needed for SHORT
            ifdBuf.getShort();
            ifdBuf.getInt();

            if (tag == TAG_ORIENTATION)
            {
                // For type SHORT (3) with count 1, the value is stored
                // in the first two bytes of the 4-byte value/offset
                // field. We re-read it to be safe.
                ifdBuf.position(entryPos + 8);
                int value = ifdBuf.getShort() & 0xFFFF;
                return clampOrientation(value);
            }
        }

        return 1;
    }

    /**
     * Clamps a raw orientation value to the valid EXIF range. Values
     * outside the [1, 8] range fall back to 1 (the default, no-op).
     */
    private static int clampOrientation(int value)
    {
        return (value >= 1 && value <= 8) ? value : 1;
    }

    private static boolean startsWith(byte[] data, byte[] prefix)
    {
        if (data.length < prefix.length)
        {
            return false;
        }
        for (int i = 0; i < prefix.length; i++)
        {
            if (data[i] != prefix[i])
            {
                return false;
            }
        }
        return true;
    }

    /**
     * Loads a JPEG file and rotates/mirrors the resulting image according
     * to its EXIF orientation tag.
     *
     * <p>This is equivalent to {@link ImageIO#read(File)} but with the
     * EXIF orientation transform already applied.</p>
     *
     * <p>If the JDK's JPEG reader throws the well-known
     * "raster bands / color space components" mismatch error caused by
     * an inconsistent ICC profile, this method falls back to a copy of
     * the file with its APP2 (ICC_PROFILE) segments removed and tries
     * again. The image is then decoded using the default sRGB color
     * space. Any other I/O or decoding failure is propagated to the
     * caller.</p>
     *
     * @param file the image file to load
     * @return the loaded and correctly oriented image, or {@code null}
     *         if the file cannot be read as an image
     * @throws IOException if the underlying image read fails
     */
    public static BufferedImage read(File file) throws IOException
    {
        if (file == null)
        {
            return null;
        }

        BufferedImage image;
        try
        {
            image = ImageIO.read(file);
        }
        catch (javax.imageio.IIOException primary)
        {
            // Only retry for the ICC-profile mismatch error; rethrow
            // anything else so callers can see real problems.
            if (!isIccProfileMismatch(primary))
            {
                throw primary;
            }

            byte[] sanitized = stripApp2Segments(file);
            if (sanitized == null)
            {
                throw primary;
            }
            image = ImageIO.read(new ByteArrayInputStream(sanitized));
        }
        if (image == null)
        {
            return null;
        }

        int orientation = readOrientation(file);
        if (orientation == 1)
        {
            // Default orientation - no transform needed.
            return image;
        }

        return applyOrientation(image, orientation);
    }

    /**
     * Returns {@code true} when the given exception is the JDK's stock
     * "ICC profile mismatch" error that this class knows how to recover
     * from. Anything else is considered unrecoverable.
     */
    private static boolean isIccProfileMismatch(Throwable t)
    {
        while (t != null)
        {
            String msg = t.getMessage();
            if (msg != null && msg.contains(ICC_MISMATCH_HINT))
            {
                return true;
            }
            t = t.getCause();
        }
        return false;
    }

    /**
     * Reads a JPEG file into a byte array, removes every APP2
     * (ICC_PROFILE) segment it finds before the SOS marker, and returns
     * the resulting bytes. Everything from the SOS marker to the end of
     * the file (compressed image data and EOI) is copied verbatim.
     *
     * <p>Returns {@code null} if the file cannot be parsed as a JPEG
     * with the expected structure.</p>
     */
    private static byte[] stripApp2Segments(File file)
    {
        try (RandomAccessFile raf = new RandomAccessFile(file, "r"))
        {
            if (raf.length() < 4)
            {
                return null;
            }

            int soi = raf.readUnsignedShort();
            if (soi != SOI_MARKER)
            {
                return null;
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream((int) raf.length());
            // SOI
            out.write(0xFF);
            out.write(0xD8);

            boolean done = false;
            while (!done && raf.getFilePointer() + 4 <= raf.length())
            {
                int marker = raf.readUnsignedShort();
                int segmentLength = raf.readUnsignedShort();

                // APPn markers run from 0xFFE0 to 0xFFEF. APP2 (0xFFE2)
                // carries ICC profiles - skip those. Keep everything else.
                if ((marker & 0xFFF0) == 0xFFE0
                    && marker != ((0xFF << 8) | APP2_MARKER))
                {
                    // Copy APP0/1/3..15 segment.
                    writeSegment(out, raf, marker, segmentLength);
                    continue;
                }
                if (marker == ((0xFF << 8) | APP2_MARKER))
                {
                    // Drop APP2 payload and continue scanning.
                    if (segmentLength > 2)
                    {
                        raf.skipBytes(segmentLength - 2);
                    }
                    continue;
                }

                // For markers that start the compressed scan (SOS, SOF,
                // DHT, DQT, DRI, COM, ...) and everything after them,
                // copy the rest of the file verbatim and stop.
                writeSegmentAndRest(out, raf, marker, segmentLength);
                done = true;
            }

            byte[] bytes = out.toByteArray();
            // Sanity: we still need a SOI + at least a couple of bytes.
            return bytes.length > 4 ? bytes : null;
        }
        catch (IOException e)
        {
            return null;
        }
    }

    /**
     * Copies a JPEG segment (marker + length + payload) to the given
     * output stream.
     */
    private static void writeSegment(ByteArrayOutputStream out,
        RandomAccessFile raf, int marker, int segmentLength) throws IOException
    {
        out.write((marker >> 8) & 0xFF);
        out.write(marker & 0xFF);
        out.write((segmentLength >> 8) & 0xFF);
        out.write(segmentLength & 0xFF);

        int payload = Math.max(0, segmentLength - 2);
        copyBytes(raf, out, payload);
    }

    /**
     * Copies a JPEG segment plus the rest of the file (typically the
     * compressed image data starting at SOS) verbatim to the output
     * stream.
     */
    private static void writeSegmentAndRest(ByteArrayOutputStream out,
        RandomAccessFile raf, int marker, int segmentLength) throws IOException
    {
        // Write the current segment.
        writeSegment(out, raf, marker, segmentLength);
        // Then dump everything else (compressed scan + EOI) verbatim.
        byte[] rest = new byte[(int) (raf.length() - raf.getFilePointer())];
        if (rest.length > 0)
        {
            raf.readFully(rest);
            out.write(rest, 0, rest.length);
        }
    }

    private static void copyBytes(RandomAccessFile raf, ByteArrayOutputStream out,
        int count) throws IOException
    {
        if (count <= 0)
        {
            return;
        }
        byte[] buf = new byte[Math.min(count, 8192)];
        int remaining = count;
        while (remaining > 0)
        {
            int chunk = Math.min(remaining, buf.length);
            raf.readFully(buf, 0, chunk);
            out.write(buf, 0, chunk);
            remaining -= chunk;
        }
    }

    /**
     * Returns a new image rotated/mirrored according to the given EXIF
     * orientation value (1-8). A value of 1 returns the original image
     * unchanged.
     *
     * @param src         the source image
     * @param orientation the EXIF orientation value (1-8)
     * @return the transformed image (or {@code src} when orientation is 1)
     */
    public static BufferedImage applyOrientation(BufferedImage src, int orientation)
    {
        if (src == null || orientation <= 1 || orientation > 8)
        {
            return src;
        }

        int srcW = src.getWidth();
        int srcH = src.getHeight();
        int outW = (orientation >= 5 && orientation <= 8) ? srcH : srcW;
        int outH = (orientation >= 5 && orientation <= 8) ? srcW : srcH;

        int type = src.getType() == 0 ? BufferedImage.TYPE_INT_ARGB : src.getType();
        BufferedImage out = new BufferedImage(outW, outH, type);
        Graphics2D g2 = out.createGraphics();
        try
        {
            AffineTransform transform = transformFor(orientation, srcW, srcH, outW, outH);
            if (transform != null)
            {
                g2.setTransform(transform);
                g2.drawImage(src, 0, 0, null);
            }
            else
            {
                g2.drawImage(src, 0, 0, null);
            }
        }
        finally
        {
            g2.dispose();
        }
        return out;
    }

    /**
     * Builds the {@link AffineTransform} that maps a source image with
     * the given EXIF orientation into the visually correct orientation.
     *
     * <p>The transform is built so that the geometric center of the
     * source extent maps to the geometric center of the destination
     * extent. Java's {@code AffineTransform} operations are applied in
     * reverse order of how they are written, so we list them here in the
     * order the resulting transform actually applies them to a point:</p>
     * <ol>
     *   <li>Translate the source so its center is at the origin.</li>
     *   <li>Apply the rotation / mirror for this orientation.</li>
     *   <li>Translate so the origin lands at the destination center.</li>
     * </ol>
     */
    private static AffineTransform transformFor(int orientation,
        int srcW, int srcH, int outW, int outH)
    {
        AffineTransform tx = new AffineTransform();

        // Step 3 (applied last): origin -> destination center.
        tx.translate(outW / 2.0, outH / 2.0);

        // Step 2: rotate / mirror around the origin.
        switch (orientation)
        {
            case 2:
                // Mirror horizontally (around the vertical center line).
                tx.scale(-1.0, 1.0);
                break;
            case 3:
                // Rotate 180 degrees.
                tx.rotate(Math.PI);
                break;
            case 4:
                // Mirror vertically (around the horizontal center line).
                tx.scale(1.0, -1.0);
                break;
            case 5:
                // Transpose: rotate 90 CW then mirror vertical.
                tx.rotate(Math.PI / 2.0);
                tx.scale(1.0, -1.0);
                break;
            case 6:
                // Rotate 90 CW.
                tx.rotate(Math.PI / 2.0);
                break;
            case 7:
                // Transverse: rotate 270 CW then mirror vertical.
                tx.rotate(-Math.PI / 2.0);
                tx.scale(1.0, -1.0);
                break;
            case 8:
                // Rotate 270 CW (= 90 CCW).
                tx.rotate(-Math.PI / 2.0);
                break;
            default:
                return null;
        }

        // Step 1 (applied first): source center -> origin.
        tx.translate(-srcW / 2.0, -srcH / 2.0);

        return tx;
    }

    /**
     * Convenience overload for callers that already have the image data
     * in an input stream. The stream is read fully and then handed to
     * the {@link File}-based overload via a temporary file.
     *
     * @param input the image data
     * @return the loaded and correctly oriented image, or {@code null}
     *         if the data cannot be read as an image
     * @throws IOException if reading or decoding fails
     */
    public static BufferedImage read(InputStream input) throws IOException
    {
        if (input == null)
        {
            return null;
        }
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        int n;
        while ((n = input.read(chunk)) > 0)
        {
            buf.write(chunk, 0, n);
        }
        return read(buf.toByteArray());
    }

    /**
     * Convenience overload for callers that already have the image data
     * in a byte array. Internally writes the bytes to a temporary file
     * so the {@link File}-based path can apply both the orientation
     * transform and the ICC-fallback.
     *
     * @param data the image bytes
     * @return the loaded and correctly oriented image, or {@code null}
     *         if the data cannot be read as an image
     * @throws IOException if reading or decoding fails
     */
    public static BufferedImage read(byte[] data) throws IOException
    {
        if (data == null || data.length == 0)
        {
            return null;
        }
        File tmp = File.createTempFile("exif-orient-", ".bin");
        tmp.deleteOnExit();
        java.nio.file.Files.write(tmp.toPath(), data);
        try
        {
            return read(tmp);
        }
        finally
        {
            // Best-effort cleanup; deleteOnExit covers the worst case.
            tmp.delete();
        }
    }
}