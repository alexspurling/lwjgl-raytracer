package raytracer.font;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class FontTexture {

    private final Font font;
    private final Map<Character, CharInfo> charMap;
    private int width;
    private int height;
    private ByteBuffer buffer;

    public FontTexture(Font font) {
        this.font = font;
        charMap = new HashMap<>();

        buildTexture();
    }

    private String getAllAvailableChars() {
        StringBuilder result = new StringBuilder();
        for (char c = ' '; c <= '~'; c++) {
            result.append(c);
        }
        result.append(' ');
        return result.toString();
    }

    private void buildTexture() {
        // Get the font metrics for each character for the selected font by using image
        BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2D = img.createGraphics();
        g2D.setFont(font);
        FontMetrics fontMetrics = g2D.getFontMetrics();

        String allChars = getAllAvailableChars();
        this.width = 0;
        this.height = 0;
        for (char c : allChars.toCharArray()) {
            // Get the size for each character and update global image size
            CharInfo charInfo = new CharInfo(width, fontMetrics.charWidth(c));
            charMap.put(c, charInfo);
            width += charInfo.getWidth();
            height = Math.max(height, fontMetrics.getHeight());
        }
        System.out.println("Rendered " + allChars.length() + " characters");

        // Create the image associated to the charset
        img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        g2D = img.createGraphics();
        g2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2D.setFont(font);
        fontMetrics = g2D.getFontMetrics();
        g2D.setColor(Color.WHITE);
        g2D.drawString(allChars, 0, fontMetrics.getAscent());

        try {
            File file = new File("Tex" + font.getSize() + ".png");
            System.out.println("Writing file: " + file);
            ImageIO.write(img, "png", file);
            System.out.println("Written file: " + file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        int[] tex = ((DataBufferInt)img.getRaster().getDataBuffer()).getData();

        buffer = ByteBuffer.allocateDirect(4 * width * height);
        for (int i : tex) {
            // Convert from ARGB to RGBA
            int colorRGBA = (i << 8) | (i >> 24);
            buffer.putInt(colorRGBA);
        }
        buffer.flip();

        g2D.dispose();
    }

    public float getCharLeftCoord(char c) {
        return (float) charMap.get(c).getStartX() / width;
    }

    public float getCharRightCoord(char c) {
        CharInfo charInfo = charMap.get(c);
        return (float) (charInfo.getStartX() + charInfo.getWidth()) / width;
    }

    public int getCharWidth(char c) {
        CharInfo charInfo = charMap.get(c);
        return charInfo.getWidth();
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public ByteBuffer getBuffer() {
        return buffer;
    }

    public static void main(String[] args) {
        for (int i = 10; i <= 25; i++) {
            FontTexture fontTexture = new FontTexture(new Font(Font.SANS_SERIF, Font.PLAIN, i));
            System.out.println("Height: " + fontTexture.height + ", width: " + fontTexture.width);
        }

    }
}
