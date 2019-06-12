package com.github.tommyettinger.dl;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import com.github.tommyettinger.dl.data.Items;
import com.github.tommyettinger.dl.data.Roles;
import squidpony.ArrayTools;
import squidpony.FakeLanguageGen;
import squidpony.NaturalLanguageCipher;
import squidpony.StringKit;
import squidpony.squidai.DijkstraMap;
import squidpony.squidgrid.Direction;
import squidpony.squidgrid.FOV;
import squidpony.squidgrid.Radius;
import squidpony.squidgrid.gui.gdx.*;
import squidpony.squidgrid.mapping.DungeonGenerator;
import squidpony.squidgrid.mapping.DungeonUtility;
import squidpony.squidgrid.mapping.LineKit;
import squidpony.squidmath.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;



/**
 * This is a small, not-overly-simple demo that presents some important features of SquidLib and shows a faster,
 * cleaner, and more recently-introduced way of displaying the map and other text. Features include dungeon map
 * generation, field of view, pathfinding (to the mouse position), continuous noise (used for a wavering torch effect),
 * language generation/ciphering, a colorful glow effect, and ever-present random number generation (with a seed).
 * You can increase the size of the map on most target platforms (but GWT struggles with large... anything) by
 * changing gridHeight and gridWidth to affect the visible area or bigWidth and bigHeight to adjust the size of the
 * dungeon you can move through, with the camera following your '@' symbol.
 * <br>
 * The assets folder of this project, if it was created with SquidSetup, will contain the necessary font files (just one
 * .fnt file and one .png are needed, but many more are included by default). You should move any font files you don't
 * use out of the assets directory when you produce a release JAR, APK, or GWT build.
 */
public class DastardlyLands extends ApplicationAdapter {
    SpriteBatch batch;
    
    // SquidLib has many methods that expect an IRNG instance, and there's several classes to choose from.
    // In this program we'll use GWTRNG, which will behave better on the HTML target than other generators.
    private StatefulRNG rng;
    private SparseLayers display, splitDisplay;
    private DungeonGenerator dungeonGen;
    // decoDungeon stores the dungeon map with features like grass and water, if present, as chars like '"' and '~'.
    // bareDungeon stores the dungeon map with just walls as '#' and anything not a wall as '.'.
    // Both of the above maps use '#' for walls, and the next two use box-drawing characters instead.
    // lineDungeon stores the whole map the same as decoDungeon except for walls, which are box-drawing characters here.
    // prunedDungeon takes lineDungeon and adjusts it so unseen segments of wall (represented by box-drawing characters)
    //   are removed from rendering; unlike the others, it is frequently changed.
    private char[][] decoDungeon, bareDungeon, lineDungeon, prunedDungeon;
    private float[][] colors, bgColors;

    //Here, gridHeight refers to the total number of rows to be displayed on the screen.
    //We're displaying 25 rows of dungeon, then 7 more rows of text generation to show some tricks with language.
    //gridHeight is 25 because that variable will be used for generating the dungeon (the actual size of the dungeon
    //will be triple gridWidth and triple gridHeight), and determines how much off the dungeon is visible at any time.
    //The bonusHeight is the number of additional rows that aren't handled like the dungeon rows and are shown in a
    //separate area; here we use them for translations. The gridWidth is 90, which means we show 90 grid spaces
    //across the whole screen, but the actual dungeon is larger. The cellWidth and cellHeight are 10 and 20, which will
    //match the starting dimensions of a cell in pixels, but won't be stuck at that value because we use a "Stretchable"
    //font, and so the cells can change size (they don't need to be scaled by equal amounts, either). While gridWidth
    //and gridHeight are measured in spaces on the grid, cellWidth and cellHeight are the initial pixel dimensions of
    //one cell; resizing the window can make the units cellWidth and cellHeight use smaller or larger than a pixel.

    /** In number of cells */
    private static final int gridWidth = 100;
    /** In number of cells */
    private static final int gridHeight = 25;

    /** In number of cells */
    private static final int bigWidth = gridWidth * 2;
    /** In number of cells */
    private static final int bigHeight = gridHeight * 2;
    
    private static final int split = 60;
    
    /** In number of cells */
    private static final int bonusHeight = 7;
    /** The pixel width of a cell */
    private static final int cellWidth = 10;
    /** The pixel height of a cell */
    private static final int cellHeight = 20;
    private SquidInput input;
    private Color bgColor;
    private Stage stage, splitStage;
    private DijkstraMap playerToCursor;
    private Coord cursor, player;
    private OrderedMap<Coord, Item> things;
    private List<Coord> toCursor;
    private List<Coord> awaitedMoves;
    // a passage from the ancient text The Art of War, which remains relevant in any era but is mostly used as a basis
    // for translation to imaginary languages using the NaturalLanguageCipher and FakeLanguageGen classes.
    private final String artOfWar =
            "Sun Tzu said: In the practical art of war, the best thing of all is to take the " +
                    "enemy's country whole and intact; to shatter and destroy it is not so good. So, " +
                    "too, it is better to recapture an army entire than to destroy it, to capture " +
                    "a regiment, a detachment or a company entire than to destroy them. Hence to fight " +
                    "and conquer in all your battles is not supreme excellence; supreme excellence " +
                    "consists in breaking the enemy's resistance without fighting.";
    // A translation dictionary for going back and forth between English and an imaginary language that this generates
    // words for, using some of the rules that the English language tends to follow to determine if two words should
    // share a common base word (such as "read" and "reader" needing similar translations). This is given randomly
    // selected languages from the FakeLanguageGen class, which is able to produce text that matches a certain style,
    // usually that of a natural language but some imitations of fictional languages, such as languages spoken by elves,
    // goblins, or demons, are present as well. An unusual trait of FakeLanguageGen is that it can mix two or more
    // languages to make a new one, which most other kinds of generators have a somewhat-hard time doing.
    private NaturalLanguageCipher translator;
    // this is initialized with the word-wrapped contents of artOfWar, then has translations of that text to imaginary
    // languages appended after the plain-English version. The contents have the first item removed with each step, and
    // have new translations added whenever the line count is too low.
    private ArrayList<String> lang;
//    private ObText.ObTextEntry playerRole, enemyRole;
//    private ObText roles;
    private Roles rolesJson;
    private OrderedMap<String,  Roles.Role> roles;
    private OrderedMap<String, Items.Item> items;
    private Map.Entry<String, Roles.Role> playerRole, enemyRole;
    private double[][] resistance;
    private double[][] visible;
    private GreasedRegion floors, blockage, seen;
    private TextCellFactory.Glyph pg;
    private static final float FLOAT_LIGHTING = SColor.COSMIC_LATTE.toFloatBits(),
            GRAY_FLOAT = SColor.CW_GRAY_BLACK.toFloatBits(), 
            VERY_DARK_FLOAT = SColor.DB_INK.toFloatBits();

    @Override
    public void create () {
        // gotta have a random number generator. We can seed a GWTRNG with any long we want, or even a String.
//        rng = new GWTRNG("Welcome to SquidLib!");
        rng = new StatefulRNG();
        final String fileText = Gdx.files.internal("classes-obtext.txt").readString();
        roles = new OrderedMap<>((Map<String, Roles.Role>) Roles.load().fromJson(Gdx.files.internal("roles.json").readString("UTF8")));
        
        //roles = new ObText(fileText);
        playerRole = roles.randomEntry(rng);
        enemyRole = roles.randomEntry(rng);
        items = new OrderedMap<>((Map<String, Items.Item>)Items.load().fromJson(Gdx.files.internal("items.json").readString("UTF8")));
        //Some classes in SquidLib need access to a batch to render certain things, so it's a good idea to have one.
        batch = new SpriteBatch();
        StretchViewport mainViewport = new StretchViewport(gridWidth * cellWidth, gridHeight * cellHeight),
                splitViewport = new StretchViewport(gridWidth * cellWidth, bonusHeight * cellHeight);
        mainViewport.setScreenBounds(0, 0, gridWidth * cellWidth, gridHeight * cellHeight);
        splitViewport.setScreenBounds(0, 0, gridWidth * cellWidth, bonusHeight * cellHeight);
        //Here we make sure our Stage, which holds any text-based grids we make, uses our Batch.
        stage = new Stage(mainViewport, batch);
        splitStage = new Stage(splitViewport, batch);
        // the font will try to load Iosevka Slab as an embedded bitmap font with a (MSDF) distance field effect.
        // the distance field effect allows the font to be stretched without getting blurry or grainy too easily.
        // this font is covered under the SIL Open Font License (fully free), so there's no reason it can't be used.
        // It is included in the assets folder if this project was made with SquidSetup, along with other fonts
        // Another option to consider is DefaultResources.getCrispSlabFamily(), which uses the same font (Iosevka Slab)
        // but treats it differently, and can be used to draw bold and/or italic text at the expense of the font being
        // slightly less detailed visually and some rare glyphs being omitted. Bold and italic text are usually handled
        // with markup in text that is passed to SquidLib's GDXMarkup class; see GDXMarkup's docs for more info.
        // There are also several other distance field fonts, including two more font families like
        // DefaultResources.getCrispSlabFamily() that allow bold/italic text. In addition to Crisp fonts, there are also
        // Stretchable fonts which use a SDF distance field effect, which is slightly slower to render and isn't as
        // detailed at high sizes, but stays sharp when resizing to very small sizes. Although some BitmapFont assets
        // are available without a distance field effect, they are discouraged for most usage because they can't cleanly
        // resize without loading a different BitmapFont per size, and there's usually one size in DefaultResources.
        display = new SparseLayers(bigWidth, bigHeight + bonusHeight, cellWidth, cellHeight,
                DefaultResources.getCrispSlabFamily());
        
        splitDisplay = new SparseLayers(gridWidth, bonusHeight - 1, cellWidth, cellHeight, display.font);
        splitDisplay.defaultPackedBackground = FLOAT_LIGHTING;

        //This uses the seeded GWTRNG we made earlier to build a procedural dungeon using a method that takes
        //rectangular sections of pre-drawn dungeon and drops them into place in a tiling pattern. It makes good winding
        //dungeons with rooms by default, but in the later call to dungeonGen.generate(), you can use a TilesetType such
        //as TilesetType.ROUND_ROOMS_DIAGONAL_CORRIDORS or TilesetType.CAVES_LIMIT_CONNECTIVITY to change the sections
        //that this will use, or just pass in a full 2D char array produced from some other generator, such as
        //SerpentMapGenerator, OrganicMapGenerator, or DenseRoomMapGenerator.
        dungeonGen = new DungeonGenerator(bigWidth, bigHeight, rng);
        //uncomment this next line to randomly add water to the dungeon in pools.
        //dungeonGen.addWater(15);
        //decoDungeon is given the dungeon with any decorations we specified. (Here, we didn't, unless you chose to add
        //water to the dungeon. In that case, decoDungeon will have different contents than bareDungeon, next.)
        decoDungeon = dungeonGen.generate();
        //getBareDungeon provides the simplest representation of the generated dungeon -- '#' for walls, '.' for floors.
        bareDungeon = dungeonGen.getBareDungeon();
        lineDungeon = DungeonUtility.hashesToLines(decoDungeon);

        resistance = DungeonUtility.generateResistances(decoDungeon);
        visible = new double[bigWidth][bigHeight];

        //Coord is the type we use as a general 2D point, usually in a dungeon.
        //Because we know dungeons won't be incredibly huge, Coord performs best for x and y values less than 256, but
        // by default it can also handle some negative x and y values (-3 is the lowest it can efficiently store). You
        // can call Coord.expandPool() or Coord.expandPoolTo() if you need larger maps to be just as fast.
        cursor = Coord.get(-1, -1);
        // here, we need to get a random floor cell to place the player upon, without the possibility of putting him
        // inside a wall. There are a few ways to do this in SquidLib. The most straightforward way is to randomly
        // choose x and y positions until a floor is found, but particularly on dungeons with few floor cells, this can
        // have serious problems -- if it takes too long to find a floor cell, either it needs to be able to figure out
        // that random choice isn't working and instead choose the first it finds in simple iteration, or potentially
        // keep trying forever on an all-wall map. There are better ways! These involve using a kind of specific storage
        // for points or regions, getting that to store only floors, and finding a random cell from that collection of
        // floors. The two kinds of such storage used commonly in SquidLib are the "packed data" as short[] produced by
        // CoordPacker (which use very little memory, but can be slow, and are treated as unchanging by CoordPacker so
        // any change makes a new array), and GreasedRegion objects (which use slightly more memory, tend to be faster
        // on almost all operations compared to the same operations with CoordPacker, and default to changing the
        // GreasedRegion object when you call a method on it instead of making a new one). Even though CoordPacker
        // sometimes has better documentation, GreasedRegion is generally a better choice; it was added to address
        // shortcomings in CoordPacker, particularly for speed, and the worst-case scenarios for data in CoordPacker are
        // no problem whatsoever for GreasedRegion. CoordPacker is called that because it compresses the information
        // for nearby Coords into a smaller amount of memory. GreasedRegion is called that because it encodes regions,
        // but is "greasy" both in the fatty-food sense of using more space, and in the "greased lightning" sense of
        // being especially fast. Both of them can be seen as storing regions of points in 2D space as "on" and "off."

        // Here we fill a GreasedRegion so it stores the cells that contain a floor, the '.' char, as "on."
        floors = new GreasedRegion(bareDungeon, '.');
        //player is, here, just a Coord that stores his position. In a real game, you would probably have a class for
        //creatures, and possibly a subclass for the player. The singleRandom() method on GreasedRegion finds one Coord
        //in that region that is "on," or -1,-1 if there are no such cells. It takes an IRNG object as a parameter, and
        //if you gave a seed to the GWTRNG constructor, then the cell this chooses will be reliable for testing. If you
        //don't seed the GWTRNG, any valid cell should be possible.
        player = floors.singleRandom(rng);
        floors.remove(player);
        Coord[] itemPositions = floors.mixedRandomSeparated(0.07, -1, rng.nextLong());
        things = new OrderedMap<>(itemPositions.length);
        for (Coord c : itemPositions)
        {
            final int pos = rng.nextSignedInt(items.size());
            things.put(c, new Item(rng, items.keyAt(pos), items.getAt(pos)));
        }
        //These need to have their positions set before adding any entities if there is an offset involved.
        //There is no offset used here, but it's still a good practice here to set positions early on.
        display.setPosition(0f, 0f);
        // Uses shadowcasting FOV and reuses the visible array without creating new arrays constantly.
        FOV.reuseFOV(resistance, visible, player.x, player.y, 9.0, Radius.CIRCLE);
        // 0.0 is the upper bound (inclusive), so any Coord in visible that is more well-lit than 0.0 will _not_ be in
        // the blockage Collection, but anything 0.0 or less will be in it. This lets us use blockage to prevent access
        // to cells we can't see from the start of the move.
        blockage = new GreasedRegion(visible, 0.0);
        // Here we mark the initially seen cells as anything that wasn't included in the unseen "blocked" region.
        // We invert the copy's contents to prepare for a later step, which makes blockage contain only the cells that
        // are above 0.0, then copy it to save this step as the seen cells. We will modify seen later independently of
        // the blocked cells, so a copy is correct here. Most methods on GreasedRegion objects will modify the
        // GreasedRegion they are called on, which can greatly help efficiency on long chains of operations.
        seen = blockage.not().copy();
        // Here is one of those methods on a GreasedRegion; fringe8way takes a GreasedRegion (here, the set of cells
        // that are visible to the player), and modifies it to contain only cells that were not in the last step, but
        // were adjacent to a cell that was present in the last step. This can be visualized as taking the area just
        // beyond the border of a region, using 8-way adjacency here because we specified fringe8way instead of fringe.
        // We do this because it means pathfinding will only have to work with a small number of cells (the area just
        // out of sight, and no further) instead of all invisible cells when figuring out if something is currently
        // impossible to enter.
        blockage.fringe8way();
        
        // prunedDungeon starts with the full lineDungeon, which includes features like water and grass but also stores
        // all walls as box-drawing characters. The issue with using lineDungeon as-is is that a character like '┬' may
        // be used because there are walls to the east, west, and south of it, even when the player is to the north of
        // that cell and so has never seen the southern connecting wall, and would have no reason to know it is there.
        // By calling LineKit.pruneLines(), we adjust prunedDungeon to hold a variant on lineDungeon that removes any
        // line segments that haven't ever been visible. This is called again whenever seen changes. 
        prunedDungeon = ArrayTools.copy(lineDungeon);
        // We call pruneLines with an optional parameter here, LineKit.lightAlt, which will allow prunedDungeon to use
        // the half-line chars "╴╵╶╷". These chars aren't supported by all fonts, but they are by the one we use here.
        // The default is to use LineKit.light , which will replace '╴' and '╶' with '─' and '╷' and '╵' with '│'.
        LineKit.pruneLines(lineDungeon, seen, LineKit.lightAlt, prunedDungeon);
        
        //This is used to allow clicks or taps to take the player to the desired area.
        toCursor = new ArrayList<>(200);
        //When a path is confirmed by clicking, we draw from this List to find which cell is next to move into.
        awaitedMoves = new ArrayList<>(200);
        //DijkstraMap is the pathfinding swiss-army knife we use here to find a path to the latest cursor position.
        //DijkstraMap.Measurement is an enum that determines the possibility or preference to enter diagonals. Here, the
        //MANHATTAN value is used, which means 4-way movement only, no diagonals possible. Alternatives are CHEBYSHEV,
        //which allows 8 directions of movement at the same cost for all directions, and EUCLIDEAN, which allows 8
        //directions, but will prefer orthogonal moves unless diagonal ones are clearly closer "as the crow flies."
        playerToCursor = new DijkstraMap(decoDungeon, DijkstraMap.Measurement.MANHATTAN);
        //These next two lines mark the player as something we want paths to go to or from, and get the distances to the
        // player from all walkable cells in the dungeon.
        playerToCursor.setGoal(player);
        // DijkstraMap.partialScan only finds the distance to get to a cell if that distance is less than some limit,
        // which is 13 here. It also won't try to find distances through an impassable cell, which here is the blockage
        // GreasedRegion that contains the cells just past the edge of the player's FOV area.
        playerToCursor.partialScan(13, blockage);


        //The next three lines set the background color for anything we don't draw on, but also create 2D arrays of the
        //same size as decoDungeon that store the colors for the foregrounds and backgrounds of each cell as packed
        //floats (a format SparseLayers can use throughout its API), using the colors for the cell with the same x and
        //y. By changing an item in SColor.LIMITED_PALETTE, we also change the color assigned by MapUtility to floors.
        bgColor = SColor.DARK_SLATE_GRAY;
        SColor.LIMITED_PALETTE[3] = SColor.DB_GRAPHITE;
        colors = MapUtility.generateDefaultColorsFloat(decoDungeon);
        bgColors = MapUtility.generateDefaultBGColorsFloat(decoDungeon);


        //places the player as an '@' at his position in orange.
        pg = display.glyph('@', SColor.SAFETY_ORANGE.toFloatBits(), player.x, player.y);

        lang = new ArrayList<>(16);
        // StringKit has various utilities for dealing with text, including wrapping text so it fits in a specific width
        // and inserting the lines into a List of Strings, as we do here with the List lang and the text artOfWar.
        StringKit.wrap(lang, artOfWar, split-2);
        // FakeLanguageGen.registered is an array of the hand-made languages in FakeLanguageGen, not any random ones and
        // not most mixes of multiple languages. We get a random language from it with our GWTRNG, and use that to build
        // our current NaturalLanguageCipher. This NaturalLanguageCipher will act as an English-to-X dictionary for
        // whatever X is our randomly chosen language, and will try to follow the loose rules English follows when
        // it translates a word into an imaginary word in the fake language.
        translator = new NaturalLanguageCipher(rng.getRandomElement(FakeLanguageGen.registered));
        StringKit.wrap(lang, translator.cipher(artOfWar), split-2);
        // the 0L here can be used to adjust the languages generated; it acts a little like a seed for an RNG.
        translator.initialize(rng.getRandomElement(FakeLanguageGen.registered), 0L);

        // this is a big one.
        // SquidInput can be constructed with a KeyHandler (which just processes specific keypresses), a SquidMouse
        // (which is given an InputProcessor implementation and can handle multiple kinds of mouse move), or both.
        // keyHandler is meant to be able to handle complex, modified key input, typically for games that distinguish
        // between, say, 'q' and 'Q' for 'quaff' and 'Quip' or whatever obtuse combination you choose. The
        // implementation here handles hjkl keys (also called vi-keys), numpad, arrow keys, and wasd for 4-way movement.
        // Shifted letter keys produce capitalized chars when passed to KeyHandler.handle(), but we don't care about
        // that so we just use two case statements with the same body, i.e. one for 'A' and one for 'a'.
        // You can also set up a series of future moves by clicking within FOV range, using mouseMoved to determine the
        // path to the mouse position with a DijkstraMap (called playerToCursor), and using touchUp to actually trigger
        // the event when someone clicks.
        input = new SquidInput(new SquidInput.KeyHandler() {
            @Override
            public void handle(char key, boolean alt, boolean ctrl, boolean shift) {
                switch (key)
                {
                    case SquidInput.UP_ARROW:
                    case 'k':
                    case 'w':
                    case 'K':
                    case 'W':
                    {
                        toCursor.clear();
                        //-1 is up on the screen
                        awaitedMoves.add(player.translate(0, -1));
                        break;
                    }
                    case SquidInput.DOWN_ARROW:
                    case 'j':
                    case 's':
                    case 'J':
                    case 'S':
                    {
                        toCursor.clear();
                        //+1 is down on the screen
                        awaitedMoves.add(player.translate(0, 1));
                        break;
                    }
                    case SquidInput.LEFT_ARROW:
                    case 'h':
                    case 'a':
                    case 'H':
                    case 'A':
                    {
                        toCursor.clear();
                        awaitedMoves.add(player.translate(-1, 0));
                        break;
                    }
                    case SquidInput.RIGHT_ARROW:
                    case 'l':
                    case 'd':
                    case 'L':
                    case 'D':
                    {
                        toCursor.clear();
                        awaitedMoves.add(player.translate(1, 0));
                        break;
                    }
                    case 'Q':
                    case 'q':
                    case SquidInput.ESCAPE:
                    {
                        Gdx.app.exit();
                        break;
                    }
                }
            }
        },
                //The second parameter passed to a SquidInput can be a SquidMouse, which takes mouse or touchscreen
                //input and converts it to grid coordinates (here, a cell is 10 wide and 20 tall, so clicking at the
                // pixel position 16,51 will pass screenX as 1 (since if you divide 16 by 10 and round down you get 1),
                // and screenY as 2 (since 51 divided by 20 rounded down is 2)).
                new SquidMouse(cellWidth, cellHeight, gridWidth, gridHeight, 0, 0, new InputAdapter() {

            // if the user clicks and mouseMoved hasn't already assigned a path to toCursor, then we call mouseMoved
            // ourselves and copy toCursor over to awaitedMoves.
            @Override
            public boolean touchUp(int screenX, int screenY, int pointer, int button) {
                mouseMoved(screenX, screenY);
                awaitedMoves.addAll(toCursor);
                return true;
            }

            @Override
            public boolean touchDragged(int screenX, int screenY, int pointer) {
                return mouseMoved(screenX, screenY);
            }

            // causes the path to the mouse position to become highlighted (toCursor contains a list of Coords that
            // receive highlighting). Uses DijkstraMap.findPathPreScanned() to find the path, which is rather fast.
            @Override
            public boolean mouseMoved(int screenX, int screenY) {
                if(!awaitedMoves.isEmpty())
                    return false;
                // This is needed because we center the camera on the player as he moves through a dungeon that is
                // multiple screens wide and tall, but the mouse still only can receive input on one screen's worth
                // of cells. (gridWidth >> 1) halves gridWidth, pretty much, and that we use to get the centered
                // position after adding to the player's position (along with the gridHeight).
                screenX += player.x - (gridWidth >> 1);
                screenY += player.y - (gridHeight >> 1);
                // we also need to check if screenX or screenY is out of bounds.
                if(screenX < 0 || screenY < 0 || screenX >= bigWidth || screenY >= bigHeight ||
                        (cursor.x == screenX && cursor.y == screenY))
                {
                    return false;
                }
                cursor = Coord.get(screenX, screenY);
                // This uses DijkstraMap.findPathPreScannned() to get a path as a List of Coord from the current
                // player position to the position the user clicked on. The "PreScanned" part is an optimization
                // that's special to DijkstraMap; because the part of the map that is viable to move into has
                // already been fully analyzed by the DijkstraMap.partialScan() method at the start of the
                // program, and re-calculated whenever the player moves, we only need to do a fraction of the
                // work to find the best path with that info.
                toCursor = playerToCursor.findPathPreScanned(cursor);
                // findPathPreScanned includes the current cell (goal) by default, which is helpful when
                // you're finding a path to a monster or loot, and want to bump into it, but here can be
                // confusing because you would "move into yourself" as your first move without this.
                // Getting a sublist avoids potential performance issues with removing from the start of an
                // ArrayList, since it keeps the original list around and only gets a "view" of it.
                if(!toCursor.isEmpty())
                {
                    toCursor = toCursor.subList(1, toCursor.size());
                }
                return false;
            }
        }));
        //Setting the InputProcessor is ABSOLUTELY NEEDED TO HANDLE INPUT
        Gdx.input.setInputProcessor(new InputMultiplexer(stage, input));
        //You might be able to get by with the next line instead of the above line, but the former is preferred.
        //Gdx.input.setInputProcessor(input);
        //we add display, our one visual component that moves, to the list of things that act in the main Stage.
        stage.addActor(display);
        //we add splitDisplay to splitStage, where it will be unchanged by camera moves in the main Stage.
        splitStage.addActor(splitDisplay);

//        System.out.println(playerRole.getKey());
        //System.out.println(playerRole.associated.get(4).primary + ": " + StringKit.join(", ", playerRole.associated.get(4).shallowContents()));
    }
    /**
     * Move the player if he isn't bumping into a wall or trying to go off the map somehow.
     * In a fully-fledged game, this would not be organized like this, but this is a one-file demo.
     * @param xmod
     * @param ymod
     */
    private void move(int xmod, int ymod) {
        int newX = player.x + xmod, newY = player.y + ymod;
        if (newX >= 0 && newY >= 0 && newX < bigWidth && newY < bigHeight
                && bareDungeon[newX][newY] != '#')
        {
            display.slide(pg, player.x, player.y, newX, newY, 0.11f, null);
            player = player.translate(xmod, ymod);
            FOV.reuseFOV(resistance, visible, player.x, player.y, 9.0, Radius.CIRCLE);
            // This is just like the constructor used earlier, but affects an existing GreasedRegion without making
            // a new one just for this movement.
            blockage.refill(visible, 0.0);
            seen.or(blockage.not());
            blockage.fringe8way();
            // By calling LineKit.pruneLines(), we adjust prunedDungeon to hold a variant on lineDungeon that removes any
            // line segments that haven't ever been visible. This is called again whenever seen changes.
            LineKit.pruneLines(lineDungeon, seen, LineKit.lightAlt, prunedDungeon);
        }
        else
        {
            // A SparseLayers knows how to move a Glyph (like the one for the player, pg) out of its normal alignment
            // on the grid, and also how to move it back again. Using bump() will move pg quickly about a third of the
            // way into a wall, then back to its former position at normal speed.
            display.bump(pg, Direction.getRoughDirection(xmod, ymod), 0.25f);
            // PanelEffect is a type of Action (from libGDX) that can run on a SparseLayers or SquidPanel.
            // This particular kind of PanelEffect creates a purple glow around the player when he bumps into a wall.
            // Other kinds can make explosions or projectiles appear.
            display.addAction(new PanelEffect.PulseEffect(display, 1f, floors, player, 3
                    , new float[]{SColor.CW_FADED_PURPLE.toFloatBits()}
                    ));
        }
        // removes the first line displayed of the Art of War text or its translation.
        lang.remove(0);
        // if the last line reduced the number of lines we can show to less than what we try to show, we fill in more
        // lines using a randomly selected fake language to translate the same Art of War text.
        while (lang.size() < bonusHeight - 1)
        {
            StringKit.wrap(lang, translator.cipher(artOfWar), split-2);
            translator.initialize(rng.getRandomElement(FakeLanguageGen.registered), 0L);
        }
    }

    /**
     * Draws the map, applies any highlighting for the path to the cursor, and then draws the player.
     */
    public void putMap()
    {
        //In many other situations, you would clear the drawn characters to prevent things that had been drawn in the
        //past from affecting the current frame. This isn't a problem here, but would probably be an issue if we had
        //monsters running in and out of our vision. If artifacts from previous frames show up, uncomment the next line.
        //display.clear();
        
        Item it;
        for (int x = 0; x < bigWidth; x++) {
            for (int y = 0; y < bigHeight; y++) {
                if(visible[x][y] > 0.0) {
                    // Here we use a convenience method in SparseLayers that puts a char at a specified position (the
                    // first three parameters), with a foreground color for that char (fourth parameter), as well as
                    // placing a background tile made of a one base color (fifth parameter) that is adjusted to bring it
                    // closer to FLOAT_LIGHTING (sixth parameter) based on how visible the cell is (seventh parameter,
                    // comes from the FOV calculations) in a way that fairly-quickly changes over time.
                    // This effect appears to shrink and grow in a circular area around the player, with the lightest
                    // cells around the player and dimmer ones near the edge of vision. This lighting is "consistent"
                    // because all cells at the same distance will have the same amount of lighting applied.
                    // We use prunedDungeon here so segments of walls that the player isn't aware of won't be shown.
                    it = things.get(Coord.get(x, y));
                    if(it != null)
                    {
                        display.putWithConsistentLight(x, y, it.symbol, it.color, bgColors[x][y], FLOAT_LIGHTING, visible[x][y]);
                    }
                    else
                        display.putWithConsistentLight(x, y, prunedDungeon[x][y], colors[x][y], bgColors[x][y], FLOAT_LIGHTING, visible[x][y]);
                } else if(seen.contains(x, y))
                {
                    // If a position isn't currently visible but was before, it will be in seen.
                    // Here, we don't show the changing light because this part of the map is remembered, not currently
                    // lit by a torch.
                    display.put(x, y, prunedDungeon[x][y], colors[x][y], SColor.lerpFloatColors(bgColors[x][y], GRAY_FLOAT, 0.45f));
                }
                // Note that if a position isn't visible or previously seen, we don't  put anything in display.
                // A full screen being replaced every round slows GWT to a crawl, though it's still rather fast on
                // desktop platforms. SparseLayers won't draw what it doesn't have to, so if nothing was placed in a
                // position, it skips over it entirely. This also applies to Glyph objects that can move, but are
                // currently in a cell with a clear background (the default); clear cells are used for cells that aren't
                // getting drawn in, so those Glyphs won't be rendered either unless you specifically draw one.
            }
        }
        display.clear(player.x, player.y, 0);
        Coord pt;
        for (int i = 0; i < toCursor.size(); i++) {
            pt = toCursor.get(i);
            // Uses a brighter light to trace the path to the cursor, mixing the background color with white.
            // putWithLight() can take mix amounts greater than 1 or less than 0 to mix with extra bias.
            display.putWithLight(pt.x, pt.y, bgColors[pt.x][pt.y], SColor.FLOAT_WHITE, 1.25f);
        }
        splitDisplay.clear(0);
        splitDisplay.fillBackground(FLOAT_LIGHTING);
        for (int i = 0; i < 6; i++) {
            splitDisplay.put(1, i, lang.get(i), SColor.DB_LEAD);
        }
        splitDisplay.fillArea(VERY_DARK_FLOAT, split, 0, gridWidth - split, bonusHeight);
        char first = playerRole.getKey().charAt(0);
        if(first == 'A' || first == 'E' || first == 'I' || first == 'O' || first == 'U')
            splitDisplay.put(split+1, 0, "You are an " + playerRole.getKey() + ".", FLOAT_LIGHTING, 0f);
        else
            splitDisplay.put(split+1, 0, "You are a " + playerRole.getKey() + ".", FLOAT_LIGHTING, 0f);
        first = enemyRole.getKey().charAt(0);
        if(first == 'A' || first == 'E' || first == 'I' || first == 'O' || first == 'U')
            splitDisplay.put(split+1, 2, "You must defeat an " + enemyRole.getKey() + ".", FLOAT_LIGHTING, 0f);
        else
            splitDisplay.put(split+1, 2, "You must defeat a " + enemyRole.getKey() + ".", FLOAT_LIGHTING, 0f);
        splitDisplay.put(split+1, 4, (playerRole.getValue().getPerks().first().toString()), FLOAT_LIGHTING, 0f);
//        for(ObText.ObTextEntry ent : playerRole.associated)
//        {
//            if(ent.primary.equals("attack"))
//            {
//                splitDisplay.put(split+1, 4, "You can make " + ent.firstAssociatedString() + " attacks.", FLOAT_LIGHTING, 0f);
//                break;
//            }
//        }
    }
    @Override
    public void render () {
        // standard clear the background routine for libGDX
        Gdx.gl.glClearColor(bgColor.r / 255.0f, bgColor.g / 255.0f, bgColor.b / 255.0f, 1.0f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // center the camera on the player's position
        stage.getCamera().position.x = pg.getX();
        stage.getCamera().position.y =  pg.getY();

        // need to display the map every frame, since we clear the screen to avoid artifacts.
        putMap();
        // if the user clicked, we have a list of moves to perform.
        if(!awaitedMoves.isEmpty())
        {
            // this doesn't check for input, but instead processes and removes Coords from awaitedMoves.
            if (!display.hasActiveAnimations()) {
                Coord m = awaitedMoves.remove(0);
                if(!toCursor.isEmpty())
                    toCursor.remove(0);
                move(m.x - player.x, m.y - player.y);
                // this only happens if we just removed the last Coord from awaitedMoves, and it's only then that we need to
                // re-calculate the distances from all cells to the player. We don't need to calculate this information on
                // each part of a many-cell move (just the end), nor do we need to calculate it whenever the mouse moves.
                if (awaitedMoves.isEmpty()) {
                    // the next two lines remove any lingering data needed for earlier paths
                    playerToCursor.clearGoals();
                    playerToCursor.resetMap();
                    // the next line marks the player as a "goal" cell, which seems counter-intuitive, but it works because all
                    // cells will try to find the distance between themselves and the nearest goal, and once this is found, the
                    // distances don't change as long as the goals don't change. Since the mouse will move and new paths will be
                    // found, but the player doesn't move until a cell is clicked, the "goal" is the non-changing cell, so the
                    // player's position, and the "target" of a pathfinding method like DijkstraMap.findPathPreScanned() is the
                    // currently-moused-over cell, which we only need to set where the mouse is being handled.
                    playerToCursor.setGoal(player);
                    // DijkstraMap.partialScan only finds the distance to get to a cell if that distance is less than some limit,
                    // which is 13 here. It also won't try to find distances through an impassable cell, which here is the blockage
                    // GreasedRegion that contains the cells just past the edge of the player's FOV area.
                    playerToCursor.partialScan(13, blockage);
                }
            }
        }
        // if we are waiting for the player's input and get input, process it.
        else if(input.hasNext()) {
            input.next();
        }
        // we need to do some work with viewports here so the language display (or game info messages in a real game)
        // will display in the same place even though the map view will move around. We have the language stuff set up
        // its viewport so it is in place and won't be altered by the map. Then we just tell the Stage for the language
        // texts to draw.
        splitStage.getViewport().apply(false);
        splitStage.draw();
        // certain classes that use scene2d.ui widgets need to be told to act() to process input.
        stage.act();
        // we have the main stage set itself up after the language stage has already drawn.
        stage.getViewport().apply(false);
        // stage has its own batch and must be explicitly told to draw().
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);

        // message box won't respond to clicks on the far right if the stage hasn't been updated with a larger size
        float currentZoomX = (float)width / gridWidth;
        // total new screen height in pixels divided by total number of rows on the screen
        float currentZoomY = (float)height / (gridHeight + bonusHeight);
        // message box should be given updated bounds since I don't think it will do this automatically
        splitDisplay.setBounds(0, 0, width, currentZoomY * bonusHeight);
        // SquidMouse turns screen positions to cell positions, and needs to be told that cell sizes have changed
        // a quirk of how the camera works requires the mouse to be offset by half a cell if the width or height is odd
        // (gridWidth & 1) is 1 if gridWidth is odd or 0 if it is even; it's good to know and faster than using % , plus
        // in some other cases it has useful traits (x % 2 can be 0, 1, or -1 depending on whether x is negative, while
        // x & 1 will always be 0 or 1).
        input.getMouse().reinitialize(currentZoomX, currentZoomY, gridWidth, gridHeight,
                (gridWidth & 1) * (int)(currentZoomX * -0.5f), (gridHeight & 1) * (int) (currentZoomY * -0.5f));        // the viewports are updated separately so each doesn't interfere with the other's drawn area.
        splitStage.getViewport().update(width, height, false);
        // we also set the bounds of that drawn area here for each viewport.
        splitStage.getViewport().setScreenBounds(0, 0, width, (int) splitDisplay.getHeight());
        // we did this for the language viewport, now again for the main viewport
        stage.getViewport().update(width, height, false);
        stage.getViewport().setScreenBounds(0, (int) splitDisplay.getHeight(),
                width, height - (int) splitDisplay.getHeight());
    }
}
// An explanation of hexadecimal float/double literals was mentioned earlier, so here it is.
// The literal 0x1p-9f is a good example; it is essentially the same as writing 0.001953125f,
// (float)Math.pow(2.0, -9.0), or (1f / 512f), but is possibly faster than the last two if the
// compiler can't optimize float division effectively, and is a good tool to have because these
// hexadecimal float or double literals always represent numbers accurately. To contrast,
// 0.3 - 0.2 is not equal to 0.1 with doubles, because tenths are inaccurate with floats and
// doubles, and hex literals won't have the option to write an inaccurate float or double.
// There's some slightly confusing syntax used to write these literals; the 0x means the first
// part uses hex digits (0123456789ABCDEF), but the p is not a hex digit and is used to start
// the "p is for power" exponent section. In the example, I used -9 for the power; this is a
// base 10 number, and is used to mean a power of 2 that the hex digits will be multiplied by.
// Because the -9 is a base 10 number, the f at the end is not a hex digit, and actually just
// means the literal is a float, in the same way 1.5f is a float. 2.0 to the -9 is the same as
// 1.0 / Math.pow(2.0, 9.0), but re-calculating Math.pow() is considerably slower if you run it
// for every cell during every frame. Though this is most useful for negative exponents because
// there are a lot of numbers after the decimal point to write out with 0.001953125 or the like,
// it is also sometimes handy when you have an integer or long written in hexadecimal and want
// to make it a float or double. You could use the hex long 0x9E3779B9L, for instance, but to
// write that as a double you would use 0x9E3779B9p0 , not the invalid syntax 0x9E3779B9.0 .
// We use p0 there because 2 to the 0 is 1, so multiplying by 1 gets us the same hex number.
// Very large numbers can also benefit by using a large positive exponent; using p10 and p+10
// as the last part of a hex literal are equivalent. You can see the hex literal for any given
// float with Float.toHexString(float), or for a double with Double.toHexString(double) .
// SColor provides the packed float versions of all color constants as hex literals in the
// documentation for each SColor.
// More information here: https://blogs.oracle.com/darcy/hexadecimal-floating-point-literals
