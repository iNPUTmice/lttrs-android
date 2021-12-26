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

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.format.DateUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.text.style.StyleSpan;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.DrawableRes;
import androidx.appcompat.widget.AppCompatSpinner;
import androidx.core.widget.ImageViewCompat;
import androidx.databinding.BindingAdapter;
import androidx.lifecycle.LiveData;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.net.MediaType;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import rs.ltt.android.R;
import rs.ltt.android.entity.From;
import rs.ltt.android.entity.IdentityWithNameAndEmail;
import rs.ltt.android.entity.SubjectWithImportance;
import rs.ltt.android.util.ConsistentColorGeneration;
import rs.ltt.android.util.MediaTypes;
import rs.ltt.jmap.common.entity.Role;
import rs.ltt.jmap.mua.util.EmailAddressUtil;
import rs.ltt.jmap.mua.util.EmailBodyUtil;
import rs.ltt.jmap.mua.util.Label;

public class BindingAdapters {

    private static final Duration SIX_HOURS = Duration.ofHours(6);
    private static final Duration THREE_MONTH = Duration.ofDays(90);

    private static final char NON_BREAKING_SPACE = '\u00a0';
    private static final char NARROW_NON_BREAKING_SPACE = '\u202F';
    private static final char RIGHT_POINTING_DOUBLE_ANGLE_QUOTATION_MARK = '\u00bb';

    private static boolean sameYear(final Instant a, final Instant b) {
        final ZoneId local = ZoneId.systemDefault();
        return LocalDateTime.ofInstant(a, local).getYear()
                == LocalDateTime.ofInstant(b, local).getYear();
    }

    private static boolean sameDay(final Instant a, final Instant b) {
        return a.truncatedTo(ChronoUnit.DAYS).equals(b.truncatedTo(ChronoUnit.DAYS));
    }

    @BindingAdapter("date")
    public static void setInteger(TextView textView, Instant receivedAt) {
        if (receivedAt == null || receivedAt.getEpochSecond() <= 0) {
            textView.setVisibility(View.GONE);
        } else {
            final Context context = textView.getContext();
            final Instant now = Instant.now();
            textView.setVisibility(View.VISIBLE);
            if (sameDay(receivedAt, now) || now.minus(SIX_HOURS).isBefore(receivedAt)) {
                textView.setText(
                        DateUtils.formatDateTime(
                                context,
                                receivedAt.getEpochSecond() * 1000,
                                DateUtils.FORMAT_SHOW_TIME));
            } else if (sameYear(receivedAt, now) || now.minus(THREE_MONTH).isBefore(receivedAt)) {
                textView.setText(
                        DateUtils.formatDateTime(
                                context,
                                receivedAt.getEpochSecond() * 1000,
                                DateUtils.FORMAT_SHOW_DATE
                                        | DateUtils.FORMAT_NO_YEAR
                                        | DateUtils.FORMAT_ABBREV_ALL));
            } else {
                textView.setText(
                        DateUtils.formatDateTime(
                                context,
                                receivedAt.getEpochSecond() * 1000,
                                DateUtils.FORMAT_SHOW_DATE
                                        | DateUtils.FORMAT_NO_MONTH_DAY
                                        | DateUtils.FORMAT_ABBREV_ALL));
            }
        }
    }

    @BindingAdapter("body")
    public static void setBody(final TextView textView, List<String> textBodies) {
        final SpannableStringBuilder builder = new SpannableStringBuilder();
        for (final EmailBodyUtil.Block block : EmailBodyUtil.parse(textBodies)) {
            if (builder.length() != 0) {
                builder.append('\n');
            }
            final int start = builder.length();
            builder.append(block.toString());
            if (block.getDepth() > 0) {
                builder.setSpan(
                        new QuoteSpan(block.getDepth(), textView.getContext()),
                        start,
                        builder.length(),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        textView.setText(builder);
    }

    @BindingAdapter("to")
    public static void setTo(final TextView textView, final Collection<String> names) {
        final boolean shorten = names.size() > 1;
        final StringBuilder builder = new StringBuilder();
        for (final String name : names) {
            if (builder.length() != 0) {
                builder.append(", ");
            }
            builder.append(shorten ? EmailAddressUtil.shorten(name) : name);
        }
        final Context context = textView.getContext();
        textView.setText(context.getString(R.string.to_x, builder.toString()));
    }

    @BindingAdapter("tint")
    public static void setTint(final ImageView imageView, final String key) {
        imageView.setImageTintList(
                ColorStateList.valueOf(
                        ConsistentColorGeneration.rgbFromKey(Strings.nullToEmpty(key))));
    }

    @BindingAdapter("from")
    public static void setFrom(final ImageView imageView, final From from) {
        if (imageView.isActivated()) {
            imageView.setImageResource(R.drawable.ic_selected_24dp);
            return;
        }
        imageView.setImageDrawable(new AvatarDrawable(imageView.getContext(), from));
    }

    @BindingAdapter("android:text")
    public static void setText(final TextView textView, final From from) {
        if (from instanceof From.Named) {
            final From.Named named = (From.Named) from;
            textView.setText(named.getName());
        } else if (from instanceof From.Draft) {
            final Context context = textView.getContext();
            final SpannableString spannable =
                    new SpannableString(context.getString(R.string.draft));
            spannable.setSpan(
                    new ForegroundColorSpan(MaterialColors.getColor(textView, R.attr.colorPrimary)),
                    0,
                    spannable.length(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            textView.setText(spannable);
        }
    }

    @BindingAdapter("from")
    public static void setFrom(final TextView textView, final From[] from) {
        final SpannableStringBuilder builder = new SpannableStringBuilder();
        if (from != null) {
            final boolean shorten = from.length > 1;
            for (int i = 0; i < from.length; ++i) {
                final From individual = from[i];
                if (builder.length() != 0) {
                    builder.append(", ");
                }
                final int start = builder.length();
                if (individual instanceof From.Draft) {
                    final Context context = textView.getContext();
                    builder.append(context.getString(R.string.draft));
                    builder.setSpan(
                            new ForegroundColorSpan(
                                    MaterialColors.getColor(textView, R.attr.colorPrimary)),
                            start,
                            builder.length(),
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                } else if (individual instanceof From.Named) {
                    final From.Named named = (From.Named) individual;
                    builder.append(
                            shorten ? EmailAddressUtil.shorten(named.getName()) : named.getName());
                    if (!named.isSeen()) {
                        builder.setSpan(
                                new StyleSpan(Typeface.BOLD),
                                start,
                                builder.length(),
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                    if (from.length > 3) {
                        if (i < from.length - 3) {
                            builder.append(" … "); // TODO small?
                            i = from.length - 3;
                        }
                    }
                } else {
                    throw new IllegalStateException(
                            String.format(
                                    "Unable to render from type %s",
                                    individual.getClass().getName()));
                }
            }
        }
        textView.setText(builder);
    }

    @BindingAdapter("android:typeface")
    public static void setTypeface(TextView v, String style) {
        switch (style) {
            case "bold":
                v.setTypeface(null, Typeface.BOLD);
                break;
            case "italic":
                v.setTypeface(null, Typeface.ITALIC);
                break;
            default:
                v.setTypeface(null, Typeface.NORMAL);
                break;
        }
    }

    @BindingAdapter("isFlagged")
    public static void setIsFlagged(final ImageView imageView, final boolean isFlagged) {
        if (isFlagged) {
            imageView.setImageResource(R.drawable.ic_star_black_no_padding_24dp);
            ImageViewCompat.setImageTintList(
                    imageView,
                    ColorStateList.valueOf(
                            MaterialColors.getColor(imageView, R.attr.colorIndicator)));
        } else {
            imageView.setImageResource(R.drawable.ic_star_border_no_padding_black_24dp);
            ImageViewCompat.setImageTintList(
                    imageView,
                    ColorStateList.valueOf(
                            MaterialColors.getColor(imageView, R.attr.colorControlNormal)));
        }
    }

    @BindingAdapter("count")
    public static void setInteger(TextView textView, Integer integer) {
        if (integer == null || integer <= 0) {
            textView.setVisibility(View.GONE);
        } else {
            textView.setVisibility(View.VISIBLE);
            textView.setText(String.valueOf(integer));
        }
    }

    @BindingAdapter("name")
    public static void setName(final TextView textView, final Label label) {
        final Role role = label.getRole();
        if (role != null) {
            textView.setText(Translations.translate(role));
        } else {
            textView.setText(label.getName());
        }
    }

    @BindingAdapter("android:text")
    public static void setText(TextView text, SubjectWithImportance subjectWithImportance) {
        if (subjectWithImportance == null || subjectWithImportance.subject == null) {
            text.setText(null);
            return;
        }
        if (subjectWithImportance.important) {
            final SpannableStringBuilder header =
                    new SpannableStringBuilder(subjectWithImportance.subject)
                            .append(NON_BREAKING_SPACE)
                            .append(NARROW_NON_BREAKING_SPACE)
                            .append(RIGHT_POINTING_DOUBLE_ANGLE_QUOTATION_MARK);
            header.setSpan(
                    new ImageSpan(
                            text.getContext(),
                            R.drawable.ic_important_indicator_22sp,
                            ImageSpan.ALIGN_BASELINE),
                    header.length() - 1,
                    header.length(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            text.setText(header);
        } else {
            text.setText(subjectWithImportance.subject);
        }
    }

    @BindingAdapter("role")
    public static void setRole(final ImageView imageView, final Role role) {
        @DrawableRes final int imageResource;
        if (role == null) {
            imageResource = R.drawable.ic_label_black_24dp;
        } else {
            switch (role) {
                case ALL:
                    imageResource = R.drawable.ic_all_inbox_24dp;
                    break;
                case INBOX:
                    imageResource = R.drawable.ic_inbox_black_24dp;
                    break;
                case ARCHIVE:
                    imageResource = R.drawable.ic_archive_black_24dp;
                    break;
                case IMPORTANT:
                    imageResource = R.drawable.ic_label_important_black_24dp;
                    break;
                case JUNK:
                    imageResource = R.drawable.ic_spam_black_24dp;
                    break;
                case DRAFTS:
                    imageResource = R.drawable.ic_drafts_black_24dp;
                    break;
                case FLAGGED:
                    imageResource = R.drawable.ic_star_black_24dp;
                    break;
                case TRASH:
                    imageResource = R.drawable.ic_delete_black_24dp;
                    break;
                case SENT:
                    imageResource = R.drawable.ic_send_black_24dp;
                    break;
                default:
                    imageResource = R.drawable.ic_folder_black_24dp;
                    break;
            }
        }
        imageView.setImageResource(imageResource);
    }

    @BindingAdapter("errorText")
    public static void setErrorText(
            final TextInputLayout textInputLayout, final LiveData<String> error) {
        textInputLayout.setError(error.getValue());
    }

    @BindingAdapter("editorAction")
    public static void setEditorAction(
            final TextInputEditText editText, final TextView.OnEditorActionListener listener) {
        editText.setOnEditorActionListener(listener);
    }

    @BindingAdapter("identities")
    public static void setIdentities(
            final AppCompatSpinner spinner, final List<IdentityWithNameAndEmail> identities) {
        final List<String> representations;
        if (identities == null) {
            representations = Collections.emptyList();
        } else {
            representations =
                    Lists.transform(
                            identities,
                            input -> EmailAddressUtil.toString(input.getEmailAddress()));
        }
        final ArrayAdapter<String> adapter =
                new ArrayAdapter<>(
                        spinner.getContext(),
                        android.R.layout.simple_spinner_item,
                        representations);
        adapter.setDropDownViewResource(R.layout.item_simple_spinner_dropdown);
        spinner.setAdapter(adapter);
    }

    @BindingAdapter("android:src")
    public static void setMediaType(final ImageView imageView, final MediaType mediaType) {
        imageView.setImageResource(toDrawable(mediaType));
    }

    private static @DrawableRes int toDrawable(final MediaType type) {
        if (type == null) {
            return R.drawable.ic_baseline_attachment_24;
        } else if (type.is(MediaType.ANY_IMAGE_TYPE)) {
            return R.drawable.ic_baseline_image_24;
        } else if (type.is(MediaType.ANY_VIDEO_TYPE)) {
            return R.drawable.ic_baseline_movie_24;
        } else if (type.is(MediaType.ANY_AUDIO_TYPE)) {
            return R.drawable.ic_baseline_audiotrack_24;
        } else if (MediaTypes.isCalendar(type)) {
            return R.drawable.ic_baseline_event_24;
        } else if (type.is(MediaType.PDF)) {
            return R.drawable.ic_baseline_pdf_box_24;
        } else if (MediaTypes.isArchive(type)) {
            return R.drawable.ic_archive_black_24dp;
        } else if (MediaTypes.isVcard(type)) {
            return R.drawable.ic_baseline_person_24;
        } else if (MediaTypes.isEbook(type)) {
            return R.drawable.ic_baseline_book_24;
        } else if (MediaTypes.isDocument(type)) {
            return R.drawable.ic_baseline_document_24;
        } else if (MediaTypes.isTour(type)) {
            return R.drawable.ic_baseline_tour_24;
        } else {
            return R.drawable.ic_baseline_attachment_24;
        }
    }

    @BindingAdapter("android:checked")
    public static void setChecked(final CompoundButton button, final boolean checked) {
        setChecked(button, Boolean.valueOf(checked));
    }

    @BindingAdapter("android:checked")
    public static void setChecked(final CompoundButton button, final Boolean checked) {
        if (checked == null) {
            return;
        }
        final Object tag = button.getTag();
        button.setChecked(checked);
        if (tag instanceof InitialValueSet) {
            return;
        }
        flagInitialValueSet(button);
    }

    public static void flagInitialValueSet(final CompoundButton button) {
        button.jumpDrawablesToCurrentState();
        button.setTag(new InitialValueSet());
        button.setVisibility(View.VISIBLE);
    }

    private static class InitialValueSet {}
}
