/*
 * Copyright 2017 Hiroyuki Tamura
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cks.hiroyuki2.worksupport3.DialogFragments;

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.TimePicker;

import com.cks.hiroyuki2.worksupport3.R;
import com.cks.hiroyuki2.worksupprotlib.Entity.TimeEvent;
import com.cks.hiroyuki2.worksupprotlib.Entity.TimeEventRange;
import com.cks.hiroyuki2.worksupprotlib.UtilDialog;

import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;

import static android.app.Activity.RESULT_OK;
import static com.cks.hiroyuki2.worksupprotlib.Util.TIME_EVENT_RANGE;
import static com.cks.hiroyuki2.worksupprotlib.Util.onError;
import static com.example.hiroyuki3.worksupportlibw.Adapters.TimeEventRVAdapter.TIME_EVENT;
import static com.example.hiroyuki3.worksupportlibw.Adapters.TimeEventRangeRVAdapter.CALLBACK_RANGE_CLICK_TIME;
import static com.example.hiroyuki3.worksupportlibw.Adapters.TimeEventRangeRVAdapter.POSITION;

/**
 * ダイアログ作成おじさん！
 */
public class RecordDialogFragmentPicker extends DialogFragment implements DialogInterface.OnClickListener, TimePickerDialog.OnTimeSetListener, TimePicker.OnTimeChangedListener, RadioGroup.OnCheckedChangeListener{

    private static final String TAG = "MANUAL_TAG: " + RecordDialogFragmentPicker.class.getSimpleName();
    static final String DIALOG_TIME_TIME = "DIALOG_TIME";
    private TimeEvent timeEvent;
    private TimeEventRange range;
    private int pos;
    private Dialog dialog;
    @BindView(R.id.time_picker) TimePicker timePicker;
    @BindView(R.id.radio_group) RadioGroup radioGroup;
    @BindView(R.id.today) RadioButton radioToday;
    @BindView(R.id.yesterday) RadioButton radioYesterday;
    @BindView(R.id.tomorrow) RadioButton radioTommorow;
    @BindView(R.id.tv) TextView tv;
    @BindString(R.string.tp_range_msg) String msg;

    public static RecordDialogFragmentPicker newInstance(Bundle bundle){
        Log.d(TAG, "newInstance: fire");
        RecordDialogFragmentPicker frag = new RecordDialogFragmentPicker();
        frag.setArguments(bundle);
        return frag;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (dialog != null)
            return dialog;

        if (getTargetRequestCode() == CALLBACK_RANGE_CLICK_TIME){

            range = (TimeEventRange) getArguments().getSerializable(TIME_EVENT_RANGE);
            pos = (int) getArguments().getInt(POSITION);
            timeEvent = range.getTimeEve(pos);

            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            View view = getActivity().getLayoutInflater().inflate(R.layout.timepicker_custom, null);
            ButterKnife.bind(this, view);
            timePicker.setIs24HourView(true);
            if (timeEvent.getOffset() >0){
                radioTommorow.toggle();
            } else if (timeEvent.getOffset() <0){
                radioYesterday.toggle();
            }
            dialog =  UtilDialog.editBuilder(builder, null, R.string.ok, R.string.cancel, view, this, null).create();
            timePicker.setOnTimeChangedListener(this);
            radioGroup.setOnCheckedChangeListener(this);

        } else {
            timeEvent = (TimeEvent) getArguments().getSerializable(TIME_EVENT);
            dialog =  new TimePickerDialog(getActivity(), this, timeEvent.getHour(), timeEvent.getMin(), true);
            ((TimePickerDialog)dialog).setButton(DialogInterface.BUTTON_NEGATIVE, getContext().getString(R.string.cancel), this);
        }
        return dialog;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        dialog = null;
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int i) {
        if (i == Dialog.BUTTON_POSITIVE){
            sendIntent(timePicker);
        }
    }

    @Override
    public void onTimeSet(TimePicker timePicker, int i, int i1) {
        sendIntent(timePicker);
    }

    private void sendIntent(TimePicker timePicker){
        Fragment target = getTargetFragment();
        if (target == null){
            onError(getContext(), TAG + "target == null", R.string.error);
            return;
        }

        if (getTargetRequestCode() == CALLBACK_RANGE_CLICK_TIME){
            timeEvent.setOffset(getOffsetFromDialog());
        }

        timeEvent.setMin(getMin(timePicker));
        timeEvent.setHour(getHour(timePicker));

        Intent intent = new Intent();
        intent.putExtras(getArguments());
        target.onActivityResult(getTargetRequestCode(), RESULT_OK, intent);//targetはRecordFragment or EditTemplateFragmentのどちらかです
    }

    @Override
    public void onTimeChanged(TimePicker timePicker, int hourOfDay, int min) {
        onChangeCallback(hourOfDay, min);
    }

    @Override
    public void onCheckedChanged(RadioGroup radioGroup, int i) {
        onChangeCallback(getHour(timePicker), getMin(timePicker));
    }

    private int getMin(@NonNull TimePicker timePicker){
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ?
                timePicker.getMinute():
                timePicker.getCurrentMinute();
    }

    private int getHour(@NonNull TimePicker timePicker){
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ?
                timePicker.getHour():
                timePicker.getCurrentHour();
    }

    private void onChangeCallback(int hourOfDay, int min){
        Button okBtn = ((AlertDialog) getDialog()).getButton(DialogInterface.BUTTON_POSITIVE);
        int pairedPos = pos == 0 ? 1 : 0;
        TimeEvent paired = range.getTimeEve(pairedPos);
        TimeEvent eve = new TimeEvent("", 0, hourOfDay, min, getOffsetFromDialog());
        if (pairedPos == 0 && compare(paired, eve) > 0){
            tv.setText(msg);
            okBtn.setEnabled(false);
        } else if (pairedPos == 1 && compare(eve, paired) > 0){
            tv.setText(msg);
            okBtn.setEnabled(false);
        } else {
            tv.setText("");
            okBtn.setEnabled(true);
        }
    }

    private int getOffsetFromDialog(){
        int offset = 0;
        switch (radioGroup.getCheckedRadioButtonId()){
            case R.id.yesterday:
                offset = -1;
                break;
            case R.id.today:
                break;
            case R.id.tomorrow:
                offset = 1;
                break;
        }
        return offset;
    }

    /**
     * @param eve1 よりも @param eve2 が進んでいた場合、
     * @return 負の数を返す。
     */
    private int compare(TimeEvent eve1, TimeEvent eve2){
        if (eve1.getOffset() != eve2.getOffset())
            return eve1.getOffset() - eve2.getOffset();

        if (eve1.getHour() != eve2.getHour())
            return eve1.getHour() - eve2.getHour();

        return eve1.getMin() - eve2.getMin();
    }
}
