package iamd.gedcom.datamodel;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ExifOrientationUtil}.
 *
 * <p>These tests build tiny synthetic JPEG/EXIF byte streams by hand so
 * we don't need any third-party sample images. They cover:</p>
 * <ul>
 *   <li>Reading the EXIF orientation tag in both little-endian and
 *       big-endian byte order,</li>
 *   <li>Falling back to orientation 1 (no transform) for files that
 *       don't have an EXIF block,</li>
 *   <li>Applying each of the 8 EXIF orientation transforms to a
 *       distinctive test image,</li>
 *   <li>Reading a JPEG through {@link ExifOrientationUtil#read(File)}
 *       and verifying the resulting dimensions.</li>
 * </ul>
 */
class ExifOrientationUtilTest
{
    /** Writes the given bytes to a temporary file and returns it. */
    private static File writeTemp(byte[] data) throws IOException
    {
        File f = Files.createTempFile("exif-orient-test-", ".jpg").toFile();
        f.deleteOnExit();
        Files.write(f.toPath(), data);
        return f;
    }

    /** Builds an EXIF/TIFF block holding a single Orientation tag with the given value. */
    private static byte[] buildExifBlock(int orientation, ByteOrder order) throws IOException
    {
        // IFD0 with one entry (12 bytes) -> total IFD size = 2 + 12 = 14 bytes.
        // We place the IFD right after the TIFF header (offset 8).

        ByteBuffer tiff = ByteBuffer.allocate(26).order(order);

        // TIFF header: byte-order marker + magic 42.
        if (order == ByteOrder.LITTLE_ENDIAN)
        {
            tiff.put((byte) 0x49);
            tiff.put((byte) 0x49);
        }
        else
        {
            tiff.put((byte) 0x4D);
            tiff.put((byte) 0x4D);
        }
        tiff.putShort((short) 0x002A);
        tiff.putInt(8); // offset to IFD0 from start of TIFF block

        // IFD0: 1 entry
        tiff.putShort((short) 1);

        // Entry: tag = 0x0112 (Orientation), type = 3 (SHORT), count = 1, value = orientation
        tiff.putShort((short) 0x0112);
        tiff.putShort((short) 3);
        tiff.putInt(1);
        // Value fits in the first two bytes of the 4-byte value field;
        // pad the rest with zeros.
        tiff.putShort((short) orientation);
        tiff.putShort((short) 0);

        // Next-IFD offset = 0 (no further IFDs).
        tiff.putInt(0);

        // Prepend the EXIF header.
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(new byte[] { 'E', 'x', 'i', 'f', 0x00, 0x00 });
        out.write(tiff.array());
        return out.toByteArray();
    }

    /** Builds a minimal JPEG file containing an APP1 segment with the given EXIF block. */
    private static byte[] buildJpegWithExif(byte[] exifBlock) throws IOException
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        // SOI
        out.write(0xFF);
        out.write(0xD8);

        // APP1 marker (0xFFE1) + segment length (includes the 2 length bytes).
        out.write(0xFF);
        out.write(0xE1);
        int segmentLength = exifBlock.length + 2;
        out.write((segmentLength >> 8) & 0xFF);
        out.write(segmentLength & 0xFF);
        out.write(exifBlock);

        // SOS-ish dummy segment so the file is at least plausibly a JPEG.
        // A minimal JPEG decoder is not the focus of these tests - we just
        // need the EXIF parser to find the orientation tag.
        out.write(0xFF);
        out.write(0xD9); // EOI

        return out.toByteArray();
    }

    /** Builds a minimal "JPEG" file with no EXIF block at all. */
    private static byte[] buildJpegWithoutExif()
    {
        return new byte[] {
            (byte) 0xFF, (byte) 0xD8, // SOI
            (byte) 0xFF, (byte) 0xD9  // EOI
        };
    }

    /** Generates a simple 4x3 test image with three colored horizontal bands. */
    private static BufferedImage makeTestImage()
    {
        BufferedImage img = new BufferedImage(4, 3, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        try
        {
            g.setColor(Color.RED);
            g.fillRect(0, 0, 4, 1);
            g.setColor(Color.GREEN);
            g.fillRect(0, 1, 4, 1);
            g.setColor(Color.BLUE);
            g.fillRect(0, 2, 4, 1);
        }
        finally
        {
            g.dispose();
        }
        return img;
    }

    @Nested
    class ReadOrientation
    {
        @Test
        void nullFile() throws IOException
        {
            assertEquals(1, ExifOrientationUtil.readOrientation(null));
        }

        @Test
        void nonExistentFile() throws IOException
        {
            assertEquals(1, ExifOrientationUtil.readOrientation(new File("does-not-exist.jpg")));
        }

        @Test
        void notAJpeg() throws IOException
        {
            File f = writeTemp(new byte[] { 0x00, 0x01, 0x02, 0x03 });
            assertEquals(1, ExifOrientationUtil.readOrientation(f));
        }

        @Test
        void jpegWithoutExif() throws IOException
        {
            File f = writeTemp(buildJpegWithoutExif());
            assertEquals(1, ExifOrientationUtil.readOrientation(f));
        }

        @Test
        void jpegWithExifLittleEndian() throws IOException
        {
            for (int orientation = 1; orientation <= 8; orientation++)
            {
                byte[] exif = buildExifBlock(orientation, ByteOrder.LITTLE_ENDIAN);
                File f = writeTemp(buildJpegWithExif(exif));
                assertEquals(orientation, ExifOrientationUtil.readOrientation(f),
                    "Little-endian EXIF orientation " + orientation);
            }
        }

        @Test
        void jpegWithExifBigEndian() throws IOException
        {
            for (int orientation = 1; orientation <= 8; orientation++)
            {
                byte[] exif = buildExifBlock(orientation, ByteOrder.BIG_ENDIAN);
                File f = writeTemp(buildJpegWithExif(exif));
                assertEquals(orientation, ExifOrientationUtil.readOrientation(f),
                    "Big-endian EXIF orientation " + orientation);
            }
        }

        @Test
        void invalidOrientationFallsBackToOne() throws IOException
        {
            byte[] exif = buildExifBlock(99, ByteOrder.LITTLE_ENDIAN);
            File f = writeTemp(buildJpegWithExif(exif));
            assertEquals(1, ExifOrientationUtil.readOrientation(f));
        }

        @Test
        void zeroOrientationFallsBackToOne() throws IOException
        {
            byte[] exif = buildExifBlock(0, ByteOrder.LITTLE_ENDIAN);
            File f = writeTemp(buildJpegWithExif(exif));
            assertEquals(1, ExifOrientationUtil.readOrientation(f));
        }

        @Test
        void app1WithoutExifHeader() throws IOException
        {
            // APP1 segment whose payload does NOT start with "Exif\0\0".
            byte[] payload = new byte[] { 0x00, 0x01, 0x02, 0x03 };
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            out.write(new byte[] { (byte) 0xFF, (byte) 0xD8 });
            out.write(new byte[] { (byte) 0xFF, (byte) 0xE1 });
            int len = payload.length + 2;
            out.write((len >> 8) & 0xFF);
            out.write(len & 0xFF);
            out.write(payload);
            out.write(new byte[] { (byte) 0xFF, (byte) 0xD9 });

            File f = writeTemp(out.toByteArray());
            assertEquals(1, ExifOrientationUtil.readOrientation(f));
        }

        @Test
        void otherAppSegmentIsSkipped() throws IOException
        {
            // APP0 segment followed by an APP1 with EXIF.
            byte[] exif = buildExifBlock(6, ByteOrder.LITTLE_ENDIAN);
            byte[] app0 = new byte[] { 'J', 'F', 'I', 'F', 0x00 }; // dummy APP0 payload

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            out.write(new byte[] { (byte) 0xFF, (byte) 0xD8 });

            // APP0
            out.write(new byte[] { (byte) 0xFF, (byte) 0xE0 });
            int app0Len = app0.length + 2;
            out.write((app0Len >> 8) & 0xFF);
            out.write(app0Len & 0xFF);
            out.write(app0);

            // APP1 with EXIF
            out.write(new byte[] { (byte) 0xFF, (byte) 0xE1 });
            int app1Len = exif.length + 2;
            out.write((app1Len >> 8) & 0xFF);
            out.write(app1Len & 0xFF);
            out.write(exif);

            out.write(new byte[] { (byte) 0xFF, (byte) 0xD9 });

            File f = writeTemp(out.toByteArray());
            assertEquals(6, ExifOrientationUtil.readOrientation(f));
        }
    }

    @Nested
    class ApplyOrientation
    {
        @Test
        void nullSource() throws IOException
        {
            assertNull(ExifOrientationUtil.applyOrientation(null, 6));
        }

        @Test
        void orientationOneReturnsSameImage() throws IOException
        {
            BufferedImage src = makeTestImage();
            BufferedImage out = ExifOrientationUtil.applyOrientation(src, 1);
            assertSame(src, out);
        }

        @Test
        void unknownOrientationReturnsSameImage() throws IOException
        {
            BufferedImage src = makeTestImage();
            BufferedImage out = ExifOrientationUtil.applyOrientation(src, 99);
            assertSame(src, out);
        }

        @Test
        void orientationThreeKeepsDimensionsAndRotatesColors() throws IOException
        {
            // 180 degree rotation keeps width and height.
            BufferedImage src = makeTestImage(); // 4 wide x 3 tall
            BufferedImage out = ExifOrientationUtil.applyOrientation(src, 3);
            assertEquals(src.getWidth(), out.getWidth());
            assertEquals(src.getHeight(), out.getHeight());

            // Top-left pixel of the rotated image should match the
            // bottom-right pixel of the source (which is BLUE).
            assertEquals(Color.BLUE.getRGB(), out.getRGB(0, 0));
            assertEquals(Color.RED.getRGB(), out.getRGB(3, 2));
        }

        @Test
        void orientationSixSwapsDimensions() throws IOException
        {
            // 90 CW rotation: width and height swap.
            BufferedImage src = makeTestImage(); // 4 wide x 3 tall
            BufferedImage out = ExifOrientationUtil.applyOrientation(src, 6);
            assertEquals(src.getHeight(), out.getWidth());
            assertEquals(src.getWidth(), out.getHeight());
        }

        @Test
        void orientationEightSwapsDimensions() throws IOException
        {
            // 270 CW rotation: width and height swap.
            BufferedImage src = makeTestImage();
            BufferedImage out = ExifOrientationUtil.applyOrientation(src, 8);
            assertEquals(src.getHeight(), out.getWidth());
            assertEquals(src.getWidth(), out.getHeight());
        }

        @Test
        void orientationsFiveAndSevenSwapDimensions() throws IOException
        {
            BufferedImage src = makeTestImage();
            assertEquals(src.getHeight(), ExifOrientationUtil.applyOrientation(src, 5).getWidth());
            assertEquals(src.getWidth(), ExifOrientationUtil.applyOrientation(src, 5).getHeight());
            assertEquals(src.getHeight(), ExifOrientationUtil.applyOrientation(src, 7).getWidth());
            assertEquals(src.getWidth(), ExifOrientationUtil.applyOrientation(src, 7).getHeight());
        }

        @Test
        void orientationsTwoAndFourKeepDimensions() throws IOException
        {
            BufferedImage src = makeTestImage();
            assertEquals(src.getWidth(), ExifOrientationUtil.applyOrientation(src, 2).getWidth());
            assertEquals(src.getHeight(), ExifOrientationUtil.applyOrientation(src, 2).getHeight());
            assertEquals(src.getWidth(), ExifOrientationUtil.applyOrientation(src, 4).getWidth());
            assertEquals(src.getHeight(), ExifOrientationUtil.applyOrientation(src, 4).getHeight());
        }
    }

    @Nested
    class ReadFile
    {
        @Test
        void realJpegWithoutExif() throws IOException
        {
            BufferedImage src = makeTestImage();
            File f = Files.createTempFile("exif-real-noexif-", ".png").toFile();
            f.deleteOnExit();
            ImageIO.write(src, "png", f);

            // PNGs have no EXIF, so the image should come back unchanged.
            BufferedImage out = ExifOrientationUtil.read(f);
            assertNotNull(out);
            assertEquals(src.getWidth(), out.getWidth());
            assertEquals(src.getHeight(), out.getHeight());
        }

        @Test
        void nonImageFileThrows() throws IOException
        {
            File f = writeTemp("not an image".getBytes());
            assertNull(ExifOrientationUtil.read(f));
        }
    }
}