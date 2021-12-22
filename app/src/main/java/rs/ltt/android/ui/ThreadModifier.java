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

package rs.ltt.android.ui;

import java.util.Collection;
import rs.ltt.android.entity.MailboxWithRoleAndName;

public interface ThreadModifier {

    void archive(Collection<String> threadIds);

    void moveToInbox(Collection<String> threadIds);

    void moveToTrash(Collection<String> threadIds);

    void removeFromMailbox(Collection<String> threadIds, MailboxWithRoleAndName mailbox);

    void markRead(Collection<String> threadIds);

    void markUnread(Collection<String> threadIds);

    void markImportant(Collection<String> threadIds);

    void markNotImportant(Collection<String> threadIds);

    void toggleFlagged(String threadId, boolean target);

    void addFlag(Collection<String> threadIds);

    void removeFlag(Collection<String> threadIds);

    void removeFromKeyword(Collection<String> threadIds, String keyword);

    void executeEmptyMailboxAction(EmptyMailboxAction action);
}
