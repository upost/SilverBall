package de.spas.silverball.model;

/**
 * Created by uwe on 01.03.16.
 */
public class CollisionResult {
    private boolean collided;
    private float vx,vy;

    public static final CollisionResult NO_COLLISION = new CollisionResult(false,0,0);

    public CollisionResult(boolean collided, float vx, float vy) {
        this.collided = collided;
        this.vx = vx;
        this.vy = vy;
    }

    public CollisionResult(float vx, float vy) {
        this.collided = true;
        this.vx = vx;
        this.vy = vy;
    }

    public boolean isCollided() {
        return collided;
    }

    public float getVx() {
        return vx;
    }

    public float getVy() {
        return vy;
    }

    @Override
    public String toString() {
        return ""+vx+"/"+vy;
    }


}
