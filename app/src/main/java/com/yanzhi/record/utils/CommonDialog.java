package com.yanzhi.record.utils;


import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;

import com.yanzhi.record.R;

import java.util.Timer;
import java.util.TimerTask;


public class CommonDialog extends Dialog implements View.OnClickListener{

    private EditText m_userInput;
    private Button m_cancel;
    private Button m_ok;
    private Context mContext;
    private OnCloseListener m_listener;


    public CommonDialog(Context context, int themeResId, OnCloseListener listener) {
        super(context, themeResId);
        this.mContext = context;
        this.m_listener = listener;
    }


    public void setOkButtonEnable(boolean enable){
        m_ok.setEnabled(enable);

        if(enable){
            m_ok.setAlpha(1f);
        }else{
            m_ok.setAlpha(0.3f);
        }
    }

    public String getUserInputText(){
        return m_userInput.getText().toString();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.prompts);
        setCanceledOnTouchOutside(false);
        initView();

        /*弹出键盘*/
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                InputMethodManager inputManager = (InputMethodManager) m_userInput.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                inputManager.showSoftInput(m_userInput, 0);
            }

        }, 200);
    }

    private void initView(){

        m_userInput = (EditText) findViewById(R.id.user_input);
        m_ok = (Button) findViewById(R.id.ok);
        m_cancel = (Button) findViewById(R.id.cancel);

        m_ok.setOnClickListener(this);
        m_cancel.setOnClickListener(this);

        setOkButtonEnable(false);

        m_userInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if(charSequence.toString().isEmpty()){
                    setOkButtonEnable(false);
                }else{
                    setOkButtonEnable(true);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });


    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.cancel:
                if(m_listener != null) {
                    m_listener.onClick(this, false);
                }
                break;
            case R.id.ok:
                if(m_listener != null){
                    m_listener.onClick(this, true);
                }
                break;
        }
        this.dismiss();
    }

    public interface OnCloseListener{
        void onClick(Dialog dialog, boolean ok);
    }
}
