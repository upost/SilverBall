package de.spas.silverball.model;

import org.simpleframework.xml.Attribute;

/**
 * Created by uwe on 24.09.13.
 */
public class Obstacle {
    @Attribute(required = false) private String type;
    @Attribute private String texture;
    @Attribute private int x;
    @Attribute private int y;
    @Attribute private int w;
    @Attribute private int h;

    public String getType() {
        return type;
    }

    public String getTexture() {
        return texture;
    }


    public int getH() {
        return h;
    }

    public int getW() {
        return w;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }
}
