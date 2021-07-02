package rs.ltt.android.ui.fragment;

import android.app.Activity;

import androidx.fragment.app.Fragment;

import rs.ltt.android.ui.activity.AccountManagerActivity;

public class AbstractAccountManagerFragment extends Fragment {


    AccountManagerActivity requireAccountManagerActivity() {
        final Activity activity = getActivity();
        if (activity instanceof AccountManagerActivity) {
            return (AccountManagerActivity) activity;
        }
        throw new IllegalStateException("Fragment is not attached to AccountManagerActivity");
    }
}
