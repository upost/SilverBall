package de.spas.silverball;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Root;

/**
 * Created by uwe on 24.09.13.
 */
public class Hole {
    @Attribute private int x;
    @Attribute private int y;

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

}
