package com.example.sander.bunqer.Helpers;
/*
 * Created by sander on 11-6-17.
 */

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.net.Uri;
import android.util.Log;

import com.example.sander.bunqer.DB.DBManager;
import com.example.sander.bunqer.ModelClasses.Account;
import com.example.sander.bunqer.ModelClasses.Category;
import com.example.sander.bunqer.ModelClasses.Transaction;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class CsvImportHelper {

    private static DBManager dbManager;

    // constructor
    public CsvImportHelper() {
    }

    /**
     * Processes the CSV file that is shared through a intent.action.SEND. Returns
     * an ArrayList of Transaction objects.
     *
     * @param context
     * @param receivedIntent
     * @return
     */
    public static ArrayList<Transaction> getTransactionList(Context context, Intent receivedIntent) {
        ArrayList<Transaction> transactions = new ArrayList<>();
        dbManager = DBManager.getInstance();

        Uri uri = receivedIntent.getClipData().getItemAt(0).getUri();
        if (uri != null) {
            FileInputStream inputStream;
            try {
                AssetFileDescriptor descriptor = context.getContentResolver()
                        .openTypedAssetFileDescriptor(uri, "text/*", null);
                if (descriptor == null) {
                    Log.d("log", "descriptor is null");
                }
                inputStream = descriptor.createInputStream();
                InputStreamReader isReader = new InputStreamReader(inputStream, "UTF-8");

                BufferedReader bReader = new BufferedReader(isReader);

                // skip over first line
                bReader.readLine();

                // for testing
                int i = 0;

                String line;
                while ((line = bReader.readLine()) != null) {

                    // for testing
                    Log.d("log", "iteratie: " + i);

                    // general formatting and splitting of line
                    line = line.replace("\"", "");
                    Log.d("log", "getTransactionlist.validate: " + line);

                    String[] rowData = line.split(";");

                    // create new transactions using the data
                    Transaction transaction = new Transaction();
                    transaction.setDate(rowData[0]);
                    transaction.setAmount(rowData[1]);
                    transaction.setAccount(rowData[2]);
                    transaction.setCounterpartyAccount(rowData[3]);
                    transaction.setCounterpartyName(rowData[4]);
                    transaction.setDescription(rowData[5].replaceAll("[^a-zA-Z\\d\\s]", ""));
                    transaction.setAccountId(getAccountId(transaction));

                    // don't add if identical transaction already exists
                    if (transaction.isNotDuplicate()) {
                        transactions.add(transaction);
                    }

                    Log.d("log", "getTransactionlist: transactions: " + transactions.toString());

                    // for testing
                    i++;
                }
                // categorize transactions
                Log.d("log","completing import");
                return CategoryHelper.categorize(transactions);

            } catch (NullPointerException e) {
                Log.w("ClipData", "Failure to create stream");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // shouldn't get here but, just to be safe
        Log.e("getTransactionList", "Failed to import transactions");
        return transactions;
    }

    public static int getAccountId(Transaction transaction) {
        // check if account already exists
        ArrayList<Account> accounts = dbManager.readAccounts();
        for (Account account: accounts) {
            if (account.getNumber().equals(transaction.getAccount())) {
                // and brand transaction with the account id
                return account.getId();
            }
        }

        // otherwise setup new account
        Account newAccount = setupAccount(transaction);
        return newAccount.getId();
    }

    private static Account setupAccount(Transaction transaction) {
        // create account
        Account account = new Account(transaction.getAccount(), transaction.getAccount());
        dbManager.createAccount(account);

        // get new account from database
        ArrayList<Account> accounts = dbManager.readAccounts();
        Account newAccount = accounts.get(accounts.size()-1);

        CategoryHelper.setupDefaultCategories(newAccount);

        return accounts.get(accounts.size()-1);
    }
}
