package com.example.nfctut_1;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.BreakIterator;

public class MainActivity extends AppCompatActivity {
    public static final String Error_Detected = "No NFC tag Detected";
    public static final String Write_Success = "Text Written Successfully:";
    public static final String Write_Error = "Error During Writing,Try Again!";
    NfcAdapter nfcAdapter;
    PendingIntent pendingIntent;
    IntentFilter[] writingTagFilters;
    boolean writeMode;
    Tag myTag;
    Context context;
    EditText editText;
    TextView textView;
    Button button;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        editText = findViewById(R.id.ettext);
        textView = findViewById(R.id.tv);
        button = findViewById(R.id.btn);
        context = this;
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    if (myTag == null){
                        Toast.makeText(context,Error_Detected,Toast.LENGTH_LONG).show();
                    }
                    else {
                       write("Plain text!"+textView.getText().toString(),myTag);
                        Toast.makeText(context,Write_Success,Toast.LENGTH_LONG).show();

                    }
                }catch (IOException e){
                    Toast.makeText(context,Write_Error,Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }catch (FormatException e){

                    Toast.makeText(context,Write_Error,Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }

            }
        });

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (nfcAdapter == null){
            Toast.makeText(this,"This Device does not Support NFC.",Toast.LENGTH_LONG).show();
            finish();
        }
        readfromIntent(getIntent());
        pendingIntent = PendingIntent.getActivity(this,0,new Intent(this,getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),0);
        IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        writingTagFilters = new IntentFilter[] { tagDetected };
    }

    private void readfromIntent(Intent intent) {
        String action = intent.getAction();
        if(NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)
        || NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)
        || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)){
            Parcelable[] redMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            NdefMessage[] msgs = null;
            if(redMsgs != null){
                msgs = new NdefMessage[redMsgs.length];
                for(int i = 0 ;i < redMsgs .length;i++){
                    msgs[i] = (NdefMessage) redMsgs[i];
                }

            }
            buildTagViews(msgs);

        }
    }

    private void buildTagViews(NdefMessage[] msgs) {
        if (msgs == null || msgs.length==0) return;
        String text = "";
        byte[] payload = msgs[0].getRecords()[0].getPayload();
        String textEncoding = ((payload[0]& 128)==0)?"UTF-8" : "UTF-16";
        int languageCodeLength = payload[0] & 0063;
        try {
            text = new String(payload, languageCodeLength+1,payload.length-languageCodeLength-1,textEncoding);

        } catch (UnsupportedEncodingException e){
            Log.e("UnsupportedEncoding", e.toString());
        }
        BreakIterator nfc_contents = null;
        nfc_contents.setText("NFC Content:"+text);
    }
    private  void write(String text, Tag tag) throws IOException, FormatException {
        NdefRecord[] records = { createRecord(text) };
        NdefMessage message = new NdefMessage(records);
        Ndef ndef = Ndef.get(tag);
        ndef.connect();
        ndef.writeNdefMessage(message);
        ndef.close();
    }

    private NdefRecord createRecord(String text) throws UnsupportedEncodingException {
        String lang = "en";
        byte[] textBytes = text.getBytes();
        byte[] langBytes = lang.getBytes("US-ASCII");
        int langLength = langBytes.length;
        int textLength = textBytes.length;
        byte[] payload = new byte[1 + langLength + textLength];
         payload[0] = (byte) langLength;

         System.arraycopy(langBytes,0,payload,1, langLength);


        NdefRecord recordNFC = new NdefRecord(NdefRecord.TNF_WELL_KNOWN,NdefRecord.RTD_TEXT, new byte[0], payload);
        return recordNFC;

    }

    protected void onNewIntent(Intent intent){
        super.onNewIntent(intent);
        setIntent(intent);
        readfromIntent(intent);
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())){
            myTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

        }

    }


    @Override
    protected void onPause() {
        super.onPause();
        writeModeOff();
    }

    private void writeModeOff() {
        writeMode = true;
        nfcAdapter.enableForegroundDispatch(this,pendingIntent,writingTagFilters,null);
    }

    @Override
    protected void onResume() {
        super.onResume();
        writeModeOn();
    }

    private void writeModeOn() {
        writeMode = false;
        nfcAdapter.disableForegroundDispatch(this);
    }
}