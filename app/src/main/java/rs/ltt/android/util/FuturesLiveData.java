package rs.ltt.android.util;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

public class FuturesLiveData {

    public static <T> LiveData<T> of(final ListenableFuture<T> future) {
        final MutableLiveData<T> liveData = new MutableLiveData<>();
        Futures.addCallback(
                future,
                new FutureCallback<T>() {
                    @Override
                    public void onSuccess(T t) {
                        liveData.postValue(t);
                    }

                    @Override
                    public void onFailure(@NonNull Throwable throwable) {}
                },
                MoreExecutors.directExecutor());
        return liveData;
    }
}
