package de.spas.math;

/**
 * Created by uwe on 02.03.18.
 */

public class Vector2 {
    public float x,y;

    public Vector2() {
    }

    public Vector2(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public Vector2 add(Vector2 v) {
        x+=v.x;
        y+=v.y;
        return this;
    }

    public float length() {
        return (float) Math.sqrt(x*x+y*y);
    }

    public Vector2 multiply(float p) {
        x*=p;
        y*=p;
        return this;
    }

    public Vector2 add(Vector2 v, float p) {
        x += v.x *p;
        y += v.y *p;
        return this;
    }

    public void copyFrom(Vector2 v) {
        x = v.x;
        y = v.y;
    }

    @Override
    public String toString() {
        return "Vector2{" +
                "x=" + x +
                ", y=" + y +
                '}';
    }


}
