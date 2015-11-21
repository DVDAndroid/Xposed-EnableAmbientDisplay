/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 DVDAndroid
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.dvd.android.xposed.enableambientdisplay.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.dvd.android.xposed.enableambientdisplay.R;

/**
 * A {@link DialogPreference} that provides a user with the means to select an
 * integer from a {@link SeekBar}, and persist it.
 *
 * @author lukehorvat
 *
 */
public class SeekBarDialogPreference extends DialogPreference {
	private static final int DEFAULT_MIN_PROGRESS = 0;
	private static final int DEFAULT_MAX_PROGRESS = 100;
	private static final int DEFAULT_PROGRESS = 0;

	private int mMinProgress;
	private int mMaxProgress;
	private int mProgress;
	private int mDefaultProgress;
	private CharSequence mProgressTextSuffix;
	private TextView mProgressText;
	private SeekBar mSeekBar;

	public SeekBarDialogPreference(Context context) {
		this(context, null);
	}

	public SeekBarDialogPreference(Context context, AttributeSet attrs) {
		super(context, attrs);

		// get attributes specified in XML
		TypedArray a = context.getTheme().obtainStyledAttributes(attrs,
				R.styleable.SeekBarDialogPreference, 0, 0);
		try {
			mDefaultProgress = a.getInteger(
					R.styleable.SeekBarDialogPreference_android_defaultValue,
					-1);

			setMinProgress(a.getInteger(R.styleable.SeekBarDialogPreference_min,
					DEFAULT_MIN_PROGRESS));
			setMaxProgress(a.getInteger(
					R.styleable.SeekBarDialogPreference_android_max,
					DEFAULT_MAX_PROGRESS));
			setProgressTextSuffix(a.getString(
					R.styleable.SeekBarDialogPreference_progressTextSuffix));
		} finally {
			a.recycle();
		}

		// set layout
		setDialogLayoutResource(R.layout.preference_seek_bar_dialog);
		setPositiveButtonText(android.R.string.ok);
		setNegativeButtonText(android.R.string.cancel);
		setDialogIcon(null);
	}

	@Override
	protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
		super.onPrepareDialogBuilder(builder);
		builder.setNeutralButton(R.string.def,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						setProgress(mDefaultProgress);
					}
				});
	}

	@Override
	protected void onSetInitialValue(boolean restore, Object defaultValue) {
		setProgress(restore ? getPersistedInt(DEFAULT_PROGRESS)
				: (Integer) defaultValue);
	}

	@Override
	protected Object onGetDefaultValue(TypedArray a, int index) {
		return a.getInt(index, DEFAULT_PROGRESS);
	}

	@Override
	protected void onBindDialogView(View view) {
		super.onBindDialogView(view);

		TextView dialogMessageText = (TextView) view
				.findViewById(R.id.text_dialog_message);
		dialogMessageText.setText(getDialogMessage());

		mProgressText = (TextView) view.findViewById(R.id.text_progress);

		mSeekBar = (SeekBar) view.findViewById(R.id.seek_bar);
		mSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				// update text that displays the current SeekBar progress value
				// note: this does not persist the progress value. that is only
				// ever done in setProgress()
				String progressStr = String.valueOf(progress + mMinProgress);
				mProgressText.setText(mProgressTextSuffix == null ? progressStr
						: progressStr.concat(mProgressTextSuffix.toString()));
			}
		});
		mSeekBar.setMax(mMaxProgress - mMinProgress);
		mSeekBar.setProgress(mProgress - mMinProgress);
	}

	public int getMinProgress() {
		return mMinProgress;
	}

	public void setMinProgress(int minProgress) {
		mMinProgress = minProgress;
		setProgress(Math.max(mProgress, mMinProgress));
	}

	public int getMaxProgress() {
		return mMaxProgress;
	}

	public void setMaxProgress(int maxProgress) {
		mMaxProgress = maxProgress;
		setProgress(Math.min(mProgress, mMaxProgress));
	}

	public int getProgress() {
		return mProgress;
	}

	public void setProgress(int progress) {
		progress = Math.max(Math.min(progress, mMaxProgress), mMinProgress);

		if (progress != mProgress) {
			mProgress = progress;
			persistInt(progress);
			notifyChanged();
		}

		setSummary(mProgressTextSuffix == null ? Integer.toString(progress)
				: Integer.toString(progress)
						.concat(mProgressTextSuffix.toString()));
	}

	public CharSequence getProgressTextSuffix() {
		return mProgressTextSuffix;
	}

	public void setProgressTextSuffix(CharSequence progressTextSuffix) {
		mProgressTextSuffix = progressTextSuffix;
	}

	@Override
	protected void onDialogClosed(boolean positiveResult) {
		super.onDialogClosed(positiveResult);

		// when the user selects "OK", persist the new value
		if (positiveResult) {
			int seekBarProgress = mSeekBar.getProgress() + mMinProgress;
			if (callChangeListener(seekBarProgress)) {
				setProgress(seekBarProgress);
			}
		}
	}

	@Override
	protected Parcelable onSaveInstanceState() {
		// save the instance state so that it will survive screen orientation
		// changes and other events that may temporarily destroy it
		final Parcelable superState = super.onSaveInstanceState();

		// set the state's value with the class member that holds current
		// setting value
		final SavedState myState = new SavedState(superState);
		myState.minProgress = getMinProgress();
		myState.maxProgress = getMaxProgress();
		myState.progress = getProgress();

		return myState;
	}

	@Override
	protected void onRestoreInstanceState(Parcelable state) {
		// check whether we saved the state in onSaveInstanceState()
		if (state == null || !state.getClass().equals(SavedState.class)) {
			// didn't save the state, so call superclass
			super.onRestoreInstanceState(state);
			return;
		}

		// restore the state
		SavedState myState = (SavedState) state;
		setMinProgress(myState.minProgress);
		setMaxProgress(myState.maxProgress);
		setProgress(myState.progress);

		super.onRestoreInstanceState(myState.getSuperState());
	}

	private static class SavedState extends BaseSavedState {
		@SuppressWarnings("unused")
		public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
			@Override
			public SavedState createFromParcel(Parcel in) {
				return new SavedState(in);
			}

			@Override
			public SavedState[] newArray(int size) {
				return new SavedState[size];
			}
		};
		int minProgress;
		int maxProgress;
		int progress;

		public SavedState(Parcelable superState) {
			super(superState);
		}

		public SavedState(Parcel source) {
			super(source);

			minProgress = source.readInt();
			maxProgress = source.readInt();
			progress = source.readInt();
		}

		@Override
		public void writeToParcel(Parcel dest, int flags) {
			super.writeToParcel(dest, flags);

			dest.writeInt(minProgress);
			dest.writeInt(maxProgress);
			dest.writeInt(progress);
		}
	}
}