package com.example.rollingball.arena;

import javafx.geometry.Bounds;
import javafx.scene.paint.Material;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Translate;

public class Hole extends Cylinder {
	public Hole ( double radius, double height, Material material, Translate position ) {
		super ( radius, height );
		
		super.setMaterial ( material );
		
		super.getTransforms ( ).add ( position );
	}
	
	public boolean handleCollision ( Sphere ball ) {
		Bounds ballBounds = ball.getBoundsInParent ( );
		
		double ballX = ballBounds.getCenterX ( );
		double ballZ = ballBounds.getCenterZ ( );
		
		Bounds holeBounds = super.getBoundsInParent ( );
		double holeX      = holeBounds.getCenterX ( );
		double holeZ      = holeBounds.getCenterZ ( );
		double holeRadius = super.getRadius ( );
		
		double dx = holeX - ballX;
		double dz = holeZ - ballZ;
		
		double distance = dx * dx + dz * dz;
		
		boolean isInHole = distance < holeRadius * holeRadius;
		
		return isInHole;
	}
	
}
