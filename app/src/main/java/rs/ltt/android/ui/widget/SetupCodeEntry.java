package rs.ltt.android.ui.widget;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.GridLayout;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import java.util.List;

public class SetupCodeEntry {

    private static final int MAX_LENGTH = 4;

    private final List<EditText> editTexts;

    private Function<String, Boolean> onSetupCodeSubmitted;

    private SetupCodeEntry(final List<EditText> editTexts) {
        this.editTexts = editTexts;
        setupListeners();
    }

    private void setupListeners() {
        for (final EditText editText : this.editTexts) {
            setupListeners(editText);
        }
    }

    private void setupListeners(final EditText editText) {
        editText.addTextChangedListener(
                new TextWatcher() {
                    @Override
                    public void beforeTextChanged(
                            CharSequence s, int start, int count, int after) {}

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {}

                    @Override
                    public void afterTextChanged(final Editable editable) {
                        if (editable.length() >= MAX_LENGTH) {
                            focusNextAfter(editText);
                        }
                    }
                });
        editText.setOnKeyListener(
                (v, keyCode, event) -> {
                    if (event.getAction() != KeyEvent.ACTION_DOWN) {
                        return false;
                    }
                    final boolean cursorAtZero =
                            editText.getSelectionEnd() == 0 && editText.getSelectionStart() == 0;
                    if (keyCode == KeyEvent.KEYCODE_DEL
                            && (cursorAtZero || editText.getText().length() == 0)) {
                        return focusPreviousBefore(editText);
                    }
                    return false;
                });
        editText.setOnEditorActionListener(
                (v, actionId, event) -> {
                    if (event != null && event.getAction() != KeyEvent.ACTION_UP) {
                        return true;
                    }
                    if (focusNextAfter((EditText) v)) {
                        return true;
                    }
                    return Boolean.TRUE.equals(onSetupCodeSubmitted.apply(getSetupCode()));
                });
    }

    private boolean focusNextAfter(final EditText editText) {
        final int pos = this.editTexts.indexOf(editText);
        if (pos == -1 || pos + 1 >= this.editTexts.size()) {
            return false;
        }
        requestFocus(this.editTexts.get(pos + 1));
        return true;
    }

    private boolean focusPreviousBefore(final EditText editText) {
        final int pos = this.editTexts.indexOf(editText);
        if (pos - 1 < 0) {
            return false;
        }
        requestFocus(this.editTexts.get(pos - 1));
        return true;
    }

    private void requestFocus(final EditText editText) {
        editText.setSelection(editText.getText().length());
        editText.requestFocus();
    }

    public String getSetupCode() {
        final StringBuilder builder = new StringBuilder();
        for (final EditText editText : this.editTexts) {
            builder.append(editText.getText().toString());
        }
        return builder.toString();
    }

    public void setOnSetupCodeSubmitted(final Function<String, Boolean> listener) {
        this.onSetupCodeSubmitted = listener;
    }

    public void setSetupCode(final String setupCode) {
        for (int i = 0; i < this.editTexts.size(); ++i) {
            final Editable editable = this.editTexts.get(i).getText();
            final int begin = i * MAX_LENGTH;
            final int end = begin + MAX_LENGTH;
            if (setupCode == null || begin > setupCode.length()) {
                editable.clear();
            } else if (end < setupCode.length()) {
                editable.clear();
                editable.append(setupCode.substring(begin, end));
            } else {
                editable.clear();
                editable.append(setupCode.substring(begin));
            }
        }
    }

    public void requestFocus() {
        for (final EditText editText : this.editTexts) {
            if (editText.length() < MAX_LENGTH) {
                editText.requestFocus();
                return;
            }
        }
    }

    public static SetupCodeEntry of(final GridLayout gridLayout) {
        final ImmutableList.Builder<EditText> builder = new ImmutableList.Builder<>();
        for (int i = 0; i < gridLayout.getChildCount(); ++i) {
            final View view = gridLayout.getChildAt(i);
            if (view instanceof EditText) {
                builder.add((EditText) view);
            }
        }
        return new SetupCodeEntry(builder.build());
    }
}
