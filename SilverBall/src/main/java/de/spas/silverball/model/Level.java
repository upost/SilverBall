package de.spas.silverball.model;

import java.util.List;

/**
 * Created by uwe on 24.09.13.
 */
public class Level {
    private int number;
    private Ball ball;
    private Hole hole;
    private int points;
    private int time;
    private List<Trap> traps;

    public int getNumber() {
        return number;
    }

    public Ball getBall() {
        return ball;
    }

    public Hole getHole() {
        return hole;
    }

    public int getPoints() {
        return points;
    }

    public int getTime() {
        return time;
    }

    public List<Trap> getTraps() {
        return traps;
    }
}
