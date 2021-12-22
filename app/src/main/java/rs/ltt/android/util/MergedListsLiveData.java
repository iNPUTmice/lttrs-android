package rs.ltt.android.util;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.Observer;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class MergedListsLiveData<T> extends MediatorLiveData<List<T>> implements Observer<List<T>> {

    private final List<LiveData<List<T>>> liveDataLists;

    public MergedListsLiveData(List<LiveData<List<T>>> liveDataLists) {
        this.liveDataLists = liveDataLists;
        for (final LiveData<List<T>> liveData : liveDataLists) {
            this.addSource(liveData, this);
        }
    }

    @Override
    public void onChanged(final List<T> s) {
        this.postValue(
                liveDataLists.stream()
                        .map(LiveData::getValue)
                        .filter(Objects::nonNull)
                        .flatMap(Collection::stream)
                        .collect(Collectors.toList()));
    }
}
