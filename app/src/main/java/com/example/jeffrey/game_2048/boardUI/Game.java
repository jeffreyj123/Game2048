package com.example.jeffrey.game_2048.boardUI;

import com.example.jeffrey.game_2048.GameMain;

import java.util.Random;

/** The input/output and GUI controller for play of a game of 2048.
 *  @author Jeffrey Jacinto */
public class Game {

    /** Probability of choosing 2 as random tile (as opposed to 4). */
    static final double LOW_TILE_PROBABILITY = 0.9;

    /** The GUI interface. */
    private GameBoard mDisplay;
    /** Number of rows and of columns. */
    private int mRows;

    /** The tiles currently on the board. */
    private Tile[][] mTiles;
    /** Tiles that are to be merged with tiles already at the
     *  indicated squares. */
    private Tile[][] mTiles2;
    /** Tiles that will be displayed after next displayMoves call. */
    private Tile[][] mNextTiles;
    /** Number of pending moves to be made by displayMoves. */
    private int mMoves;

    /** PRNG for generating random tiles or keys. */
    private final Random mRandom;

    /** A new ROWS x ROWS Game whose window title is TITLE.  SEED is a seed for
     *  the PRNG, or 0 to indicate random seeding.  LOG is true iff all keys
     *  pressed and random tiles returned are to be written to standard output.
     *  GRAPHIC is true iff the window is to be displayed.  TESTING is true
     *  iff key pressings and random tiles come from the standard input rather
     *  than user input.
     * @param board
     * @param rows */
    public Game(GameBoard board, int rows) {
        if (rows < 4) {
            throw new IllegalArgumentException("rows must be >= 4");
        }
        mRows = rows;
        mRandom = new Random();

        mDisplay = board;

        clear();
    }

    /** Clear and reset the current state to an empty board. */
    public void clear() {
        mTiles = new Tile[mRows][mRows];
        mTiles2 = new Tile[mRows][mRows];
        mNextTiles = new Tile[mRows][mRows];
        mMoves = 0;

        mDisplay.clear();
    }

    /** Create a new Tile showing VALUE at ROW and COL.
     * @param value
     * @param row
     * @param col */
    public void addTile(int value, int row, int col) {
        if (mMoves != 0) {
            throw badArg("must do pending moves before addTile");
        }
        if (mTiles[row][col] != null) {
            throw badArg("square at (%d, %d) is already occupied", row, col);
        }

        // add tile to board
        mTiles[row][col] = new Tile(value);
        mTiles[row][col].setPosition(row, col);

        // display tile add, then clear new boards
        mDisplay.displayMoves(mTiles, mTiles2, mTiles);
        mTiles2 = new Tile[mRows][mRows];
        mNextTiles = new Tile[mRows][mRows];
    }

    /** Move a tile whose value is VALUE from (ROW, COL) to (NEWROW, NEWCOL).
     *  An appropriate tile must be present at (ROW, COL).
     * @param value
     * @param row
     * @param col
     * @param newRow
     * @param newCol */
    public void moveTile(int value, int row, int col, int newRow, int newCol) {
        Tile tile = mTiles[row][col];
        if (tile == null) {
            throw badArg("no tile at (%d, %d)", row, col);
        }
        if (row == newRow && col == newCol) {
            return;
        }
        if (mTiles2[row][col] != null) {
            throw badArg("tile at (%d, %d) is already merged", row, col);
        } else if (tile.getValue() != value) {
            throw badArg("wrong value (%d) for tile at (%d, %d)",
                    value, row, col);
        } else if (mTiles[newRow][newCol] != null) {
            // other tile should have been moved and cleared first
            throw badArg("square at (%d, %d) is occupied", newRow, newCol);
        }

        // update number of moves to be completed before accepting new command
        mMoves += 1;
        mTiles[row][col] = null; // mark tile as moved
        // place tile in new position and mark for display
        mNextTiles[newRow][newCol] = mTiles[newRow][newCol] = tile;
    }

    /** Move a tile whose value is VALUE from (ROW, COL) to (NEWROW, NEWCOL),
     *  merging it with the tile of the same value that is present there to
     *  create a new one with value NEWVALUE. Appropriate tiles must be
     *  present at (ROW, COL) and (NEWROW, NEWCOL).
     * @param value
     * @param newValue
     * @param row
     * @param col
     * @param newRow
     * @param newCol */
    public void mergeTile(int value, int newValue, int row, int col,
                          int newRow, int newCol) {
        Tile tile = mTiles[row][col];
        if (tile == null) {
            throw badArg("no tile at (%d, %d)", row, col);
        } else if (tile.getValue() != value) {
            throw badArg("wrong value (%d) for tile at (%d, %d)",
                    value, row, col);
        } else if (mTiles[newRow][newCol] == null) {
            throw badArg("no tile to merge with at (%d, %d)", row, col);
        } else if (mTiles2[newRow][newCol] != null) {
            throw badArg("tile at (%d, %d) is already merged", newRow, newCol);
        } else if (mTiles[newRow][newCol].getValue() != tile.getValue()) {
            throw badArg("merging mismatched tiles at (%d, %d)",
                    newRow, newCol);
        }

        // update number of moves to be completed before accepting new command
        mMoves += 1;
        mTiles[row][col] = null; // mark tile as merged
        mTiles2[newRow][newCol] = tile; // mark tile to be merged in display
        // mark new tile to be added to board
        mNextTiles[newRow][newCol] = new Tile(newValue);
        if (newValue == GameMain.WIN) {
            GameMain.hasWon = true;
        }
    }

    /** Animate and complete all pending moves. Has no effect (and logs no
     *  output) if there are no moves. */
    public void displayMoves() {
        if (mMoves == 0) { // no moves
            return;
        }
        for (int r = 0; r < mRows; r += 1) {
            for (int c = 0; c < mRows; c += 1) {
                // move all unmerged tiles to new board
                if (mNextTiles[r][c] == null) {
                    mNextTiles[r][c] = mTiles[r][c];
                }
            }
        }
        mDisplay.displayMoves(mTiles, mTiles2, mNextTiles);
        mMoves = 0; // reset moves
        mTiles = mNextTiles; // update board
        // reset new boards
        mTiles2 = new Tile[mRows][mRows];
        mNextTiles = new Tile[mRows][mRows];
    }

    /** Indicate end of game. */
    public void endGame() {
        mDisplay.markEnd();
    }


    /** Generate the specs for a random tile, ignoring current board contents.
     *  Return a triple { V, R, C }, giving the tile value (either 2 or 4),
     *  row, and column.
     * @param emptyTiles
     * @return  */
    public int[] getRandomTile(int emptyTiles) {
        int result[], randomVal = mRandom.nextInt(emptyTiles);
        int value = 2 * (1 + (int) (mRandom.nextDouble()
                / LOW_TILE_PROBABILITY));
        result = new int[] { value, randomVal };
        return result;
    }

    /** Add tiles to display. Tile is represented by a triple { V, R, C },
     *  giving the tile value, row, and column. */
    public void setTiles(int[][] tiles) {
        for (int[] tile : tiles) {
            addTile(tile[0], tile[1], tile[2]);
        }
    }

    /** Return the display state as a JSON string. */
    public String toJSON() {
        return mDisplay.toJSON();
    }

    /** Return an IllegalArgumentException with the message given by
     *  MSG and ARGS as for String.format. */
    static IllegalArgumentException badArg(String msg, Object... args) {
        return new IllegalArgumentException(String.format(msg, args));
    }

}
