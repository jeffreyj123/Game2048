package com.example.jeffrey.game_2048.boardUI;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.example.jeffrey.game_2048.GameMain;

import java.util.ArrayList;

/**
 * Created by Jeffrey on 5/4/2016.
 */
public class GameBoard extends View {
    private Paint mGraphics;

    /** Length of side of board */
    private int mBoardSide;

    /** True iff game over and marks "GAME OVER" to be displayed */
    private boolean mEnd;

    /** List of Tiles being displayed */
    private final ArrayList<Tile> mTiles = new ArrayList<>();

    /** Number of rows/columns */
    private final int mSize = 4;

    /** True iff size has not been set */
    private boolean sizesNotSet;

    /** Colors of empty squares and grid lines. */
    static final int
            EMPTY_SQUARE_COLOR = Color.rgb(205, 192, 176),
            BAR_COLOR = Color.rgb(184, 173, 158);

    /** Bar width separating tiles and length of tile's side
     *  (pixels). */
    static int
            TILE_SEP = 15,
            TILE_SIDE = 100,
            ROW_SIZE = TILE_SEP + TILE_SIDE;

    static final Typeface TILE_FONT = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);
    /** Font used for numbering on tiles with <= 2 digits. */
    static int TILE_FONT2_SIZE = 48;
    /** Font used for numbering on tiles with 3 digits. */
    static int TILE_FONT3_SIZE = 40;
    /** Font used for numbering on tiles with 4 digits. */
    static int TILE_FONT4_SIZE = 32;

    /** Color for overlay text on board. */
    static final int OVERLAY_COLOR = Color.argb(64, 200, 0, 0);

    /** Font for overlay text on board. */
    static int OVERLAY_FONT_SIZE = 64;

    /** Initial sizes for member data above */
    static final int INIT_SIZE = 475;
    static final int TILE_SEP_IN = TILE_SEP, TILE_SIDE_IN = TILE_SIDE, ROW_SIZE_IN = ROW_SIZE;
    static final int FONT2_SIZE_IN = TILE_FONT2_SIZE, FONT3_SIZE_IN = TILE_FONT3_SIZE;
    static final int FONT4_SIZE_IN = TILE_FONT4_SIZE, OVERLAY_SIZE_IN = OVERLAY_FONT_SIZE;

    /** Wait between animation steps (in milliseconds). */
    static final int TICK = 16;

    public GameBoard(Context context, AttributeSet attrs) {
        super(context, attrs);

        mGraphics = new Paint();
        mGraphics.setStyle(Paint.Style.FILL);
        mGraphics.setTypeface(TILE_FONT);
        mEnd = false;
        sizesNotSet = true;

        Log.d("gameboard", "constrcuted board");
    }

    /** Calculate sizes based on initial values to maintain ratios. */
    private void calcSizes() {
        float multiplier = mBoardSide / (float) INIT_SIZE;
        TILE_SEP = (int) (multiplier * TILE_SEP_IN);
        TILE_SIDE = (int) (multiplier * TILE_SIDE_IN);
        ROW_SIZE = (int) (multiplier * ROW_SIZE_IN);
        Log.d("Row size", Integer.toString(ROW_SIZE));
        TILE_FONT2_SIZE = (int) (multiplier * FONT2_SIZE_IN);
        TILE_FONT3_SIZE = (int) (multiplier * FONT3_SIZE_IN);
        TILE_FONT4_SIZE = (int) (multiplier * FONT4_SIZE_IN);
        OVERLAY_FONT_SIZE = (int) (multiplier * OVERLAY_SIZE_IN);
        sizesNotSet = false;
    }

    /** Clear all tiles from the board. */
    synchronized void clear() {
        mTiles.clear();
        mEnd = false;
        invalidate();
    }

    /** Indicate that "GAME OVER" label should be displayed. */
    synchronized void markEnd() {
        mEnd = true;
        invalidate();
    }

    /** Return the pixel distance corresponding to A rows or columns. */
    static int toCoord(int a) {
        return TILE_SEP + a * ROW_SIZE;
    }

    @Override
    /** Draw the gameboard. */
    protected void onDraw(Canvas canvas) {
        mBoardSide = getWidth();
        // Init sizes if opening for first time
        if (sizesNotSet) {
            calcSizes();
            for (Tile tile : mTiles) {
                tile.setCoord();
            }
        }
        // draw board background
        mGraphics.setColor(EMPTY_SQUARE_COLOR);
        canvas.drawRect(0, 0, mBoardSide, mBoardSide, mGraphics);

        // draw grid bars
        mGraphics.setColor(BAR_COLOR);
        for (int k = 0; k <= mBoardSide; k += ROW_SIZE) {
            canvas.drawRect(0, k, mBoardSide, k + TILE_SEP, mGraphics);
            canvas.drawRect(k, 0, k + TILE_SEP, mBoardSide, mGraphics);
        }

        // draw tiles
        for (Tile tile : mTiles) {
            tile.draw(canvas);
        }

        // draw end game text ("GAME OVER" if lost, "YOU WON" if won)
        if (mEnd) {
            String endText = "GAME OVER";
            if (GameMain.hasWon) {
                endText = "YOU WON";
                GameMain.hasWon = false;
            }
            // get text sizes relative to canvas
            int textWidth = (int) mGraphics.measureText(endText);
            mGraphics.setTextSize(OVERLAY_FONT_SIZE);
            Paint.FontMetrics metrics = mGraphics.getFontMetrics();

            mGraphics.setColor(OVERLAY_COLOR);
            canvas.drawText(endText, (mBoardSide - textWidth) / 2,
                    (2 * mBoardSide + metrics.ascent) / 4, mGraphics);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(mBoardSide, mBoardSide, oldw, oldh);
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec){
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int squareDim = getMeasuredWidth();
        setMeasuredDimension(squareDim, squareDim);

    }


    /** Given that TILES represents the state of a board (with TILES[r][c]
     *  being the tile at (r, c), or null if there is no tile
     *  there), TILES2 represents the state of tiles that are to be
     *  merged into existing tiles, and NEXTTILES represents the desired
     *  resulting state, animate the depicted moves and update mTiles
     *  accordingly. */
    public synchronized void displayMoves(Tile[][] tiles,
                                          Tile[][] tiles2,
                                          Tile[][] nextTiles) {
        boolean changing;
        do {
            mTiles.clear();
            changing = false;
            for (int r = 0; r < mSize; r += 1) {
                for (int c = 0; c < mSize; c += 1) {
                    boolean change;
                    change = false;
                    Tile tile = tiles[r][c];
                    if (tile == null) {
                        continue;
                    }
                    Tile tile2 = tiles2[r][c];
                    double xDest = toCoord(c), yDest = toCoord(r);
                    if (tile.tick(xDest, yDest)) {
                        change = true;
                        mTiles.add(tile);
                    }
                    if (tile2 != null) {
                        if (tile2.tick(xDest, yDest)) {
                            change = true;
                        }
                    }
                    if (change) {
                        mTiles.add(tile);
                        if (tile2 != null) {
                            mTiles.add(tile2);
                        }
                    }
                    if (!change) {
                        Tile next = nextTiles[r][c];
                        next.setPosition(r, c);
                        mTiles.add(next);
                        change = next.tick(xDest, yDest);
                    }
                    changing |= change;
                }
            }
            invalidate();
            try {
                wait(TICK);
            } catch (InterruptedException excp) {
                assert false : "Internal error: unexpected interrupt";
            }
        } while (changing);
    }

    /** Return state as JSON string, contain array of tile info stored in mTiles
     */
    public String toJSON() {
        StringBuilder stateJSON = new StringBuilder("[");
        if (mTiles.size() > 0) {
            for (Tile tile : mTiles) {
                stateJSON.append(tile.toJSON());
                stateJSON.append(",");
            }
            stateJSON.setLength(stateJSON.length() - 1);
        }
        stateJSON.append("]");
        return stateJSON.toString();
    }

}
