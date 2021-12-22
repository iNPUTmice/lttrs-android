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

package rs.ltt.android.ui.widget;

import android.content.Context;
import android.text.Editable;
import android.util.AttributeSet;
import androidx.appcompat.widget.AppCompatEditText;
import rs.ltt.android.ui.ChipDrawableSpan;

public class EmailAddressEditText extends AppCompatEditText {
    public EmailAddressEditText(Context context) {
        super(context);
    }

    public EmailAddressEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public EmailAddressEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onSelectionChanged(final int start, final int end) {
        super.onSelectionChanged(start, end);
        final Editable editable = getEditableText();
        final ChipDrawableSpan[] spans =
                editable.getSpans(0, editable.length(), ChipDrawableSpan.class);
        int beginEditableArea = 0;
        for (ChipDrawableSpan span : spans) {
            beginEditableArea = editable.getSpanEnd(span);
        }
        final int resetStartTo = Math.max(beginEditableArea, start);
        final int resetEndTo = Math.max(beginEditableArea, end);
        if (resetStartTo != start || resetEndTo != end) {
            setSelection(resetStartTo, resetEndTo);
        }
    }
}
