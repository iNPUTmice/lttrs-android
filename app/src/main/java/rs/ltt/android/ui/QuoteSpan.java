/*
 * Copyright 2019 Daniel Gultsch
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package rs.ltt.android.ui;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.text.Layout;
import android.text.TextPaint;
import android.text.style.CharacterStyle;
import android.text.style.LeadingMarginSpan;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import org.hsluv.HUSLColorConverter;

public class QuoteSpan extends CharacterStyle implements LeadingMarginSpan {

    private static final float WIDTH_SP = 2f;
    private static final float PADDING_LEFT_SP = 1.5f;
    private static final float PADDING_RIGHT_SP = 8f;
    private final int depth;
    private final int color;
    private final boolean nightMode;
    private final int paddingPerLayer;
    private final int width;
    private final int paddingLeft;
    private final int paddingRight;

    public QuoteSpan(final int depth, final Context context) {
        final DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        this.depth = depth;
        this.nightMode =
                (context.getResources().getConfiguration().uiMode
                                & Configuration.UI_MODE_NIGHT_MASK)
                        == Configuration.UI_MODE_NIGHT_YES;
        this.color = color(depth - 1, nightMode);
        this.width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, WIDTH_SP, metrics);

        this.paddingPerLayer =
                (int)
                        TypedValue.applyDimension(
                                TypedValue.COMPLEX_UNIT_SP, PADDING_LEFT_SP, metrics);

        this.paddingLeft =
                depth
                        * width
                        * (int)
                                TypedValue.applyDimension(
                                        TypedValue.COMPLEX_UNIT_SP, PADDING_LEFT_SP, metrics);
        this.paddingRight =
                (int)
                        TypedValue.applyDimension(
                                TypedValue.COMPLEX_UNIT_SP, PADDING_RIGHT_SP, metrics);
    }

    private static int color(int level, boolean dark) {
        final double[] hsluv =
                new double[] {
                    (173.1 + (level * 80)) % 360, 80, dark ? 70 : 50,
                };
        final double[] rgb = HUSLColorConverter.hsluvToRgb(hsluv);
        return Color.rgb(
                (int) Math.round(rgb[0] * 255),
                (int) Math.round(rgb[1] * 255),
                (int) Math.round(rgb[2] * 255));
    }

    @Override
    public void updateDrawState(TextPaint tp) {
        tp.setColor(this.color);
    }

    @Override
    public int getLeadingMargin(boolean first) {
        return paddingLeft + width + paddingRight;
    }

    @Override
    public void drawLeadingMargin(
            Canvas c,
            Paint p,
            int x,
            int dir,
            int top,
            int baseline,
            int bottom,
            CharSequence text,
            int start,
            int end,
            boolean first,
            Layout layout) {
        final Paint.Style style = p.getStyle();
        final int color = p.getColor();
        p.setStyle(Paint.Style.FILL);
        p.setColor(this.color);
        for (int i = 0; i < depth; ++i) {
            final int dc = color(i, this.nightMode);
            p.setColor(dc);
            int additionalDistance = paddingPerLayer * i * width;
            c.drawRect(
                    x + dir * paddingPerLayer + additionalDistance,
                    top,
                    x + dir * (paddingPerLayer + additionalDistance + width),
                    bottom,
                    p);
        }
        p.setStyle(style);
        p.setColor(color);
    }
}
