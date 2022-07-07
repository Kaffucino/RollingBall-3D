package com.example.rollingball.arena;

import javafx.scene.Group;
import javafx.scene.PointLight;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.paint.Material;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.transform.Translate;

public class Lamp extends Box {

    private boolean lightOn;
    private  PhongMaterial material;

    public Lamp(double a, double b, double c, PhongMaterial material, Translate position){
        super(a,b,c);
        super.setMaterial(material);
        this.material=material;
        super.getTransforms().add(position);
        lightOn=true;
    }

    public boolean isLightOn(){
        return lightOn;
    }

    public void turnOnLight(){
        Image illumi_image = new Image("selfIllumination.png");
        material.setSelfIlluminationMap(illumi_image);
        super.setMaterial(material);

        lightOn=true;
    }

    public void turnOffLight(){
        this.material=new PhongMaterial(Color.GRAY);
        super.setMaterial(material);
        lightOn=false;
    }





}
