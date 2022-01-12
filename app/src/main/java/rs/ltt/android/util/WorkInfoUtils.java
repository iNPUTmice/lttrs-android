package rs.ltt.android.util;

import androidx.work.WorkInfo;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;
import java.util.Collection;

public class WorkInfoUtils {
    private WorkInfoUtils() {}

    public static boolean finished(final Collection<WorkInfo> workInfo) {
        final Collection<WorkInfo.State> states =
                Collections2.transform(workInfo, WorkInfo::getState);
        return Iterables.all(states, WorkInfo.State::isFinished);
    }

    public static Collection<WorkInfo> failed(final Collection<WorkInfo> workInfo) {
        return Collections2.filter(workInfo, wi -> wi.getState() == WorkInfo.State.FAILED);
    }
}
