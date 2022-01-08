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

package rs.ltt.android.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.Collection;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rs.ltt.android.entity.EntityStateEntity;
import rs.ltt.android.entity.IdentityEmailAddressEntity;
import rs.ltt.android.entity.IdentityEntity;
import rs.ltt.android.entity.IdentityWithNameAndEmail;
import rs.ltt.jmap.common.entity.Identity;
import rs.ltt.jmap.mua.cache.Update;

@Dao
public abstract class IdentityDao extends AbstractEntityDao {

    private static final Logger LOGGER = LoggerFactory.getLogger(IdentityDao.class);

    @Insert
    abstract void insert(IdentityEntity identityEntity);

    @Insert
    abstract void insert(Collection<IdentityEmailAddressEntity> identityEmailAddressEntities);

    protected void insert(Identity[] identities) {
        for (Identity identity : identities) {
            insert(IdentityEntity.of(identity));
            insert(IdentityEmailAddressEntity.of(identity));
        }
    }

    @Query("delete from identity")
    abstract void deleteAll();

    @Query("delete from identity where id=:id")
    abstract void delete(String id);

    @Query("select :accountId as accountId,id,name,email from identity")
    public abstract LiveData<List<IdentityWithNameAndEmail>> getIdentitiesLiveData(
            final Long accountId);

    @Query("select :accountId as accountId,id,name,email from identity where id=:id limit 1")
    public abstract IdentityWithNameAndEmail get(final Long accountId, final String id);

    @Query("select email from identity")
    public abstract ListenableFuture<List<String>> getEmailAddresses();

    @Transaction
    public void set(Identity[] identities, String state) {
        if (state != null && state.equals(getState(Identity.class))) {
            LOGGER.debug("nothing to do. identities with this state have already been set");
            return;
        }
        deleteAll();
        if (identities.length > 0) {
            insert(identities);
        }
        insert(new EntityStateEntity(Identity.class, state));
    }

    public void update(final Update<Identity> update) {
        final String newState = update.getNewTypedState().getState();
        if (newState != null && newState.equals(getState(Identity.class))) {
            LOGGER.debug("nothing to do. identities already at newest state");
            return;
        }
        insert(update.getCreated());
        insert(update.getUpdated());
        for (final String id : update.getDestroyed()) {
            delete(id);
        }
        throwOnUpdateConflict(Identity.class, update.getOldTypedState(), update.getNewTypedState());
    }
}
