package com.example.yaginuma.nfcsample;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.ndeftools.Message;
import org.ndeftools.Record;
import org.ndeftools.externaltype.AndroidApplicationRecord;
import org.ndeftools.util.activity.NfcTagWriterActivity;
import org.ndeftools.wellknown.Action;
import org.ndeftools.wellknown.ActionRecord;
import org.ndeftools.wellknown.SmartPosterRecord;
import org.ndeftools.wellknown.TextRecord;
import org.ndeftools.wellknown.UriRecord;

import java.util.List;


public class MyActivity extends NfcTagWriterActivity {
    private static final String TAG = MyActivity.class.getSimpleName();
    private TextView mResultText;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);
        Button writeButton = (Button) findViewById(R.id.writeButton);
        mResultText = (TextView) findViewById(R.id.resultText);

        final Context context = this;

        writeButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // NFCの検出を有効にします
                        Toast.makeText(context, "データの書き込みを行います. NFCタグを近づけて下さい", Toast.LENGTH_SHORT).show();
                        setDetecting(true);
                    }
                }
        );
        // initialize NFC
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        nfcPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, this.getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

    }

    @Override
    public void onResume() {
        super.onResume();
        enableForegroundMode();
    }

    @Override
    public void onPause() {
        super.onPause();
        disableForegroundMode();
    }

    public void enableForegroundMode() {
        Log.d(TAG, "enableForegroundMode");

        // foreground mode gives the current active application priority for reading scanned tags
        IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED); // filter for tags
        IntentFilter[] writeTagFilters = new IntentFilter[] {tagDetected};
        nfcAdapter.enableForegroundDispatch(this, nfcPendingIntent, writeTagFilters, null);
    }

    public void disableForegroundMode() {
        Log.d(TAG, "disableForegroundMode");

        nfcAdapter.disableForegroundDispatch(this);
    }

    @Override
    public void onNewIntent(Intent intent) { // this method is called when an NFC tag is scanned
        Log.d(TAG, "onNewIntent");

        // check for NFC related actions
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {
            Toast.makeText(this, "NFCタグのデータを読み込んでいます...", Toast.LENGTH_SHORT).show();
            String result = readDataFromNfc(intent);
            mResultText.setText(result);
            Toast.makeText(this, "読み込み完了しました", Toast.LENGTH_SHORT).show();
        } else {
            // ignore
        }
    }

    public String readDataFromNfc(Intent intent) {
        Parcelable[] messages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
        String result = "";
        if (messages != null) {
            Log.d(TAG, "Found " + messages.length + " NDEF messages");
            result += "Found " + messages.length + " NDEF messages" + "\n";

            // parse to records
            for (int i = 0; i < messages.length; i++) {
                try {
                    List<Record> records = new org.ndeftools.Message((NdefMessage)messages[i]);

                    Log.d(TAG, "Found " + records.size() + " records in message " + i);
                    result += "Found " + records.size() + " records in message " + i + "\n";

                    for(int k = 0; k < records.size(); k++) {
                        Log.d(TAG, " Record #" + k + " is of class " + records.get(k).getClass().getSimpleName());
                       result += " Record #" + k + " is of class " + records.get(k).getClass().getSimpleName() + "\n";

                        Record record = records.get(k);
                        if(record instanceof AndroidApplicationRecord) {
                            AndroidApplicationRecord aar = (AndroidApplicationRecord)record;
                            Log.d(TAG, "Package is " + aar.getPackageName());
                            result += "Package is " + aar.getPackageName() + "\n";
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Problem parsing message", e);
                }

            }
        }
        return result;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.my, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * NFC機能がない場合に呼ばれます
     */
    @Override
    protected void onNfcFeatureNotFound() {
        Toast.makeText(this, "お使いの端末はNFCに対応していません", Toast.LENGTH_SHORT).show();
        finish();
    }
    /**
     * NFC機能はあるが、有効になっていない場合に呼ばれます<br>
     * 設定アプリにてNFCを有効にすればNFCが使えます
     */
    @Override
    protected void onNfcStateDisabled() {
        Toast.makeText(this, "NFCが有効になっていません\n設定アプリでNFCを有効にしてください", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent("android.settings.NFC_SETTINGS");
        startActivity(intent);
    }
    /**
     * NFCが使える状態の際に呼ばれます
     */
    @Override
    protected void onNfcStateEnabled() {
        Toast.makeText(this, "NFCが使えます", Toast.LENGTH_SHORT).show();
    }
    /**
     * NFC機能の状態が変化した場合に呼ばれます<br>
     * (ex. NFC OFF -> ON, ON -> OFF)
     *
     * @param enabled
     */
    @Override
    protected void onNfcStateChange(boolean enabled) {
        String message = String.format("NFCが%sになりました", enabled ? "有効" : "無効");
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
    /**
     * 書き込むNDEFメッセージを作成するためのメソッドです<br>
     * 書き込み直前に呼ばれます
     */
    @Override
    protected NdefMessage createNdefMessage() {
        // NDEFレコードの作成
        TextRecord title = new TextRecord("Android");
        UriRecord uri = new UriRecord("http://y-yagi.tumblr.com/");
        ActionRecord action = new ActionRecord(Action.DEFAULT_ACTION);
        SmartPosterRecord smartPosterRecord = new SmartPosterRecord(title, uri, action);
       // NDEFメッセージの作成
        Message message = new Message();
        message.add(smartPosterRecord);
        return message.getNdefMessage();
    }
    /**
     * NDEFに対応していないNFCタグを検出した際に呼ばれます
     */
    @Override
    protected void writeNdefCannotWriteTech() {
        Toast.makeText(this, "このNFCタグはNDEFに対応していません。\n違うNFCタグをかざしてください。", Toast.LENGTH_SHORT)
                .show();
    }
    /**
     * NFCタグが書き込み禁止になっている場合に呼ばれます
     */
    @Override
    protected void writeNdefNotWritable() {
        Toast.makeText(this, "このNFCタグは書き込み禁止になっています。\n違うNFCタグをかざしてください。", Toast.LENGTH_SHORT)
                .show();
    }
    /**
     * 書き込もうとしたNDEFメッセージのサイズが大きすぎた場合に呼ばれます
     *
     * @param required 書き込もうとしたNDEFメッセージのサイズ
     * @param capacity 検出されたNFCタグに書き込み可能なサイズ
     */
    @Override
    protected void writeNdefTooSmall(int required, int capacity) {
        Toast.makeText(this, "NFCタグのメモリが小さすぎて書き込めません。\n違うNFCタグをかざしてください。", Toast.LENGTH_SHORT)
                .show();
    }
    /**
     * NDEFの書き込みに成功した際に呼ばれます
     */
    @Override
    protected void writeNdefSuccess() {
        Toast.makeText(this, "書き込みに成功しました", Toast.LENGTH_SHORT).show();
    }
    /**
     * 書き込みに失敗した際に呼ばれます
     */
    @Override
    protected void writeNdefFailed(Exception e) {
        Log.d("Ndef tools for Android demo", "An exception has been occured", e);
        Toast.makeText(this, "書き込みに失敗しました。\nもう一度NFCタグをかざしてください。", Toast.LENGTH_SHORT).show();
    }

}
