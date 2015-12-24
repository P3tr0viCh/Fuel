package ru.p3tr0vich.fuel;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Context;
import android.text.TextUtils;

class SyncAccount {

    private static final String TAG = "SyncAccount";

    private static final String YANDEX_DISK_TOKEN = "yandex disk token";

    private final AccountManager mAccountManager;

    private final String mAuthority;
    private final String mAccountName;
    private final String mAccountType;

    private final Account mAccount;

    SyncAccount(Context context) {
        mAccountManager = (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);

        mAuthority = context.getString(R.string.sync_authority);
        mAccountName = context.getString(R.string.sync_account_name);
        mAccountType = context.getString(R.string.sync_account_type);

        mAccount = createAccount();
    }

    private Account createAccount() {
        Account account;

        Account accounts[] = mAccountManager.getAccountsByType(getAccountType());

        if (accounts.length > 0) {
            account = accounts[0];

//            Functions.logD("SyncAccount -- getAccount: accounts[0] == " + accounts[0].toString());
        } else {
            account = new Account(getAccountName(), getAccountType());

            if (mAccountManager.addAccountExplicitly(account, null, null)) {
                setIsSyncable(account, PreferenceManagerFuel.isSyncEnabled());

                UtilsLog.d(TAG, "createAccount", "addAccountExplicitly == true");
            } else {
                UtilsLog.d(TAG, "createAccount", "addAccountExplicitly == false");
            }
        }

        return account;
    }

    public String getAuthority() {
        return mAuthority;
    }

    private String getAccountName() {
        return mAccountName;
    }

    private String getAccountType() {
        return mAccountType;
    }

    public Account getAccount() {
        return mAccount;
    }

    public boolean isSyncActive() {
        return ContentResolver.isSyncActive(getAccount(), getAuthority());
    }

    private void setIsSyncable(final Account account, final boolean syncable) {
        ContentResolver.setIsSyncable(account, getAuthority(), syncable ? 1 : 0);
    }

    public void setIsSyncable(final boolean syncable) {
        setIsSyncable(getAccount(), syncable);
    }

    @SuppressWarnings("SameParameterValue")
    private String getUserData(final String key) {
        return mAccountManager.getUserData(getAccount(), key);
    }

    @SuppressWarnings("SameParameterValue")
    private void setUserData(final String key, final String value) {
        mAccountManager.setUserData(getAccount(), key, value);
    }

    public String getYandexDiskToken() {
        return getUserData(YANDEX_DISK_TOKEN);
    }

    public boolean isYandexDiskTokenEmpty() {
        return TextUtils.isEmpty(getYandexDiskToken());
    }

    public void setYandexDiskToken(final String token) {
        setUserData(YANDEX_DISK_TOKEN, token);
    }
}
