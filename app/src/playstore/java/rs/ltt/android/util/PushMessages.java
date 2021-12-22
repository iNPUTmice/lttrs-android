package rs.ltt.android.util;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.primitives.Longs;
import java.util.Map;
import rs.ltt.jmap.common.entity.AbstractIdentifiableEntity;
import rs.ltt.jmap.common.entity.PushMessage;
import rs.ltt.jmap.common.entity.PushVerification;
import rs.ltt.jmap.common.entity.StateChange;
import rs.ltt.jmap.common.util.Mapper;

public final class PushMessages {

    private static final String DATA_KEY_TYPE = "type";
    private static final String DATA_KEY_CID = "cid";
    private static final String DATA_KEY_ACCOUNT = "account";
    private static final String DATA_KEY_VERIFICATION_CODE = "verificationCode";
    private static final String DATA_KEY_SUBSCRIPTION_ID = "subscriptionId";

    private PushMessages() {}

    public static PushMessage of(final Map<String, String> data) {
        final String type = data.get(DATA_KEY_TYPE);
        if ("PushVerification".equals(type)) {
            final String subscriptionId = data.get(DATA_KEY_SUBSCRIPTION_ID);
            final String verificationCode = data.get(DATA_KEY_VERIFICATION_CODE);
            return PushVerification.builder()
                    .pushSubscriptionId(subscriptionId)
                    .verificationCode(verificationCode)
                    .build();
        } else if ("StateChange".equals(type)) {
            final String account = data.get(DATA_KEY_ACCOUNT);
            final ImmutableMap.Builder<Class<? extends AbstractIdentifiableEntity>, String>
                    stateBuilder = ImmutableMap.builder();
            for (final Map.Entry<String, String> entry : data.entrySet()) {
                final String key = entry.getKey();
                final String value = entry.getValue();
                if (key.equals(DATA_KEY_ACCOUNT) || key.equals(DATA_KEY_CID)) {
                    continue;
                }
                final Class<? extends AbstractIdentifiableEntity> clazz = Mapper.ENTITIES.get(key);
                if (clazz != null) {
                    stateBuilder.put(clazz, value);
                }
            }
            return StateChange.builder().changed(account, stateBuilder.build()).build();

        } else {
            throw new IllegalArgumentException(
                    String.format("%s is not a valid type", Strings.nullToEmpty(type)));
        }
    }

    public static long getCredentialsId(final Map<String, String> data) {
        final String cid = data.get(DATA_KEY_CID);
        if (Strings.isNullOrEmpty(cid)) {
            throw new IllegalArgumentException("Missing cid");
        }
        final Long credentialsId = Longs.tryParse(cid);
        if (credentialsId == null) {
            throw new IllegalArgumentException("cid is not a number");
        }
        return credentialsId;
    }
}
