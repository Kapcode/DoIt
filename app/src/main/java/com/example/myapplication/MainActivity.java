package com.example.myapplication;

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
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
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
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends AppCompatActivity implements SensorEventListener , ShakeDetector.Listener {
    static final int scoreIncrement = 25;
    volatile AtomicBoolean isGameOverBool = new AtomicBoolean(true);//NEEDS TO BE VOLITILE!
    static AlertDialog.Builder gameOverAlertDialog;
   volatile ShakeDetector.Listener listener_ShakeDetector;
    volatile SensorEventListener sensorEventListener;
    volatile Thread shakeThread = null,blockThread=null;
    volatile ShakeDetector shakeDetector;
    final boolean fail_for_block_when_not_correct_play = true;// some might find that annoying,
    // deffinatly could be issue for shake or tilt / acidentaly tilting phone while plying  (maybey check if its even on the board?)
    static final int swipeThreshhold = 35;
    final int [] playColors = new int[]{
            Color.rgb(106,0,128),
            Color.rgb(51,0,77),
            Color.rgb(34,0,102),
    Color.rgb(0,84,77),
    Color.rgb(10,64,77),
    Color.rgb(102,0,34),
    Color.rgb(102,34,0),
    Color.rgb(13,77,0),
    Color.rgb(204, 0, 102)};

    static final int seekWidth = 75;
    public static ImageView blockImageAnimatedSignal;
    static final int switchSeekWidth = seekWidth+20;
    static final int sliderHeight = 50;
    static Activity activity;
    volatile static int play = 0;
    static Handler handler;
    static Button start;
    int uiReactionDelay = 150;
    public static final int TAP=0,SWITCH=1,HOLD=2,BLOCK=3,SLIDE_RIGHT=4,SLIDE_LEFT=5,SLIDE_UP=6,SLIDE_DOWN=7,SHAKE=8;
    public static ImageView playImageView;
    public static final int ENGLISH = 0;
    public static final int SPANISH = 1;
    public static int language = ENGLISH;
    public static TextToSpeech textToSpeech;
    HashMap <Integer,String[]> instructionTypes = new HashMap<Integer,String[]>();
    Drawable[] drawables_used_for_play_cards, drawables_used_for_play_image_view;
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



    //shaking
    /* put this into your activity class */
    private SensorManager mSensorManagerShake;
    private float mAccel; // acceleration apart from gravity
    private float mAccelCurrent; // current acceleration including gravity
    private float mAccelLast; // last acceleration including gravity



    @Override
    public void onSensorChanged(SensorEvent event) {

        float maxRange = mSensor.getMaximumRange();
        if(maxRange == event.values[0]) {
        // Do something when something is far away.
        }
        else {// Do something when something is near.
            if(start.getVisibility() == View.GONE){// don't start game or talk with sensor
                if(play==BLOCK){
                    //correct
                    toast(null,"BLOCK");
                    correctAnswer();
                }else if(fail_for_block_when_not_correct_play){
                    wrongAnswer();
                }else{
                    stopDetectingBlock();
                    say("game is over stopping block dettection");
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
        //TODO
        loadAds();


         listener_ShakeDetector=this;
         sensorEventListener = this;
        currentPlayLayout = findViewById(R.id.playArea);
        playImageView = (ImageView) findViewById(R.id.playImageView);
        toaster = new Toast(this);
        start = findViewById(R.id.start);
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        scoreTV = (TextView)findViewById(R.id.scoretv) ;
        livesTV = (TextView)findViewById(R.id.livestv);
        blockImageAnimatedSignal = (ImageView)findViewById(R.id.blockImageAnimatedSignal);
        handler = new Handler();
        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //don't increment score, just start game.....

                changeLive(RESET);
                nextPlay();
                start.setVisibility(View.GONE);
                isGameOverBool.set(false);

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
        instructionTypes.put(8,new String[]{"Shake",""});
        //add drawables in order by index
        drawables_used_for_play_cards = new Drawable[]
                {getResources().getDrawable(R.drawable.tap),//
                        getResources().getDrawable(R.drawable.sswitch),
                        getResources().getDrawable(R.drawable.handshake),
                        getResources().getDrawable(R.drawable.block),
                        getResources().getDrawable(R.drawable.right),//r
                        getResources().getDrawable(R.drawable.left),//l
                        getResources().getDrawable(R.drawable.actual_up_arrow),//u
                        getResources().getDrawable(R.drawable.actual_down_arrow),
                getResources().getDrawable(R.drawable.shake)};//d

        drawables_used_for_play_image_view = new Drawable[]
                {getResources().getDrawable(R.drawable.tap),
                        getResources().getDrawable(R.drawable.sswitch),
                        getResources().getDrawable(R.drawable.handshake),
                        getResources().getDrawable(R.drawable.block),
                        getResources().getDrawable(R.drawable.right),
                        getResources().getDrawable(R.drawable.left),
                        getResources().getDrawable(R.drawable.actual_up_arrow),
                        getResources().getDrawable(R.drawable.actual_down_arrow),
                        getResources().getDrawable(R.drawable.shake)};

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
        if(play==BLOCK) startDetectingBlock();
        if(play==SHAKE)startDetectingShake();

    }
    @Override
    protected void onPause() {
        // Register a listener for the sensor.
        super.onResume();
        stopDetectingBlock();
        stopDetectingShake();
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




    public void loadAds(){
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
    }

    public void startDetectingShake(){
        stopDetectingBlock();
        //shakeDetector.setSensitivity();//TODO

        shakeThread = new Thread(new Runnable() {
            @Override
            public void run() {

                while( play==SHAKE){
                    SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
                    if(shakeDetector==null)shakeDetector = new ShakeDetector(listener_ShakeDetector);
                    shakeDetector.start(sensorManager);
                }

            }
        });
        shakeThread.start();
    }
    public void stopDetectingShake(){
        if(shakeDetector!=null)shakeDetector.stop();//won't need null check after done creating shake Detection
    }

    public void startDetectingBlock(){
        stopDetectingShake();
        blockThread =new Thread(new Runnable() {
            @Override
            public void run() {
                while(play==BLOCK){
                    mSensorManager.registerListener(sensorEventListener, mSensor,
                            SensorManager.SENSOR_DELAY_NORMAL);
                }

            }
        });
        blockThread.start();



    }

    public void stopDetectingBlock(){
        mSensorManager.unregisterListener(this, mSensor);
    }










    public void addPlayTap(){
        Button button = new Button(this);
        button.setTextSize(16);
        button.setWidth(screen_width/2);
        button.setHeight(play_height);
        button.setBackgroundColor(playColors[TAP]);
        button.setForeground(drawables_used_for_play_cards[TAP]);
        //button.setText("TAP");
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(play==TAP ){
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
        button.setForeground(drawables_used_for_play_cards[HOLD]);
        button.setTextSize(16);
        button.setWidth(screen_width/2);
        button.setHeight(play_height);
        //button.setText("HOLD");
        button.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if( play==HOLD){
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

    public void swipe(int play__direction){
        Button button = new Button(this);
        button.setLayoutParams(new ViewGroup.LayoutParams(screen_width/2,play_height));
        button.setBackgroundColor(playColors[play__direction]);
        button.setForeground(drawables_used_for_play_cards[play__direction]);
        currentPlayLayout.addView(button);
        final float[] dX = new float[1];
        final float[] dY = new float[1];
        final int[] startXY = new int[2];


        button.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        //save starting location of view x and y
                        startXY[0]=(int)view.getX();
                        startXY[1]=(int)view.getY();
                        dX[0] = view.getX() - motionEvent.getRawX();
                        dY[0] = view.getY() - motionEvent.getRawY();


                        break;
                    case MotionEvent.ACTION_MOVE:
                        view.animate()
                                .x(motionEvent.getRawX() + dX[0])
                                .y(motionEvent.getRawY() + dY[0])
                                .setDuration(0)
                                .start();
                        break;
                    case MotionEvent.ACTION_UP:
                        //get xy of view difference, how far it moved since finger was put down
                        int[] difXY = new int[]{startXY[0]-(int)view.getX(),startXY[1]-(int)view.getY()};
                        int xdif = difXY[0],ydif = difXY[1];
                        toast(null,"swipe " + xdif);
                        if( play__direction == play &&play__direction==SLIDE_RIGHT && xdif < swipeThreshhold){

                            correctAnswer();
                        } else if(play__direction == play &&play__direction==SLIDE_UP && ydif > swipeThreshhold){
                            //toast(seekBar,"slide " + play__direction);
                            correctAnswer();
                        } else if(play__direction == play &&play__direction==SLIDE_LEFT && xdif > swipeThreshhold){
                            //toast(seekBar,"slide " + play__direction);
                            correctAnswer();
                        } else if(play__direction == play &&play__direction==SLIDE_DOWN && ydif < swipeThreshhold){
                            //toast(seekBar,"slide " + thisPlay);
                            correctAnswer();
                        }else{
                            wrongAnswer();
                        }
                    default:
                        return false;
                }
                return true;
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
        Drawable drawable = drawables_used_for_play_cards[SWITCH];
        switchSeek.setThumb(drawable);


        Switch sswitch = new Switch(this);
        //sswitch.setChecked(false);
        sswitch.toggle();

        sswitch.setTextSize(16);
        sswitch.setWidth(screen_width/2);
        sswitch.setHeight(play_height);
        sswitch.setThumbDrawable(drawables_used_for_play_cards[SWITCH]);
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

                if(play==SWITCH){
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
        say(isGameOverBool.get()+"");
        if(!isGameOverBool.get()){

            int score = Integer.parseInt(scoreTV.getText().toString());
            score = score + scoreIncrement;
            scoreTV.setText(score+"");
            //next play
            nextPlay();
        }

    }

    public void wrongAnswer(){

        if(!isGameOverBool.get()){
            stopBlockImageAnimatedSignal();
            say("Wrong!");

            int score = Integer.parseInt(scoreTV.getText().toString());


            changeLive(-1);
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
        stopBlockImageAnimatedSignal();
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
            playImageView.setForeground(drawables_used_for_play_image_view[play]);
            //
        findViewById(R.id.topImage).setBackgroundColor(playColors[play]);

    }


    public void addPlayShake(){
        ImageView v = new ImageView(this);
        v.setForeground(drawables_used_for_play_cards[SHAKE].mutate());
        RelativeLayout rl = new RelativeLayout(this);
        rl.setBackgroundColor(playColors[SHAKE]);
        //seekBar.setBackgroundColor(Color.LTGRAY);
        //ll.setBackgroundColor(Color.BLUE);
        rl.addView(v);
        currentPlayLayout.addView(rl);
        RelativeLayout.LayoutParams seek_lp = (RelativeLayout.LayoutParams) v.getLayoutParams();
        seek_lp.height = play_height;//Make the switch thicker!
        seek_lp.width= screen_width/2;
        seek_lp.addRule(RelativeLayout.CENTER_IN_PARENT);
        seek_lp.addRule(RelativeLayout.CENTER_VERTICAL);
        seek_lp.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        seek_lp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        ViewGroup.LayoutParams rl_lp = rl.getLayoutParams();
        rl_lp.width=screen_width/2;
        rl_lp.height=play_height;
        rl.setLayoutParams(rl_lp);

        startDetectingShake();


    }



    public void addPlayBlock(){

        Button button = new Button(this);
        button.setForeground(drawables_used_for_play_cards[BLOCK]);
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




        //block animation

        if(play==BLOCK){
            startBlockImageAnimatedSignal();
        }
        startDetectingBlock();






    }

    public void startBlockImageAnimatedSignal(){
        blockImageAnimatedSignal.setVisibility(View.VISIBLE);
        Animation mAnimation = new AlphaAnimation(1, 0);
        mAnimation.setDuration(500);
        mAnimation.setInterpolator(new LinearInterpolator());
        mAnimation.setRepeatCount(Animation.INFINITE);
        mAnimation.setRepeatMode(Animation.REVERSE);
        blockImageAnimatedSignal.startAnimation(mAnimation);
    }
    public void stopBlockImageAnimatedSignal(){
        blockImageAnimatedSignal.setVisibility(View.GONE);
        blockImageAnimatedSignal.clearAnimation();
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
                    //addPlaySlide(play);
                    swipe(play);
                }else if(key == BLOCK){
                    addPlayBlock();
                }else if(key == SHAKE){
                    addPlayShake();
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

    @Override
    public void hearShake() {//todo
        System.out.println("SHOOK!");
        if(play == SHAKE){
            correctAnswer();
        }//TODO catch a wrong answer if in play!
    }

    /*
    ideas
        shake it (shake phone)
        squeeze
     */
    static final int RESET = -99;
    static final int STARTING_LIVES = 2;
    public synchronized void changeLive(int offset){
        boolean over = false;
        if(offset==RESET){//restart game
            livesTV.setText(""+STARTING_LIVES);
            //over=true;
        }else{
            int oldValue = Integer.parseInt(livesTV.getText().toString());
            int i = oldValue + offset;
            if(i == 0){
                over=true;

            }else{
                livesTV.setText(String.valueOf( i   ));
                nextPlay();
            }

        }
        if(over){
            textToSpeech.speak("Game Over!",TextToSpeech.QUEUE_FLUSH,null);
            //end game

            showGameoverDialog();

        }else{
           // nextPlay();
        }

    }


    

    public void showGameoverDialog(){
        isGameOverBool.set(true);
        gameOverAlertDialog =
                new AlertDialog.Builder(this)
                        .setTitle("Game Over!")
                        .setMessage(scoreTV.getText().toString()+"").setOnDismissListener(new DialogInterface.OnDismissListener(){
                            @Override
                            public void onDismiss(DialogInterface dialogInterface) {

                                scoreTV.setText("000");
                                animateRemoveAllViews();
                                start.setVisibility(View.VISIBLE);
                            }
                        })
                        // Specifying a listener allows you to take an action before dismissing the dialog.
                        // The dialog is automatically dismissed when a dialog button is clicked.
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // Continue with delete operation

                                scoreTV.setText("000");
                                animateRemoveAllViews();
                                start.setVisibility(View.VISIBLE);

                            }
                        })
                        .setIcon(android.R.drawable.ic_dialog_alert);
        gameOverAlertDialog.show();
    }



    public void istAd(){

        if(mInterstitialAd!=null) mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback(){
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


}