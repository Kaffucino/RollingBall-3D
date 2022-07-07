package com.example.rollingball;

import com.example.rollingball.arena.*;
import com.example.rollingball.timer.Timer;
import javafx.application.Application;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.*;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.paint.Material;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.*;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;

public class Main extends Application implements EventHandler {
    private static final double WINDOW_WIDTH = 800;
    private static final double WINDOW_HEIGHT = 800;

    private static final double PODIUM_WIDTH = 2000;
    private static final double PODIUM_HEIGHT = 10;
    private static final double PODIUM_DEPTH = 2000;

    private static final double CAMERA_FAR_CLIP = 100000;
    private static final double CAMERA_Z = -5000;
    private static final double CAMERA_X_ANGLE = -45;

    private static final double BALL_RADIUS = 50;

    private static final double DAMP = 0.999;
    private static final double ARENA_DAMP = 0.995;

    private static final int NUMBER_OF_TRIES = 5;
    private static final double MAX_ANGLE_OFFSET = 30;
    private static double MAX_ACCELERATION = 400;
    private static final int POINT_FOR_HOLE = 5;
    private static final int POINT_FOR_FAKE_HOLE = 2;
    private static final int GAME_TIME = 90; //u sekundama

    private static final int POINT_FOT_COIN = 5;

    private static final double HOLE_RADIUS = 2 * Main.BALL_RADIUS;
    private static final double HOLE_HEIGHT = PODIUM_HEIGHT;

    private Group root;
    private Group root_tries;
    private Group game_root;

    private Ball ball;
    private Arena arena;
    private Hole hole;
    private Scene scene;
    private SubScene game_scene;


    private Camera default_camera;
    private Camera bird_view_camera;
    private Translate zoom;
    private Rotate xSphere;
    private Rotate ySphere;
    private double previousX;
    private double previousY;


    private ArrayList<Fence> obstacle_list;
    private ArrayList<Circle> tries_list;
    private int remaining_tries;
    private Text end_game_text;

    private int score;
    private Text score_text;

    private ArrayList<Coin> coin_list;

    private Lamp lamp;
    private PointLight lamp_light;

    private ArrayList<Cylinder> cylinder_obstacle_list;

    private SubScene minimap;
    private Group minimap_root;
    private Line speed_vector;

    private ArrayList<Hole> fake_holes_list;

    private int time_remaining;
    private Text time_remaining_text;

    private double time;

    private ArrayList<Special_Obstacle> special_obstacles_list;


    private String ball_choice;
    private String field_choice;

    @Override
    public void start(Stage stage) throws IOException {
        //  this.root = new Group();

        game_root = new Group();
        game_scene = new SubScene(
                this.game_root,
                Main.WINDOW_WIDTH,
                Main.WINDOW_HEIGHT,
                true,
                SceneAntialiasing.BALANCED
        );


        Box podium = new Box(
                Main.PODIUM_WIDTH,
                Main.PODIUM_HEIGHT,
                Main.PODIUM_DEPTH
        );
        podium.setMaterial(new PhongMaterial(Color.BLUE));

        default_camera = new PerspectiveCamera(true);
        default_camera.setFarClip(Main.CAMERA_FAR_CLIP);
        this.zoom = new Translate(0, 0, CAMERA_Z);
        this.xSphere = new Rotate(Main.CAMERA_X_ANGLE, Rotate.X_AXIS);
        this.ySphere = new Rotate(0, Rotate.Y_AXIS);

        default_camera.getTransforms().addAll(
                this.ySphere,
                this.xSphere,
                this.zoom
        );
        game_scene.setCamera(default_camera);


        this.arena = new Arena();
        this.arena.getChildren().add(podium);


        this.game_root.getChildren().add(this.arena);

        Timer timer = new Timer(

                deltaSeconds -> {
                    this.arena.update(ARENA_DAMP);
                    update_speed_vector();

                    time += deltaSeconds;
                    if (time >= 1) {
                        update_time();
                        time = 0;
                    }

                    if (Main.this.ball != null && time_remaining > 0) {
                        boolean outOfArena = Main.this.ball.update(
                                deltaSeconds,
                                Main.PODIUM_DEPTH / 2,
                                -Main.PODIUM_DEPTH / 2,
                                -Main.PODIUM_WIDTH / 2,
                                Main.PODIUM_WIDTH / 2,
                                this.arena.getXAngle(),
                                this.arena.getZAngle(),
                                Main.MAX_ANGLE_OFFSET,
                                Main.MAX_ACCELERATION,
                                Main.DAMP,
                                obstacle_list,
                                cylinder_obstacle_list,
                                special_obstacles_list
                        );


                        boolean isInHole = this.hole.handleCollision(this.ball);

                        boolean isInFakeHole = false;
                        for (int i = 0; i < fake_holes_list.size(); ++i)
                            if (fake_holes_list.get(i).handleCollision(ball)) {
                                isInFakeHole = true;
                                break;
                            }


                        //coin collison

                        for (int i = 0; i < coin_list.size(); ++i) {


                            if (coin_list.get(i).handle_collision(ball, this.arena)) {
                                score += POINT_FOT_COIN;
                                score_text.setText("" + score);
                                this.arena.getChildren().remove(coin_list.get(i));
                            }

                        }


                        if (outOfArena || isInHole || isInFakeHole) {
                            this.arena.getChildren().remove(this.ball);
                            Main.this.ball = null;

                            if (time_remaining == 0) {
                                add_end_game_text();
                                return;
                            }

                            if (isInHole) {
                                score += POINT_FOR_HOLE;
                                score_text.setText("" + score);
                            }

                            if (isInFakeHole) {
                                score -= POINT_FOR_FAKE_HOLE;
                                score_text.setText("" + score);
                            }

                            if (remaining_tries != 0) {
                                remaining_tries--;
                                this.root_tries.getChildren().remove(this.tries_list.remove(remaining_tries));
                                reset();
                            } else { // kraj igre
                                add_end_game_text();

                            }


                        }
                    } else if (time_remaining <= 0)//isteklo vreme
                    {
                        add_end_game_text();

                    }
                }
        );
        //    timer.start();


        //Ball scene
        Group ball_root = new Group();
        Scene ball_scene = new Scene(ball_root,
                Main.WINDOW_WIDTH,
                Main.WINDOW_HEIGHT, Color.BLACK);

        Text text = new Text("CHOOSE A BALL ");

        Text title = new Text("Rolling Ball 3D");
        title.getTransforms().add(new Translate(WINDOW_WIDTH / 5, WINDOW_HEIGHT / 5));
        title.setFont(Font.font(80));
        title.setFill(Color.WHITE);

        text.setFill(Color.RED);
        text.setFont(Font.font(50));
        text.getTransforms().add(new Translate(WINDOW_WIDTH / 4, WINDOW_HEIGHT / 2));


        Text regular_text = new Text("Regular");
        regular_text.setFill(Color.RED);
        regular_text.getTransforms().add(new Translate(WINDOW_WIDTH / 5, WINDOW_HEIGHT * 2 / 3));
        regular_text.setFont(Font.font(30));

        Text fast_text = new Text("Fast");
        fast_text.setFill(Color.GOLD);
        fast_text.getTransforms().add(new Translate(WINDOW_WIDTH / 2 - 40, WINDOW_HEIGHT * 2 / 3));
        fast_text.setFont(Font.font(30));


        Text slow_text = new Text("Slow");
        slow_text.setFill(Color.WHITE);
        slow_text.getTransforms().add(new Translate(WINDOW_WIDTH * 2 / 3, WINDOW_HEIGHT * 2 / 3));
        slow_text.setFont(Font.font(30));

        ball_root.getChildren().addAll(title, text, regular_text, fast_text, slow_text);

        regular_text.setOnMouseEntered(mouseDragEvent -> {
            regular_text.setFill(Color.GREEN);
        });
        regular_text.setOnMouseExited(mouseDragEvent -> {
            regular_text.setFill(Color.RED);

        });

        fast_text.setOnMouseEntered(mouseDragEvent -> {
            fast_text.setFill(Color.GREEN);
        });
        fast_text.setOnMouseExited(mouseDragEvent -> {
            fast_text.setFill(Color.GOLD);

        });

        slow_text.setOnMouseEntered(mouseDragEvent -> {
            slow_text.setFill(Color.GREEN);
        });
        slow_text.setOnMouseExited(mouseDragEvent -> {
            slow_text.setFill(Color.WHITE);

        });

        //Field scene
        Group field_root = new Group();
        Scene field_scene = new Scene(field_root,
                Main.WINDOW_WIDTH,
                Main.WINDOW_HEIGHT, Color.BLACK);

        Text field_text = new Text("CHOOSE A FIELD ");

        Text field_title = new Text("Rolling Ball 3D");
        field_title.getTransforms().add(new Translate(WINDOW_WIDTH / 5, WINDOW_HEIGHT / 5));
        field_title.setFont(Font.font(80));
        field_title.setFill(Color.WHITE);

        field_text.setFill(Color.RED);
        field_text.setFont(Font.font(50));
        field_text.getTransforms().add(new Translate(WINDOW_WIDTH / 4, WINDOW_HEIGHT / 2));


        Text medium_text = new Text("Medium");
        medium_text.setFill(Color.YELLOW);
        medium_text.getTransforms().add(new Translate(WINDOW_WIDTH / 2 - 60, WINDOW_HEIGHT * 2 / 3));

        medium_text.setFont(Font.font(30));

        Text easy_text = new Text("Easy");
        easy_text.getTransforms().add(new Translate(WINDOW_WIDTH / 5 + 20, WINDOW_HEIGHT * 2 / 3));

        easy_text.setFill(Color.GREEN);
        easy_text.setFont(Font.font(30));


        Text hard_text = new Text("Hard");
        hard_text.setFill(Color.RED);
        hard_text.getTransforms().add(new Translate(WINDOW_WIDTH * 2 / 3, WINDOW_HEIGHT * 2 / 3));
        hard_text.setFont(Font.font(30));

        field_root.getChildren().addAll(field_text, field_title, easy_text, medium_text, hard_text);

        medium_text.setOnMouseEntered(mouseDragEvent -> {
            medium_text.setFill(Color.WHITE);
        });
        medium_text.setOnMouseExited(mouseDragEvent -> {
            medium_text.setFill(Color.YELLOW);

        });

        easy_text.setOnMouseEntered(mouseDragEvent -> {
            easy_text.setFill(Color.WHITE);
        });
        easy_text.setOnMouseExited(mouseDragEvent -> {
            easy_text.setFill(Color.GREEN);

        });

        hard_text.setOnMouseEntered(mouseDragEvent -> {
            hard_text.setFill(Color.WHITE);
        });
        hard_text.setOnMouseExited(mouseDragEvent -> {
            hard_text.setFill(Color.RED);

        });


        this.root = new Group(game_scene);

        this.scene = new Scene(this.root,
                Main.WINDOW_WIDTH,
                Main.WINDOW_HEIGHT,
                true,
                SceneAntialiasing.BALANCED);

        scene.addEventHandler(KeyEvent.ANY, event -> this.arena.handleKeyEvent(event, Main.MAX_ANGLE_OFFSET));


        Image background_image = new Image("background.jpg");
        ImagePattern backgorund_texture = new ImagePattern(background_image);

        scene.setFill(backgorund_texture);
        scene.addEventHandler(KeyEvent.ANY, this);
        scene.addEventHandler(ScrollEvent.ANY, this);
        scene.addEventHandler(MouseEvent.ANY, this);


        regular_text.setOnMouseClicked(mouseEvent -> {
            ball_choice = "regular";

            stage.setScene(field_scene);

        });

        fast_text.setOnMouseClicked(mouseEvent -> {
            ball_choice = "fast";


            stage.setScene(field_scene);

        });

        slow_text.setOnMouseClicked(mouseEvent -> {
            ball_choice = "slow";


            stage.setScene(field_scene);

        });

        medium_text.setOnMouseClicked(mouseEvent -> {
            field_choice = "medium";
            setField(stage, scene);
            setBall(ball_choice, scene);
            timer.start();

        });

        easy_text.setOnMouseClicked(mouseEvent -> {
            field_choice = "easy";
            setField(stage, scene);
            setBall(ball_choice, scene);

            timer.start();

        });

        hard_text.setOnMouseClicked(mouseEvent -> {
            field_choice = "hard";
            setField(stage, scene);
            setBall(ball_choice, scene);

            timer.start();

        });


        stage.setTitle("Rolling Ball");
        stage.setScene(ball_scene);
        stage.setResizable(false);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }


    public void setField(Stage stage, Scene scene) {


        stage.setScene(scene);
        add_tries();
        add_score();
        add_coins();
        add_lamp();
        add_minimap();
        add_holes();
        add_obstacles();
        add_cylinder_obstacles();
        add_time_remaining();
        add_special_obstacles();

    }


    public void setBall(String ballType, Scene scene) {

        Translate ballPosition = new Translate();

        if (field_choice.equals("medium")) {
            ballPosition = new Translate(
                    -(Main.PODIUM_WIDTH / 2 - 2 * Main.BALL_RADIUS),
                    -(Main.BALL_RADIUS + Main.PODIUM_HEIGHT / 2),
                    Main.PODIUM_DEPTH / 2 - 2 * Main.BALL_RADIUS
            );
        } else if (field_choice.equals("easy")) {

            ballPosition = new Translate(
                    0,
                    -(Main.BALL_RADIUS + Main.PODIUM_HEIGHT / 2),
                    Main.PODIUM_DEPTH / 2 - 2 * Main.BALL_RADIUS
            );


        } else if (field_choice.equals("hard")) {

            ballPosition = new Translate(
                    0,
                    -(Main.BALL_RADIUS + Main.PODIUM_HEIGHT / 2),
                    0
            );
        }

        if (ballType.equals("regular")) {
            Material ballMaterial = new PhongMaterial(Color.RED);


            this.ball = new Ball(Main.BALL_RADIUS, ballMaterial, ballPosition);

            this.bird_view_camera = new PerspectiveCamera(true);
            bird_view_camera.setFarClip(CAMERA_FAR_CLIP);
            bird_view_camera.getTransforms().addAll(
                    new Translate(0, -1000, 0),
                    ballPosition,
                    new Rotate(-90, Rotate.X_AXIS)
            );


            this.arena.getChildren().add(ball);
        } else if (ballType.equals("fast")) {
            MAX_ACCELERATION = 3.5 * MAX_ACCELERATION;
            Material ballMaterial = new PhongMaterial(Color.GOLD);

            this.ball = new Ball(Main.BALL_RADIUS, ballMaterial, ballPosition);

            this.bird_view_camera = new PerspectiveCamera(true);
            bird_view_camera.setFarClip(CAMERA_FAR_CLIP);
            bird_view_camera.getTransforms().addAll(
                    new Translate(0, -1000, 0),
                    ballPosition,
                    new Rotate(-90, Rotate.X_AXIS)
            );

            this.arena.getChildren().add(ball);
        } else if (ballType.equals("slow")) {
            MAX_ACCELERATION = 0.25 * MAX_ACCELERATION;
            Material ballMaterial = new PhongMaterial(Color.WHITE);

            this.ball = new Ball(Main.BALL_RADIUS, ballMaterial, ballPosition);

            this.bird_view_camera = new PerspectiveCamera(true);
            bird_view_camera.setFarClip(CAMERA_FAR_CLIP);
            bird_view_camera.getTransforms().addAll(
                    new Translate(0, -1000, 0),
                    ballPosition,
                    new Rotate(-90, Rotate.X_AXIS)
            );

            this.arena.getChildren().add(ball);

        }


    }


    public void update_time() {

        if (time_remaining > 0) {

            time_remaining--;
            time_remaining_text.setText("" + time_remaining);

        }


    }

    public void add_cylinder_obstacles() {

        cylinder_obstacle_list = new ArrayList<>();
        Image obstacle_image = new Image("obstacle.jpg");
        PhongMaterial material = new PhongMaterial();
        material.setDiffuseMap(obstacle_image);

        if (field_choice.equals("medium")) {
            for (int i = 0; i < 4; ++i) {

                Cylinder obs = new Cylinder(HOLE_RADIUS / 2, 200);
                obs.setMaterial(material);

                this.arena.getChildren().add(obs);

                this.cylinder_obstacle_list.add(obs);

                if (i == 0)
                    obs.getTransforms().add(
                            new Translate(-450, -100, 450)
                    );
                else if (i == 1)
                    obs.getTransforms().add(
                            new Translate(450, -100, 450)
                    );
                else if (i == 2)
                    obs.getTransforms().add(
                            new Translate(-450, -100, -450)
                    );
                else
                    obs.getTransforms().add(
                            new Translate(450, -100, -450)
                    );

            }
        }

        if (field_choice.equals("easy")) {
            for (int i = 0; i < 4; ++i) {

                Cylinder obs = new Cylinder(HOLE_RADIUS / 2, 200);
                obs.setMaterial(material);

                this.arena.getChildren().add(obs);

                this.cylinder_obstacle_list.add(obs);

                if (i == 0)
                    obs.getTransforms().add(
                            new Translate(-900, -100, 900)
                    );
                else if (i == 1)
                    obs.getTransforms().add(
                            new Translate(900, -100, 900)
                    );
                else if (i == 2)
                    obs.getTransforms().add(
                            new Translate(900, -100, -900)
                    );
                else
                    obs.getTransforms().add(
                            new Translate(-900, -100, -900)
                    );

            }
        }

        if (field_choice.equals("hard")) {
            for (int i = 0; i < 3; ++i) {

                Cylinder obs = new Cylinder(HOLE_RADIUS / 2, 200);
                obs.setMaterial(material);

                this.arena.getChildren().add(obs);

                this.cylinder_obstacle_list.add(obs);

                if (i == 0)
                    obs.getTransforms().add(
                            new Translate(-600, -100, 0)
                    );
                else if (i == 1)
                    obs.getTransforms().add(
                            new Translate(-800, -100, -200)
                    );
                else if (i == 2)
                    obs.getTransforms().add(
                            new Translate(-800, -100, 200)
                    );


            }
        }


    }

    public void update_speed_vector() {

        double width = minimap.getWidth();
        double step = width / MAX_ANGLE_OFFSET;

        speed_vector.setEndY(arena.getXAngle() * (step / 2));

        speed_vector.setEndX(arena.getZAngle() * (step / 2));


    }

    public void add_minimap() {

        minimap_root = new Group();
        minimap = new SubScene(minimap_root, 150, 150, true, SceneAntialiasing.BALANCED);
        minimap.setDepthTest(DepthTest.DISABLE);

        Rectangle rectangle = new Rectangle(2, 2, 145, 145);
        rectangle.setFill(Color.GREEN);
        minimap_root.getChildren().add(rectangle);
        minimap.setFill(Color.RED);

        minimap.getTransforms().add(
                new Translate(0, WINDOW_HEIGHT - 150)
        );


        this.speed_vector = new Line();
        this.speed_vector.setStroke(Color.RED);
        this.speed_vector.getTransforms().add(
                new Translate(75, 75)
        );
        speed_vector.setStrokeWidth(2);
        minimap_root.getChildren().add(speed_vector);


        root.getChildren().add(minimap);

    }

    public void add_obstacles() {

        obstacle_list = new ArrayList<>();

        Image obstacle_image = new Image("obstacle.jpg");
        PhongMaterial obstacle_material = new PhongMaterial();
        obstacle_material.setDiffuseMap(obstacle_image);
        for (int i = 0; i < 4; ++i) {
            Fence obst = new Fence(PODIUM_WIDTH / 2, 150, 10, obstacle_material);
            if (i == 0) {
                Translate position = new Translate(PODIUM_WIDTH / 15 - 100, -80, PODIUM_DEPTH - 1000);
                obst.getTransforms().add(position);
            } else if (i == 1) {
                Translate position = new Translate(PODIUM_WIDTH / 15 - 1130, -80, PODIUM_DEPTH - 2000);
                Rotate rotate = new Rotate(-90, Rotate.Y_AXIS);
                obst.getTransforms().addAll(position, rotate);

            } else if (i == 2) {
                Translate position = new Translate(PODIUM_WIDTH / 2, -80, PODIUM_DEPTH - 2000);
                Rotate rotate = new Rotate(-90, Rotate.Y_AXIS);
                obst.getTransforms().addAll(position, rotate);

            } else {
                Translate position = new Translate(PODIUM_WIDTH / 15 - 100, -80, -PODIUM_DEPTH + 1000);
                obst.getTransforms().addAll(position);
            }
            obstacle_list.add(obst);

            this.arena.getChildren().add(obst);
        }


    }

    public void add_end_game_text() {
        Group end_game_group = new Group();

        SubScene end_game_scene = new SubScene(end_game_group, 100, 30, true, SceneAntialiasing.BALANCED);


        end_game_scene.getTransforms().add(
                new Translate(WINDOW_WIDTH / 3 + 100, 100)
        );
        end_game_scene.setDepthTest(DepthTest.DISABLE);

        this.end_game_text = new Text("Kraj Igre");
        this.end_game_text.setFill(Color.RED);
        this.end_game_text.setFont(Font.font(20));
        this.end_game_text.getTransforms().add(
                new Translate(10, 20)
        );

        end_game_group.getChildren().add(end_game_text);


        this.root.getChildren().add(end_game_scene);


    }

    public void reset() {
        Material ballMaterial = new PhongMaterial();
        Translate ballPosition = new Translate();

        if (field_choice.equals("medium")) {
            ballPosition = new Translate(
                    -(Main.PODIUM_WIDTH / 2 - 2 * Main.BALL_RADIUS),
                    -(Main.BALL_RADIUS + Main.PODIUM_HEIGHT / 2),
                    Main.PODIUM_DEPTH / 2 - 2 * Main.BALL_RADIUS
            );
        } else if (field_choice.equals("easy")) {
            ballPosition = new Translate(
                    0,
                    -(Main.BALL_RADIUS + Main.PODIUM_HEIGHT / 2),
                    Main.PODIUM_DEPTH / 2 - 2 * Main.BALL_RADIUS
            );
        } else if (field_choice.equals("hard")) {
            ballPosition = new Translate(
                    0,
                    -(Main.BALL_RADIUS + Main.PODIUM_HEIGHT / 2),
                    0
            );
        }


        if (ball_choice.equals("regular")) {
            ballMaterial = new PhongMaterial(Color.RED);
        } else if (ball_choice.equals("fast"))
            ballMaterial = new PhongMaterial(Color.GOLD);
        else if (ball_choice.equals("slow"))
            ballMaterial = new PhongMaterial(Color.WHITE);


        this.arena.set_xAngle(0);
        this.arena.set_zAngle(0);
        this.bird_view_camera = new PerspectiveCamera(true);
        bird_view_camera.setFarClip(CAMERA_FAR_CLIP);

        bird_view_camera.getTransforms().addAll(
                new Translate(0, -1000, 0),
                ballPosition,
                new Rotate(-90, Rotate.X_AXIS)
        );

        this.ball = new Ball(Main.BALL_RADIUS, ballMaterial, ballPosition);
        this.arena.getChildren().add(this.ball);


    }

    public void add_coins() {
        PhongMaterial material = new PhongMaterial(Color.GOLD);
        coin_list = new ArrayList<>();

        if (field_choice.equals("medium")) {

            for (int i = 0; i < 4; ++i) {
                Translate position = new Translate();
                if (i == 0)
                    position = new Translate(-PODIUM_WIDTH / 2 + 1050, 0, PODIUM_DEPTH / 8 + 200);
                else if (i == 1) position = new Translate(-PODIUM_WIDTH / 2 + 1500, 0, PODIUM_DEPTH / 8 - 300);

                else if (i == 2) position = new Translate(-PODIUM_WIDTH / 2 + 1050, 0, PODIUM_DEPTH / 8 - 700);

                else
                    position = new Translate(-PODIUM_WIDTH / 2 + 600, 0, PODIUM_DEPTH / 8 - 300);

                Coin coin = new Coin(HOLE_RADIUS / 2, 10, material, position);

                this.coin_list.add(coin);
                this.arena.getChildren().add(coin);
            }

        }


        if (field_choice.equals("easy")) {

            for (int i = 0; i < 6; ++i) {
                Translate position = new Translate();
                if (i == 0)
                    position = new Translate(-PODIUM_WIDTH / 2 + 1050, 0, PODIUM_DEPTH / 8 + 200);
                else if (i == 1) position = new Translate(-PODIUM_WIDTH / 2 + 1500, 0, PODIUM_DEPTH / 8 + 200);

                else if (i == 2) position = new Translate(-PODIUM_WIDTH / 2 + 600, 0, PODIUM_DEPTH / 8 + 200);

                else if (i == 3)
                    position = new Translate(-PODIUM_WIDTH / 2 + 1050, 0, PODIUM_DEPTH / 8 - 700);

                else if (i == 4)
                    position = new Translate(-PODIUM_WIDTH / 2 + 1500, 0, PODIUM_DEPTH / 8 - 700);
                else
                    position = new Translate(-PODIUM_WIDTH / 2 + 600, 0, PODIUM_DEPTH / 8 - 700);


                Coin coin = new Coin(HOLE_RADIUS / 2, 10, material, position);

                this.coin_list.add(coin);
                this.arena.getChildren().add(coin);
            }

        }
        if (field_choice.equals("hard")) {

            for (int i = 0; i < 8; ++i) {
                Translate position = new Translate();
                if (i == 0)
                    position = new Translate(-200, 0, -200);
                else if (i == 1) position = new Translate(200, 0, -200);

                else if (i == 2) position = new Translate(-200, 0, 200);

                else if (i == 3)
                    position = new Translate(200, 0, 200);

                else if (i == 4)
                    position = new Translate(650, 0, 650);
                else if (i == 5)
                    position = new Translate(-650, 0, 650);
                else if (i == 6)
                    position = new Translate(650, 0, -650);
                else if (i == 7)
                    position = new Translate(-650, 0, -650);

                Coin coin = new Coin(HOLE_RADIUS / 2, 10, material, position);

                this.coin_list.add(coin);
                this.arena.getChildren().add(coin);
            }

        }

    }

    public void add_score() {
        Group score_group = new Group();
        SubScene subScene = new SubScene(score_group, 50, 50, true, SceneAntialiasing.BALANCED);
        subScene.setDepthTest(DepthTest.DISABLE);
        //  subScene.setFill(scene.getFill());

        subScene.getTransforms().add(
                new Translate(WINDOW_WIDTH - 50, 0)
        );


        score = 0;
        score_text = new Text("0");
        score_text.setFill(Color.RED);
        score_text.setFont(Font.font(30));
        score_text.getTransforms().add(
                new Translate(15, 35)
        );

        score_group.getChildren().add(score_text);


        root.getChildren().add(subScene);


    }

    public void add_lamp() {


        PhongMaterial material = new PhongMaterial(Color.GRAY);
        Image illumi_image = new Image("selfIllumination.png");
        material.setSelfIlluminationMap(illumi_image);
        Translate position = new Translate(0, -1300, 0);
        this.lamp = new Lamp(100, 100, 100, material, position);
        this.game_root.getChildren().add(lamp);

        lamp_light = new PointLight(Color.WHITE);


        lamp_light.getTransforms().add(position);


    }

    public void add_tries() {
        root_tries = new Group();

        SubScene tries_sub = new SubScene(root_tries, 130, 30, true, SceneAntialiasing.BALANCED);

        root_tries.setDepthTest(DepthTest.DISABLE);
        tries_sub.setDepthTest(DepthTest.DISABLE);
        //  tries_sub.setFill(scene.getFill());

        this.tries_list = new ArrayList<>();

        remaining_tries = NUMBER_OF_TRIES;

        double x_value = 15;
        for (int i = 0; i < NUMBER_OF_TRIES; ++i) {


            Circle try_ball = new Circle(9);
            try_ball.setFill(Color.RED);


            Translate position = new Translate(x_value, 20);
            x_value += 25;
            this.tries_list.add(try_ball);
            try_ball.getTransforms().add(position);
            root_tries.getChildren().add(try_ball);

        }
        this.root.getChildren().add(tries_sub);

    }

    public void add_holes() {

        this.fake_holes_list = new ArrayList<>();
        double x = -1;
        double z = -1;
        if (field_choice.equals("medium")) {
            x = (Main.PODIUM_WIDTH / 2 - 2 * Main.HOLE_RADIUS);
            z = -(Main.PODIUM_DEPTH / 2 - 2 * Main.HOLE_RADIUS);
        } else if (field_choice.equals("easy")) {

            x = 0;
            z = 0;
        } else if (field_choice.equals("hard")) {
            x = -800;
            z = 0;
        }


        Translate holePosition = new Translate(x, -30, z);
        Material holeMaterial = new PhongMaterial(Color.YELLOW);

        this.hole = new Hole(
                Main.HOLE_RADIUS,
                Main.HOLE_HEIGHT,
                holeMaterial,
                holePosition
        );
        this.arena.getChildren().addAll(this.hole);

        if (field_choice.equals("medium")) {
            for (int i = 0; i < 2; ++i) {
                if (i == 0) {
                    x = (Main.PODIUM_WIDTH / 2 - 2 * Main.HOLE_RADIUS);
                    z = (Main.PODIUM_DEPTH / 2 - 2 * Main.HOLE_RADIUS);
                }
                if (i == 1) {
                    x = -(Main.PODIUM_WIDTH / 2 - 2 * Main.HOLE_RADIUS);
                    z = -(Main.PODIUM_DEPTH / 2 - 2 * Main.HOLE_RADIUS);
                }


                Material fake_holeMaterial = new PhongMaterial(Color.BLACK);
                Translate fake_hole_position = new Translate(x, -30, z);
                Hole fake_hole = new Hole(Main.HOLE_RADIUS, Main.HOLE_HEIGHT, fake_holeMaterial, fake_hole_position);
                fake_holes_list.add(fake_hole);
                this.arena.getChildren().add(fake_hole);
            }
        } else if (field_choice.equals("easy")) {

            x = 0;
            z = -(Main.PODIUM_DEPTH / 2 - 2 * Main.HOLE_RADIUS);
            Material fake_holeMaterial = new PhongMaterial(Color.BLACK);
            Translate fake_hole_position = new Translate(x, -30, z);
            Hole fake_hole = new Hole(Main.HOLE_RADIUS, Main.HOLE_HEIGHT, fake_holeMaterial, fake_hole_position);
            fake_holes_list.add(fake_hole);
            this.arena.getChildren().add(fake_hole);


        } else if (field_choice.equals("hard")) {
            for (int i = 0; i < 8; ++i) {
                if (i == 0) {
                    x = -300;
                    z = 0;
                }
                if (i == 1) {
                    x = 300;
                    z = 0;
                }

                if (i == 2) {
                    x = 0;
                    z = 300;
                }

                if (i == 3) {
                    x = 0;
                    z = -300;
                }
                if (i == 4) {
                    x = -800;
                    z = -800;
                }
                if (i == 5) {
                    x = 800;
                    z = -800;
                }

                if (i == 6) {
                    x = -800;
                    z = 800;
                }
                if (i == 7) {
                    x = 800;
                    z = 800;
                }

                Material fake_holeMaterial = new PhongMaterial(Color.BLACK);
                Translate fake_hole_position = new Translate(x, -30, z);
                Hole fake_hole = new Hole(Main.HOLE_RADIUS, Main.HOLE_HEIGHT, fake_holeMaterial, fake_hole_position);
                fake_holes_list.add(fake_hole);
                this.arena.getChildren().add(fake_hole);
            }

        }

    }

    public void add_time_remaining() {
        Group time_group = new Group();
        SubScene subScene = new SubScene(time_group, 50, 50, true, SceneAntialiasing.BALANCED);
        subScene.setDepthTest(DepthTest.DISABLE);
        //    subScene.setFill(scene.getFill());

        subScene.getTransforms().add(
                new Translate(WINDOW_WIDTH / 2, WINDOW_HEIGHT - 100)
        );


        time_remaining = GAME_TIME;
        time_remaining_text = new Text("" + GAME_TIME);
        time_remaining_text.setFill(Color.RED);
        time_remaining_text.setFont(Font.font(30));
        time_remaining_text.getTransforms().add(
                new Translate(10, 35)
        );

        time_group.getChildren().add(time_remaining_text);


        root.getChildren().add(subScene);
    }

    public void add_special_obstacles() {

        special_obstacles_list = new ArrayList<>();
        double x = -1;
        double z = -1;
        PhongMaterial material = new PhongMaterial(Color.GREEN);
        if (field_choice.equals("medium")) {
            x = 0;
            z = 0;
            Translate poistion = new Translate(x, -50, z);
            Special_Obstacle obs = new Special_Obstacle(HOLE_RADIUS, HOLE_RADIUS, HOLE_RADIUS, material, poistion, 2.5);
            this.special_obstacles_list.add(obs);
            arena.getChildren().add(obs);
        } else if (field_choice.equals("easy")) {

            for (int i = 0; i < 2; ++i) {
                if (i == 0) {
                    x = -800;
                    z = 0;
                } else {
                    x = 800;
                    z = 0;
                }

                Translate poistion = new Translate(x, -50, z);
                Special_Obstacle obs = new Special_Obstacle(HOLE_RADIUS, HOLE_RADIUS, HOLE_RADIUS, material, poistion, 2.5);
                this.special_obstacles_list.add(obs);
                arena.getChildren().add(obs);
            }


        } else if (field_choice.equals("hard")) {

            for (int i = 0; i < 3; ++i) {
                if (i == 0) {
                    x = 0;
                    z = -900;
                } else if (i == 1) {
                    x = 0;
                    z = 900;
                } else if (i == 2) {

                    x = 900;
                    z = 0;
                }

                Translate poistion = new Translate(x, -50, z);
                Special_Obstacle obs = new Special_Obstacle(HOLE_RADIUS, HOLE_RADIUS, HOLE_RADIUS, material, poistion, 2.5);
                this.special_obstacles_list.add(obs);
                arena.getChildren().add(obs);
            }

        }


    }

    public void HandleKey(KeyEvent event) {


        if (event.getCode().equals(KeyCode.DIGIT1) || event.getCode().equals(KeyCode.NUMPAD1)) {
            this.game_scene.setCamera(default_camera);
        }

        if (event.getCode().equals(KeyCode.DIGIT2) || event.getCode().equals(KeyCode.NUMPAD2)) {
            this.game_scene.setCamera(bird_view_camera);

        }

        if ((event.getCode().equals(KeyCode.DIGIT0) || event.getCode().equals(KeyCode.NUMPAD0)) && event.getEventType().equals(KeyEvent.KEY_PRESSED)) {


            if (lamp.isLightOn()) {

                lamp.turnOffLight();
                this.arena.getChildren().remove(lamp_light);
            } else {
                lamp.turnOnLight();
                Translate position = new Translate(0, -1300, 0);
                lamp_light = new PointLight(Color.WHITE);

                lamp_light.getTransforms().add(position);
                arena.getChildren().add(lamp_light);
            }


        }


    }

    public void HandleMouse(MouseEvent event) {
        if (event.getEventType().equals(MouseEvent.MOUSE_DRAGGED)) {

            if (previousX == 0)
                previousX = event.getSceneX();
            if (previousY == 0)
                previousY = event.getSceneY();

            double x = event.getSceneX();
            double y = event.getSceneY();

            double dx = x - previousX;
            double dy = y - previousY;

            int singX = dx > 0 ? 1 : -1;
            int singY = dy > 0 ? 1 : -1;
            if (event.isPrimaryButtonDown() || (event.isSecondaryButtonDown())) {
                this.ySphere.setAngle(this.ySphere.getAngle() + singX * 0.2);

                if (this.xSphere.getAngle() - singY * 0.2 > 0)
                    this.xSphere.setAngle(0);
                else if (this.xSphere.getAngle() - singY * 0.2 < -90)
                    this.xSphere.setAngle(-90);
                else
                    this.xSphere.setAngle(this.xSphere.getAngle() - singY * 0.2);
            }

        }


    }

    public void ScrollHandle(ScrollEvent event) {

        if (event.getDeltaY() > 0)
            this.zoom.setZ(this.zoom.getZ() + 15);
        else
            this.zoom.setZ(this.zoom.getZ() - 15);

    }


    @Override
    public void handle(Event event) {

        if (event instanceof KeyEvent)
            HandleKey((KeyEvent) event);

        if (event instanceof MouseEvent)
            HandleMouse((MouseEvent) event);

        if (event instanceof ScrollEvent)
            ScrollHandle((ScrollEvent) event);


    }
}