/*
 * Copyright (C) 2016 Jake Wharton
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rs.ltt.android;

import androidx.annotation.NonNull;
import androidx.test.espresso.IdlingResource;
import com.google.common.base.Preconditions;
import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;

public final class OkHttp3IdlingResource implements IdlingResource {

    private final String name;
    private final Dispatcher dispatcher;
    volatile ResourceCallback callback;

    private OkHttp3IdlingResource(final String name, final Dispatcher dispatcher) {
        this.name = name;
        this.dispatcher = dispatcher;
        dispatcher.setIdleCallback(
                () -> {
                    final ResourceCallback callback = this.callback;
                    if (callback != null) {
                        callback.onTransitionToIdle();
                    }
                });
    }

    @NonNull
    public static OkHttp3IdlingResource create(@NonNull String name, @NonNull OkHttpClient client) {
        Preconditions.checkNotNull(name);
        Preconditions.checkNotNull(client);
        return new OkHttp3IdlingResource(name, client.dispatcher());
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isIdleNow() {
        boolean idle = (dispatcher.runningCallsCount() == 0);
        if (idle && callback != null) callback.onTransitionToIdle();
        return idle;
    }

    @Override
    public void registerIdleTransitionCallback(ResourceCallback callback) {
        this.callback = callback;
    }
}
