package com.example.jeffrey.game_2048;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by Jeffrey on 5/7/2016.
 */
public class ScoreView extends View {
    /** Paint for drawing label. */
    private Paint mGraphics;
    /** Title and text values. */
    private String mTitle, mText;
    /** Title color. */
    private static final int mTitleColor = 0xffeee4da;
    /** Background color. */
    private static final int mBackgroundColor = Color.rgb(184, 173, 158);
    /** Font to use for title and text. */
    private static final Typeface mFont = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD);

    public ScoreView(Context context, AttributeSet attrs) {
        super(context, attrs);

        mGraphics = new Paint();
        mGraphics.setStyle(Paint.Style.FILL);
        mGraphics.setTypeface(mFont);

        mTitle = "";
        mText = "";
    }

    @Override
    /** Draw label. */
    protected void onDraw(Canvas canvas) {
        int width = getWidth();
        int height = getHeight();

        // draw label background
        mGraphics.setColor(mBackgroundColor);
        canvas.drawRect(0, 0, width, height, mGraphics);

        Paint.FontMetrics metrics = mGraphics.getFontMetrics();

        // draw title
        mGraphics.setColor(mTitleColor);
        mGraphics.setTextSize(40);
        int titleWidth = (int) mGraphics.measureText(mTitle);
        canvas.drawText(mTitle, (width - titleWidth) / 2, height / 4 - metrics.ascent / 2, mGraphics);

        // draw text
        mGraphics.setColor(Color.WHITE);
        mGraphics.setTextSize(50);
        int textWidth = (int) mGraphics.measureText(mText);
        canvas.drawText(mText, (width - textWidth) / 2, 3 * height / 4 - metrics.ascent / 2, mGraphics);
    }

    /** Set title and redraw. */
    public void setTitle(String title) {
        mTitle = title;
        invalidate();
    }

    /** Set text and redraw. */
    public void setText(String text) {
        mText = text;
        invalidate();
    }

}
