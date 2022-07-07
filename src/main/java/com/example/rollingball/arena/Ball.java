package com.example.rollingball.arena;

import javafx.geometry.Bounds;
import javafx.geometry.Point3D;
import javafx.scene.paint.Material;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Translate;

import java.util.ArrayList;

public class Ball extends Sphere {
    private Translate position;
    private Point3D speed;

    public Ball(double radius, Material material, Translate position) {
        super(radius);
        super.setMaterial(material);

        this.position = position;

        super.getTransforms().add(this.position);

        this.speed = new Point3D(0, 0, 0);
    }

    public double getSpeed(){
        return this.speed.magnitude();
    }

    public boolean update(
            double deltaSeconds,
            double top,
            double bottom,
            double left,
            double right,
            double xAngle,
            double zAngle,
            double maxAngleOffset,
            double maxAcceleration,
            double damp,
            ArrayList<Fence> obstacles,
            ArrayList<Cylinder> cylinder_obstacles,
            ArrayList<Special_Obstacle> special_obstacles
    ) {
        double newPositionX = this.position.getX() + this.speed.getX() * deltaSeconds;
        double newPositionZ = this.position.getZ() + this.speed.getZ() * deltaSeconds;

        this.position.setX(newPositionX);
        this.position.setZ(newPositionZ);

        double accelerationX = maxAcceleration * zAngle / maxAngleOffset;
        double accelerationZ = -maxAcceleration * xAngle / maxAngleOffset;

        double newSpeedX = (this.speed.getX() + accelerationX * deltaSeconds) * damp;
        double newSpeedZ = (this.speed.getZ() + accelerationZ * deltaSeconds) * damp;


        //fence collision
        for (int i = 0; i < obstacles.size(); ++i) {

            Bounds obstl_bound = obstacles.get(i).getBoundsInParent();

            double centerX = obstl_bound.getCenterX();
            double centerZ = obstl_bound.getCenterZ();


            if (newPositionX + 50 > centerX - 500 && newPositionX - 50 < centerX + 500 && (i == 0 || i == 3)) {

                for (int j = ((int) centerX - 1000); j < centerX + 1000; ++j) {
                    double distance = Math.sqrt(Math.pow(j - newPositionX, 2) + Math.pow(centerZ - newPositionZ, 2));

                    if (distance < 80) {
                        newSpeedZ = (-this.speed.getZ() + accelerationZ * deltaSeconds) * damp;
                        break;
                    }
                    if(distance < 200 && this.getSpeed() > 3000)
                    {
                        newSpeedZ = (-this.speed.getZ() + accelerationZ * deltaSeconds) * damp;
                        break;
                    }

                }


            }

            if (newPositionZ + 50 >= centerZ - 500 && newPositionZ - 50 <= centerZ + 500 && (i == 1 || i == 2)) {

                for (int j = ((int) centerZ - 1000); j < centerZ + 1000; ++j) {
                    double distance = Math.sqrt(Math.pow(centerX - newPositionX, 2) + Math.pow(j - newPositionZ, 2));

                    if (distance < 80) {
                        newSpeedX = (-this.speed.getX() + accelerationX * deltaSeconds) * damp;
                        break;
                    }

                    if(distance < 200 && this.getSpeed() > 3000)
                    {
                        newSpeedX = (-this.speed.getX() + accelerationX * deltaSeconds) * damp;
                        break;
                    }

                }


            }

        }

        //Cilindir obstacle

        for (int i = 0; i < cylinder_obstacles.size(); ++i) {

            Bounds ballBounds = this.getBoundsInParent();

            double ballX = ballBounds.getCenterX();
            double ballZ = ballBounds.getCenterZ();

            Bounds obst_bounds = cylinder_obstacles.get(i).getBoundsInParent();
            double obstX = obst_bounds.getCenterX();
            double obstZ = obst_bounds.getCenterZ();
            double obst_radius = cylinder_obstacles.get(i).getRadius();

            double dx = obstX - ballX;
            double dz = obstZ - ballZ;

            double distance = dx * dx + dz * dz;

            boolean collision = distance <= 2.5*(obst_radius * obst_radius);

            //znaci bilo je kolizije
            if(collision){
                if(Math.abs(ballX - obstX) <= 2.5*obst_radius)
                    newSpeedZ = (-this.speed.getZ() + accelerationZ * deltaSeconds) * damp;

                if(Math.abs(ballZ - obstZ) <= 2.5*obst_radius)
                    newSpeedX = (-this.speed.getX() + accelerationX * deltaSeconds) * damp;


            }


        }
        //special obstacle kolizija
        for(int i=0;i<special_obstacles.size();++i){

            double faktor=special_obstacles.get(i).getSpeed_factor();

            Bounds ballBounds = this.getBoundsInParent();

            double ballX = ballBounds.getCenterX();
            double ballZ = ballBounds.getCenterZ();

            Bounds obsBounds=special_obstacles.get(i).getBoundsInParent();

            double minX=obsBounds.getMinX();
            double maxX=obsBounds.getMaxX();
            double centerX=obsBounds.getCenterX();
            double centerZ=obsBounds.getCenterZ();


            double minZ=obsBounds.getMinZ();
            double maxZ=obsBounds.getMaxZ();


            if(ballX >= minX && ballX <= maxX){
                double distance=Math.sqrt(Math.pow(centerX-ballX,2) + Math.pow(centerZ-ballZ,2));
                if(distance <= 50 + this.getRadius() ){

                    if(this.getSpeed() <= 2000)
                    newSpeedZ = faktor*(-this.speed.getZ() + accelerationZ * deltaSeconds) * damp;
                    else
                        newSpeedZ = (-this.speed.getZ() + accelerationZ * deltaSeconds) * damp;

                }

            }

            if(ballZ >= minZ && ballZ <= maxZ){
                double distance=Math.sqrt(Math.pow(centerX-ballX,2) + Math.pow(centerZ-ballZ,2));
                if(distance <= 50 + this.getRadius() ){
                    if(this.getSpeed() <= 2000)
                    newSpeedX = faktor*(-this.speed.getX() + accelerationX * deltaSeconds) * damp;
                    else newSpeedX = (-this.speed.getX() + accelerationX * deltaSeconds) * damp;

                }

            }




        }


        this.speed = new Point3D(newSpeedX, 0, newSpeedZ);

        boolean xOutOfBounds = (newPositionX > right) || (newPositionX < left);
        boolean zOutOfBounds = (newPositionZ > top) || (newPositionZ < bottom);

        return xOutOfBounds || zOutOfBounds;
    }

}
