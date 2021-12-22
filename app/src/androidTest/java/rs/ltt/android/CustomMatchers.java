package rs.ltt.android;

import android.view.View;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.espresso.matcher.BoundedMatcher;
import com.google.android.material.textfield.TextInputLayout;
import com.google.common.base.Preconditions;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class CustomMatchers {

    public static Matcher<View> withError(final String expectedError) {
        return new TypeSafeMatcher<View>() {

            @Override
            public boolean matchesSafely(final View view) {
                if (view instanceof TextInputLayout) {
                    final CharSequence error = ((TextInputLayout) view).getError();
                    if (error == null) {
                        return false;
                    }
                    final String hint = error.toString();
                    return expectedError.equals(hint);
                }
                return false;
            }

            @Override
            public void describeTo(Description description) {}
        };
    }

    public static Matcher<View> atPosition(
            final int position, @NonNull final Matcher<View> itemMatcher) {
        Preconditions.checkNotNull(itemMatcher);
        return new BoundedMatcher<View, RecyclerView>(RecyclerView.class) {
            @Override
            public void describeTo(Description description) {
                description.appendText("has item at position " + position + ": ");
                itemMatcher.describeTo(description);
            }

            @Override
            protected boolean matchesSafely(final RecyclerView view) {
                final RecyclerView.ViewHolder viewHolder =
                        view.findViewHolderForAdapterPosition(position);
                if (viewHolder == null) {
                    return false;
                }
                return itemMatcher.matches(viewHolder.itemView);
            }
        };
    }
}
