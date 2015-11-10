package de.spas.silverball;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.util.List;

/**
 * Created by uwe on 24.09.13.
 */
@Root
public class LevelPack {
    @Attribute private String name;
    @ElementList private List<Level> levels;

    public String getName() {
        return name;
    }

    public List<Level> getLevels() {
        return levels;
    }
}
