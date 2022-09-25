package raytracer.font;

import lwjglutil.DemoUtils;
import org.joml.Vector2d;
import org.lwjgl.system.MemoryUtil;

import java.awt.*;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL11C.*;
import static org.lwjgl.opengl.GL15C.*;
import static org.lwjgl.opengl.GL20C.*;
import static org.lwjgl.opengl.GL30C.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL30C.GL_LINK_STATUS;
import static org.lwjgl.opengl.GL30C.GL_VERTEX_SHADER;
import static org.lwjgl.opengl.GL30C.glAttachShader;
import static org.lwjgl.opengl.GL30C.glCreateProgram;
import static org.lwjgl.opengl.GL30C.glGetProgramInfoLog;
import static org.lwjgl.opengl.GL30C.glGetProgrami;
import static org.lwjgl.opengl.GL30C.glGetUniformLocation;
import static org.lwjgl.opengl.GL30C.glLinkProgram;
import static org.lwjgl.opengl.GL30C.glUniform1i;
import static org.lwjgl.opengl.GL30C.*;
import static org.lwjgl.opengl.GL33C.glBindSampler;
import static org.lwjgl.system.MemoryUtil.memFree;

public class Text {

    private FontTexture texture;
    private final int quadProgram;
    private int fontTextureId;
    private int fontSampler;
    private int vaoId;
    private int vboId;
    private int textureVboId;

    public Text() throws IOException {
        quadProgram = createQuadProgram();
        initQuadProgram(quadProgram);
        initTexture();
    }

    /**
     * Create the full-scren quad shader.
     *
     * @return
     * @throws IOException
     */
    private int createQuadProgram() throws IOException {
        int program = glCreateProgram();
        int vshader = DemoUtils.createShader("raytracer/fontquad.vs", GL_VERTEX_SHADER, "330");
        int fshader = DemoUtils.createShader("raytracer/fontquad.fs", GL_FRAGMENT_SHADER, "330");
        glAttachShader(program, vshader);
        glAttachShader(program, fshader);
        glBindFragDataLocation(program, 0, "color");
        glLinkProgram(program);
        int linked = glGetProgrami(program, GL_LINK_STATUS);
        String programLog = glGetProgramInfoLog(program);
        if (programLog.trim().length() > 0) {
            System.err.println(programLog);
        }
        if (linked == 0) {
            throw new AssertionError("Could not link program");
        }
        return program;
    }

    /**
     * Initialize the full-screen-quad program.
     */
    private void initQuadProgram(int quadProgram) {
        glUseProgram(quadProgram);
        int texUniform = glGetUniformLocation(quadProgram, "tex");
        glUniform1i(texUniform, 0);
        glUseProgram(0);
    }

    private void initTexture() {
        texture = new FontTexture(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        // Create a new OpenGL texture
        fontTextureId = glGenTextures();
        // Bind the texture
        glBindTexture(GL_TEXTURE_2D, fontTextureId);
        glPixelStorei(GL_UNPACK_ALIGNMENT, 1);

        int width = texture.getWidth();
        int height = texture.getHeight();

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, texture.getBuffer());
        glGenerateMipmap(GL_TEXTURE_2D);

        vboId = glGenBuffers();
        textureVboId = glGenBuffers();
    }

    private void updateQuadVertices(Vector2d pos, String string) {

        float fontWidth = 0.001953125f; // 2 / windowWidth
        float fontHeight = 0.049479166666666f; // 2 * textureHeight / windowHeight

        List<Float> vertices = new ArrayList<>(12 * string.length());

        float xPos = (float) pos.x();
        float textTop = (float) pos.y();
        float textBottom = (float) pos.y() - fontHeight;

        for (int i = 0; i < string.length(); i++) {
            char c = string.charAt(i);
            float charWidth = fontWidth * texture.getCharWidth(c);

            vertices.add(xPos);
            vertices.add(textTop);
            vertices.add(0.0f);

            vertices.add(xPos);
            vertices.add(textBottom);
            vertices.add(0.0f);

            vertices.add(xPos + charWidth);
            vertices.add(textTop);
            vertices.add(0.0f);

            vertices.add(xPos + charWidth);
            vertices.add(textBottom);
            vertices.add(0.0f);

            xPos += charWidth;
        }

        FloatBuffer verticesBuffer = MemoryUtil.memAllocFloat(vertices.size());
        verticesBuffer.put(toFloatArray(vertices)).flip();
        vaoId = glGenVertexArrays();
        glBindVertexArray(vaoId);

        glBindBuffer(GL_ARRAY_BUFFER, vboId);
        glBufferData(GL_ARRAY_BUFFER, verticesBuffer, GL_STATIC_DRAW);
        memFree(verticesBuffer);

        glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);


        List<Float> textCoords = new ArrayList<>(8 * string.length());

        for (int i = 0; i < string.length(); i++) {
            char c = string.charAt(i);

            textCoords.add(texture.getCharLeftCoord(c));
            textCoords.add(0.0f);

            textCoords.add(texture.getCharLeftCoord(c));
            textCoords.add(1.0f);

            textCoords.add(texture.getCharRightCoord(c));
            textCoords.add(0.0f);

            textCoords.add(texture.getCharRightCoord(c));
            textCoords.add(1.0f);
        }

        FloatBuffer textCoordsBuffer = MemoryUtil.memAllocFloat(textCoords.size());
        textCoordsBuffer.put(toFloatArray(textCoords)).flip();

        glBindBuffer(GL_ARRAY_BUFFER, textureVboId);
        glBufferData(GL_ARRAY_BUFFER, textCoordsBuffer, GL_STATIC_DRAW);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);

        glEnableVertexAttribArray(0);
        glEnableVertexAttribArray(1);
    }

    private float[] toFloatArray(List<Float> textCoords) {
        float[] floatArr = new float[textCoords.size()];
        for (int i = 0; i < textCoords.size(); i++) {
            floatArr[i] = textCoords.get(i);
        }
        return floatArr;
    }

    public void drawText(Vector2d pos, String string) {

        updateQuadVertices(pos, string);
        /*
         * Draw the rendered image on the screen using textured full-screen
         * quad.
         */
        glUseProgram(quadProgram);

        // Bind to the VAO
        glBindVertexArray(vaoId);

        // Bind the font texture
        glBindTexture(GL_TEXTURE_2D, fontTextureId);
        glBindSampler(0, this.fontSampler);

        // Draw the vertices
        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4 * string.length());

        // Restore state
        glBindVertexArray(0);
        glUseProgram(0);
    }
}
