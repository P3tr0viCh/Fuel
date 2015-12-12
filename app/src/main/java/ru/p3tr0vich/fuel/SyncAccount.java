package ru.p3tr0vich.fuel;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Context;

public class SyncAccount {

    private final AccountManager mAccountManager;

    private final String mAuthority;
    private final String mAccountName;
    private final String mAccountType;

    SyncAccount(Context context) {
        mAccountManager = (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);

        mAuthority = context.getString(R.string.sync_authority);
        mAccountName = context.getString(R.string.sync_account_name);
        mAccountType = context.getString(R.string.sync_account_type);
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
        Account account;

        Account accounts[] = mAccountManager.getAccountsByType(getAccountType());

        if (accounts.length > 0) {
            account = accounts[0];

//            Functions.logD("SyncAccount -- getAccount: accounts[0] == " + accounts[0].toString());
        } else {
            account = new Account(getAccountName(), getAccountType());

            if (mAccountManager.addAccountExplicitly(account, null, null)) {
                setIsSyncable(account, FuelingPreferenceManager.isSyncEnabled());

                Functions.logD("SyncAccount -- getAccount: addAccountExplicitly == true");
            } else {
                Functions.logD("SyncAccount -- getAccount: addAccountExplicitly == false");
            }
        }

        return account;
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
}
