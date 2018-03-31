package Core;

import TileEngine.TileRenderer;
import TileEngine.Tile;
import TileEngine.TileSet;
import edu.princeton.cs.introcs.StdDraw;

import java.awt.Color;
import java.awt.Font;
import java.util.Random;
import java.io.Serializable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Arrays;
import java.lang.System;

public class Game implements Serializable {
    TileRenderer ter = new TileRenderer();
    /* Feel free to change the width and height. */
    public static final int WIDTH = 80;
    public static final int HEIGHT = 30;
    private int[] playerPos = new int[]{-1, -1};
    private int[] ladderPos = new int[]{-1, -1};
    private int[] overladderPos = new int[]{-1, -1};
    private Tile[][] world;
    private Tile[][] overworld;
    private int seed;
    private boolean climbing = true;


    /* ==================================================================== */
    /* =========== PROCEDURAL WORLD GENERATION HELPER FUNCTIONS =========== */
    /* ==================================================================== */

    /* ++++++++++ STRUCTURE GENERATION FUNCTIONS +++++++++++* */

    /**
     * Generates a rectangular room of a random size. The height and width
     * of the room must be less than the world dimensions.
     *
     * @param rand a random number generator calibrated to a seed.
     */
    public static int[] genRandomRoom(Random rand) {
        int[] shape = new int[2];
        shape[0] = RandomUtils.uniform(rand, 2, 10);
        shape[1] = RandomUtils.uniform(rand, 2, 10);
        return shape;
    }

    /**
     * Generates a rectangular room of a random size up to n. The height
     * and width of the room must be less than the world dimensions.
     *
     * @param rand a random number generator calibrated to a seed.
     * @param n    a integer upper bound for room dimensions.
     */
    public static int[] genRandomRoom(Random rand, int n) {
        int[] shape = new int[2];
        shape[0] = RandomUtils.uniform(rand, 2, n);
        shape[1] = RandomUtils.uniform(rand, 2, n);
        return shape;
    }

    /**
     * Generates a hallway of a random size. The length of the hallway must
     * be less than either of the world dimensions.
     *
     * @param rand a random number generator calibrated to a seed.
     */
    private static int[] genRandomHall(Random rand) {
        int[] shape = new int[2];
        /* Randomly determines whether hallway is vertical or horizontal. */
        double n = RandomUtils.uniform(rand);
        if (n < 0.5) {
            shape[1] = 1;
            shape[0] = 0;
            while (shape[0] == 0 || Math.abs(shape[0]) == 1) {
                shape[0] = RandomUtils.uniform(rand, 2, 14);
            }
        } else {
            shape[0] = 1;
            shape[1] = 0;
            while (shape[1] == 0 || Math.abs(shape[1]) == 1) {
                shape[1] = RandomUtils.uniform(rand, 2, 14);
            }
        }
        return shape;
    }

    /* ++++++++++++++++++ MAP BUILDING FUNCTIONS ++++++++++++++++++++* */

    /**
     * Initializes empty game world.
     */
    public static Tile[][] initWorld() {
        Tile[][] world = new Tile[WIDTH][HEIGHT];
        for (int x = 0; x < WIDTH; x += 1) {
            for (int y = 0; y < HEIGHT; y += 1) {
                world[x][y] = TileSet.NOTHING;
            }
        }
        return world;
    }

    /**
     * Places initial room.
     *
     * @param world the game world, a 2-d array.
     * @param rand  a random number generator calibrated to a seed.
     */
    public static void initMap(Tile[][] world, Random rand) {
        boolean success = false;
        while (!success) {
            int[] shape = genRandomRoom(rand, 15);
            int[] pos = new int[2];
            pos[0] = RandomUtils.uniform(rand, WIDTH / 2, WIDTH);
            pos[1] = RandomUtils.uniform(rand, HEIGHT / 2, HEIGHT);

            if (checkEmptyTiles(world, shape, pos, "room")) {
                placerHelper(world, shape, pos);
                success = true;
            }
        }
    }

    /**
     * Places a room-hallway system at a valid location in the world.
     * If no valid locations are found, then we try another generated
     * system.
     *
     * @param world the game world, a 2-d array.
     * @param rand  a random number generator calibrated to a seed.
     */
    public static void placeSystem(Tile[][] world, Random rand) {
        /* Initializes structure sizes */
        int[] hallShape = genRandomHall(rand);
        int[] roomShape = genRandomRoom(rand);

        /* Chooses structure positions */
        int[] roomPos = new int[2];
        roomPos[0] = RandomUtils.uniform(rand, 1, WIDTH);
        roomPos[1] = RandomUtils.uniform(rand, 1, HEIGHT);

        int[] hallPos = new int[2];
        hallPos = findHallwayPos(world, rand, roomShape, roomPos, hallShape);

        /* Places system if it passes checks */
        if (checkEmptyTiles(world, roomShape, roomPos, "room")
                && checkEmptyTiles(world, hallShape, hallPos, "hallway")
                /* Ensures that new structure connects to existing structure */
                && checkSystemConnection(world, hallShape, hallPos)) {
            placerHelper(world, roomShape, roomPos);
            placerHelper(world, hallShape, hallPos);
        }
    }

    /**
     * Initializes game world and map.
     *
     * @param g    a Game object
     * @param seed a integer seed for procedural generation.
     */
    public static void genRandomWorld(Game g, int seed) {
        g.seed = seed;
        Random rand = new Random(seed); // Generate Random object
        int numStructures = RandomUtils.uniform(rand, 10000);

        /* generate new map */
        g.world = initWorld();
        initMap(g.world, rand);
        for (int i = 0; i < numStructures; i++) { // Place all other rooms
            placeSystem(g.world, rand);
        }
        placeWalls(g.world); // Place walls
        spawnPlayer(g, rand);
        spawnLadder(g, g.world, rand);
    }

    /**
     * Initializes overworld and map.
     *
     * @param g    a Game object
     * @param seed a integer seed for procedural generation.
     */
    public static void genRandomOverworld(Game g, int seed) {
        Random rand = new Random(seed); // Generate Random object
        int numStructures = RandomUtils.uniform(rand, 10000);

        /* generate new map */
        g.overworld = initWorld();
        initMap(g.overworld, rand);
        for (int i = 0; i < numStructures; i++) { // Place all other rooms
            placeSystem(g.overworld, rand);
        }
        placeWalls(g.overworld); // Place walls
        spawnoverLadder(g, g.overworld, rand);
    }
    /* ++++++++++++++++++++ TILE PLACEMENT FUNCTIONS +++++++++++++++++++++++ */

    /**
     * Helper function for placeStructure. Declares a tile as "FLOOR"
     * given the parameters of a valid position and structure.
     * system.
     *
     * @param world the game world, a 2-d array.
     * @param shape two-element integer array with dimensions of structure.
     * @param pos   two-element integer array with placement coordinates.
     */
    public static void placerHelper(Tile[][] world, int[] shape, int[] pos) {
        if (pos[0] > 0 && pos[1] > 0) {
            for (int i = 0; i < shape[0]; i++) {
                for (int j = 0; j < shape[1]; j++) {
                    world[pos[0] + i][pos[1] + j] = TileSet.FLOOR;
                }
            }
        } else if (pos[0] < 0 && pos[1] < 0) {
            for (int i = 0; i > shape[0]; i--) {
                for (int j = 0; j > shape[1]; j--) {
                    world[pos[0] + i][pos[1] + j] = TileSet.FLOOR;
                }
            }
        } else if (pos[0] > 0) {
            for (int i = 0; i < shape[0]; i++) {
                for (int j = 0; j > shape[1]; j--) {
                    world[pos[0] + i][pos[1] + j] = TileSet.FLOOR;
                }
            }
        } else {
            for (int i = 0; i > shape[0]; i--) {
                for (int j = 0; j < shape[1]; j++) {
                    world[pos[0] + i][pos[1] + j] = TileSet.FLOOR;
                }
            }
        }
    }

    /**
     * Checks if a set of tiles in the world are empty and available for
     * structure placement.
     *
     * @param world the game world, a 2-d array.
     * @param shape two-element integer array with dimensions of structure.
     * @param pos   two-element integer array with placement coordinates.
     */
    public static boolean checkEmptyTiles(Tile[][] world, int[] shape,
                                          int[] pos, String type) {
        for (int i = -1; i < shape[0] + 1; i++) {
            for (int j = -1; j < shape[1] + 1; j++) {
                int x = pos[0] + i;
                int y = pos[1] + j;
                if (x >= WIDTH - 1 || y >= HEIGHT - 1) {
                    return false;
                } else if (x < 1 || y < 1) {
                    return false;
                } else if (type.equals("room") && world[x][y] == TileSet.FLOOR) {
                    return false;
                }
            }
        }
        return true;
    }

    /* ++++++++++++++++++++++ SYSTEM GENERATION FUNCTIONS ++++++++++++++++++++++++ */

    /**
     * Finds a random border tile on a staged room.
     * that has at least one adjacent empty tile.
     *
     * @param world     the game world, a 2-d array.
     * @param rand      a random number generator calibrated to a seed.
     * @param roomShape two-element integer array with dimensions of structure.
     * @param pos       two-element integer array with placement coordinates.
     */
    public static int[] findHallwayPos(Tile[][] world, Random rand,
                                       int[] roomShape, int[] pos,
                                       int[] hallwayShape) {
        int[] tilePos = new int[2];
        double k = RandomUtils.uniform(rand);
        int[] roomDist = dist(roomShape);
        int[] hallDist = dist(hallwayShape);

        if (hallwayShape[0] == 1) {
            if (k < 0.5) { /* Top border */
                tilePos[0] = RandomUtils.uniform(rand, pos[0], pos[0] + roomDist[0]);
                tilePos[1] = pos[1] + roomDist[1];
            } else { /* Bottom border */
                tilePos[0] = RandomUtils.uniform(rand, pos[0], pos[0] + roomDist[0]);
                tilePos[1] = pos[1] - hallDist[1];
            }
        } else if (hallwayShape[1] == 1) {
            if (k < 0.5) { /* Left border */
                tilePos[0] = pos[0] - hallDist[0];
                tilePos[1] = RandomUtils.uniform(rand, pos[1], pos[1] + roomDist[1]);
            } else { /* Right border */
                tilePos[0] = pos[0] + roomDist[0];
                tilePos[1] = RandomUtils.uniform(rand, pos[1], pos[1] + roomDist[1]);
            }
        }
        return tilePos;
    }

    /**
     * Checks if a tile is a border tile. Let a border tile be any tile
     * that has at least one adjacent empty tile.
     *
     * @param world the game world, a 2-d array.
     * @param pos   two-element integer array with placement coordinates.
     */
    public static boolean checkBorderTile(Tile[][] world, int[] pos) {
        if (pos[0] < 0 || pos[1] < 0) {
            return false;
        } else if (world[pos[0]][pos[1]] == TileSet.NOTHING) {
            return false;
        } else {
            for (int i = -1; i < 2; i++) {
                for (int j = -1; j < 2; j++) {
                    int x = pos[0] + i;
                    int y = pos[1] + j;
                    if (x >= WIDTH - 1 || y >= HEIGHT - 1) {
                        return false;
                    } else if (world[x][y] == TileSet.NOTHING) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Finds a random border tile. Let a border tile be any tile
     * that has at least one adjacent empty tile.
     *
     * @param world the game world, a 2-d array.
     * @param rand  a random number generator calibrated to a seed.
     */
    public static int[] findBorderTile(Tile[][] world, Random rand) {
        int[] pos = new int[2];
        pos[0] = -1;
        while (!isEmpty(pos)) {
            pos[0] = RandomUtils.uniform(rand, WIDTH);
            pos[1] = RandomUtils.uniform(rand, HEIGHT);
            if (world[pos[0]][pos[1]] == TileSet.FLOOR
                    && checkBorderTile(world, pos)) {
                break;
            }
            pos[0] = -1;
        }
        return pos;
    }

    /**
     * Checks if a staged system placement position is valid. To clarify,
     * this function returns true when the hallway of a staged system will
     * connect to the existing map.
     *
     * @param world the game world, a 2-d array.
     * @param shape two-element integer array with dimensions of structure.
     * @param pos   two-element integer array with placement coordinates.
     */
    public static boolean checkSystemConnection(Tile[][] world, int[] shape,
                                                int[] pos) {
        int[] targetPos = new int[2];
        int[] distance = dist(shape);
        if (distance[0] > 0 && distance[1] > 0) {
            targetPos[0] = pos[0] + distance[0];
            targetPos[1] = pos[1] + distance[1];
        } else if (distance[0] < 0 && distance[1] < 0) {
            targetPos[0] = pos[0] - distance[0];
            targetPos[1] = pos[1] - distance[1];
        } else if (distance[0] > 0) {
            targetPos[0] = pos[0] + distance[0];
            targetPos[1] = pos[1] - distance[1];
        } else {
            targetPos[0] = pos[0] - distance[0];
            targetPos[1] = pos[1] + distance[1];
        }
        return checkBorderTile(world, targetPos);
    }

    /* ++++++++++++++++++++++ WALL FUNCTIONS ++++++++++++++++++++++++ */
    /**
     * Generates and places all wall tiles. This is run after all structures
     * in the world are placed.
     *
     * @param world the game world, a 2-d array.
     */
    public static void placeWalls(Tile[][] world) {
        for (int i = 0; i < WIDTH; i++) {
            for (int j = 0; j < HEIGHT; j++) {
                if (world[i][j].equals(TileSet.FLOOR)) {
                    setWall(world, i, j);
                }
            }
        }
    }

    /**
     * Helper function for placeWalls. Checks tiles next to target tile and
     * places walls on empty spaces.
     *
     * @param world the game world, a 2-d array.
     * @param i     the x-coordinate of the target tile.
     * @param j     the y-coordinate of the target tile.
     */
    public static void setWall(Tile[][] world, int i, int j) {
        for (int n = -1; n < 2; n++) {
            for (int k = -1; k < 2; k++) {
                if (world[i + n][j + k].equals(TileSet.NOTHING)) {
                    world[i + n][j + k] = TileSet.WALL;
                }
            }
        }
    }

    /**
     * Removes walls between two adjacent structures without
     * affecting the integrity of the game world border.
     *
     * @param world the game world, a 2-d array.
     */
    public static void cleanWalls(Tile[][] world) {
        for (int n = 1; n < WIDTH - 1; n++) {
            for (int k = 1; k < HEIGHT - 1; k++) {
                if (world[n][k].equals(TileSet.WALL)) {
                    if (world[n + 1][k].equals(TileSet.FLOOR)
                            && world[n - 1][k].equals(TileSet.FLOOR)) {
                        world[n][k] = TileSet.FLOOR;
                    } else if (world[n][k + 1].equals(TileSet.FLOOR)
                            && world[n][k - 1].equals(TileSet.FLOOR)) {
                        world[n][k] = TileSet.FLOOR;
                    }
                }
            }
        }
    }

    /**
     * Fills all empty tiles with walls.
     *
     * @param world the game world, a 2-d array.
     */
    public static void fillWalls(Tile[][] world) {
        for (int x = 0; x < WIDTH; x += 1) { // Make unused spaces walls.
            for (int y = 0; y < HEIGHT; y += 1) {
                if (world[x][y] == TileSet.NOTHING) {
                    world[x][y] = TileSet.WALL;
                }
            }
        }
    }

    /* ++++++++++++++++++++++ UTILITY FUNCTIONS++++++++++++++++++++++++ */

    /**
     * Checks if an integer array is empty. Let an empty array be
     * any array such that all its members are negative.
     *
     * @param array An integer array.
     */
    public static boolean isEmpty(int[] array) {
        for (int i = 0; i < array.length; i++) {
            if (array[i] < 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Retrieves the distance values for an array.
     *
     * @param shape An integer array representing structure dimensions.
     */
    public static int[] dist(int[] shape) {
        int[] distance = new int[shape.length];
        for (int i = 0; i < shape.length; i++) {
            if (shape[i] > 0) {
                distance[i] = shape[i] - 1;
            } else {
                distance[i] = shape[i] + 1;
            }
        }
        return distance;
    }

    /* ==================================================================== */
    /* =================== INTERACTIVE GAME UI FUNCTIONS ================== */
    /* ==================================================================== */
    /**
     * Generates a new world with a random map.
     */
    public static Game newGame() {
        Game newGame = new Game();
        newGame.genRandomWorld(newGame, RandomUtils.uniform(
                new Random(System.currentTimeMillis() % Integer.MAX_VALUE), 10000));
        newGame.genRandomOverworld(newGame, RandomUtils.uniform(
                new Random(System.currentTimeMillis() % Integer.MAX_VALUE), 10000));
        return newGame;
    }

    /**
     * Saves and quits current game.
     *
     * @param g a serializable game object
     */
    public static void quitGame(Game g) {
        File f = new File("./game.ser");
        try {
            if (!f.exists()) {
                f.createNewFile();
            }
            FileOutputStream fs = new FileOutputStream(f);
            ObjectOutputStream os = new ObjectOutputStream(fs);
            os.writeObject(g);
            os.close();
        } catch (FileNotFoundException e) {
            System.out.println("file not found");
            System.exit(0);
        } catch (IOException e) {
            System.out.println(e);
            System.exit(0);
        }
    }

    /**
     * Loads saved game if available. If there is no save file
     * then a new random game is generated.
     */
    private static Game loadGame() {
        File f = new File("./game.ser");
        if (f.exists()) {
            try {
                FileInputStream fs = new FileInputStream(f);
                ObjectInputStream os = new ObjectInputStream(fs);
                Game loadGame = (Game) os.readObject();
                os.close();
                return loadGame;
            } catch (FileNotFoundException e) {
                System.out.println("file not found");
//                System.exit(0);
                return newGame();
            } catch (IOException e) {
                System.out.println(e);
//                System.exit(0);
                return newGame();
            } catch (ClassNotFoundException e) {
                System.out.println("class not found");
//                System.exit(0);
                return newGame();
            }
        }
        return newGame();
    }

    /** Places the Player at a random point.
     *
     * @param g a serializable game object
     * @param rand a calibrated random number generator.
     */
    public static void spawnPlayer(Game g, Random rand) {
        int x = RandomUtils.uniform(rand, WIDTH / 3, 2 * WIDTH / 3);
        int y = RandomUtils.uniform(rand, HEIGHT / 3, 2 * HEIGHT / 3);
        while (g.world[x][y] != TileSet.FLOOR) {
            x = RandomUtils.uniform(rand, 0, WIDTH);
            y = RandomUtils.uniform(rand, 0, HEIGHT);
        }
        g.playerPos[0] = x;
        g.playerPos[1] = y;
        g.world[x][y] = TileSet.PLAYER;
    }

    /** Places the ladder at a random point.
     *
     * @param g a serializable game object
     * @param rand a calibrated random number generator.
     */
    public static void spawnLadder(Game g, Tile[][] world, Random rand) {
        int x = RandomUtils.uniform(rand, WIDTH / 3, 2 * WIDTH / 3);
        int y = RandomUtils.uniform(rand, HEIGHT / 3, 2 * HEIGHT / 3);
        while (world[x][y] != TileSet.FLOOR) {
            x = RandomUtils.uniform(rand, 0, WIDTH);
            y = RandomUtils.uniform(rand, 0, HEIGHT);
        }
        g.ladderPos[0] = x;
        g.ladderPos[1] = y;
        world[x][y] = TileSet.LADDER;
    }

    /** Places the ladder at a random point.
     *
     * @param g a serializable game object
     * @param rand a calibrated random number generator.
     */
    public static void spawnoverLadder(Game g, Tile[][] world, Random rand) {
        int x = RandomUtils.uniform(rand, WIDTH / 3, 2 * WIDTH / 3);
        int y = RandomUtils.uniform(rand, HEIGHT / 3, 2 * HEIGHT / 3);
        while (world[x][y] != TileSet.FLOOR) {
            x = RandomUtils.uniform(rand, 0, WIDTH);
            y = RandomUtils.uniform(rand, 0, HEIGHT);
        }
        g.overladderPos[0] = x;
        g.overladderPos[1] = y;
        world[x][y] = TileSet.LADDER;
    }

    /**
     * Moves the Player to an adjacent tile.
     *
     * @param g a game object
     * @param k a directional input character
     */
    public static void movePlayer(Game g, Tile[][] world, char k) {
        int x = g.playerPos[0];
        int y = g.playerPos[1];
        if (k == 'w') { // move up
            y++;
        } else if (k == 'a') { // move left
            x--;
        } else if (k == 's') { // move down
            y--;
        } else { // k == 'd'; move right
            x++;
        }
        if (!world[x][y].equals(TileSet.WALL)) {
            world[g.playerPos[0]][g.playerPos[1]] = TileSet.FLOOR;
            g.playerPos[0] = x;
            g.playerPos[1] = y;
            world[g.playerPos[0]][g.playerPos[1]] = TileSet.PLAYER;
            g.climbing = false;
        }
        if (!Arrays.equals(g.playerPos, g.ladderPos)) {
            world[g.ladderPos[0]][g.ladderPos[1]] = TileSet.LADDER;
        }
    }

    /**
     * Renders the main menu.
     */
    public void drawMenu(String input) {
        int midWidth = WIDTH / 2;
        int midHeight = HEIGHT / 2 + 3;
        int topHeight = 3 * HEIGHT / 4 + 3;

        StdDraw.clear(Color.black);

        // Draw the actual text
        Font defaultFont = new Font("Sans Serif", Font.PLAIN, 16);
        Font bigFont = new Font("Monaco", Font.BOLD, 30);
        StdDraw.setFont(bigFont);
        StdDraw.setPenColor(Color.white);
        StdDraw.text(midWidth, topHeight, "Abyss");
        if (input.length() == 0) {
        StdDraw.text(midWidth, midHeight, "New Game (N)");
        StdDraw.text(midWidth, midHeight - 2, "Load (L)");
        StdDraw.text(midWidth, midHeight - 4, "Quit (:Q)");
        } else {
            StdDraw.text(midWidth, midHeight, input);
        }

        StdDraw.show();
        StdDraw.setFont(defaultFont);
    }

    /**
     * Renders the heads up display (HUD).
     *
     * @param g The current game object
     * */
    public void drawFrame(Game g) {
        int x = (int) StdDraw.mouseX();
        int y = (int) StdDraw.mouseY() - 3;
        String substrate = "";
        if (x < WIDTH && y < HEIGHT) {
            if (0 < x && 0 < y) {
                substrate = g.world[x][y].description();
            }
        }

        String timeStamp = new SimpleDateFormat("MMM dd, yyyy HH:mm:ss").format(new Date());

        g.ter.renderFrame(g.world);

        StdDraw.setPenColor(Color.white);
        StdDraw.textLeft(1, HEIGHT + 4, "Seed:" + Integer.toString(seed));
        StdDraw.text(WIDTH / 2, HEIGHT + 4, substrate);
        StdDraw.textRight(WIDTH - 1, HEIGHT + 4, timeStamp);
        StdDraw.line(0, HEIGHT + 3, WIDTH, HEIGHT + 3);

        StdDraw.textLeft(1,  1,  "Movement:   Up (W)   Left (A)   Down (S)   Right (D)");
        StdDraw.textRight(WIDTH - 1,  1,  "Quit (:Q)");
        StdDraw.line(0, 2, WIDTH, 2);
        StdDraw.show();
        StdDraw.clear();

    }


    /* ==================================================================== */
    /* ========================= GAMEPLAY METHODS ========================= */
    /* ==================================================================== */

    /**
     * Method used for playing a fresh game. The game should
     * start from the main menu.
     */
    public void playWithKeyboard() {
        // Show menu
        this.ter.initialize(WIDTH, HEIGHT + 5, 0, 3);
//        StdDraw.disableDoubleBuffering();
        this.drawMenu("");
        StdDraw.show();
        boolean play = false;
        // Player input
        while (true) {
            if (play) {
                if (Arrays.equals(playerPos, ladderPos)
                        && !climbing) {
                    Tile[][] stagedWorld = overworld;
                    overworld = world;
                    world = stagedWorld;
                    int[] stagedLadder = new int[] {-1, -1};
                    java.lang.System.arraycopy(overladderPos, 0, stagedLadder, 0, 2);
                    java.lang.System.arraycopy(ladderPos, 0, overladderPos, 0, 2);
                    java.lang.System.arraycopy(stagedLadder,0,  ladderPos, 0 ,2);
                    java.lang.System.arraycopy(ladderPos, 0, playerPos, 0 ,2);
                    world[playerPos[0]][playerPos[1]] = TileSet.PLAYER;
                    climbing = true;
                }
                drawFrame(this);
            }
            if (!StdDraw.hasNextKeyTyped()) {
                continue;
            }
            char next = StdDraw.nextKeyTyped();
            if (next == 'n') { // Handle new game command "n"
                // Handle seed argument int
                String rawSeed = "";
                drawMenu("Type an integer seed. Press S to confirm.");
                while (next != 's') {
                    if (!StdDraw.hasNextKeyTyped()) {
                        continue;
                    }
                    next = StdDraw.nextKeyTyped();
                    rawSeed += String.valueOf(next);
                    drawMenu(rawSeed);
                }
                seed = (int) Long.parseLong(
                        rawSeed.substring(0, rawSeed.length() - 1));
                genRandomWorld(this, seed);
                genRandomOverworld(this, seed ^ 2);
                drawFrame(this);
                play = true;
                /* Concurrent game commands */
            } else if (next == ':') { // Handle quit and save command ":q"
                while (!StdDraw.hasNextKeyTyped()) {
                    continue;
                }
                if (StdDraw.nextKeyTyped() == 'q') {
                    /* End and save game */
                    StdDraw.clear(StdDraw.BLACK);
                    StdDraw.show();
                    quitGame(this);
                    System.exit(0);
                    break;
                }
            } else if (next == 'l') { // Handle load command "l"
                /* Load existing game */
                Game oldGame = loadGame();
                this.world = oldGame.world;
                this.overworld = oldGame.overworld;
                this.playerPos = oldGame.playerPos;
                this.ladderPos = oldGame.ladderPos;
                this.overladderPos = oldGame.overladderPos;
                this.seed = oldGame.seed;
                drawFrame(this);
                play = true;
                // Handle movement commands "wasd"
            } else if (Arrays.asList('w', 'a', 's', 'd').contains(next)) {
                /* Move character */
                movePlayer(this, this.world, next);
                drawFrame(this);
            }
        }
    }

    /**
     * Method used for testing the game code. The input string will be a series
     * of characters (for example, "n123sswwdasdassadwas", "n123sss:q", "lwww". The game should
     * behave exactly as if the user typed these characters into the game after playing
     * playWithKeyboard. If the string ends in ":q", the same world should be returned as if the
     * string did not end with q. For example "n123sss" and "n123sss:q" should return the same
     * world. However, the behavior is slightly different. After playing with "n123sss:q", the game
     * should save, and thus if we then called playWithInputString with the string "l", we'd expect
     * to get the exact same world back again, since this corresponds to loading the saved game.
     * <p>
     *
     * @param input the input string to feed to your program
     * @return the 2D Tile[][] representing the state of the world
     */
    public Tile[][] playWithInput(String input) {
        // Fill out this method to run the game using the input passed in,
        // and return a 2D tile representation of the world that would have
        // been drawn if the same inputs had been given to playWithKeyboard().

        // #===================== PARSE COMMAND STRING =======================#
        String rawSeed = "";
        String[] inputArray = input.split("");
        int i = 0;
        while (i < inputArray.length) {
            if (inputArray[i].equals("n")) {
                i++;
                while (!inputArray[i].equals("s")) {
                    rawSeed += inputArray[i];
                    i++;
                }
                seed = (int) Long.parseLong(rawSeed);
                genRandomWorld(this, seed);
                genRandomOverworld(this, seed ^ 2);
            } else if (inputArray[i].equals(":")) { // Handle quit and save command ":q
                if (inputArray[i + 1].equals("q")) {
                    /* End and save game */
                    quitGame(this);
                    break;
                }
            } else if (inputArray[i].equals("l")) { // Handle load command "l"
                /* Load existing game */
                Game oldGame = loadGame();
                this.world = oldGame.world;
                this.playerPos = oldGame.playerPos;
                this.ladderPos = oldGame.ladderPos;
                this.overladderPos = oldGame.overladderPos;
                this.seed = oldGame.seed;

                // Handle movement commands "wasd"
            } else if (java.util.Arrays.asList("w", "a", "s", "d").contains(inputArray[i])) {
                /* Move character */
                movePlayer(this, world, inputArray[i].charAt(0));
            }
            if (Arrays.equals(playerPos, ladderPos)
                    && !climbing) {
                Tile[][] stagedWorld = overworld;
                overworld = world;
                world = stagedWorld;
                int[] stagedLadder = new int[] {-1, -1};
                java.lang.System.arraycopy(overladderPos, 0, stagedLadder, 0, 2);
                java.lang.System.arraycopy(ladderPos, 0, overladderPos, 0, 2);
                java.lang.System.arraycopy(stagedLadder,0,  ladderPos, 0 ,2);
                java.lang.System.arraycopy(ladderPos, 0, playerPos, 0 ,2);
                world[playerPos[0]][playerPos[1]] = TileSet.PLAYER;
                climbing = true;
            }
            i++;
        }
        return this.world;
    }
}