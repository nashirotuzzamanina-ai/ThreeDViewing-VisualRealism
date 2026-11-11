package com.grafkom.viewing3d;

import org.joml.Matrix4f;
import org.joml.Vector3f;

public class Camera3D {
    private Vector3f position;
    private Vector3f target;
    private Vector3f up;

    public Camera3D(Vector3f position, Vector3f target, Vector3f up) {
        this.position = position;
        this.target = target;
        this.up = up;
    }

    public Matrix4f getViewMatrix() {
        return new Matrix4f().lookAt(position, target, up);
    }

    public Vector3f getPosition() {
        return position;
    }

    public void setPosition(Vector3f position) {
        this.position = position;
    }
}