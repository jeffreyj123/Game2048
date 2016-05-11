package com.example.jeffrey.game_2048.boardUI;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.util.Log;

import java.util.HashMap;

import static com.example.jeffrey.game_2048.boardUI.GameBoard.*;

/** Represents the image of a numbered tile on a 2048 board.
 *  @author Jeffrey Jacinto
 */
public class Tile extends Drawable {
    /** Amount to move in TICK milliseconds (in pixels). */
    static final double MOVE_DELTA = 0.016 * ROW_SIZE * TICK * 2;

    /** Mapping from numbers on tiles to their text and background
     *  colors. */
    static final HashMap<Integer, int[]> TILE_COLORS = new HashMap<>();

    /** List of tile values and corresponding background and foreground
     *  color values. */
    private static final int[][] TILE_COLOR_MAP = {
            { 2, 0xff776e65, 0xffeee4da },
            { 4, 0xff776e65, 0xffede0c8 },
            { 8, 0xfff9f6f2, 0xfff2b179 },
            { 16, 0xfff9f6f2, 0xfff59563 },
            { 32, 0xfff9f6f2, 0xfff67c5f },
            { 64, 0xfff9f6f2, 0xfff65e3b },
            { 128, 0xfff9f6f2, 0xffedcf72 },
            { 256, 0xfff9f6f2, 0xffedcc61 },
            { 512, 0xfff9f6f2, 0xffedc850 },
            { 1024, 0xfff9f6f2, 0xffedc53f },
            { 2048, 0xfff9f6f2, 0xffedc22e },
    };

    static {
        /* { "LABEL", "TEXT COLOR (hex)", "BACKGROUND COLOR (hex)" } */
        for (int[] tileData : TILE_COLOR_MAP) {
            TILE_COLORS.put(tileData[0],
                    new int[] { tileData[1], tileData[2] });
        }
    };

    /** Paint for drawing tile. */
    private final Paint graphics = new Paint();

    /** Row and column data. */
    private int mRow, mCol;

    /** A new tile at (0, 0) displaying VALUE. */
    public Tile(int value) {
        graphics.setStyle(Paint.Style.FILL);
        graphics.setTypeface(TILE_FONT);
        mValue = value;
    }

    /** Set my position to the square at (ROW, COL). */
    public void setPosition(int row, int col) {
        mCol = col;
        mRow = row;
        setCoord();
    }

    /** Set X, Y coordinates based on COL, ROW respectively */
    void setCoord() {
        mX = toCoord(mCol);
        mY = toCoord(mRow);
    }

    /** Return the value supplied to my constructor. */
    int getValue() {
        return mValue;
    }

    /** Return the value after one animation step for a coordinate
     *  transitioning from X0 to X1. */
    double step(double x0, double x1) {
        if (x0 > x1) {
            return Math.max(x1, x0 - MOVE_DELTA);
        } else if (x0 < x1) {
            return Math.min(x1, x0 + MOVE_DELTA);
        } else {
            return x0;
        }
    }

    /** Update my position toward (XDEST, YDEST) and size for one animation
     *  step.  Returns true iff there was a change. */
    boolean tick(double xdest, double ydest) {
        if (xdest != mX || ydest != mY) {
            mX = step(mX, xdest);
            mY = step(mY, ydest);
            return true;
        }
        return false;
    }

    /** My tile value. */
    private final int mValue;
    /** My current position. */
    private double mX, mY;

    @Override
    /** Draw the tile. */
    public void draw(Canvas canvas) {
        int x = (int) Math.rint(mX), y = (int) Math.rint(mY);
        int textWidth;

        // set text size by value
        if (mValue < 100) {
            graphics.setTextSize(TILE_FONT2_SIZE);
        } else if (mValue < 1000) {
            graphics.setTextSize(TILE_FONT3_SIZE);
        } else {
            graphics.setTextSize(TILE_FONT4_SIZE);
        }

        // set text values according to tile value
        Paint.FontMetrics metrics = graphics.getFontMetrics();
        String label = Integer.toString(mValue);
        textWidth = (int) graphics.measureText(label);

        // draw tile background
        graphics.setColor(TILE_COLORS.get(mValue)[1]);
        canvas.drawRect(x, y, x + TILE_SIDE, y + TILE_SIDE, graphics);

        // draw tile text
        graphics.setColor(TILE_COLORS.get(mValue)[0]);
        canvas.drawText(label, x + (TILE_SIDE - textWidth) / 2,
                y + (2 * TILE_SIDE - metrics.ascent) / 4, graphics);
    }

    @Override
    /** Drawable abstract method not used/not implemented. */
    public void setAlpha(int alpha) { }

    @Override
    /** Drawable abstract method not used/not implemented. */
    public void setColorFilter(ColorFilter colorFilter) { }

    @Override
    /** Drawable abstract method not used/not implemented. */
    public int getOpacity() {
        return 0;
    }

    /** Return tile data as JSON string with values ROW, COL, and VALUE. */
    public String toJSON() {
        StringBuilder tileJSON = new StringBuilder("{ row : ");
        tileJSON.append(mRow).append(", col : ").append(mCol).
                append(", value : ").append(mValue);
        tileJSON.append("}");
        return tileJSON.toString();
    }

}
