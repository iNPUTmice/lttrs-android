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

package rs.ltt.android.util;

import android.graphics.Rect;
import android.view.TouchDelegate;
import android.view.View;
import android.view.ViewParent;

public class Touch {

    public static void expandTouchArea(final View view, final int dp) {
        final ViewParent viewParent = view.getParent();
        if (viewParent instanceof View) {
            expandTouchArea((View) viewParent, view, dp);
        } else {
            throw new IllegalArgumentException(String.format("%s is not a view", viewParent.getClass()));
        }
    }

    private static void expandTouchArea(final View parent, final View view, final int dp) {
        float scale = parent.getContext().getResources().getDisplayMetrics().density;
        int padding = (int) (scale * dp);
        parent.post(() -> {
            Rect rect = new Rect();
            view.getHitRect(rect);
            rect.top -= padding;
            rect.left -= padding;
            rect.right += padding;
            rect.bottom += padding;
            parent.setTouchDelegate(new TouchDelegate(rect, view));
        });
    }
}
