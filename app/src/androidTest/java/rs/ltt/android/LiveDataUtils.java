package rs.ltt.android;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import rs.ltt.android.util.MainThreadExecutor;

/* Copyright 2019 Google LLC.
SPDX-License-Identifier: Apache-2.0 */
public class LiveDataUtils {

    private LiveDataUtils() {}

    public static <T> T getOrAwaitValue(final LiveData<T> liveData) throws InterruptedException {
        final Object[] data = new Object[1];
        final CountDownLatch latch = new CountDownLatch(1);
        final Observer<T> observer =
                new Observer<T>() {
                    @Override
                    public void onChanged(@Nullable T o) {
                        data[0] = o;
                        latch.countDown();
                        liveData.removeObserver(this);
                    }
                };
        MainThreadExecutor.getInstance().execute(() -> liveData.observeForever(observer));

        // Don't wait indefinitely if the LiveData is not set.
        if (!latch.await(2, TimeUnit.SECONDS)) {
            throw new RuntimeException("LiveData value was never set.");
        }
        //noinspection unchecked
        return (T) data[0];
    }
}
