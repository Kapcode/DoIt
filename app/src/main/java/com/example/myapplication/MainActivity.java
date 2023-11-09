package com.example.myapplication;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.OptionalInt;
import java.util.Random;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    static final int scoreIncrement = 25;
    final int [] playColors = new int[]{
            Color.rgb(106,0,128),
            Color.rgb(51,0,77),
            Color.rgb(34,0,102),
    Color.rgb(0,84,77),
    Color.rgb(10,64,77),
    Color.rgb(102,0,34),
    Color.rgb(102,34,0),
    Color.rgb(13,77,0)};



    static final int seekWidth = 75;
    static final int switchSeekWidth = seekWidth+20;
    static final int sliderHeight = 50;
    static Activity activity;
    static int play = 0;
    static Handler handler;
    static Button start;
    int uiReactionDelay = 150;
    public static final int TAP=0,SWITCH=1,HOLD=2,BLOCK=3,SLIDE_RIGHT=4,SLIDE_LEFT=5,SLIDE_UP=6,SLIDE_DOWN=7;
    public static ImageView playImageView;
    public static final int ENGLISH = 0;
    public static final int SPANISH = 1;
    public static int language = ENGLISH;
    public static TextToSpeech textToSpeech;
    HashMap <Integer,String[]> instructionTypes = new HashMap<Integer,String[]>();
    Drawable[] drawables,drawables2;
    TextView scoreTV,livesTV;


Toast toaster;
TextView instruction;
 LinearLayout currentPlayLayout;
    int screen_width  = Resources.getSystem().getDisplayMetrics().widthPixels;
    int screen_height = Resources.getSystem().getDisplayMetrics().heightPixels;
    int play_height = 600;
    public SensorManager mSensorManager;
    public Sensor mSensor;
    private InterstitialAd mInterstitialAd;
    @Override
    public void onSensorChanged(SensorEvent event) {
        float maxRange = mSensor.getMaximumRange();
        if(maxRange == event.values[0]) {
        // Do something when something is far away.
        }
        else {// Do something when something is near.
            if(start.getVisibility() == View.GONE){// don't start game or talk with sensor
                if( play==BLOCK){
                    //correct
                    toast(null,"BLOCK");
                    correctAnswer();
                }else{
                    wrongAnswer();
                }
            }


        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_main);
        activity=this;

        //AdMob
        // Initialize the Mobile Ads SDK
        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
                Toast.makeText(MainActivity.this, " successful ", Toast.LENGTH_SHORT).show();
            }
        });
        AdView mAdView;
        mAdView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        InterstitialAd.load(this,"ca-app-pub-3940256099942544/1033173712", adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        // The mInterstitialAd reference will be null until
                        // an ad is loaded.
                        mInterstitialAd = interstitialAd;
                        Toast.makeText(MainActivity.this, " onAdLoaded ", Toast.LENGTH_SHORT).show();

                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        // Handle the error

                        Toast.makeText(MainActivity.this, loadAdError.toString(), Toast.LENGTH_SHORT).show();
                        mInterstitialAd = null;
                    }
                });




        currentPlayLayout = findViewById(R.id.playArea);
        playImageView = (ImageView) findViewById(R.id.playImageView);
        toaster = new Toast(this);
        start = findViewById(R.id.start);
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        scoreTV = (TextView)findViewById(R.id.scoretv) ;
        livesTV = (TextView)findViewById(R.id.livestv);
        handler = new Handler();
        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //don't increment score, just start game.....
                livesTV.setText("2");
                nextPlay();
                start.setVisibility(View.GONE);
            }
        });
        instruction = findViewById(R.id.instruction);
        instructionTypes.put(0,new String[]{"Tap","Ja"});
        instructionTypes.put(1,new String[]{"Switch","Cambiar"});
        instructionTypes.put(2,new String[]{"Hold","Sostener"});
        instructionTypes.put(3,new String[]{"Block","Bloquea"});

        instructionTypes.put(4,new String[]{"Slide Right",""});
        instructionTypes.put(5,new String[]{"Slide Left",""});
        instructionTypes.put(6,new String[]{"Slide Up",""});
        instructionTypes.put(7,new String[]{"Slide Down",""});
        //add drawables in order by index
        drawables= new Drawable[]
                {getResources().getDrawable(R.drawable.tap),//
                        getResources().getDrawable(R.drawable.sswitch),
                        getResources().getDrawable(R.drawable.handshake),
                        getResources().getDrawable(R.drawable.block),
                        getResources().getDrawable(R.drawable.right),//r
                        getResources().getDrawable(R.drawable.left),//l
                        getResources().getDrawable(R.drawable.up),//u
                        getResources().getDrawable(R.drawable.down)};//d

        drawables2= new Drawable[]
                {getResources().getDrawable(R.drawable.tap),
                        getResources().getDrawable(R.drawable.sswitch),
                        getResources().getDrawable(R.drawable.handshake),
                        getResources().getDrawable(R.drawable.block),
                        getResources().getDrawable(R.drawable.right),
                        getResources().getDrawable(R.drawable.left),
                        getResources().getDrawable(R.drawable.actual_up_arrow),
                        getResources().getDrawable(R.drawable.actual_down_arrow)};

        textToSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int i) {

                // if No error is found then only it will run
                if(i!=TextToSpeech.ERROR){
                    // To Choose language of speech
                    textToSpeech.setLanguage(Locale.US);
                }
            }
        });
        removePlays();

        
        

        super.onCreate(savedInstanceState);
    }
    @Override
    protected void onResume() {
        // Register a listener for the sensor.
        super.onResume();
        mSensorManager.registerListener(this, mSensor,
                SensorManager.SENSOR_DELAY_NORMAL);
    }
    public void toast(View v,String s){
        cancel();
        toaster=new Toast(this);
        toaster.setText(s);
        toaster.show();
    }
    public void cancel(){
        toaster.cancel();
    }

    public void removePlays(){
        currentPlayLayout.removeAllViews();
    };


    public void say(String text){
        textToSpeech.speak(text,TextToSpeech.QUEUE_ADD,null);
    }
















    public void addPlayTap(){
        Button button = new Button(this);
        button.setTextSize(16);
        button.setWidth(screen_width/2);
        button.setHeight(play_height);
        button.setBackgroundColor(playColors[TAP]);
        button.setForeground(drawables[TAP]);
        //button.setText("TAP");
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(play==TAP){
                    //correct
                    toast(view,"TAP");
                    correctAnswer();
                }else{
                    wrongAnswer();
                }
            }
        });


        button.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                wrongAnswer();
                return true;
            }
        });

        currentPlayLayout.addView(button);
    }


    public void addHoldit(){
        Button button = new Button(this);
        button.setBackgroundColor(playColors[HOLD]);
        button.setForeground(drawables[HOLD]);
        button.setTextSize(16);
        button.setWidth(screen_width/2);
        button.setHeight(play_height);
        //button.setText("HOLD");
        button.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if(play==HOLD){
                    toast(view,"HOLD");
                    correctAnswer();
                }else{
                    wrongAnswer();
                }
                return true;//Return true or error!
            }
        });
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                wrongAnswer();
            }
        });
        currentPlayLayout.addView(button);
    }


    public void addPlaySlide(int play__direction){

        //set vert or horiz
        //and set the thumb arrow

        SeekBar seekBar = new SeekBar(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            seekBar.setMinHeight(sliderHeight);
            seekBar.setMaxHeight(sliderHeight);

        }
        Drawable progressDrawable = getResources().getDrawable(R.drawable.blue_texture);
        seekBar.setProgressDrawable(progressDrawable);
        Drawable drawable = null;


        // do the same thing, but with freakin rotation now! four times!

        if(play__direction == SLIDE_LEFT){
            //horizontal
            seekBar.setRotation(0);
            drawable= drawables[SLIDE_LEFT];
        }else if(play__direction==SLIDE_RIGHT){
            //horizontal

            seekBar.setRotation(0);
            drawable= drawables[SLIDE_RIGHT];
        }else if(play__direction == SLIDE_UP){
            //vertical
            seekBar.setRotation(270);
            drawable= drawables[SLIDE_UP];
        }else if(play__direction == SLIDE_DOWN){
            //vertical
            seekBar.setRotation(270);
            drawable= drawables[SLIDE_DOWN];
        }

        drawable.setTint(Color.WHITE);
        seekBar.setThumb(drawable);
        seekBar.setMax(100);
        seekBar.setProgress(50);



        RelativeLayout ll = new RelativeLayout(this);

        //seekBar.setBackgroundColor(Color.LTGRAY);
        //ll.setBackgroundColor(Color.BLUE);
        ll.addView(seekBar);
        currentPlayLayout.addView(ll);
        RelativeLayout.LayoutParams seek_lp = (RelativeLayout.LayoutParams) seekBar.getLayoutParams();
        seek_lp.addRule(RelativeLayout.CENTER_IN_PARENT);
        ViewGroup.LayoutParams ll_lp = ll.getLayoutParams();
        ll_lp.width=screen_width/2;
        ll_lp.height=play_height;
        ll.setLayoutParams(ll_lp);

        if(play__direction == SLIDE_LEFT){
            ll.setBackgroundColor(playColors[SLIDE_LEFT]);
            seek_lp.width = screen_width/2;
            seek_lp.height = seekWidth;
            seekBar.setLayoutParams(seek_lp);
            //ll.setPadding(0,play_height/2,0,0);
        }else if(play__direction==SLIDE_RIGHT){
            ll.setBackgroundColor(playColors[SLIDE_RIGHT]);
            seek_lp.width = screen_width/2;
            seek_lp.height = seekWidth;
            seekBar.setLayoutParams(seek_lp);
            //ll.setPadding(0,play_height/2,0,0);
        }else if(play__direction == SLIDE_UP){//rotate
            ll.setBackgroundColor(playColors[SLIDE_UP]);
            seek_lp.width = screen_width/2;
            seek_lp.height = seekWidth;
            //seekBar.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }else if(play__direction == SLIDE_DOWN){//rotate
            ll.setBackgroundColor(playColors[SLIDE_DOWN]);
            seek_lp.width = screen_width/2;
            seek_lp.height = seekWidth;
            //seek_lp =new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

            seekBar.setLayoutParams(seek_lp);
        }





        //currentPlayLayout.addView(seekBar);
        //ViewGroup.LayoutParams layoutParams = seekBar.getLayoutParams();
        //layoutParams.width = screen_width/2;
        //layoutParams.height = play_height;
        //seekBar.setLayoutParams(layoutParams);







        int startedOn = 50;
        final int thisPlay = play__direction;
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                //don't need

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

                if(thisPlay == play &&thisPlay==SLIDE_RIGHT && seekBar.getProgress() > startedOn){
                    toast(seekBar,"slide " + thisPlay);
                    correctAnswer();
                } else if(thisPlay == play &&thisPlay==SLIDE_UP && seekBar.getProgress() > startedOn){
                    toast(seekBar,"slide " + thisPlay);
                    correctAnswer();
                } else if(thisPlay == play &&thisPlay==SLIDE_LEFT && seekBar.getProgress() < startedOn){
                    toast(seekBar,"slide " + thisPlay);
                    correctAnswer();
                } else if(thisPlay == play &&thisPlay==SLIDE_DOWN && seekBar.getProgress() < startedOn){
                    toast(seekBar,"slide " + thisPlay);
                    correctAnswer();
                }else{
                    wrongAnswer();
                }

            }
        });


    }



    public void addPlaySwtich(){
        SeekBar switchSeek = new SeekBar(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            switchSeek.setMinHeight(sliderHeight);
            switchSeek.setMaxHeight(sliderHeight);
        }
        Drawable progressDrawable = getResources().getDrawable(R.drawable.gray_texture);
        switchSeek.setProgressDrawable(progressDrawable);
        Drawable drawable = drawables[SWITCH];
        switchSeek.setThumb(drawable);


        Switch sswitch = new Switch(this);
        //sswitch.setChecked(false);
        sswitch.toggle();

        sswitch.setTextSize(16);
        sswitch.setWidth(screen_width/2);
        sswitch.setHeight(play_height);
        sswitch.setThumbDrawable(drawables[SWITCH]);
        //sswitch.getThumbDrawable().setTint(android.R.color.holo_orange_light);//orange
        //sswitch.setText("Switch");
        sswitch.setSwitchMinWidth((screen_width/2));




        RelativeLayout ll = new RelativeLayout(this);
        ll.setBackgroundColor(playColors[SWITCH]);
        //seekBar.setBackgroundColor(Color.LTGRAY);
        //ll.setBackgroundColor(Color.BLUE);
        ll.addView(switchSeek);
        currentPlayLayout.addView(ll);
        RelativeLayout.LayoutParams seek_lp = (RelativeLayout.LayoutParams) switchSeek.getLayoutParams();
        seek_lp.height = switchSeekWidth;//Make the switch thicker!
        seek_lp.addRule(RelativeLayout.CENTER_IN_PARENT);
        seek_lp.addRule(RelativeLayout.CENTER_VERTICAL);
        seek_lp.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        seek_lp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        ViewGroup.LayoutParams ll_lp = ll.getLayoutParams();
        ll_lp.width=screen_width/2;
        ll_lp.height=play_height;
        ll.setLayoutParams(ll_lp);









        switchSeek.setMax(100);
        switchSeek.setProgress(10);

        int startedOn = 50;



        switchSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                //don't need

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

                if(play==SWITCH && seekBar.getProgress() > startedOn){
                    toast(seekBar,"slide " + play);
                    correctAnswer();
                }else{
                    wrongAnswer();
                }

            }
        });








        //sswitch.setBackgroundColor(Color.GRAY);










    }










    public void correctAnswer(){
        //increment score
        int score = Integer.parseInt(scoreTV.getText().toString());
        score = score + scoreIncrement;
        scoreTV.setText(score+"");
        //next play

        nextPlay();
    }

    public void wrongAnswer(){

        say("Wrong!");

        int score = Integer.parseInt(scoreTV.getText().toString());


        int lives = Integer.parseInt(livesTV.getText().toString());
        lives--;
        livesTV.setText(String.valueOf(lives));
        if(lives==0){
            textToSpeech.speak("Game Over!",TextToSpeech.QUEUE_FLUSH,null);
            //end game
            new AlertDialog.Builder(this)
                    .setTitle("Game Over!")
                    .setMessage(score+"")
                    // Specifying a listener allows you to take an action before dismissing the dialog.
                    // The dialog is automatically dismissed when a dialog button is clicked.
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // Continue with delete operation
                            scoreTV.setText("000");
                            animateRemoveAllViews();
                            start.setVisibility(View.VISIBLE);



                            mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback(){
                                @Override
                                public void onAdClicked() {
                                    // Called when a click is recorded for an ad.
                                    Toast.makeText(MainActivity.this, "Ad was clicked.", Toast.LENGTH_SHORT).show();

                                }

                                @Override
                                public void onAdDismissedFullScreenContent() {
                                    // Called when ad is dismissed.
                                    // Set the ad reference to null so you don't show the ad a second time.
                                    //Log.d(TAG, "Ad dismissed fullscreen content.");
                                    Toast.makeText(MainActivity.this, "Ad dismissed fullscreen content.", Toast.LENGTH_SHORT).show();
                                    mInterstitialAd = null;
                                }

                                @Override
                                public void onAdFailedToShowFullScreenContent(AdError adError) {
                                    // Called when ad fails to show.
                                    //Log.e(TAG, "Ad failed to show fullscreen content.");
                                    Toast.makeText(MainActivity.this, "Ad failed to show fullscreen content.", Toast.LENGTH_SHORT).show();
                                    mInterstitialAd = null;
                                }

                                @Override
                                public void onAdImpression() {
                                    // Called when an impression is recorded for an ad.
                                    //Log.d(TAG, "Ad recorded an impression.");
                                    Toast.makeText(MainActivity.this, "Ad recorded an impression.", Toast.LENGTH_SHORT).show();
                                }

                                @Override
                                public void onAdShowedFullScreenContent() {
                                    // Called when ad is shown.
                                    Toast.makeText(MainActivity.this, "Ad showed fullscreen content.", Toast.LENGTH_SHORT).show();
                                    //Log.d(TAG, "Ad showed fullscreen content.");
                                }
                            });

                            if (mInterstitialAd != null) {
                                mInterstitialAd.show(activity);
                            } else {
                                toast(null,"The interstitial ad wasn't ready yet.");
                            }
                        }
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();


        }else{
            nextPlay();
        }


    }

//...
    public void animateRemoveAllViews(){
        currentPlayLayout.removeAllViews();
        //animateRemoveView(currentPlayLayout.getChildAt(0));
    }

    public void animateRemoveView(View v){
        
        // Create an animation instance
        Animation an = new RotateAnimation(0.0f, 360.0f, 90, 0);

        // Set the animation's parameters
        an.setDuration(100);               // duration in ms
        an.setRepeatCount(0);                // -1 = infinite repeated
        an.setRepeatMode(Animation.REVERSE); // reverses each repeat
        an.setFillAfter(true);               // keep rotation after animation

        // Aply animation to image view
        v.setAnimation(an);
        handler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                say("remove");
                currentPlayLayout.removeView(v);
            }
        });

    }

    public void animateAddView(View v){
        AlphaAnimation animation1 = new AlphaAnimation(0.2f, 1.0f);
        animation1.setDuration(uiReactionDelay);
        animation1.setStartOffset(5000);
        animation1.setFillAfter(true);
        v.startAnimation(animation1);
        currentPlayLayout.addView(v);
    }
    public void nextPlay(){

        //remove plays

        //pick next correct play at random

        //pick next incorrect play at random / or possibly 2 incorrect
        // (example correct could be shake it, and wrongs could be tap and switch)
        //set text
        //add views
            currentPlayLayout.removeAllViews();
            play = getRandomWithExclusion(0,instructionTypes.size()-1,new int[]{});
            
            String[] instructions = instructionTypes.get(play);

            instruction.setText(instructions[language]);

            addPlayByNumber(play);
            int slideToExclude = Integer.MAX_VALUE;
            if(play==SLIDE_DOWN)slideToExclude=SLIDE_UP;
            if(play==SLIDE_UP)slideToExclude=SLIDE_DOWN;
            if(play==SLIDE_LEFT)slideToExclude=SLIDE_RIGHT;
            if(play==SLIDE_RIGHT)slideToExclude=SLIDE_LEFT;
            int antiPlay = getRandomWithExclusion(0,instructionTypes.size()-1,new int[]{play,slideToExclude});
            addPlayByNumber(antiPlay);

            //stop from always playing on left correct answer on left
            int shuffle = getRandomWithExclusion(0,1,new int[]{});
            if(shuffle>0){
                View v = currentPlayLayout.getChildAt(0);
                currentPlayLayout.removeView(v);
                currentPlayLayout.addView(v);
            }

            say(instruction.getText().toString());

            playImageView.setForeground(drawables2[play]);





    }

    public void addPlayBlock(){
        Button button = new Button(this);
        button.setForeground(drawables[BLOCK]);
        button.setBackgroundColor(playColors[BLOCK]);
        button.setWidth(screen_width/2);
        button.setHeight(play_height);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                wrongAnswer();
            }
        });
        button.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                wrongAnswer();
                return true;//return true or error!
            }
        });
        currentPlayLayout.addView(button);


    }


    public void addPlayByNumber(int play){
        for (int key : instructionTypes.keySet()) {
            if(key==play){
                if(key==TAP){
                    addPlayTap();
                }else if(key == SWITCH){
                    addPlaySwtich();
                }else if(key == HOLD){
                    addHoldit();
                }else if(key == SLIDE_LEFT || key == SLIDE_RIGHT || key == SLIDE_UP || key == SLIDE_DOWN){
                    addPlaySlide(play);
                }else if(key == BLOCK){
                    addPlayBlock();
                }


            }
        }
    }
    int getRandomWithExclusion(int min, int max, int [] exclude) {
        int i = getRandomWithExclusion2(min,max,exclude);
        return i;
    }



    int getRandomWithExclusion2(int min, int max, int [] exclude) {
        //SOURCE
        //https://www.baeldung.com/java-generating-random-numbers-in-range
        //Written by:baeldung
        Random rnd = new Random();
        OptionalInt random = rnd.ints(min, max + 1)
                .filter(num -> Arrays.stream(exclude).noneMatch(ex -> num == ex))
                .findFirst();

        int start = 0;
        return random.orElse(start);
    }

    /*
    ideas
        shake it (shake phone)
        squeeze
     */






}