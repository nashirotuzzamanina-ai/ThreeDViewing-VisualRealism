package com.grafkom.viewing3d;

import org.joml.Matrix4f;

public class ProjectionTaxonomyManager {
    private int currentMode = 4;
    private PerspectiveProjection proj;

    public ProjectionTaxonomyManager(PerspectiveProjection proj) {
        this.proj = proj;
    }

    public void setMode(int mode) {
        this.currentMode = mode;
    }

    public int getMode() {
        return currentMode;
    }

    public Matrix4f getProjectionMatrix() {
        switch (currentMode) {
            case 1: // Ortografik
                return new Matrix4f().ortho(-2.0f, 2.0f, -2.0f, 2.0f, 0.1f, 100.0f);
            case 2: // Isometrik
                return new Matrix4f().ortho(-2.0f, 2.0f, -2.0f, 2.0f, 0.1f, 100.0f)
                        .rotateX((float) Math.toRadians(35.264f))
                        .rotateY((float) Math.toRadians(45.0f));
            case 3: // Oblique Cavalier
                return new Matrix4f().ortho(-2.0f, 2.0f, -2.0f, 2.0f, 0.1f, 100.0f);
            case 4: // Perspektif Standard
            default:
                return proj.calculateProjectionMatrix();
        }
    }
}