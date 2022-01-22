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

package rs.ltt.android.database;

import android.annotation.SuppressLint;
import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rs.ltt.android.database.dao.AutocryptDao;
import rs.ltt.android.database.dao.IdentityDao;
import rs.ltt.android.database.dao.MailboxDao;
import rs.ltt.android.database.dao.OverwriteDao;
import rs.ltt.android.database.dao.QueryDao;
import rs.ltt.android.database.dao.StateDao;
import rs.ltt.android.database.dao.ThreadAndEmailDao;
import rs.ltt.android.entity.AccountStateEntity;
import rs.ltt.android.entity.EmailBodyPartEntity;
import rs.ltt.android.entity.EmailBodyValueEntity;
import rs.ltt.android.entity.EmailEmailAddressEntity;
import rs.ltt.android.entity.EmailEntity;
import rs.ltt.android.entity.EmailInReplyToEntity;
import rs.ltt.android.entity.EmailKeywordEntity;
import rs.ltt.android.entity.EmailMailboxEntity;
import rs.ltt.android.entity.EmailMessageIdEntity;
import rs.ltt.android.entity.EntityStateEntity;
import rs.ltt.android.entity.IdentityEmailAddressEntity;
import rs.ltt.android.entity.IdentityEntity;
import rs.ltt.android.entity.KeywordOverwriteEntity;
import rs.ltt.android.entity.MailboxEntity;
import rs.ltt.android.entity.MailboxOverwriteEntity;
import rs.ltt.android.entity.PeerStateEntity;
import rs.ltt.android.entity.QueryEntity;
import rs.ltt.android.entity.QueryItemEntity;
import rs.ltt.android.entity.QueryItemOverwriteEntity;
import rs.ltt.android.entity.ThreadEntity;
import rs.ltt.android.entity.ThreadItemEntity;

@Database(
        entities = {
            MailboxEntity.class,
            EntityStateEntity.class,
            ThreadEntity.class,
            ThreadItemEntity.class,
            EmailEntity.class,
            EmailInReplyToEntity.class,
            EmailMessageIdEntity.class,
            EmailEmailAddressEntity.class,
            EmailKeywordEntity.class,
            EmailMailboxEntity.class,
            EmailBodyValueEntity.class,
            EmailBodyPartEntity.class,
            IdentityEntity.class,
            IdentityEmailAddressEntity.class,
            QueryEntity.class,
            QueryItemEntity.class,
            KeywordOverwriteEntity.class,
            MailboxOverwriteEntity.class,
            QueryItemOverwriteEntity.class,
            AccountStateEntity.class,
            PeerStateEntity.class
        },
        version = 3,
        exportSchema = false)
@TypeConverters(Converters.class)
public abstract class LttrsDatabase extends RoomDatabase {

    private static final Logger LOGGER = LoggerFactory.getLogger(LttrsDatabase.class);

    @SuppressLint("UseSparseArrays")
    private static final Map<Long, LttrsDatabase> INSTANCES = new HashMap<>();

    private static final Set<Long> RECENTLY_CLOSED = new HashSet<>();

    public static File close(final Long account) {
        synchronized (LttrsDatabase.class) {
            final LttrsDatabase lttrsDatabase = INSTANCES.remove(account);
            if (lttrsDatabase != null) {
                LOGGER.info("Closing LttrsDatabase account id {}", account);
                final File file =
                        new File(lttrsDatabase.getOpenHelper().getReadableDatabase().getPath());
                lttrsDatabase.close();
                RECENTLY_CLOSED.add(account);
                return file;
            }
        }
        return null;
    }

    public static LttrsDatabase getInstance(final Context context, final Long account) {
        final LttrsDatabase instance = INSTANCES.get(account);
        if (instance != null) {
            return instance;
        }
        synchronized (LttrsDatabase.class) {
            final LttrsDatabase existing = INSTANCES.get(account);
            if (existing != null) {
                return existing;
            }
            if (RECENTLY_CLOSED.contains(account)) {
                throw new IllegalStateException(
                        String.format(
                                Locale.US,
                                "Database for account %d has recently been closed",
                                account));
            }
            LOGGER.info("Building LttrsDatabase account id {}", account);
            final Context application = context.getApplicationContext();
            final String filename = String.format("lttrs-%x", account);
            final LttrsDatabase lttrsDatabase =
                    Room.databaseBuilder(application, LttrsDatabase.class, filename)
                            .fallbackToDestructiveMigration()
                            .build();
            INSTANCES.put(account, lttrsDatabase);
            return lttrsDatabase;
        }
    }

    public abstract ThreadAndEmailDao threadAndEmailDao();

    public abstract MailboxDao mailboxDao();

    public abstract IdentityDao identityDao();

    public abstract StateDao stateDao();

    public abstract QueryDao queryDao();

    public abstract OverwriteDao overwriteDao();

    public abstract AutocryptDao autocryptDao();
}
