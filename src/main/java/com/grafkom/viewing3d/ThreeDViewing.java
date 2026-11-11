package com.grafkom.viewing3d;

import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import org.lwjgl.system.*;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;

public class ThreeDViewing {

    private long window;
    private int width = 1024;
    private int height = 768;

    private Camera3D camera;
    private PerspectiveProjection projection;

    // Status Kontrol Interaktif (Indikator 1 - 4)
    private boolean useBlinnPhong = false;
    private boolean useFlatShading = false;
    private boolean enableDepthTest = true;
    private boolean enableCulling = true;
    private boolean useTexture = true;

    // Shader & Buffers
    private int phongShader;
    private int shadowShader;
    private int cubeVAO, cubeVBO;
    private int planeVAO, planeVBO;

    private Texture2D texture;
    private ShadowMapFBO shadowFBO;

    public void run() {
        init();
        loop();

        glfwFreeCallbacks(window);
        glfwDestroyWindow(window);
        glfwTerminate();
        glfwSetErrorCallback(null).free();
    }

    private void init() {
        GLFWErrorCallback.createPrint(System.err).set();
        if (!glfwInit()) throw new IllegalStateException("Gagal menginisialisasi GLFW");

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);

        window = glfwCreateWindow(width, height, "3D Visual Realism Suite - Week 11", NULL, NULL);
        if (window == NULL) throw new RuntimeException("Gagal membuat jendela GLFW");

        // Regristrasi Kontrol Tombol Keyboard
        glfwSetKeyCallback(window, (window, key, scancode, action, mods) -> {
            if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE) glfwSetWindowShouldClose(window, true);
            if (action == GLFW_PRESS) {
                if (key == GLFW_KEY_B) {
                    useBlinnPhong = !useBlinnPhong;
                    System.out.println("Mode Specular: " + (useBlinnPhong ? "Blinn-Phong" : "Phong Standard"));
                }
                if (key == GLFW_KEY_F) {
                    useFlatShading = !useFlatShading;
                    System.out.println("Mode Shading: " + (useFlatShading ? "Flat Shading" : "Smooth Shading"));
                }
                if (key == GLFW_KEY_Z) {
                    enableDepthTest = !enableDepthTest;
                    HSRManager.setupDepthTest(enableDepthTest);
                    System.out.println("Z-Buffer Depth Test: " + enableDepthTest);
                }
                if (key == GLFW_KEY_C) {
                    enableCulling = !enableCulling;
                    HSRManager.setupBackFaceCulling(enableCulling);
                    System.out.println("Back-Face Culling: " + enableCulling);
                }
                if (key == GLFW_KEY_T) {
                    useTexture = !useTexture;
                    System.out.println("Texture Mapping: " + useTexture);
                }
            }
        });

        glfwSetFramebufferSizeCallback(window, (window, w, h) -> {
            this.width = w;
            this.height = h;
            glViewport(0, 0, w, h);
            projection.updateAspectRatio((float) w / h);
        });

        try (MemoryStack stack = stackPush()) {
            IntBuffer pWidth = stack.mallocInt(1);
            IntBuffer pHeight = stack.mallocInt(1);
            glfwGetWindowSize(window, pWidth, pHeight);
            GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
            glfwSetWindowPos(window, (vidmode.width() - pWidth.get(0)) / 2, (vidmode.height() - pHeight.get(0)) / 2);
        }

        glfwMakeContextCurrent(window);
        glfwSwapInterval(1);
        glfwShowWindow(window);

        GL.createCapabilities();

        HSRManager.setupDepthTest(enableDepthTest);
        HSRManager.setupBackFaceCulling(enableCulling);

        camera = new Camera3D(new Vector3f(0.0f, 3.0f, 6.0f), new Vector3f(0.0f, 0.0f, 0.0f), new Vector3f(0.0f, 1.0f, 0.0f));
        projection = new PerspectiveProjection(45.0f, (float) width / height, 0.1f, 100.0f);

        phongShader = createShaderProgram("src/main/resources/shaders/phong.vert", "src/main/resources/shaders/phong.frag");
        shadowShader = createShaderProgram("src/main/resources/shaders/shadow.vert", "src/main/resources/shaders/shadow.frag");

        setupCubeMesh();
        setupPlaneMesh();

        texture = new Texture2D("src/main/resources/wood.jpg");
        shadowFBO = new ShadowMapFBO();
    }

    private void setupCubeMesh() {
        float[] vertices = {
            // Posisi (X,Y,Z)      // Normal (X,Y,Z)     // UV (U,V)
            -0.5f, -0.5f, -0.5f,  0.0f,  0.0f, -1.0f,  0.0f, 0.0f,
             0.5f,  0.5f, -0.5f,  0.0f,  0.0f, -1.0f,  1.0f, 1.0f,
             0.5f, -0.5f, -0.5f,  0.0f,  0.0f, -1.0f,  1.0f, 0.0f,
             0.5f,  0.5f, -0.5f,  0.0f,  0.0f, -1.0f,  1.0f, 1.0f,
            -0.5f, -0.5f, -0.5f,  0.0f,  0.0f, -1.0f,  0.0f, 0.0f,
            -0.5f,  0.5f, -0.5f,  0.0f,  0.0f, -1.0f,  0.0f, 1.0f,

            -0.5f, -0.5f,  0.5f,  0.0f,  0.0f,  1.0f,  0.0f, 0.0f,
             0.5f, -0.5f,  0.5f,  0.0f,  0.0f,  1.0f,  1.0f, 0.0f,
             0.5f,  0.5f,  0.5f,  0.0f,  0.0f,  1.0f,  1.0f, 1.0f,
             0.5f,  0.5f,  0.5f,  0.0f,  0.0f,  1.0f,  1.0f, 1.0f,
            -0.5f,  0.5f,  0.5f,  0.0f,  0.0f,  1.0f,  0.0f, 1.0f,
            -0.5f, -0.5f,  0.5f,  0.0f,  0.0f,  1.0f,  0.0f, 0.0f,

            -0.5f,  0.5f,  0.5f, -1.0f,  0.0f,  0.0f,  1.0f, 0.0f,
            -0.5f,  0.5f, -0.5f, -1.0f,  0.0f,  0.0f,  1.0f, 1.0f,
            -0.5f, -0.5f, -0.5f, -1.0f,  0.0f,  0.0f,  0.0f, 1.0f,
            -0.5f, -0.5f, -0.5f, -1.0f,  0.0f,  0.0f,  0.0f, 1.0f,
            -0.5f, -0.5f,  0.5f, -1.0f,  0.0f,  0.0f,  0.0f, 0.0f,
            -0.5f,  0.5f,  0.5f, -1.0f,  0.0f,  0.0f,  1.0f, 0.0f,

             0.5f,  0.5f,  0.5f,  1.0f,  0.0f,  0.0f,  1.0f, 0.0f,
             0.5f, -0.5f, -0.5f,  1.0f,  0.0f,  0.0f,  0.0f, 1.0f,
             0.5f,  0.5f, -0.5f,  1.0f,  0.0f,  0.0f,  1.0f, 1.0f,
             0.5f, -0.5f, -0.5f,  1.0f,  0.0f,  0.0f,  0.0f, 1.0f,
             0.5f,  0.5f,  0.5f,  1.0f,  0.0f,  0.0f,  1.0f, 0.0f,
             0.5f, -0.5f,  0.5f,  1.0f,  0.0f,  0.0f,  0.0f, 0.0f,

            -0.5f, -0.5f, -0.5f,  0.0f, -1.0f,  0.0f,  0.0f, 1.0f,
             0.5f, -0.5f, -0.5f,  0.0f, -1.0f,  0.0f,  1.0f, 1.0f,
             0.5f, -0.5f,  0.5f,  0.0f, -1.0f,  0.0f,  1.0f, 0.0f,
             0.5f, -0.5f,  0.5f,  0.0f, -1.0f,  0.0f,  1.0f, 0.0f,
            -0.5f, -0.5f,  0.5f,  0.0f, -1.0f,  0.0f,  0.0f, 0.0f,
            -0.5f, -0.5f, -0.5f,  0.0f, -1.0f,  0.0f,  0.0f, 1.0f,

            -0.5f,  0.5f, -0.5f,  0.0f,  1.0f,  0.0f,  0.0f, 1.0f,
             0.5f,  0.5f,  0.5f,  0.0f,  1.0f,  0.0f,  1.0f, 0.0f,
             0.5f,  0.5f, -0.5f,  0.0f,  1.0f,  0.0f,  1.0f, 1.0f,
             0.5f,  0.5f,  0.5f,  0.0f,  1.0f,  0.0f,  1.0f, 0.0f,
            -0.5f,  0.5f, -0.5f,  0.0f,  1.0f,  0.0f,  0.0f, 1.0f,
            -0.5f,  0.5f,  0.5f,  0.0f,  1.0f,  0.0f,  0.0f, 0.0f
        };

        cubeVAO = glGenVertexArrays();
        cubeVBO = glGenBuffers();
        glBindVertexArray(cubeVAO);
        glBindBuffer(GL_ARRAY_BUFFER, cubeVBO);
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW);

        int stride = 8 * Float.BYTES;
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 3, GL_FLOAT, false, stride, 3 * Float.BYTES);
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(2, 2, GL_FLOAT, false, stride, 6 * Float.BYTES);
        glEnableVertexAttribArray(2);
    }

    private void setupPlaneMesh() {
        float[] planeVertices = {
            // Posisi (X,Y,Z)      // Normal (X,Y,Z)     // UV (U,V)
             5.0f, -0.5f,  5.0f,  0.0f, 1.0f, 0.0f,  2.0f, 0.0f,
            -5.0f, -0.5f,  5.0f,  0.0f, 1.0f, 0.0f,  0.0f, 0.0f,
            -5.0f, -0.5f, -5.0f,  0.0f, 1.0f, 0.0f,  0.0f, 2.0f,

             5.0f, -0.5f,  5.0f,  0.0f, 1.0f, 0.0f,  2.0f, 0.0f,
            -5.0f, -0.5f, -5.0f,  0.0f, 1.0f, 0.0f,  0.0f, 2.0f,
             5.0f, -0.5f, -5.0f,  0.0f, 1.0f, 0.0f,  2.0f, 2.0f
        };

        planeVAO = glGenVertexArrays();
        planeVBO = glGenBuffers();
        glBindVertexArray(planeVAO);
        glBindBuffer(GL_ARRAY_BUFFER, planeVBO);
        glBufferData(GL_ARRAY_BUFFER, planeVertices, GL_STATIC_DRAW);

        int stride = 8 * Float.BYTES;
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 3, GL_FLOAT, false, stride, 3 * Float.BYTES);
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(2, 2, GL_FLOAT, false, stride, 6 * Float.BYTES);
        glEnableVertexAttribArray(2);
    }

    private void loop() {
        glClearColor(0.1f, 0.1f, 0.15f, 1.0f);

        Vector3f lightPos = new Vector3f(2.0f, 4.0f, 2.0f);

        while (!glfwWindowShouldClose(window)) {
            // Matriks Sumber Cahaya (Light Space Matrix)
            Matrix4f lightProjection = new Matrix4f().ortho(-10.0f, 10.0f, -10.0f, 10.0f, 1.0f, 20.0f);
            Matrix4f lightView = new Matrix4f().lookAt(lightPos, new Vector3f(0.0f, 0.0f, 0.0f), new Vector3f(0.0f, 1.0f, 0.0f));
            Matrix4f lightSpaceMatrix = new Matrix4f(lightProjection).mul(lightView);

            Matrix4f cubeModel = new Matrix4f().translate(0.0f, 0.5f, 0.0f).rotate((float) glfwGetTime() * 0.5f, new Vector3f(0.5f, 1.0f, 0.0f));
            Matrix4f planeModel = new Matrix4f();

            // ==========================================
            // PASS 1: RENDER SHADOW MAP KE DEPTH FBO
            // ==========================================
            shadowFBO.bindForWriting();
            glUseProgram(shadowShader);

            try (MemoryStack stack = stackPush()) {
                FloatBuffer fbLightSpace = stack.mallocFloat(16);
                FloatBuffer fbModel = stack.mallocFloat(16);

                glUniformMatrix4fv(glGetUniformLocation(shadowShader, "lightSpaceMatrix"), false, lightSpaceMatrix.get(fbLightSpace));

                // Render Cube ke Depth FBO
                glUniformMatrix4fv(glGetUniformLocation(shadowShader, "model"), false, cubeModel.get(fbModel));
                glBindVertexArray(cubeVAO);
                glDrawArrays(GL_TRIANGLES, 0, 36);

                // Render Plane ke Depth FBO
                glUniformMatrix4fv(glGetUniformLocation(shadowShader, "model"), false, planeModel.get(fbModel));
                glBindVertexArray(planeVAO);
                glDrawArrays(GL_TRIANGLES, 0, 6);
            }
            shadowFBO.unbind(width, height);

            // ==========================================
            // PASS 2: RENDER SCENE UTAMA DENGAN BAYANGAN
            // ==========================================
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            glUseProgram(phongShader);

            Matrix4f view = camera.getViewMatrix();
            Matrix4f proj = projection.getProjectionMatrix();

            try (MemoryStack stack = stackPush()) {
                FloatBuffer fbModel = stack.mallocFloat(16);
                FloatBuffer fbView = stack.mallocFloat(16);
                FloatBuffer fbProj = stack.mallocFloat(16);
                FloatBuffer fbLightSpace = stack.mallocFloat(16);

                glUniformMatrix4fv(glGetUniformLocation(phongShader, "view"), false, view.get(fbView));
                glUniformMatrix4fv(glGetUniformLocation(phongShader, "projection"), false, proj.get(fbProj));
                glUniformMatrix4fv(glGetUniformLocation(phongShader, "lightSpaceMatrix"), false, lightSpaceMatrix.get(fbLightSpace));

                glUniform3f(glGetUniformLocation(phongShader, "lightPos"), lightPos.x, lightPos.y, lightPos.z);
                glUniform3f(glGetUniformLocation(phongShader, "viewPos"), camera.getPosition().x, camera.getPosition().y, camera.getPosition().z);
                glUniform3f(glGetUniformLocation(phongShader, "lightColor"), 1.0f, 1.0f, 1.0f);
                glUniform1i(glGetUniformLocation(phongShader, "useBlinnPhong"), useBlinnPhong ? 1 : 0);
                glUniform1i(glGetUniformLocation(phongShader, "useFlatShading"), useFlatShading ? 1 : 0);
                glUniform1i(glGetUniformLocation(phongShader, "useTexture"), useTexture ? 1 : 0);

                // Bind Shadow Map Texture ke Texture Unit 1
                glActiveTexture(GL_TEXTURE1);
                glBindTexture(GL_TEXTURE_2D, shadowFBO.getDepthTextureID());
                glUniform1i(glGetUniformLocation(phongShader, "shadowMap"), 1);

                // Bind Tekstur Kayu ke Texture Unit 0
                texture.bind(0);
                glUniform1i(glGetUniformLocation(phongShader, "textureSampler"), 0);

                // Render Cube (Objek Utama)
                glUniformMatrix4fv(glGetUniformLocation(phongShader, "model"), false, cubeModel.get(fbModel));
                glUniform3f(glGetUniformLocation(phongShader, "objectColor"), 0.8f, 0.5f, 0.3f);
                glBindVertexArray(cubeVAO);
                glDrawArrays(GL_TRIANGLES, 0, 36);

                // Render Plane (Lantai Penerima Bayangan)
                glUniformMatrix4fv(glGetUniformLocation(phongShader, "model"), false, planeModel.get(fbModel));
                glUniform3f(glGetUniformLocation(phongShader, "objectColor"), 0.6f, 0.6f, 0.6f);
                glBindVertexArray(planeVAO);
                glDrawArrays(GL_TRIANGLES, 0, 6);
            }

            glfwSwapBuffers(window);
            glfwPollEvents();
        }
    }

    private int createShaderProgram(String vertPath, String fragPath) {
        try {
            String vertCode = new String(Files.readAllBytes(Paths.get(vertPath)));
            String fragCode = new String(Files.readAllBytes(Paths.get(fragPath)));

            int vertShader = glCreateShader(GL_VERTEX_SHADER);
            glShaderSource(vertShader, vertCode);
            glCompileShader(vertShader);

            int fragShader = glCreateShader(GL_FRAGMENT_SHADER);
            glShaderSource(fragShader, fragCode);
            glCompileShader(fragShader);

            int program = glCreateProgram();
            glAttachShader(program, vertShader);
            glAttachShader(program, fragShader);
            glLinkProgram(program);

            glDeleteShader(vertShader);
            glDeleteShader(fragShader);

            return program;
        } catch (IOException e) {
            throw new RuntimeException("Gagal membaca file shader", e);
        }
    }

    public static void main(String[] args) {
        new ThreeDViewing().run();
    }
}