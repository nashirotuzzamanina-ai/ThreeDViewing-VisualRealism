package com.grafkom.viewing3d;

import org.joml.Matrix4f;

public class PerspectiveProjection {
    private float fov;
    private float aspectRatio;
    private float zNear;
    private float zFar;

    public PerspectiveProjection(float fov, float aspectRatio, float zNear, float zFar) {
        this.fov = fov;
        this.aspectRatio = aspectRatio;
        this.zNear = zNear;
        this.zFar = zFar;
    }

    public Matrix4f getProjectionMatrix() {
        return new Matrix4f().perspective((float) Math.toRadians(fov), aspectRatio, zNear, zFar);
    }

    // Alias method untuk kompatibilitas
    public Matrix4f calculateProjectionMatrix() {
        return getProjectionMatrix();
    }

    public void updateAspectRatio(float aspectRatio) {
        this.aspectRatio = aspectRatio;
    }
}