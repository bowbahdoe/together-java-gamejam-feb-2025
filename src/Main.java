import hsa2.GraphicsConsole;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.ArrayList;

class Main {

    record Vec2(double x, double y) {
        Vec2 plus(Vec2 other) {
            return new Vec2(x + other.x, y + other.y);
        }

        Vec2 withX(double x) {
            return new Vec2(x, y);
        }

        Vec2 withY(double y) {
            return new Vec2(x, y);
        }

        Vec2 mul(double v) {
            return new Vec2(v * x, v * y);
        }

        Vec2 mulX(double v) {
            return new Vec2(v * x, y);
        }

        Vec2 mulY(double v) {
            return new Vec2(x, v * y);
        }
    }

    interface Movement {
        Vec2 update(Vec2 current);

        static Movement velocity(Vec2 vel) {
            return c -> c.plus(vel);
        }
    }
    abstract class Obstacle {
        Vec2 pos;
        Obstacle(Vec2 pos) {
            this.pos = pos;
        }

        abstract Obstacle tick();

        abstract void draw(GraphicsConsole g);
    }

    class FallingSquare extends Obstacle {
        final int ticks;

        FallingSquare(Vec2 pos) {
            this(0, pos);
        }

        FallingSquare(int ticks, Vec2 pos) {
            super(pos);
            this.ticks = ticks;
        }

        @Override
        FallingSquare tick() {
            return new FallingSquare(Math.min(ticks + 1, 10), pos.plus(new Vec2(0, 6)));
        }

        @Override
        void draw(GraphicsConsole gc) {
            gc.setColor(Color.RED);
            var w = (int) (30 * ticks / 10.0);
            var h = (int) (30 * ticks / 10.0);
            gc.fillRect(
                    center((int) pos.x, w),
                    center((int) pos.y, h),
                    w,
                    h
            );
        }
    }

    class SlidingSquare extends Obstacle {
        private final boolean forwards;

        SlidingSquare(Vec2 pos, boolean forwards) {
            super(pos);
            this.forwards = forwards;
        }

        @Override
        SlidingSquare tick() {
            return new SlidingSquare(pos.plus(new Vec2((forwards ? 1 : -1) * 6, 0)), forwards);
        }

        @Override
        void draw(GraphicsConsole gc) {
            gc.setColor(Color.ORANGE);
            gc.fillRect(
                    center((int) pos.x, 30),
                    center((int) pos.y, 30),
                    30,
                    30
            );
        }
    }


    ArrayList<Obstacle> obstacles = new ArrayList<>();

    final int WIDTH = 650;
    final int HEIGHT = 650;

    double groundY = 540;
    Vec2 playerPos = new Vec2(WIDTH / 2, groundY);
    Vec2 playerVelocity = new Vec2(0, 0);
    Vec2 gravity = new Vec2(0, 2);
    Rotation skyRotation = new Rotation(0);

    int center(int xOrY, int widthOrHeight) {
        return xOrY - (widthOrHeight / 2);
    }

    void drawBackground(GraphicsConsole gc) {
    /* for (int y = 0; y < HEIGHT; y+=50) {
        for (int x = 0; x < WIDTH; x++) {
            if (!(y > 530 && y < 570)) {
                gc.setColor(Color.CYAN);
                gc.fillRect(x, (int) (y + Math.sin(Math.toRadians(skyRotation.value) + x / (10 * Math.PI)) * 80), 6, 6);

                gc.setColor(Color.GREEN);
                gc.fillRect(x, (int) (y + Math.sin(Math.toRadians(skyRotation.value) + x / (10 * Math.PI)) * 40), 4, 4);

                gc.setColor(Color.BLUE);
                gc.fillRect(x, (int) (y + Math.sin(Math.toRadians(skyRotation.value) + x / (10 * Math.PI)) * 20), 2, 2);

            }

        }
    } */

        gc.setColor(Color.WHITE);
        gc.fillRect(0, 500, WIDTH, HEIGHT);

        for (int y = 0; y < HEIGHT; y+=10) {
            for (int x = 0; x < WIDTH; x++) {
                if (y > 530 && y < 570) {
                    gc.setColor(Color.GREEN);
                    gc.fillRect(x, (int) (y + Math.sin(x) * 10), 2, 2);
                }

            }
        }

        gc.setColor(Color.GRAY);
        gc.fillRect(0, 500, WIDTH, 30);


        gc.setColor(Color.LIGHT_GRAY);
        gc.fillRect(0, 570, WIDTH, (HEIGHT - 570));

        gc.setColor(Color.BLACK);
        gc.fillRect(0, 500, 650, 6);
    }

    int score = 0;

    Font font = new Font(Font.SANS_SERIF, Font.BOLD, 26);
    void drawScore(GraphicsConsole gc) {
        gc.setColor(Color.BLACK);
        gc.drawString("Score: " + score, 20, 640);
    }
    void drawPlayer(GraphicsConsole gc) {
        gc.setColor(Color.BLACK);
        gc.fillOval(
                center((int) playerPos.x, 30),
                center((int) playerPos.y, 30),
                30,
                30
        );


        gc.setColor(Color.WHITE);
        gc.fillArc(
                center((int) playerPos.x, 25),
                center((int) playerPos.y, 25),
                25,
                25,
                0 + currentRotation.value,
                -180
        );


        gc.setColor(Color.BLACK);
        gc.fillOval(
                center((int) playerPos.x, 15),
                center((int) playerPos.y, 15),
                15,
                15
        );

        gc.setColor(Color.RED);
        gc.fillOval(
                center((int) playerPos.x, 12),
                center((int) playerPos.y, 12),
                12,
                12
        );




    }

    boolean onGround() {
        return playerPos.y == groundY;
    }

    record Rotation(int value) {
        Rotation(int value) {
            this.value = value % 360;
        }

        Rotation next() {
            return new Rotation(value + 1);
        }

        Rotation next(int times) {
            while (times < 0) {
                times = 360 + times;
            }
            times = times % 360;
            var r = this;
            for (int i = 0; i < times; i++) {
                r = r.next();
            }
            return r;
        }
    }


    Rotation currentRotation = new Rotation(0);

    double horizontalAcceleration = 5;
    boolean movingL = false;
    boolean movingR = false;
    boolean movingLR() {
        return movingL || movingR;
    }
    boolean doubleJumped = false;

    final class ResettableInt {
        final int initial;
        int current;

        ResettableInt(int initial) {
            this.initial = initial;
            this.current = initial;
        }

        void reset() {
            this.current = initial;
        }
    }
    ResettableInt ticksUntilDoubleJump = new ResettableInt(8);
    boolean running = false;
    void main() {

        var gc = new GraphicsConsole(WIDTH, HEIGHT);
        gc.setFont(font);

        while (true) {
            Vec2 playerAcceleration = new Vec2(0, 0);
            if (gc.isKeyDown(KeyEvent.VK_SPACE) && onGround()) {
                playerAcceleration = playerAcceleration.plus(new Vec2(0, -20));
            }

            ticksUntilDoubleJump.current = Math.max(ticksUntilDoubleJump.current - 1, 0);
            if (gc.isKeyDown(KeyEvent.VK_SPACE) && !onGround() && !doubleJumped && ticksUntilDoubleJump.current == 0) {
                playerAcceleration = playerAcceleration.plus(new Vec2(0, -20));
                doubleJumped = true;
            }

            if (!onGround()) {
                playerAcceleration = playerAcceleration.plus(gravity);
            }


            movingR = gc.isKeyDown(KeyEvent.VK_D);
            movingL = gc.isKeyDown(KeyEvent.VK_A);


            playerVelocity = playerVelocity.plus(playerAcceleration);


            running = gc.isKeyDown(KeyEvent.VK_SHIFT);
            var horizontalVelocity = new Vec2(5, 0);
            Vec2 instantaneousPlayerVelocity = playerVelocity;
            if (movingL) {
                instantaneousPlayerVelocity = instantaneousPlayerVelocity.plus(horizontalVelocity.mul(-1));
            }
            if (movingR) {
                instantaneousPlayerVelocity = instantaneousPlayerVelocity.plus(horizontalVelocity);
            }

            if (running) {
                instantaneousPlayerVelocity = instantaneousPlayerVelocity.mulX(2).mulY(1.5);
            }

            playerPos = playerPos.plus(instantaneousPlayerVelocity);

            if (playerPos.y >= groundY) {
                playerPos = playerPos.withY(groundY);
                playerVelocity = playerVelocity.withY(0);
                doubleJumped = false;
                ticksUntilDoubleJump.reset();
            }


            if (Math.random() < 0.025) {
                obstacles.add(new FallingSquare(
                        new Vec2((int) (Math.random() * WIDTH),  (int) (Math.random() * HEIGHT / 4))
                ));
            }

            if (Math.random() < 0.0125) {
                if (Math.random() < 0.5) {
                    obstacles.add(new SlidingSquare(
                            new Vec2(1,  groundY),
                            true
                    ));
                }
                else {
                    obstacles.add(new SlidingSquare(
                            new Vec2(WIDTH,  groundY),
                            false
                    ));
                }

            }


            int angularVelocity = 0;
            if (movingR) {
                angularVelocity -= 20;
            }
            if (movingL) {
                angularVelocity += 20;
            }

            if (playerPos.x <= 0) {
                playerPos = playerPos.withX(0);
                playerVelocity = playerVelocity.withX(0);
                angularVelocity = 0;
            }

            if (playerPos.x >= WIDTH) {
                playerPos = playerPos.withX(WIDTH);
                playerVelocity = playerVelocity.withX(0);
                angularVelocity = 0;
            }

            currentRotation = currentRotation.next(angularVelocity);



            for (int i = 0; i < obstacles.size(); i++) {
                var obstacle = obstacles.get(i);
                obstacles.set(i, obstacle.tick());
            }

            var obstacleIter = obstacles.iterator();
            while (obstacleIter.hasNext()) {
                var obstacle = obstacleIter.next();

                if (obstacle.pos.x < 0 || obstacle.pos.y > HEIGHT || obstacle.pos.x > WIDTH ) {

                    Toolkit.getDefaultToolkit().beep();
                    obstacleIter.remove();
                    score++;
                }
            }

            synchronized (gc) {
                gc.clear();
                drawBackground(gc);
                drawPlayer(gc);
                drawScore(gc);

                for (var obstacle : obstacles) {
                    obstacle.draw(gc);
                }
            }

            gc.sleep((long) (1000 / 30.0));
        }
    }

    public static void main(String[] args) {
        new Main().main();
    }
}