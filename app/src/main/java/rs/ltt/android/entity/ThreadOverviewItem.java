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

package rs.ltt.android.entity;

import androidx.room.Ignore;
import androidx.room.Relation;

import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import rs.ltt.jmap.common.entity.Keyword;
import rs.ltt.jmap.mua.util.KeywordUtil;

public class ThreadOverviewItem {

    @Ignore
    private final AtomicReference<Map<String, From>> fromMap = new AtomicReference<>();
    @Ignore
    private final AtomicReference<List<EmailPreviewWithMailboxes>> orderedEmails = new AtomicReference<>();

    public String emailId;
    public String threadId;

    @Relation(parentColumn = "threadId", entityColumn = "threadId", entity = EmailEntity.class)
    public List<EmailPreviewWithMailboxes> emails;

    @Relation(parentColumn = "threadId", entityColumn = "threadId")
    public List<ThreadItemEntity> threadItemEntities;

    @Relation(parentColumn = "threadId", entityColumn = "threadId")
    public Set<KeywordOverwriteEntity> keywordOverwriteEntities;

    @Relation(parentColumn = "threadId", entityColumn = "threadId")
    public Set<MailboxOverwriteEntity> mailboxOverwriteEntities;


    public String getPreview() {
        final EmailPreviewWithMailboxes email = Iterables.getLast(getOrderedEmails(), null);
        return email == null ? "(no preview)" : Strings.nullToEmpty(email.preview).trim();
    }

    public String getSubject() {
        final EmailPreviewWithMailboxes email = Iterables.getFirst(getOrderedEmails(), null);
        return email == null ? "(no subject)" : Strings.nullToEmpty(email.subject).trim();
    }

    public Instant getReceivedAt() {
        final EmailPreviewWithMailboxes email = Iterables.tryFind(emails, e -> e != null && emailId.equals(e.id)).orNull();
        return email == null ? null : email.receivedAt;
    }

    public boolean everyHasSeenKeyword() {
        KeywordOverwriteEntity seenOverwrite = KeywordOverwriteEntity.getKeywordOverwrite(keywordOverwriteEntities, Keyword.SEEN);
        return seenOverwrite != null ? seenOverwrite.value : KeywordUtil.everyHas(getOrderedEmails(), Keyword.SEEN);
    }

    public boolean showAsFlagged() {
        KeywordOverwriteEntity flaggedOverwrite = KeywordOverwriteEntity.getKeywordOverwrite(keywordOverwriteEntities, Keyword.FLAGGED);
        return flaggedOverwrite != null ? flaggedOverwrite.value : KeywordUtil.anyHas(getOrderedEmails(), Keyword.FLAGGED);
    }


    public Integer getCount() {
        final int count = threadItemEntities.size();
        return count <= 1 ? null : count;
    }

    public Map.Entry<String, From> getFrom() {
        return Iterables.getFirst(getFromMap().entrySet(), null);
    }

    private Map<String, From> getFromMap() {
        Map<String, From> map = this.fromMap.get();
        if (map == null) {
            synchronized (this.fromMap) {
                map = this.fromMap.get();
                if (map == null) {
                    map = calculateFromMap();
                    this.fromMap.set(map);
                }
            }
        }
        return map;
    }

    private Map<String, From> calculateFromMap() {
        KeywordOverwriteEntity seenOverwrite = KeywordOverwriteEntity.getKeywordOverwrite(keywordOverwriteEntities, Keyword.SEEN);
        LinkedHashMap<String, From> fromMap = new LinkedHashMap<>();
        final List<EmailPreviewWithMailboxes> emails = getOrderedEmails();
        for (EmailPreviewWithMailboxes email : emails) {
            if (email.keywords.contains(Keyword.DRAFT)) {
                fromMap.put("", new DraftFrom());
                continue;
            }
            final boolean seen = seenOverwrite != null ? seenOverwrite.value : email.keywords.contains(Keyword.SEEN);
            for (EmailAddress emailAddress : email.emailAddresses) {
                if (emailAddress.type == EmailAddressType.FROM) {
                    From from = fromMap.get(emailAddress.getEmail());
                    if (from == null) {
                        from = new NamedFrom(emailAddress.getName(), seen);
                        fromMap.put(emailAddress.getEmail(), from);
                    } else if (from instanceof NamedFrom) {
                        final NamedFrom namedFrom = (NamedFrom) from;
                        namedFrom.seen &= seen;
                    }
                }
            }
        }
        return fromMap;
    }

    private List<EmailPreviewWithMailboxes> getOrderedEmails() {
        List<EmailPreviewWithMailboxes> list = this.orderedEmails.get();
        if (list == null) {
            synchronized (this.orderedEmails) {
                list = this.orderedEmails.get();
                if (list == null) {
                    list = calculateOrderedEmails();
                    this.orderedEmails.set(list);
                }
            }
        }
        return list;
    }

    private List<EmailPreviewWithMailboxes> calculateOrderedEmails() {
        final List<ThreadItemEntity> threadItemEntities = new ArrayList<>(this.threadItemEntities);
        Collections.sort(threadItemEntities, (o1, o2) -> o1.getPosition() - o2.getPosition());
        final Map<String, EmailPreviewWithMailboxes> emailMap = Maps.uniqueIndex(emails, input -> input.id);
        final List<EmailPreviewWithMailboxes> orderedList = new ArrayList<>(emails.size());
        for (ThreadItemEntity threadItemEntity : threadItemEntities) {
            EmailPreviewWithMailboxes email = emailMap.get(threadItemEntity.emailId);
            if (email != null) {
                orderedList.add(email);
            }
        }
        return orderedList;
    }

    private Set<String> getMailboxIds() {
        ImmutableSet.Builder<String> builder = ImmutableSet.builder();
        for (EmailPreviewWithMailboxes email : emails) {
            builder.addAll(email.mailboxes);
        }
        return builder.build();
    }

    public boolean isInMailbox(MailboxWithRoleAndName mailbox) {
        if (mailbox == null) {
            return false;
        }
        MailboxOverwriteEntity overwrite = MailboxOverwriteEntity.find(this.mailboxOverwriteEntities, mailbox.role);
        if (overwrite != null) {
            return overwrite.value;
        }
        return getMailboxIds().contains(mailbox.id);
    }

    public From[] getFromValues() {
        return getFromMap().values().toArray(new From[0]);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ThreadOverviewItem item = (ThreadOverviewItem) o;
        return Objects.equal(getSubject(), item.getSubject()) &&
                Objects.equal(getPreview(), item.getPreview()) &&
                Objects.equal(showAsFlagged(), item.showAsFlagged()) &&
                Objects.equal(getReceivedAt(), item.getReceivedAt()) &&
                Objects.equal(mailboxOverwriteEntities, item.mailboxOverwriteEntities) &&
                Objects.equal(getMailboxIds(), item.getMailboxIds()) &&
                Objects.equal(everyHasSeenKeyword(), item.everyHasSeenKeyword()) &&
                Arrays.equals(getFromValues(), item.getFromValues());
    }

    public String[] getKeywords() {
        ImmutableSet.Builder<String> keywordBuilder = new ImmutableSet.Builder<>();
        if (everyHasSeenKeyword()) {
            keywordBuilder.add(Keyword.SEEN);
        }
        if (showAsFlagged()) {
            keywordBuilder.add(Keyword.FLAGGED);
        }
        return keywordBuilder.build().toArray(new String[0]);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(emailId, threadId, getOrderedEmails(), threadItemEntities);
    }

    public interface From {

    }

    public static class DraftFrom implements From {

    }

    public static class NamedFrom implements From {
        public final String name;
        public boolean seen;

        NamedFrom(String name, boolean seen) {
            this.name = name;
            this.seen = seen;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            NamedFrom from = (NamedFrom) o;
            return seen == from.seen &&
                    Objects.equal(name, from.name);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(name, seen);
        }
    }
}
