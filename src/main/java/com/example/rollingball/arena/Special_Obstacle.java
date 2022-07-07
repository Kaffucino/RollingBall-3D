package com.example.rollingball.arena;

import javafx.scene.paint.Material;
import javafx.scene.shape.Box;
import javafx.scene.transform.Translate;

public class Special_Obstacle extends Box {

    double speed_factor;

    public Special_Obstacle(double a, double b, double c, Material material, Translate position,double speed_factor){
        super(a,b,c);
        super.setMaterial(material);
        super.getTransforms().add(position);
        this.speed_factor=speed_factor;
    }

    double getSpeed_factor(){
        return speed_factor;
    }


}
