package iamd.gedcom.datamodel;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the APP2 (ICC_PROFILE) stripping fallback in
 * {@link ExifOrientationUtil}. The JDK's stock JPEG reader refuses to
 * decode JPEGs whose embedded ICC profile declares a different number
 * of components than the actual image data; this test verifies that
 * {@link ExifOrientationUtil#read(File)} recovers from that error.
 */
class ExifOrientationUtilApp2Test
{
    /** Builds a minimal but valid JPEG (using the JDK encoder) with the given dimensions. */
    private static byte[] buildValidJpeg(int width, int height) throws IOException
    {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        // Fill with a non-uniform pattern so it's not trivially compressible.
        for (int y = 0; y < height; y++)
        {
            for (int x = 0; x < width; x++)
            {
                img.setRGB(x, y, ((x * 31 + y * 17) & 0xFFFFFF) | 0xFF000000);
            }
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "jpg", out);
        return out.toByteArray();
    }

    /** Builds a fake ICC_PROFILE segment whose header declares the requested component count. */
    private static byte[] buildFakeIccSegment(int declaredComponents) throws IOException
    {
        // APP2 payload: 'ICC_PROFILE\0' (12 bytes) + chunk index (1) +
        // chunk count (1) + profile payload.
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write('I'); out.write('C'); out.write('C'); out.write('_');
        out.write('P'); out.write('R'); out.write('O'); out.write('F');
        out.write('I'); out.write('L'); out.write('E'); out.write(0x00);
        out.write(1);   // chunk index
        out.write(1);   // chunk count
        // 128-byte ICC header: bytes 16-19 hold the number of components
        // as a 4-byte big-endian int.
        byte[] profile = new byte[128];
        profile[16] = 0;
        profile[17] = 0;
        profile[18] = 0;
        profile[19] = (byte) declaredComponents;
        // Profile size field (bytes 0-3).
        profile[0] = 0;
        profile[1] = 0;
        profile[2] = 1; // 128 = 0x80
        profile[3] = 0;
        out.write(profile);
        return out.toByteArray();
    }

    /**
     * Injects a fake APP2 (ICC_PROFILE) segment right after the last APP
     * segment (typically right after JFIF's APP0) and returns the
     * modified bytes.
     */
    private static byte[] injectApp2(byte[] jpeg, int declaredComponents) throws IOException
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        // SOI
        out.write(0xFF);
        out.write(0xD8);

        // Walk the segments, copying each APPn segment verbatim and
        // stopping just before the first non-APP segment so we can
        // inject our APP2 right after the last APP marker.
        int i = 2;
        while (i + 4 <= jpeg.length)
        {
            int marker = ((jpeg[i] & 0xFF) << 8) | (jpeg[i + 1] & 0xFF);
            if ((marker & 0xFFF0) != 0xFFE0)
            {
                // First non-APP segment: stop here without copying it.
                break;
            }
            int segmentLength = ((jpeg[i + 2] & 0xFF) << 8) | (jpeg[i + 3] & 0xFF);
            // Copy this APP segment verbatim.
            out.write(jpeg, i, 2 + segmentLength);
            i += 2 + segmentLength;
        }

        // Inject the fake APP2 segment.
        byte[] icc = buildFakeIccSegment(declaredComponents);
        out.write(0xFF);
        out.write(0xE2);
        int app2Length = icc.length + 2;
        out.write((app2Length >> 8) & 0xFF);
        out.write(app2Length & 0xFF);
        out.write(icc);

        // Copy the rest of the original (the first non-APP segment plus
        // everything after it - the scan data and EOI).
        if (i < jpeg.length)
        {
            out.write(jpeg, i, jpeg.length - i);
        }
        return out.toByteArray();
    }

    /**
     * Invokes the package-private {@code stripApp2Segments} helper via
     * reflection so we can test it directly without coupling to the
     * private API.
     */
    private static byte[] callStripApp2(File file) throws Exception
    {
        Method m = ExifOrientationUtil.class.getDeclaredMethod("stripApp2Segments", File.class);
        m.setAccessible(true);
        return (byte[]) m.invoke(null, file);
    }

    @Nested
    class StripApp2
    {
        @Test
        void noApp2IsReturnedUnchanged() throws Exception
        {
            byte[] original = buildValidJpeg(32, 24);
            File f = File.createTempFile("exif-noapp2-", ".jpg");
            f.deleteOnExit();
            Files.write(f.toPath(), original);

            byte[] stripped = callStripApp2(f);
            assertNotNull(stripped);
            // Without an APP2 segment, the bytes should be identical to the input.
            assertArrayEquals(original, stripped);
        }

        @Test
        void injectedApp2IsRemoved() throws Exception
        {
            byte[] original = buildValidJpeg(32, 24);
            byte[] withApp2 = injectApp2(original, 4);
            assertTrue(withApp2.length > original.length,
                "Injecting an APP2 should grow the file");

            File f = File.createTempFile("exif-withapp2-", ".jpg");
            f.deleteOnExit();
            Files.write(f.toPath(), withApp2);

            byte[] stripped = callStripApp2(f);
            assertNotNull(stripped);
            // After stripping, the bytes should match the original again.
            assertArrayEquals(original, stripped,
                "Stripping the APP2 should reproduce the original bytes");
        }

        @Test
        void nonJpegReturnsNull() throws Exception
        {
            File f = File.createTempFile("exif-notajpeg-", ".bin");
            f.deleteOnExit();
            Files.write(f.toPath(), new byte[] { 0x00, 0x01, 0x02, 0x03 });
            assertNull(callStripApp2(f));
        }

        @Test
        void missingFileReturnsNull() throws Exception
        {
            assertNull(callStripApp2(new File("does-not-exist.jpg")));
        }
    }

    @Nested
    class FallbackRead
    {
        @Test
        void fallsBackWhenApp2TriggersMismatch() throws Exception
        {
            // Sanity: a clean JPEG with no APP2 should always load.
            byte[] original = buildValidJpeg(48, 32);
            File clean = File.createTempFile("exif-clean-", ".jpg");
            clean.deleteOnExit();
            Files.write(clean.toPath(), original);
            BufferedImage ok = ExifOrientationUtil.read(clean);
            assertNotNull(ok);
            assertEquals(48, ok.getWidth());
            assertEquals(32, ok.getHeight());

            // If the JDK imageio happens to ignore the fake ICC profile
            // on this VM (it sometimes does, depending on which color
            // space library is bundled), the injected file will just
            // load normally too - either way the read should succeed.
            byte[] withApp2 = injectApp2(original, 4);
            File broken = File.createTempFile("exif-broken-", ".jpg");
            broken.deleteOnExit();
            Files.write(broken.toPath(), withApp2);
            BufferedImage recovered = ExifOrientationUtil.read(broken);
            assertNotNull(recovered, "Fallback must load the image even if the JDK refuses the ICC profile");
            assertEquals(48, recovered.getWidth());
            assertEquals(32, recovered.getHeight());
        }

        @Test
        void nullFileReturnsNull() throws Exception
        {
            assertNull(ExifOrientationUtil.read((File) null));
        }

        @Test
        void nonImageFileReturnsNull() throws Exception
        {
            File f = File.createTempFile("exif-text-", ".jpg");
            f.deleteOnExit();
            Files.write(f.toPath(), "this is not a JPEG".getBytes());
            assertNull(ExifOrientationUtil.read(f));
        }
    }
}