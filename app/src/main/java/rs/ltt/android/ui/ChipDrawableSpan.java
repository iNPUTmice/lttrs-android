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
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ImageSpan;
import android.widget.EditText;

import androidx.annotation.AttrRes;
import androidx.annotation.NonNull;

import com.google.android.material.chip.ChipDrawable;
import com.google.android.material.color.MaterialColors;

import java.util.HashSet;
import java.util.Set;

import rs.ltt.android.R;
import rs.ltt.jmap.common.entity.EmailAddress;
import rs.ltt.jmap.mua.util.EmailAddressToken;
import rs.ltt.jmap.mua.util.EmailAddressTokenizer;
import rs.ltt.jmap.mua.util.EmailAddressUtil;


public class ChipDrawableSpan extends ImageSpan {

    private final EmailAddressToken emailAddressToken;

    private ChipDrawableSpan(@NonNull final ChipDrawable drawable, final EmailAddressToken token) {
        super(drawable);
        this.emailAddressToken = token;
    }

    public static void apply(final Context context, final Editable editable, final boolean requireExplicitDelimiter) {
        final Set<EmailAddressToken> tokens = new HashSet<>(EmailAddressTokenizer.tokenize(editable, requireExplicitDelimiter));
        final ChipDrawableSpan[] spans = editable.getSpans(0, editable.length() - 1, ChipDrawableSpan.class);
        for (ChipDrawableSpan span : spans) {
            if (tokens.remove(span.emailAddressToken)) {
                continue;
            }
            editable.removeSpan(span);
        }
        for (final EmailAddressToken token : tokens) {
            final ChipDrawable chip = ChipDrawable.createFromResource(context, R.xml.address);
            if (EmailAddressUtil.isValid(token.getEmailAddress())) {
                chip.setChipBackgroundColor(getBackgroundColor(context, R.attr.colorSurface));
            } else {
                chip.setChipBackgroundColor(getBackgroundColor(context, R.attr.colorSurfaceWarning));
            }
            final EmailAddress emailAddress = token.getEmailAddress();
            if (TextUtils.isEmpty(emailAddress.getName())) {
                chip.setText(emailAddress.getEmail());
            } else {
                chip.setText(emailAddress.getName());
            }
            chip.setBounds(0, 0, chip.getIntrinsicWidth(), chip.getIntrinsicHeight());
            ChipDrawableSpan span = new ChipDrawableSpan(chip, token);
            editable.setSpan(span, token.getStart(), token.getEnd() + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    private static ColorStateList getBackgroundColor(final Context context, final @AttrRes int attrRes) {
        return ColorStateList.valueOf(MaterialColors.getColor(context, attrRes, ChipDrawableSpan.class.getCanonicalName()));
    }

    public static void reset(EditText editText) {
        final Editable editable = editText.getEditableText();
        if (TextUtils.isEmpty(editable)) {
            return;
        }
        editable.clearSpans();
        ChipDrawableSpan.apply(editText.getContext(), editable, editText.hasFocus());
    }

    @Override
    public int getSize(@NonNull final Paint paint,
                       final CharSequence text,
                       final int start,
                       final int end,
                       final Paint.FontMetricsInt fontMetrics) {
        final Rect bounds = getDrawable().getBounds();
        if (fontMetrics != null) {
            final Paint.FontMetricsInt fmPaint = paint.getFontMetricsInt();
            int fontHeight = fmPaint.descent - fmPaint.ascent;
            int drHeight = bounds.bottom - bounds.top;
            int centerY = fmPaint.ascent + fontHeight / 2;

            fontMetrics.ascent = centerY - drHeight / 2;
            fontMetrics.top = fontMetrics.ascent;
            fontMetrics.bottom = centerY + drHeight / 2;
            fontMetrics.descent = fontMetrics.bottom;
        }
        return bounds.right + (int) (paint.getFontSpacing() / 6);
    }

    @Override
    public void draw(@NonNull final Canvas canvas,
                     final CharSequence text,
                     final int start,
                     final int end,
                     final float x,
                     final int top,
                     final int y,
                     final int bottom,
                     @NonNull final Paint paint) {
        final Drawable drawable = getDrawable();
        final Rect bounds = drawable.getBounds();
        canvas.save();
        final Paint.FontMetricsInt fontMetrics = paint.getFontMetricsInt();
        final int fontHeight = fontMetrics.descent - fontMetrics.ascent;
        final int centerY = y + fontMetrics.descent - fontHeight / 2;
        final int transY = centerY - (bounds.bottom - bounds.top) / 2;
        canvas.translate(x, transY);
        drawable.draw(canvas);
        canvas.restore();
    }

}
