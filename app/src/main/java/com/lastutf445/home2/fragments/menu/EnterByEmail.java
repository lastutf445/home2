package com.lastutf445.home2.fragments.menu;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.lastutf445.home2.R;
import com.lastutf445.home2.activities.MainActivity;
import com.lastutf445.home2.loaders.NotificationsLoader;
import com.lastutf445.home2.network.Sync;
import com.lastutf445.home2.util.NavigationFragment;
import com.lastutf445.home2.util.SimpleAnimator;
import com.lastutf445.home2.util.SyncProvider;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;

public class EnterByEmail extends NavigationFragment {

    private LinearLayout firstWrapper, secondWrapper;
    private EditText email, code;
    private TextView emailValue;
    private Button resend;

    private AltAuthenticator provider;
    private Updater updater;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.enter_by_email, container, false);
        init();
        return view;
    }

    @Override
    protected void init() {
        updater = new Updater(this);

        try {
            provider = new AltAuthenticator(updater);

        } catch (JSONException e) {
            NotificationsLoader.makeToast("Unexpected error", true);
            getActivity().onBackPressed();
            //e.printStackTrace();
            return;
        }

        View.OnClickListener c = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.emailSend:
                        emailSend();
                        break;
                    case R.id.emailCodeReceiveAgain:
                        emailResend();
                        break;
                    case R.id.emailCodeSend:
                        codeSend();
                        break;

                }
            }
        };

        firstWrapper = view.findViewById(R.id.emailFirstWrapper);
        secondWrapper = view.findViewById(R.id.emailSecondWrapper);
        SimpleAnimator.collapse(secondWrapper, 0);
        secondWrapper.setVisibility(View.GONE);

        email = view.findViewById(R.id.emailField);
        code = view.findViewById(R.id.codeField);
        resend = view.findViewById(R.id.emailCodeReceiveAgain);
        emailValue = view.findViewById(R.id.emailValue);

        view.findViewById(R.id.emailSend).setOnClickListener(c);
        view.findViewById(R.id.emailCodeReceiveAgain).setOnClickListener(c);
        view.findViewById(R.id.emailCodeSend).setOnClickListener(c);
    }

    private void emailSend() {
        if (!isValidEmail(email.getText().toString())) {
            NotificationsLoader.makeToast("Invalid email", true);
            return;
        }

        nextStep();
    }

    private void nextStep() {
        MainActivity.hideKeyboard();
        emailValue.setText(email.getText());
        secondWrapper.setVisibility(View.VISIBLE);
        SimpleAnimator.collapse(firstWrapper, 200);

        updater.postDelayed(
                new Runnable() {
                    @Override
                    public void run() {
                        firstWrapper.setVisibility(View.GONE);
                        SimpleAnimator.expand(secondWrapper, 400);
                    }
                }, 200
        );
    }

    private void emailResend() {

    }

    private void codeSend() {

    }

    public boolean isValidEmail(@NonNull String target) {
        return !TextUtils.isEmpty(target) && Patterns.EMAIL_ADDRESS.matcher(target).matches();
    }

    @Override
    public void onDestroy() {
        MainActivity.hideKeyboard();
        super.onDestroy();
    }

    public static class Updater extends Handler {
        private WeakReference<EnterByEmail> weakEnter;

        public Updater(@NonNull EnterByEmail enter) {
            weakEnter = new WeakReference<>(enter);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    end(
                            msg.getData().getInt("status")
                    );
                    break;
                case 1:
                    result(msg.getData());
                    break;
            }
        }

        private void result(Bundle data) {

        }

        private void end(int status) {

        }
    }

    public class AltAuthenticator extends SyncProvider {
        private WeakReference<Updater> weakUpdater;
        private String act = "emailAuthCode";
        private boolean isWaiting = true;
        private boolean tainted = true;
        private String email;

        public AltAuthenticator(@NonNull Updater updater) throws JSONException {
            super(
                    Sync.PROVIDER_ENTER_BY_EMAIL,
                    "emailAuthCode",
                    new JSONObject(),
                    null,
                    Sync.DEFAULT_PORT
            );

            weakUpdater = new WeakReference<>(updater);
        }

        @Override
        public boolean isWaiting() {
            return isWaiting;
        }

        public void firstStep(@NonNull String email) {
            this.email = email;
            tainted = true;
        }

        public void secondStep(int code) {

        }

        public void resend() {

        }
    }
}
