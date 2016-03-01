package de.spas.silverball.model;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Root;

/**
 * Created by uwe on 24.09.13.
 */
public class Ball {
    @Attribute private int startx;
    @Attribute private int starty;

    public int getStartx() {
        return startx;
    }

    public int getStarty() {
        return starty;
    }
}
