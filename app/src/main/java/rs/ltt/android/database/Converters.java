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

import androidx.room.TypeConverter;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import okhttp3.HttpUrl;
import rs.ltt.android.entity.EmailAddressType;
import rs.ltt.android.entity.EmailBodyPartType;
import rs.ltt.android.entity.QueryItemOverwriteEntity;
import rs.ltt.jmap.common.entity.AbstractIdentifiableEntity;
import rs.ltt.jmap.common.entity.Role;
import rs.ltt.jmap.common.util.Mapper;

public class Converters {

    @TypeConverter
    public static String toString(final Role role) {
        return role == null ? null : role.toString();
    }

    @TypeConverter
    public static Role toRole(String role) {
        return role == null ? null : Role.valueOf(role);
    }

    @TypeConverter
    public static String toString(final Class<? extends AbstractIdentifiableEntity> clazz) {
        final String type = Mapper.ENTITIES.inverse().get(clazz);
        if (type == null) {
            throw new IllegalArgumentException(
                    String.format("%s is not a known entity class", clazz.getName()));
        }
        return type;
    }

    @TypeConverter
    public static Class<? extends AbstractIdentifiableEntity> toEntityType(
            final String entityType) {
        final Class<? extends AbstractIdentifiableEntity> clazz = Mapper.ENTITIES.get(entityType);
        if (clazz == null) {
            throw new IllegalArgumentException(
                    String.format("%s is not a known entity type", entityType));
        }
        return clazz;
    }

    @TypeConverter
    public static EmailAddressType toEmailAddressType(String type) {
        return EmailAddressType.valueOf(type);
    }

    @TypeConverter
    public static String toString(EmailAddressType type) {
        return type.toString();
    }

    @TypeConverter
    public static EmailBodyPartType toEmailBodyPartType(String type) {
        return EmailBodyPartType.valueOf(type);
    }

    @TypeConverter
    public static String toString(EmailBodyPartType type) {
        return type.toString();
    }

    @TypeConverter
    public static QueryItemOverwriteEntity.Type toType(String type) {
        return QueryItemOverwriteEntity.Type.valueOf(type);
    }

    @TypeConverter
    public static String toString(QueryItemOverwriteEntity.Type type) {
        return type.toString();
    }

    @TypeConverter
    public static Instant toInstant(long timestamp) {
        return Instant.ofEpochMilli(timestamp);
    }

    @TypeConverter
    public static long toTimestamp(Instant instant) {
        return instant.getEpochSecond() * 1000;
    }

    @TypeConverter
    public static OffsetDateTime toOffsetDateTime(final String dateTime) {
        return dateTime == null ? null : OffsetDateTime.parse(dateTime);
    }

    @TypeConverter
    public static String toString(final OffsetDateTime dateTime) {
        return dateTime == null ? null : DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(dateTime);
    }

    @TypeConverter
    public static HttpUrl toHttpUrl(final String url) {
        return url == null ? null : HttpUrl.get(url);
    }

    @TypeConverter
    public static String toString(final HttpUrl httpUrl) {
        return httpUrl == null ? null : httpUrl.toString();
    }
}
