package de.spas.math;

/**
 * Created by uwe on 02.03.18.
 */

public class Matrix2 {
    public float xx,xy,yx,yy;

    public void unity() {
        xx=1; xy=0; yx=0; yy=1;
    }

    public  Vector2 multiply(Vector2 v) {
        float x2 = xx*v.x + xy*v.y;
        float y2 = yx*v.x + yy*v.y;
        return new Vector2(x2,y2);
    }

    public float det() {
        return xx*yy-xy*yx;
    }
}
