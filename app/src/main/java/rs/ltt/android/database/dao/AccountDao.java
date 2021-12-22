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
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.List;
import java.util.Map;
import okhttp3.HttpUrl;
import rs.ltt.android.entity.AccountEntity;
import rs.ltt.android.entity.AccountName;
import rs.ltt.android.entity.AccountWithCredentials;
import rs.ltt.android.entity.CredentialsEntity;
import rs.ltt.jmap.common.entity.Account;

@Dao
public abstract class AccountDao {

    @Query(
            "select account.id as id, username,password,sessionResource,accountId,name from"
                    + " credentials join account on credentialsId = credentials.id where"
                    + " account.id=:id limit 1")
    public abstract ListenableFuture<AccountWithCredentials> getAccountFuture(Long id);

    @Query(
            "select account.id as id, username,password,sessionResource,accountId,name from"
                    + " credentials join account on credentialsId = credentials.id")
    public abstract ListenableFuture<List<AccountWithCredentials>> getAccounts();

    @Query(
            "select account.id as id, username,password,sessionResource,accountId,name from"
                    + " credentials join account on credentialsId = credentials.id where"
                    + " account.id=:id limit 1")
    public abstract AccountWithCredentials getAccount(Long id);

    @Query(
            "select account.id as id, username,password,sessionResource,accountId,name from"
                    + " credentials join account on credentialsId = credentials.id where"
                    + " account.accountId=:accountId  and credentialsId=:cid limit 1")
    public abstract AccountWithCredentials getAccount(Long cid, String accountId);

    @Query("select id,name from account where id=:id limit 1")
    public abstract LiveData<AccountName> getAccountNameLiveData(Long id);

    @Query("select id,name from account where id=:id limit 1")
    public abstract AccountName getAccountName(Long id);

    @Query("select id,name from account order by name")
    public abstract LiveData<List<AccountName>> getAccountNames();

    @Query("select id from account")
    public abstract LiveData<List<Long>> getAccountIds();

    @Query("select id from account order by selected desc limit 1")
    public abstract Long getMostRecentlySelectedAccountId();

    @Query("select exists (select 1 from account)")
    public abstract boolean hasAccounts();

    @Query("select credentialsId from account where id=:id")
    abstract Long getCredentialsId(final Long id);

    @Query("delete from account where id=:id")
    abstract void deleteAccount(final Long id);

    @Query("delete from credentials where id=:id")
    abstract void deleteCredentials(final Long id);

    @Query("select exists (select 1 from account where credentialsId=:credentialsId)")
    abstract boolean hasAccountsWithCredentialsId(final Long credentialsId);

    @Insert
    abstract Long insert(CredentialsEntity entity);

    @Query(
            "select * from credentials where id=(select credentialsId from account where"
                    + " id=:accountId) limit 1")
    public abstract CredentialsEntity getCredentialsForAccount(Long accountId);

    @Query("select * from credentials where id=:id")
    public abstract CredentialsEntity getCredentials(Long id);

    @Insert
    abstract Long insert(AccountEntity entity);

    @Transaction
    public List<AccountWithCredentials> insert(
            String username,
            String password,
            HttpUrl sessionResource,
            Map<String, Account> accounts) {
        final ImmutableList.Builder<AccountWithCredentials> builder = ImmutableList.builder();
        final Long credentialId =
                insert(new CredentialsEntity(username, password, sessionResource));
        for (final Map.Entry<String, Account> entry : accounts.entrySet()) {
            final String accountId = entry.getKey();
            final String name = entry.getValue().getName();
            final Long id = insert(new AccountEntity(credentialId, accountId, name));
            builder.add(
                    new AccountWithCredentials(
                            id, accountId, name, username, password, sessionResource));
        }
        return builder.build();
    }

    @Transaction
    public void delete(final long id) {
        final Long credentialsId = getCredentialsId(id);
        deleteAccount(id);
        if (hasAccountsWithCredentialsId(credentialsId)) {
            return;
        }
        deleteCredentials(id);
    }

    @Query("update account set selected=1 where id=:id")
    abstract void setSelected(final Long id);

    @Query("update account set selected=0 where id is not :id")
    abstract void setNotSelected(final Long id);

    @Transaction
    public void selectAccount(final Long id) {
        setSelected(id);
        setNotSelected(id);
    }
}
