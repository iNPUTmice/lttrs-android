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

package rs.ltt.android.ui;

import androidx.paging.PagedList;
import java.util.List;
import java.util.Set;
import rs.ltt.android.entity.ThreadOverviewItem;
import rs.ltt.android.ui.adapter.ThreadOverviewAdapter;

public class ActionModeMenuConfiguration {

    public enum QueryType {
        ARCHIVE,
        INBOX,
        FLAGGED,
        IMPORTANT,
        LABEL,
        SPECIAL,
        TRASH
    }

    public static class SelectionInfo {
        public final boolean read;
        public final boolean important;
        public final boolean flagged;

        private SelectionInfo(boolean read, boolean important, boolean flagged) {
            this.read = read;
            this.important = important;
            this.flagged = flagged;
        }

        public static SelectionInfo vote(
                final Set<String> selection, final ThreadOverviewAdapter threadOverviewAdapter) {
            final PagedList<ThreadOverviewItem> currentList =
                    threadOverviewAdapter.getCurrentList();
            if (currentList == null) {
                return new SelectionInfo(false, false, false);
            }
            int read = 0;
            int important = 0;
            int flagged = 0;
            final List<ThreadOverviewItem> threadOverviewItems = currentList.snapshot();
            for (ThreadOverviewItem thread : threadOverviewItems) {
                if (thread != null && selection.contains(thread.threadId)) {
                    if (thread.everyHasSeenKeyword()) {
                        read++;
                    } else {
                        read--;
                    }
                    if (threadOverviewAdapter.isImportant(thread)) {
                        important++;
                    } else {
                        important--;
                    }
                    if (thread.showAsFlagged()) {
                        flagged++;
                    } else {
                        flagged--;
                    }
                }
            }
            return new SelectionInfo(read >= 0, important >= 0, flagged >= 0);
        }
    }
}
