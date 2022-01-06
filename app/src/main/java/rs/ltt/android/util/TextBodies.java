package rs.ltt.android.util;

import com.google.common.base.CharMatcher;
import com.google.common.base.Joiner;
import com.google.common.collect.Maps;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import rs.ltt.android.entity.EmailBodyPartEntity;
import rs.ltt.android.entity.EmailBodyPartType;
import rs.ltt.android.entity.EmailBodyValueEntity;

public class TextBodies {

    private static final int PREVIEW_LENGTH = 1024;

    public static List<String> getTextBodies(
            final List<EmailBodyPartEntity> bodyPartEntities,
            final List<EmailBodyValueEntity> bodyValueEntities) {
        final List<EmailBodyPartEntity> textBodies =
                EmailBodyPartEntity.filter(bodyPartEntities, EmailBodyPartType.TEXT_BODY);
        final Map<String, EmailBodyValueEntity> map =
                Maps.uniqueIndex(bodyValueEntities, value -> value.partId);
        return textBodies.stream()
                .map(body -> map.get(body.partId))
                .filter(java.util.Objects::nonNull)
                .map(value -> value.value)
                .collect(Collectors.toList());
    }

    public static String getPreview(
            final List<EmailBodyPartEntity> bodyPartEntities,
            final List<EmailBodyValueEntity> bodyValueEntities) {
        final String body = Joiner.on(' ').join(getTextBodies(bodyPartEntities, bodyValueEntities));
        return CharMatcher.is('\n')
                .replaceFrom(body.substring(0, Math.min(PREVIEW_LENGTH, body.length())), ' ');
    }
}
