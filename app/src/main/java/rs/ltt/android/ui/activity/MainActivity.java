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

package rs.ltt.android.ui.activity;

import android.os.Bundle;
import android.os.SystemClock;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rs.ltt.android.LttrsApplication;

public class MainActivity extends AppCompatActivity {

    private static final Logger LOGGER = LoggerFactory.getLogger(MainActivity.class);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final long start = SystemClock.elapsedRealtime();
        final Long accountId = LttrsApplication.get(this).getMostRecentlySelectedAccountId();
        if (accountId != null) {
            LttrsActivity.launch(this, accountId);
        } else {
            SetupActivity.launch(this);
        }
        finish();
        LOGGER.debug("splash screen was visible for {}ms", (SystemClock.elapsedRealtime() - start));
    }
}
