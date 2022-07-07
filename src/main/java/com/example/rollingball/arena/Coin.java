package com.example.rollingball.arena;

import javafx.animation.*;
import javafx.geometry.Bounds;
import javafx.scene.Group;
import javafx.scene.paint.Material;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import javafx.util.Duration;

public class Coin extends Cylinder {

    public Coin(double radius, double height, Material material, Translate position) {
        super(radius, height);
        super.setMaterial(material);

        Rotate rotate_animation = new Rotate(0, Rotate.Y_AXIS);
        Translate move_animation = new Translate();

        super.getTransforms().addAll(new Translate(0, -80, 0), position, move_animation, rotate_animation, new Rotate(90, Rotate.X_AXIS));

        Timeline rotate_timeline = new Timeline(new KeyFrame(Duration.ZERO, new KeyValue(rotate_animation.angleProperty(), 0, Interpolator.LINEAR)),
                new KeyFrame(Duration.seconds(3), new KeyValue(rotate_animation.angleProperty(), 360, Interpolator.LINEAR)));

        Timeline move_timeline = new Timeline(new KeyFrame(Duration.ZERO, new KeyValue(move_animation.yProperty(), 0, Interpolator.LINEAR)),
                new KeyFrame(Duration.seconds(2), new KeyValue(move_animation.yProperty(), -70, Interpolator.LINEAR)));

        move_timeline.setCycleCount(Animation.INDEFINITE);
        move_timeline.setAutoReverse(true);
        move_timeline.play();

        rotate_timeline.setCycleCount(Animation.INDEFINITE);
        rotate_timeline.play();

    }


    public boolean handle_collision(Sphere ball, Group arena) {

        if(!arena.getChildren().contains(this))
            return false;

        Bounds ballBounds = ball.getBoundsInParent();

        double ballX = ballBounds.getCenterX();
        double ballZ = ballBounds.getCenterZ();

        Bounds coin_bounds = super.getBoundsInParent ( );
        double coinX      = coin_bounds.getCenterX ( );
        double coinZ      = coin_bounds.getCenterZ ( );
        double coinRadius = super.getRadius ( );

        double dx = coinX - ballX;
        double dz = coinZ - ballZ;

        double distance = dx * dx + dz * dz;

        boolean coin_hit = distance < 1.5 * coinRadius * coinRadius;

        return coin_hit;

    }

}
