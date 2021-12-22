/*
 * Copyright 2020 Daniel Gultsch
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

import android.content.Context;
import android.content.pm.PackageManager;
import rs.ltt.android.R;

public class UserAgent {

    private static final String UNKNOWN = "unknown";

    public static String get(final Context context) {
        return String.format("%s/%s", context.getString(R.string.app_name), getVersion(context));
    }

    private static String getVersion(final Context context) {
        final String packageName = context.getPackageName();
        try {
            return context.getPackageManager().getPackageInfo(packageName, 0).versionName;
        } catch (final PackageManager.NameNotFoundException | RuntimeException e) {
            return UNKNOWN;
        }
    }
}
