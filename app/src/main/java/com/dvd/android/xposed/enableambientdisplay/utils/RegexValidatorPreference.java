package com.dvd.android.xposed.enableambientdisplay.utils;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;

public class RegexValidatorPreference extends EditTextPreference implements TextWatcher {

    public RegexValidatorPreference(Context context) {
        this(context, null);
    }

    public RegexValidatorPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        getEditText().setSingleLine(true);
        getEditText().setMaxLines(1);
        getEditText().setLines(1);
    }

    @Override
    protected void showDialog(Bundle state) {
        super.showDialog(state);

        getEditText().removeTextChangedListener(this);
        getEditText().addTextChangedListener(this);
        onEditTextChanged();
    }

    @Override
    public void afterTextChanged(Editable s) {
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        onEditTextChanged();
    }

    private void onEditTextChanged() {
        try {
            boolean matches = getEditText().getText().toString().matches("^\\d+s(,\\d+s)*$");

            Dialog dlg = getDialog();
            if (dlg instanceof AlertDialog) {
                ((AlertDialog) dlg).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(matches);
            }
        } catch (NullPointerException ignore) {
        }
    }


    @Override
    protected boolean persistString(String value) {
        setSummary(value);
        return super.persistString(value);
    }

}