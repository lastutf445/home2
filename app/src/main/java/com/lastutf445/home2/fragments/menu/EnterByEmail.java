package com.lastutf445.home2.fragments.menu;

import android.content.DialogInterface;
import android.graphics.Color;
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
import androidx.annotation.StringRes;

import com.lastutf445.home2.R;
import com.lastutf445.home2.activities.MainActivity;
import com.lastutf445.home2.fragments.dialog.Processing;
import com.lastutf445.home2.loaders.CryptoLoader;
import com.lastutf445.home2.loaders.DataLoader;
import com.lastutf445.home2.loaders.NotificationsLoader;
import com.lastutf445.home2.loaders.UserLoader;
import com.lastutf445.home2.network.Sync;
import com.lastutf445.home2.util.NavigationFragment;
import com.lastutf445.home2.util.SimpleAnimator;
import com.lastutf445.home2.util.SyncProvider;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.Locale;

public class EnterByEmail extends NavigationFragment {

    private LinearLayout firstWrapper, secondWrapper;
    private EditText email, code;
    private TextView emailValue;
    private Button resend;

    private boolean isEmailSent = false;
    private boolean authSuccess = false;
    private UiUpdate uiUpdate;
    private int sec_left = -1;

    private NoPopConnector connector;
    private AltAuthenticator provider;
    private Processing processing;
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

        processing = new Processing();
        processing.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                Sync.removeSyncProvider(Sync.PROVIDER_ENTER_BY_EMAIL);
                Sync.removeSyncProvider(Sync.PROVIDER_GET_PUBLIC_KEY);
                dialog.cancel();
            }
        });

        uiUpdate = new UiUpdate();

        try {
            provider = new AltAuthenticator(updater);
            Sync.addSyncProvider(provider);

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

        SimpleAnimator.drawableTint(
                (Button) view.findViewById(R.id.emailSend),
                DataLoader.getAppResources().getColor(R.color.colorPrimary)
        );

        SimpleAnimator.drawableTint(
                (Button) view.findViewById(R.id.emailCodeReceiveAgain),
                Color.parseColor("#666666")
        );

        SimpleAnimator.drawableTint(
                (Button) view.findViewById(R.id.emailCodeSend),
                DataLoader.getAppResources().getColor(R.color.colorPrimary)
        );
    }

    public void setConnector(@Nullable NoPopConnector connector) {
        this.connector = connector;
    }

    private void emailSend() {
        MainActivity.hideKeyboard();

        if (!isValidEmail(email.getText().toString())) {
            NotificationsLoader.makeToast("Invalid email", true);
            return;
        }

        getChildFragmentManager().beginTransaction()
                .add(processing, "processing")
                .show(processing).commitAllowingStateLoss();

        provider.firstStep(email.getText().toString());
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

    private void setupResendButton(int sec_left) {
        if (sec_left > 0) {
            updater.postDelayed(uiUpdate, 1000);
        }

        setupResendButton2(sec_left);
    }

    private void setupResendButton2(int sec_left) {
        if (sec_left > 0) {
            resend.setText(
                    String.format(
                            Locale.UK,
                            "%s (0:%02d)",
                            DataLoader.getAppResources().getString(R.string.emailCodeResend),
                            sec_left
                    )
            );

        } else {
            resend.setText(
                    DataLoader.getAppResources().getString(R.string.emailCodeResend)
            );
        }
    }

    private void emailResend() {
        if (sec_left <= 0) {
            getChildFragmentManager().beginTransaction()
                    .add(processing, "processing")
                    .show(processing).commitAllowingStateLoss();

            provider.sendEmail();
        }
    }

    private void codeSend() {
        String t = code.getText().toString().trim();

        try {
            int code = Integer.valueOf(t);

            getChildFragmentManager().beginTransaction()
                    .add(processing, "processing")
                    .show(processing).commitAllowingStateLoss();

            provider.secondStep(code);

        } catch (Exception e) {
            NotificationsLoader.makeToast("Invalid code", true);
            //e.printStackTrace();
        }
    }

    public boolean isValidEmail(@NonNull String target) {
        return !TextUtils.isEmpty(target) && Patterns.EMAIL_ADDRESS.matcher(target).matches();
    }

    @Override
    public boolean onBackPressed() {
        if (connector != null && authSuccess) {
            connector.onPop();
            return false;
        }

        return true;
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        if (hidden) {
            MainActivity.hideKeyboard();
        }
    }

    @Override
    public void onDestroy() {
        Sync.removeSyncProvider(Sync.PROVIDER_GET_PUBLIC_KEY);
        Sync.removeSyncProvider(Sync.PROVIDER_ENTER_BY_EMAIL);
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
                    finish(R.string.unexpectedError);
                    break;
                case 1:
                    setTitle(R.string.processing);
                    break;
                case 2:
                case 3:
                    setTitle(R.string.waitingForAConnection);
                    break;
                case 4:
                    finish(R.string.encryptionError);
                    break;
                case 5:
                    setTitle(R.string.authRequestingPubKey);
                    break;
                case 6:
                    finish(R.string.authInvalidPubKey);
                    break;
                case 7:
                    setTitle(R.string.authRequestingCode);
                    break;
                case 8:
                    setTitle(R.string.authSendingCode);
                    break;
                case 9:
                    finish(R.string.authNoEmailMatches);
                    break;
                case 10:
                    finish(R.string.authRequestingCodeTimeout);
                    break;
                case 11:
                    finish(R.string.authNoMoreRequests);
                    finish(R.string.authNoMoreRequests2);
                    break;
                case 12:
                    codeSent();
                    break;
                case 13:
                    ok();
                    break;
                case 14:
                    finish(R.string.authCodeIncorrect);
                    break;
                case 15:
                    finish(R.string.altAuthDisabled);
                    break;
            }
        }

        private void codeSent() {
            EnterByEmail enter = weakEnter.get();

            if (enter != null) {
                if (!enter.isEmailSent) enter.nextStep();
                enter.isEmailSent = true;
            }

            finish(R.string.authCodeSent);
        }

        private void ok() {
            finish(R.string.success);
            EnterByEmail enter = weakEnter.get();

            if (enter != null) {
                Bundle toParent = enter.toParent;
                toParent.putBoolean("auth", true);
                enter.authSuccess = true;
                enter.getActivity().onBackPressed();
            }
        }

        private void finish(@StringRes int id) {
            if (id != 0) {
                NotificationsLoader.makeToast(
                        DataLoader.getAppResources().getString(id),
                        true
                );
            }

            closeDialog();
        }

        private void setTitle(@StringRes int id) {
            EnterByEmail enter = weakEnter.get();

            if (enter != null) {
                Processing processing = enter.processing;

                if (processing != null && !processing.isInactive()) {
                    processing.setTitle(
                            DataLoader.getAppResources().getString(id)
                    );
                }
            }
        }

        private void closeDialog() {
            EnterByEmail enter = weakEnter.get();

            if (enter != null) {
                Processing processing = enter.processing;

                if (processing != null && processing.getDialog() != null) {
                    processing.getDialog().cancel();
                }
            }
        }

        private void setupResendButton(int sec_left) {
            EnterByEmail editor = weakEnter.get();

            if (editor != null) {
                editor.sec_left = sec_left + 1;
                removeCallbacks(editor.uiUpdate);
                post(editor.uiUpdate);
            }
        }
    }

    public class UiUpdate implements Runnable {
        @Override
        public void run() {
            setupResendButton(--sec_left);
        }
    }

    public class AltAuthenticator extends SyncProvider implements UserLoader.GetPublicKey.Callback {
        private WeakReference<Updater> weakUpdater;
        private String act = "emailGetCode";
        private boolean isWaiting = true;
        private boolean tainted = true;
        private String key;
        private int stage = 5;
        private String email;
        private int code;

        public AltAuthenticator(@NonNull Updater updater) throws JSONException {
            super(
                    Sync.PROVIDER_ENTER_BY_EMAIL,
                    "emailGetCode",
                    new JSONObject(),
                    null,
                    Sync.DEFAULT_PORT,
                    false);

            weakUpdater = new WeakReference<>(updater);
            key = CryptoLoader.createMaxAESKey();
        }

        @Override
        public boolean isWaiting() {
            return isWaiting;
        }

        public void firstStep(@NonNull String email) {
            Updater updater = weakUpdater.get();
            if (updater == null) return;

            this.email = email;
            tainted = true;

            if (stage == 5) {
                try {
                    Sync.addSyncProvider(new UserLoader.GetPublicKey(this));

                } catch (JSONException e) {
                    NotificationsLoader.makeToast("Unexpected error", true);
                    //e.printStackTrace();
                    return;
                }

            } else {
                sendEmail();
            }
        }

        /**
         * HANDLER RETURN CODES:
         * 5 - requesting publicKey
         * 6 - invalid publicKey
         * 7 - request code
         * 8 - send code
         * 9 - auth failed
         * 10 - code request timeout
         * 11 - no more code requests
         * 12 - code send
         * 13 - auth success
         * 14 - code incorrect
         */

        @Override
        public void onValid(@NonNull String modulus, @NonNull String pubExp) {
            try {
                CryptoLoader.setPublicKey(modulus, pubExp);
                sendEmail();

            } catch (NumberFormatException e) {
                //e.printStackTrace();
                onInvalid();
            }
        }

        @Override
        public void onInvalid() {
            Handler handler = weakUpdater.get();
            if (handler == null) return;

            handler.sendEmptyMessage(6);
        }

        private synchronized void sendEmail() {
            Sync.addSyncProvider(this);
            stage = 7;
            act = "emailGetCode";
            tainted = true;
            isWaiting = false;
        }

        @Override
        public void onPostPublish(int statusCode) {
            Updater handler = weakUpdater.get();

            if (handler != null) {
                if (statusCode != 1) {
                    handler.sendEmptyMessage(statusCode);
                    isWaiting = true;
                } else {
                    handler.sendEmptyMessage(stage);
                }

            } else {
                isWaiting = true;
            }
        }

        public synchronized void secondStep(int code) {
            Sync.addSyncProvider(this);
            this.code = code;
            act = "emailCheckCode";
            stage = 8;
            tainted = true;
            isWaiting = false;
        }

        public synchronized void cancel() {
            isWaiting = true;
        }

        @Override
        public void onReceive(JSONObject data) {
            if (!data.has("status")) return;
            Sync.removeSyncProvider(Sync.PROVIDER_ENTER_BY_EMAIL);

            try {
                int status = data.getInt("status");

                switch (status) {
                    case Sync.UNKNOWN_USER:
                        onPostPublish(9);
                        return;
                    case Sync.UNEXPECTED_ERROR:
                        onPostPublish(0);
                        return;
                    case Sync.CODE_REQUEST_TIMEOUT:
                        onPostPublish(10);
                        return;
                    case Sync.NO_MORE_CODE_REQUESTS:
                        onPostPublish(11);
                        return;
                    case Sync.UNKNOWN_ACCESS_CODE:
                        onPostPublish(14);
                        return;
                    case Sync.ALT_AUTH_DISABLED:
                        onPostPublish(15);
                        return;
                }

                if (status == Sync.OK) {
                    if (stage == 7) {
                        int sec_left = data.getInt("left");
                        updater.setupResendButton(sec_left);
                        onPostPublish(12);

                    } else {
                        DataLoader.setWithoutSync("Username", DataLoader.getAppResources().getString(R.string.usernameDefault));
                        DataLoader.setWithoutSync("Session", data.getString("session"));
                        DataLoader.setWithoutSync("AESKey", key);
                        CryptoLoader.setAESKey(key);
                        DataLoader.save();

                        onPostPublish(13);
                    }
                } else {
                    onPostPublish(0);
                }

            } catch (JSONException e) {
                onPostPublish(0);
                //e.printStackTrace();
            }
        }

        @Override
        public JSONObject getQuery() {
            if (tainted) {
                try {
                    JSONObject data = new JSONObject();
                    data.put("email", email);

                    if (stage != 7) {
                        data.put("code", code);
                        data.put("key", key);
                    }

                    query.put("act", act);
                    query.put("data", data);

                    tainted = false;

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            return query;
        }
    }

    public interface NoPopConnector {
        void onPop();
    }
}
