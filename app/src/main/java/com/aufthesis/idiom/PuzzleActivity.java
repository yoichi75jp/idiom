package com.aufthesis.idiom;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.res.ResourcesCompat;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.MobileAds;
import com.google.firebase.analytics.FirebaseAnalytics;

import org.json.JSONArray;
import org.w3c.dom.Text;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

//import android.support.v7.app.AppCompatActivity;

// Created by yoichi75jp2 on 2017/03/04.
public class PuzzleActivity extends Activity implements View.OnClickListener {

    private Context m_context = null;

    private Map<Integer, Button> m_mapButton = new ConcurrentHashMap<>();
    private List<Integer> m_listID = new ArrayList<>();
    //private List<Pair<String,String>> m_listIdiom = new ArrayList<>();
    private List<Map<String,String>> m_listQuestion = new ArrayList<>();
    private List<Map<String,String>> m_listPreQuestion = new ArrayList<>();
//    private List<Button> m_listClickButton = new ArrayList<>();
//    private List<Button> m_listAnswerButton = new ArrayList<>();
    private List<TextView> m_listIdiomText = new ArrayList<>();

    private ArrayList<String> m_answeredList = new ArrayList<>();

//    private CheckBox m_checkHint;
    private TextView m_record;

    private Button m_charAns1;
    private Button m_charAns2;
    private Button m_charAns3;
    private Button m_charAns4;
    private Button m_charAns5;
    private Button m_charAns6;
    private Button m_charAns7;

    final private int m_tolerance = 50;

    private AdView m_adView;
//    private AdView m_adView2;
    private static InterstitialAd m_InterstitialAd;

    private class Idiom
    {
        String m_idiom;
        String m_char1;
        String m_char2;
        int m_level;
        int m_category;

        Idiom(String idiom, String char1, String char2, int level, int category)
        {
            m_idiom = idiom;
            m_char1 = char1;
            m_char2 = char2;
            m_level = level;
            m_category = category;
        }
        public boolean equals(String obj){
            return this.m_idiom.equals(obj);
        }
    }
    private List<Idiom> m_listIdiom = new ArrayList<>();
    //private List<Idiom> m_listLowLevelIdiom = new ArrayList<>();
    private int m_sizeLevel1 = 0;
    private int m_sizeLevel2 = 0;

    private int m_ansId1 = 0;
    private int m_ansId2 = 0;

    private String m_mode;

    //private Context m_context;
    //private DBOpenHelper m_DbHelper;
    private SQLiteDatabase m_db;

    private int m_correctCount;
    private int m_preCorrectCount;
    private int m_incorrectCount;

    private Drawable m_originalDrawable;

    //private final int m_defaultColor = Color.parseColor("#bcffff"); // LightCyan
    private final int m_defaultColor = Color.parseColor("#E0E0E0"); // Gray
    private final int m_onCorrectColor = Color.parseColor("#008000"); // green
    private final int m_onIncorrectColor = Color.parseColor("#dc143c"); // crimson
    //private final int m_correctColor = Color.parseColor("#00FF00"); // Lime

    // 効果音用
    final int SOUND_POOL_MAX = 6;
    private SoundPool m_soundPool;
    private int m_correctSound;
    private int m_incorrectSound;
    private int m_clearSoundID;
    private int m_levelUpID;
    private float m_volume;

    private SharedPreferences m_prefs;
    private DateFormat m_format;

    private boolean m_lookedAnswer = false;

    //private FirebaseAnalytics m_FirebaseAnalytics;
    private List<Integer> m_listNum = new ArrayList<>(Arrays.asList(1,2,3));

    private Integer m_twitterDisplayScore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_puzzle);

        m_context = this;

        // Obtain the FirebaseAnalytics instance.
        //m_FirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        Bundle fireLogBundle = new Bundle();
        fireLogBundle.putString("TEST", "MyApp MainActivity.onCreate() is called.");
        MyApp.getFirebaseAnalytics().logEvent(FirebaseAnalytics.Event.APP_OPEN, fireLogBundle);

        m_prefs = getSharedPreferences(getPackageName(), Context.MODE_PRIVATE);
        m_format = android.text.format.DateFormat.getDateFormat(getApplicationContext());
        m_volume = m_prefs.getInt(getString(R.string.seek_volume), 100)/100.f;
        m_mode = m_prefs.getString(getString(R.string.level), getString(R.string.level_normal));

        // スマートフォンの液晶のサイズを取得を開始
        // ウィンドウマネージャのインスタンス取得
        WindowManager wm = (WindowManager)getSystemService(Context.WINDOW_SERVICE);
        // ディスプレイのインスタンス生成
        Display disp = wm.getDefaultDisplay();
        // スマートフォンの画面のサイズ
        Point point = new Point();
        disp.getSize(point);
        //int swsize = point.x;

//        int textSize1 = 30;
//        int textSize2 = 13;
//        int textSize3 = 45;
        int textSize1 = 29;
        int textSize2 = 12;
        int textSize3 = 41;
        int textSize4 = 15;
        if(this.getResources().getConfiguration().smallestScreenWidthDp >= 600)
        {
            //Tabletの場合
            textSize1 = 63;
            textSize2 = 18;
            textSize3 = 85;
            textSize4 = 20;
        }

        //フォントを変える場合
        //Typeface typefaceOriginal = Typeface.createFromAsset(getAssets(), "fonts/hkgyoprokk.ttf");

        m_listID.clear();
        m_listID.add(R.id.ans1);
        m_listID.add(R.id.ans2);
        m_listID.add(R.id.ans3);
        m_listID.add(R.id.ans4);
        m_listID.add(R.id.ans5);
        m_listID.add(R.id.ans6);
        m_listID.add(R.id.ans7);
        m_listID.add(R.id.clear);
        m_listID.add(R.id.renew);
        m_listID.add(R.id.put_back);
        m_listID.add(R.id.answer_btn);
        m_listID.add(R.id.look_answer_btn);
        for(int i = 0; i < m_listID.size(); i++)
        {
            Button button = findViewById(m_listID.get(i));
            button.setOnClickListener(this);
            if(i < 7)
            {
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
//                    button.setAutoSizeTextTypeWithDefaults(TextView.AUTO_SIZE_TEXT_TYPE_UNIFORM);
//                else
                    button.setTextSize(textSize1);
            }
            else
                button.setTextSize(textSize4);
//            button.setOnLongClickListener(new View.OnLongClickListener() {
//                @Override
//                public boolean onLongClick(View v) {
//                    Button charaButton = (Button)v;
//                    String kanji = charaButton.getText().toString();
//                    ClipboardManager clipboard = (ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
//                    // Creates a new text clip to put on the clipboard
//                    ClipData clip = ClipData.newPlainText("idiom", kanji);
//                    clipboard.setPrimaryClip(clip);
//
//                    Toast toast = Toast.makeText(m_context, getString(R.string.copied, kanji),Toast.LENGTH_LONG);
//                    toast.setGravity(Gravity.CENTER, 0, 0);
//                    toast.show();
//                    return true;
//                }
//            });
            m_mapButton.put(m_listID.get(i),button);
        }
        Button putBackButton = m_mapButton.get(R.id.put_back);
        putBackButton.setEnabled(false);

        TextView instruction1 = findViewById(R.id.instruction1);
        m_originalDrawable = instruction1.getBackground();
        m_record = findViewById(R.id.record);

        instruction1.setTextSize(textSize2);
        m_record.setTextSize(textSize2);

        m_charAns1 = findViewById(R.id.ans1);
        m_charAns2 = findViewById(R.id.ans2);
        m_charAns3 = findViewById(R.id.ans3);
        m_charAns4 = findViewById(R.id.ans4);
        m_charAns5 = findViewById(R.id.ans5);
        m_charAns6 = findViewById(R.id.ans6);
        m_charAns7 = findViewById(R.id.ans7);

        m_charAns1.setTextSize(textSize3);
        m_charAns2.setTextSize(textSize3);
        m_charAns3.setTextSize(textSize3);
        m_charAns4.setTextSize(textSize3);
        m_charAns5.setTextSize(textSize3);
        m_charAns6.setTextSize(textSize3);
        m_charAns7.setTextSize(textSize3);

        m_listIdiomText.add((TextView)findViewById(R.id.idiom1));
        m_listIdiomText.add((TextView)findViewById(R.id.idiom2));
        m_listIdiomText.add((TextView)findViewById(R.id.idiom3));
        m_listIdiomText.add((TextView)findViewById(R.id.idiom4));
        m_listIdiomText.add((TextView)findViewById(R.id.idiom5));
        m_listIdiomText.add((TextView)findViewById(R.id.idiom6));
        m_listIdiomText.add((TextView)findViewById(R.id.idiom7));
        m_listIdiomText.add((TextView)findViewById(R.id.idiom8));
        m_listIdiomText.add((TextView)findViewById(R.id.idiom9));
        m_listIdiomText.add((TextView)findViewById(R.id.idiom10));
        m_listIdiomText.add((TextView)findViewById(R.id.idiom11));
        m_listIdiomText.add((TextView)findViewById(R.id.idiom12));
        m_listIdiomText.add((TextView)findViewById(R.id.idiom13));
        m_listIdiomText.add((TextView)findViewById(R.id.idiom14));
        m_listIdiomText.add((TextView)findViewById(R.id.idiom15));
        m_listIdiomText.add((TextView)findViewById(R.id.idiom16));
        m_listIdiomText.add((TextView)findViewById(R.id.idiom17));
        m_listIdiomText.add((TextView)findViewById(R.id.idiom18));
        m_listIdiomText.add((TextView)findViewById(R.id.idiom19));
        m_listIdiomText.add((TextView)findViewById(R.id.idiom20));
        for(int i = 0; i < m_listIdiomText.size(); i++)
        {
            m_listIdiomText.get(i).setTextSize(textSize3);
        }

        DBOpenHelper dbHelper = new DBOpenHelper(this);
        m_db = dbHelper.getDataBase();

        this.setCharacterSet(true);

        // Initialize the Mobile Ads SDK.
        MobileAds.initialize(this, "ca-app-pub-1485554329820885~8148693159");

        //バナー広告
        m_adView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        if(!MainActivity.g_isDebug)
            m_adView.loadAd(adRequest);

        // AdMobインターステイシャル
        m_InterstitialAd = new InterstitialAd(this);
        m_InterstitialAd.setAdUnitId(getString(R.string.adUnitInterId));
        m_InterstitialAd.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                if (m_lookedAnswer && m_InterstitialAd.isLoaded()) {
                    m_InterstitialAd.show();
                }
            }
        });
    }
    //Button押下時処理
    public void onClick(View view)
    {
        Intent intent;
        TextView answerText;
        String c;
        int id = view.getId();
        final Button button = m_mapButton.get(id);
        switch(id)
        {
            case R.id.ans1:
            case R.id.ans2:
            case R.id.ans3:
            case R.id.ans4:
            case R.id.ans5:
            case R.id.ans6:
            case R.id.ans7:
                answerText = findViewById(m_ansId1);
                c = answerText.getText().toString();
                if(c.equals(getString(R.string.blank)))
                    answerText.setText(button.getText());
                else if(m_ansId2 != 0)
                {
                    answerText = findViewById(m_ansId2);
                    c = answerText.getText().toString();
                    if(c.equals(getString(R.string.blank)))
                        answerText.setText(button.getText());
                }
                break;

            case R.id.renew:
                for(int i = 0; i < m_listIdiomText.size(); i++)
                {
                    m_listIdiomText.get(i).setText(getString(R.string.blank));
                    m_listIdiomText.get(i).setBackground(m_originalDrawable);
                }
                m_charAns7.setText(getString(R.string.blank));
                m_charAns6.setText(getString(R.string.blank));
                m_charAns5.setText(getString(R.string.blank));
                m_charAns4.setText(getString(R.string.blank));
                m_charAns3.setText(getString(R.string.blank));
                m_charAns2.setText(getString(R.string.blank));
                m_charAns1.setText(getString(R.string.blank));
                intent = new Intent(this, DummyActivity.class);
                startActivityForResult(intent, 1);
                //this.setCharacterSet();
                //add 2018/01/08
                m_preCorrectCount = m_correctCount;
                m_listPreQuestion.clear();
                m_listPreQuestion.addAll(m_listQuestion);
                Button putBackButton = m_mapButton.get(R.id.put_back);
                putBackButton.setEnabled(true);
                break;
            case R.id.put_back:
                new AlertDialog.Builder(this)
                        .setTitle(getString(R.string.back_to_question_title))
                        .setMessage(getString(R.string.back_to_question_message))
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            //m_listPreQuestionを表示
                            int preCount = 0;
                            if(m_preCorrectCount > 0)
                            {
                                for(int i = 0; i < m_listPreQuestion.size(); i++)
                                {
                                    String idiom = m_listPreQuestion.get(i).get("idiom");
                                    for(int j = m_answeredList.size() - 1; j >= 0; j--)
                                    {
                                        if(m_answeredList.get(j).equals(idiom))
                                        {
                                            m_answeredList.remove(j);
                                            preCount++;
                                            break;
                                        }
                                    }
                                    if(preCount == m_preCorrectCount) break;
                                }
                            }
                            saveList(getString(R.string.answered_list), m_answeredList);
                            m_record.setText(getString(R.string.record, m_answeredList.size()));

                            setCharacterSet(false);
                            button.setEnabled(false);
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
                break;
            case R.id.clear:
                if(m_correctCount == 0)
                {
                    if(m_ansId1 != 0)
                    {
                        answerText = findViewById(m_ansId1);
                        answerText.setText(getString(R.string.blank));
                    }
                    if(m_ansId2 != 0)
                    {
                        answerText = findViewById(m_ansId2);
                        answerText.setText(getString(R.string.blank));
                    }
                }
                break;
            case R.id.answer_btn:
                if(m_correctCount != 0)
                    this.showAnswer();
                else {
                    String idiom;
                    StringBuilder stringBuilder = new StringBuilder();
                    for(int i = 0; i < m_listIdiomText.size(); i++)
                    {
                        c = m_listIdiomText.get(i).getText().toString();
                        if(!c.equals(getString(R.string.blank)))
                            stringBuilder.append(c);
                    }
                    idiom = stringBuilder.toString();
                    if (m_listQuestion.get(0).containsValue(idiom)) {
                        if(!m_lookedAnswer && m_incorrectCount <= 3) {
                            m_answeredList.add(idiom);
                            saveList(getString(R.string.answered_list), m_answeredList);
                        }
                        m_record.setText(getString(R.string.record, m_answeredList.size()));
                        m_correctCount++;

                        if (DashboardActivity.m_listMax.indexOf(m_answeredList.size()) < 0)
                            m_soundPool.play(m_correctSound, m_volume, m_volume, 0, 0, 1.0F);
                        else {
                            m_soundPool.play(m_levelUpID, m_volume, m_volume, 0, 0, 1.0F);
                            LayoutInflater inflater = getLayoutInflater();
                            view = inflater.inflate(R.layout.toast_layout, null);
                            TextView text = view.findViewById(R.id.toast_text);
                            text.setText(getString(R.string.goal_achievement));
                            text.setGravity(Gravity.CENTER);
                            Toast toast = Toast.makeText(this, getString(R.string.goal_achievement), Toast.LENGTH_LONG);
                            toast.setView(view);
                            toast.setGravity(Gravity.CENTER, 0, 0);
                            toast.show();
                            this.induceReview();
                        }
                        // 正解時のテキストの色変更
                        for(int i = 0; i < m_listIdiomText.size(); i++)
                        {
                            c = m_listIdiomText.get(i).getText().toString();
                            if(!c.equals(getString(R.string.blank)))
                                m_listIdiomText.get(i).setTextColor(m_onCorrectColor);
                        }
                        m_incorrectCount = 0;
                    } else {
                        m_soundPool.play(m_incorrectSound, m_volume, m_volume, 0, 0, 1.0F);
                        // 答えを消す
                        if(m_ansId1 != 0)
                        {
                            answerText = findViewById(m_ansId1);
                            answerText.setText(getString(R.string.blank));
                        }
                        if(m_ansId2 != 0)
                        {
                            answerText = findViewById(m_ansId2);
                            answerText.setText(getString(R.string.blank));
                        }
                        m_incorrectCount++;
                        if(m_incorrectCount == 3)
                        {
                            //3回以上間違えた場合の対応
                            Toast toast = Toast.makeText(this, getString(R.string.msg_3times_incorrect), Toast.LENGTH_LONG);
                            toast.setGravity(Gravity.CENTER, 0, 0);
                            toast.show();
                        }
                    }
                    if (m_correctCount != 0) {
                        //m_soundPool.play(m_clearSoundID, m_volume, m_volume, 0, 0, 1.0F);
                        button.setBackgroundResource(R.drawable.circle2);
                        button.setText(getString(R.string.look_answer));
                        Button look_answer_btn = m_mapButton.get(R.id.look_answer_btn);
                        look_answer_btn.setEnabled(false);

                        if(!MainActivity.g_doneReview)
                        {
                            int count = m_prefs.getInt(getString(R.string.count_induce), 0);
                            count++;
                            SharedPreferences.Editor editor = m_prefs.edit();
                            editor.putInt(getString(R.string.count_induce), count);
                            editor.apply();
                        }
                    }
                }
                break;
            case R.id.look_answer_btn:
                new AlertDialog.Builder(this)
                        .setTitle(R.string.confirm)
                        .setMessage(R.string.confirm_message1)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                showAnswer();
                                m_lookedAnswer = true;
                                //Intent intent = new Intent(m_context, DummyActivity.class);
                                //startActivityForResult(intent, 1);
                                int count = m_prefs.getInt(getString(R.string.look_count), 0);
                                count++;
                                if(count >= 4)
                                {
                                    Collections.shuffle(m_listNum);
                                    if(m_listNum.get(0) == 1) {
                                        count = 0;
                                        m_InterstitialAd.loadAd(new AdRequest.Builder().build());
                                    }
                                }
                                SharedPreferences.Editor editor = m_prefs.edit();
                                editor.putInt(getString(R.string.look_count), count);
                                editor.apply();

                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
                break;
            case R.id.twitter:
                String shareMessage = Uri.encode(getString(R.string.share_message, m_twitterDisplayScore.toString()));
                String url = getString(R.string.twitter_url, shareMessage);
                Intent twitterIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(twitterIntent);
                break;
            default:break;
        }
    }

    // 答えを見せてWeb検索もできる
    @SuppressWarnings("unchecked")
    private void showAnswer()
    {
        String idiom = m_listQuestion.get(0).get("idiom");
        Intent intent = new Intent(m_context, WebBrowserActivity.class);
        intent.putExtra(SearchManager.QUERY, getString(R.string.search_word, idiom));
        startActivity(intent);
    }

    // 慣用句をバラして設定する
    private void setCharacterSet(Boolean isNormalQuestion) {
        //既に本日解答した熟語を取得する

        boolean isJP = Locale.getDefault().toString().equals(Locale.JAPAN.toString());
//        m_checkHint.setEnabled(true);
//        m_checkHint.setChecked(false);

        m_lookedAnswer = false;
        m_answeredList = loadList(getString(R.string.answered_list));
        int lastSize = m_answeredList.size();
        if(lastSize == 0)
            MainActivity.g_isInduceReviewTarget = false;
        String saveDay = m_prefs.getString(getString(R.string.save_day), "");
        //saveDay = "2016/11/06";
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy", Locale.US);
        if(isJP)
            sdf = new SimpleDateFormat("yyyy/MM/dd", Locale.JAPAN);

        Date formatSaveDate = new Date();
        try {
            // 文字列→Date型変換
            if (saveDay.equals("")) saveDay = m_format.format(new Date());
            formatSaveDate = sdf.parse(saveDay);
        } catch (ParseException exp) {
            Toast toast = Toast.makeText(this, exp.getMessage(), Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
        }
        //int diffDay = differenceDays(new Date(), formatSaveDate);

        Button look_answer_btn = m_mapButton.get(R.id.look_answer_btn);
        look_answer_btn.setEnabled(true);

        Calendar calendar = Calendar.getInstance();
        //int date = calender.get(Calendar.DATE);
        int thisMonth = calendar.get(Calendar.MONTH);
        calendar.setTime(formatSaveDate);
        int saveMonth = calendar.get(Calendar.MONTH);

        if (lastSize > 0 && thisMonth != saveMonth)
        {
            int maxScore = m_prefs.getInt(getString(R.string.max_score), 0);
            showMaxScoreMessage(maxScore, lastSize);
            if(maxScore < lastSize)
            {
                SharedPreferences.Editor editor = m_prefs.edit();
                editor.putInt(getString(R.string.max_score), lastSize);
                editor.apply();
            }
            m_answeredList.clear();
            saveList(getString(R.string.answered_list), m_answeredList);
        }
        m_record.setText(getString(R.string.record, m_answeredList.size()));
        m_charAns7.setText(getString(R.string.blank));
        m_charAns6.setText(getString(R.string.blank));
        m_charAns5.setText(getString(R.string.blank));
        m_charAns4.setText(getString(R.string.blank));
        m_charAns3.setText(getString(R.string.blank));
        m_charAns2.setText(getString(R.string.blank));
        m_charAns1.setText(getString(R.string.blank));

        m_correctCount = 0;
        m_incorrectCount = 0;
        m_listQuestion.clear();
        for(int i = 0; i < m_listIdiomText.size(); i++)
        {
            m_listIdiomText.get(i).setText(getText(R.string.blank));
        }
        if(m_listIdiom.size() <= 0)
        {
            try
            {
                String sql  = "select idiom, char1, char2, level, category " + " from idioms";
                Cursor cursor = m_db.rawQuery(sql, null);
                cursor.moveToFirst();
                if (cursor.getCount() != 0) {

                    for (int i = 0; i < cursor.getCount(); i++) {
                        String idiom = (cursor.getString(0));
                        String char1 = (cursor.getString(1));
                        String char2 = (cursor.getString(2));
                        int level = 0;
                        if(!cursor.isNull(3)) {
                            level = (cursor.getInt(3));
                            if(level == 1) m_sizeLevel1++;
                            if(level == 2) m_sizeLevel2++;
                        }
                        int category = cursor.getInt(4);
                        //Pair<String,String> keyValue = new Pair<>(idiom, read);
                        //m_listIdiom.add(keyValue);
                        m_listIdiom.add(new Idiom(idiom, char1, char2, level, category));
                        //if(level == 1 || level == 2)
                        //    m_listLowLevelIdiom.add(new Idiom(idiom, read, level));
                        cursor.moveToNext();
                    }
                }
                cursor.close();
            }
            catch(Exception exp)
            {
                Toast toast = Toast.makeText(this, String.valueOf(exp.getMessage()),Toast.LENGTH_LONG);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
            }
        }
        if(m_listIdiom.size() > 0 && isNormalQuestion)
        {
            Collections.shuffle(m_listIdiom);
            List<String> listCharacter = new ArrayList<>();
            int thisTimeCategory = m_listIdiom.get(0).m_category;
            String idiom = "";
            String char1 = "";
            String char2 = "";
            for(int i = 0; i < m_listIdiom.size(); i++)
            {
                idiom = m_listIdiom.get(i).m_idiom;
                char1 = m_listIdiom.get(i).m_char1;
                char2 = m_listIdiom.get(i).m_char2;
                int level = m_listIdiom.get(i).m_level;
                int category = m_listIdiom.get(i).m_category;
                if(idiom.length() > 20) continue;
                if(thisTimeCategory != category) continue;
                if(m_answeredList.indexOf(idiom) >= 0) continue;  // 既に解答した熟語を省く
                if(m_mode.equals(getString(R.string.level_normal)))
                {
                    if(m_sizeLevel1 + m_sizeLevel2 - m_tolerance > m_answeredList.size() && level == 0 || level >= 3) continue;
                    if(m_sizeLevel1 - m_tolerance > m_answeredList.size() && level == 2) continue;
                }
                else if (level == 1) continue;

                if(listCharacter.indexOf(char1) >= 0 || listCharacter.indexOf(char2) >= 0) continue;

                listCharacter.add(char1);
                if(!char2.equals("")) listCharacter.add(char2);

                Map<String,String> mapQuestion = new HashMap<>();
                mapQuestion.put("idiom",idiom);
                m_listQuestion.add(mapQuestion);
                break;
            }
            if(m_listQuestion.size() == 0)
            {
                //TODO:すべての問題を解いてしまった場合の処理
            }

            // 答えの選択肢を抽出
            for(int i = 0; i < m_listIdiom.size(); i++)
            {
                String tmp1 = m_listIdiom.get(i).m_char1;
                String tmp2 = m_listIdiom.get(i).m_char2;

                if(m_listIdiom.get(i).m_category != thisTimeCategory) continue;
                if(listCharacter.indexOf(tmp1) >= 0 || listCharacter.indexOf(tmp2) >= 0) continue;
                if(!char2.equals(""))
                {
                    if(tmp2.equals(""))
                        continue;
                }
                boolean isSame = false;
                String tmpIdiom2 = idiom.replace(char1.charAt(0), tmp1.charAt(0));
                String tmpIdiom3;
                for(int j = 0; j < m_listIdiom.size(); j++)
                {
                    tmpIdiom3 = m_listIdiom.get(j).m_idiom;
                    if(tmpIdiom2.equals(tmpIdiom3) && !tmpIdiom3.equals(idiom))
                    {
                        isSame = true;
                        break;
                    }
                }
                if(isSame)
                    continue;

                listCharacter.add(tmp1);
                if(listCharacter.size() >= 7) break;
                if(!tmp2.equals("")) listCharacter.add(tmp2);
                if(listCharacter.size() >= 7) break;
            }

            Collections.shuffle(listCharacter);
            for(int i = 0; i < 7; i++)
            {
                Button button = m_mapButton.get(m_listID.get(i));
                button.setText(listCharacter.get(i));
            }
            Button button = m_mapButton.get(R.id.answer_btn);
            button.setText(getString(R.string.to_answer));
            button.setBackgroundResource(R.drawable.circle);

            //慣用句Editorを作成する
            m_ansId1 = 0;
            m_ansId2 = 0;
            int length = idiom.length();
            int start = 0;
            if(length <= 15) start = 5;
            for(int i = 0; i < length; i++)
            {
                TextView text = m_listIdiomText.get(i+start);
                String c = idiom.substring(i, i+1);
                if(c.equals(char1) || c.equals(char2))
                {
                    text.setText(getString(R.string.blank));
                    text.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.text_border, null));
                    if(m_ansId1 == 0)
                        m_ansId1 = text.getId();
                    else
                        m_ansId2 = text.getId();
                }
                else
                {
                    text.setText(c);
                    text.setBackground(m_originalDrawable);
                }
                // テキストの色設定
                text.setTextColor(Color.BLACK);
            }
        }
        //前の画面に戻る 2018/01/30
        else if(m_listPreQuestion.size() > 0 && !isNormalQuestion)
        {
            List<String> listCharacter = new ArrayList<>();
            String idiom = m_listPreQuestion.get(0).get("idiom");
            //String read = m_listPreQuestion.get(i).get("read");
            Map<String,String> mapQuestion = new HashMap<>();
            mapQuestion.put("idiom",idiom);
            m_listQuestion.add(mapQuestion);

            int thisTimeCategory = 0;
            String char1 = "";
            String char2 = "";
            for(int i = 0; i < m_listIdiom.size(); i++)
            {
                if(!m_listIdiom.get(i).m_idiom.equals(idiom))
                    continue;

                char1 = m_listIdiom.get(i).m_char1;
                char2 = m_listIdiom.get(i).m_char2;
                thisTimeCategory = m_listIdiom.get(i).m_category;
                listCharacter.add(char1);
                if(!char2.equals("")) listCharacter.add(char2);
                break;
            }
            // 答えの選択肢を抽出
            for(int i = 0; i < m_listIdiom.size(); i++)
            {
                String tmp1 = m_listIdiom.get(i).m_char1;
                String tmp2 = m_listIdiom.get(i).m_char2;

                if(m_listIdiom.get(i).m_category != thisTimeCategory) continue;
                if(listCharacter.indexOf(tmp1) >= 0 || listCharacter.indexOf(tmp2) >= 0) continue;
                if(!char2.equals(""))
                {
                    if(tmp2.equals(""))
                        continue;
                }
                boolean isSame = false;
                String tmpIdiom2 = idiom.replace(char1.charAt(0), tmp1.charAt(0));
                String tmpIdiom3;
                for(int j = 0; j < m_listIdiom.size(); j++)
                {
                    tmpIdiom3 = m_listIdiom.get(j).m_idiom;
                    if(tmpIdiom2.equals(tmpIdiom3) && !tmpIdiom3.equals(idiom))
                    {
                        isSame = true;
                        break;
                    }
                }
                if(isSame)
                    continue;

                listCharacter.add(tmp1);
                if(listCharacter.size() >= 7) break;
                if(!tmp2.equals("")) listCharacter.add(tmp2);
                if(listCharacter.size() >= 7) break;
            }

            Collections.shuffle(listCharacter);
            for(int i = 0; i < 7; i++)
            {
                Button button = m_mapButton.get(m_listID.get(i));
                button.setText(listCharacter.get(i));
            }
            Button button = m_mapButton.get(R.id.answer_btn);
            button.setText(getString(R.string.to_answer));
            button.setBackgroundResource(R.drawable.circle);

            //慣用句Editorを作成する
            m_ansId1 = 0;
            m_ansId2 = 0;
            int length = idiom.length();
            int start = 0;
            if(length <= 15) start = 5;
            for(int i = 0; i < length; i++)
            {
                TextView text = m_listIdiomText.get(i+start);
                String c = idiom.substring(i, i+1);
                if(c.equals(char1) || c.equals(char2))
                {
                    text.setText(getString(R.string.blank));
                    text.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.text_border, null));
                    if(m_ansId1 == 0)
                        m_ansId1 = text.getId();
                    else
                        m_ansId2 = text.getId();
                }
                else
                {
                    text.setText(c);
                    text.setBackground(m_originalDrawable);
                }
                // テキストの色設定
                text.setTextColor(Color.BLACK);
            }
        }
    }

/*
    //日付の差（日数）を算出する
    public int differenceDays(Date date1, Date date2) {
        long datetime1 = date1.getTime();
        long datetime2 = date2.getTime();
        long one_date_time = 1000 * 60 * 60 * 24;
        long diffDays = (datetime1 - datetime2) / one_date_time;
        return (int)diffDays;
    }
*/
    //１週間のスコアがこれまで最高スコアであるときに表示する
    public void showMaxScoreMessage(int score1, int score2)
    {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle(getString(R.string.next_title));

        LayoutInflater inflater = getLayoutInflater();
        View view = inflater.inflate(R.layout.max_score_layout, null);
        TextView highScoreText = view.findViewById(R.id.high_score);
        TextView lastWeeksScoreText = view.findViewById(R.id.last_week_score);
        TextView messageText = view.findViewById(R.id.message);

        highScoreText.setText(String.valueOf(score1));
        lastWeeksScoreText.setText(String.valueOf(score2));

        if(score1 > score2)
        {
            highScoreText.setTextSize(30);
            highScoreText.setTextColor(Color.RED);
            messageText.setText(getString(R.string.next_message1));
            messageText.setTextColor(Color.BLUE);
        }
        else if(score1 <= score2)
        {
            lastWeeksScoreText.setTextSize(30);
            lastWeeksScoreText.setTextColor(Color.RED);
            messageText.setText(getString(R.string.next_message2));
            messageText.setTextColor(Color.RED);
        }
        messageText.setGravity(Gravity.CENTER);

        m_twitterDisplayScore = score2;
        ImageButton shareButton = view.findViewById(R.id.twitter);
        shareButton.setOnClickListener(this);

        dialog.setView(view);
        dialog.setPositiveButton(getString(R.string.next_ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        dialog.show();
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish();
            /*
            AlertDialog.Builder dialog = new AlertDialog.Builder(this);
            dialog.setTitle(getString(R.string.final_title));
            dialog.setMessage(getString(R.string.final_message));
            dialog.setPositiveButton(getString(R.string.final_ok), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                    //moveTaskToBack(true);
                }
            });
            dialog.setNegativeButton(getString(R.string.final_cancel), new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                }
            });
            dialog.show();
            */
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    // 設定値 ArrayList<String> を保存（Context は Activity や Application や Service）
    private void saveList(String key, ArrayList<String> list) {
        JSONArray jsonAry = new JSONArray();
        for(int i = 0; i < list.size(); i++) {
            jsonAry.put(list.get(i));
        }
        SharedPreferences.Editor editor = m_prefs.edit();
        editor.putString(key, jsonAry.toString());
        editor.putString(getString(R.string.save_day), m_format.format(new Date()));
        editor.apply();
    }

    // 設定値 ArrayList<String> を取得（Context は Activity や Application や Service）
    private ArrayList<String> loadList(String key) {
        ArrayList<String> list = new ArrayList<>();
        String strJson = m_prefs.getString(key, ""); // 第２引数はkeyが存在しない時に返す初期値
        if(!strJson.equals("")) {
            try {
                JSONArray jsonAry = new JSONArray(strJson);
                for(int i = 0; i < jsonAry.length(); i++) {
                    list.add(jsonAry.getString(i));
                }
            } catch(Exception ex) {
                ex.printStackTrace();
            }
        }
        return list;
    }

    @Override
    public void onResume()
    {
        super.onResume();
        // 予め音声データを読み込む
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
        {
            m_soundPool = new SoundPool(SOUND_POOL_MAX, AudioManager.STREAM_MUSIC, 0);
        }
        else
        {
            AudioAttributes attr = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build();
            m_soundPool = new SoundPool.Builder()
                    .setAudioAttributes(attr)
                    .setMaxStreams(SOUND_POOL_MAX)
                    .build();
        }
        m_correctSound = m_soundPool.load(getApplicationContext(), R.raw.correct2, 1);
        m_incorrectSound = m_soundPool.load(getApplicationContext(), R.raw.incorrect1, 1);
        m_clearSoundID = m_soundPool.load(getApplicationContext(), R.raw.cheer, 1);
        m_levelUpID = m_soundPool.load(getApplicationContext(), R.raw.ji_023, 1);
        if (m_adView != null) {
            m_adView.resume();
        }
//        if (m_adView2 != null) {
//            m_adView2.resume();
//        }
    }

    @Override
    public void onPause() {
        if (m_adView != null) {
            m_adView.pause();
        }
//        if (m_adView2 != null) {
//            m_adView2.pause();
//        }
        super.onPause();
        m_soundPool.release();
    }
    @Override
    protected void onRestart() {
        super.onRestart();
    }

    @Override
    public void onDestroy()
    {
        if (m_adView != null) {
            m_adView.destroy();
        }
//        if (m_adView2 != null) {
//            m_adView2.destroy();
//        }
        super.onDestroy();
        setResult(RESULT_OK);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_play, menu);
        //m_menu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();
        if(id == R.id.restore)
        {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.confirm)
                    .setMessage(getString(R.string.confirm_message2))
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            int lastSize = m_answeredList.size();
                            if (lastSize > 0)
                            {
                                int maxScore = m_prefs.getInt(getString(R.string.max_score), 0);
                                showMaxScoreMessage(maxScore, lastSize);
                                if(maxScore < lastSize)
                                {
                                    SharedPreferences.Editor editor = m_prefs.edit();
                                    editor.putInt(getString(R.string.max_score), lastSize);
                                    editor.apply();
                                }
                            }
                            m_answeredList.clear();
                            saveList(getString(R.string.answered_list), m_answeredList);
                            m_record.setText(getString(R.string.record, m_answeredList.size()));
                            Intent intent = new Intent(m_context, DummyActivity.class);
                            startActivityForResult(intent, 1);
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        }
        if(id == R.id.close)
        {
            AlertDialog.Builder dialog = new AlertDialog.Builder(this);
            dialog.setTitle(getString(R.string.final_title));
            dialog.setMessage(getString(R.string.final_message));
            dialog.setPositiveButton(getString(R.string.final_ok), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    //finish();
                    moveTaskToBack(true);
                }
            });
            dialog.setNegativeButton(getString(R.string.final_cancel), new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                }
            });
            dialog.show();
        }
        if(id == R.id.dashboard)
        {
            //既に本日解答した熟語を取得する
            ArrayList<String> answerList = loadList(getString(R.string.answered_list));
            ArrayList<String> readList = new ArrayList<>();
//            for(int i = 0; i < answerList.size(); i++)
//            {
//                for(int j = 0; j < m_listIdiom.size(); j++)
//                {
//                    if(m_listIdiom.get(j).m_idiom.equals(answerList.get(i)))
//                    {
//                        readList.add(m_listIdiom.get(j).m_read);
//                        break;
//                    }
//                }
//            }
            Intent intent = new Intent(this, DashboardActivity.class);
            intent.putStringArrayListExtra("idiom", answerList);
            intent.putStringArrayListExtra("read", readList);
            int requestCode = 1;
            startActivityForResult(intent, requestCode);
            //startActivity(intent);
            // アニメーションの設定
            overridePendingTransition(R.animator.slide_in_right, R.animator.slide_out_left);
            return true;
        }

        if(id == R.id.setting)
        {
            Intent intent = new Intent(this, SettingsActivity.class);
            int requestCode = 2;
            startActivityForResult(intent, requestCode);
            //startActivity(intent);
            // アニメーションの設定
            overridePendingTransition(R.animator.slide_in_right, R.animator.slide_out_left);
            return true;
        }

        if(id == android.R.id.home)
        {
            finish();
            // アニメーションの設定
            overridePendingTransition(R.animator.slide_in_left, R.animator.slide_out_right);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case 1:
                this.setCharacterSet(true);
                break;
            case 2:
                m_volume = m_prefs.getInt(getString(R.string.seek_volume), 100)/100.f;
                break;
            case 3:
                break;
            case 4:
                break;
            default:break;
        }
    }

    private void induceReview()
    {
        if(MainActivity.g_doneReview) return;
        int count = m_prefs.getInt(getString(R.string.count_induce), 0);
        if(count >= 500)
        {
            try
            {
                Thread.sleep(500);
            }
            catch(Exception e){}
            AlertDialog.Builder dialog = new AlertDialog.Builder(this);
            dialog.setTitle(getString(R.string.induce_title));
            dialog.setMessage(getString(R.string.induce_message));
            dialog.setPositiveButton(getString(R.string.induce_ok), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    SharedPreferences.Editor editor = m_prefs.edit();
                    editor.putInt(getString(R.string.count_induce), 0);
                    editor.putBoolean(getString(R.string.review_done), true);
                    editor.apply();
                    Intent googlePlayIntent = new Intent(Intent.ACTION_VIEW);
                    googlePlayIntent.setData(Uri.parse("market://details?id=com.aufthesis.idiom"));
                    startActivity(googlePlayIntent);
                }
            });
            dialog.setNegativeButton(getString(R.string.induce_cancel), new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                }
            });
            dialog.setCancelable(false);
            dialog.show();
        }
    }

}
