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

package rs.ltt.android;

import android.app.Activity;
import android.app.Application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rs.ltt.android.database.AppDatabase;
import rs.ltt.android.ui.notification.ForegroundServiceNotification;

public class LttrsApplication extends Application {

    private final static Object CACHE_LOCK = new Object();
    private final Logger LOGGER = LoggerFactory.getLogger(LttrsApplication.class);
    private Long mostRecentlySelectedAccountId = null;

    public static LttrsApplication get(final Application application) {
        if (application instanceof LttrsApplication) {
            return (LttrsApplication) application;
        }
        throw new IllegalStateException("Application is not a " + LttrsApplication.class.getSimpleName());
    }

    public static LttrsApplication get(final Activity activity) {
        return get(activity.getApplication());
    }

    @Override
    public void onCreate() {
        super.onCreate();
        ForegroundServiceNotification.createChannel(getApplicationContext());
    }

    public boolean noAccountsConfigured() {
        synchronized (CACHE_LOCK) {
            if (mostRecentlySelectedAccountId != null) {
                return false;
            }
        }
        return !AppDatabase.getInstance(this).accountDao().hasAccounts();
    }

    public Long getMostRecentlySelectedAccountId() {
        synchronized (CACHE_LOCK) {
            if (this.mostRecentlySelectedAccountId == null) {
                final Long id = AppDatabase.getInstance(this).accountDao().getMostRecentlySelectedAccountId();
                LOGGER.info("read most recently selected account id from database: {}", id);
                this.mostRecentlySelectedAccountId = id;
            }
            return mostRecentlySelectedAccountId;
        }
    }

    public void invalidateMostRecentlySelectedAccountId() {
        synchronized (CACHE_LOCK) {
            this.mostRecentlySelectedAccountId = null;
        }
    }
}
