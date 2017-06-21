package com.example.hassenalex.bluetoothgame;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.view.View;

/********************************************************           
    * GameActivity									        *   
    *                                                      	*   
    * Author:  Hassen Kassim, Alexander Brummer				*   
    *                                                      	*   
    * Purpose:  Demonstration of a simple program for 		*
    * connecting two android devices via the bluetooth    	*   
    *                                                      	*     
    ********************************************************/  

public class GameActivity extends AppCompatActivity {

    private static BluetoothService btS;
    private static int xcoordinate = 0;
    private static int ycoordinate = 0;
    private static int xvelocity;
    private static int yvelocity;
    private static int radius;
    private static int myball = 0;
    private static int screenwidth;
    private static int screenheight;
    private static Context ctx;


    /**
     * The Handler that gets information back from the BluetoothChatService
     */
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    processMsg(readMessage);
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ctx = this;
        actionbarhide();

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay()
                .getMetrics(metrics);
        screenwidth = metrics.widthPixels;
        screenheight = metrics.heightPixels;
        radius = (int)Math.round(0.05*screenwidth);
        xvelocity = (int) Math.round(0.01 * screenwidth);
        yvelocity = (int) Math.round(0.02 * screenheight);
        xcoordinate = 50;
        ycoordinate = 50;

        setContentView(R.layout.activity_game);
        btS = ConnectActivity.btService;
        btS.setHandler(mHandler);

        Bundle extras = getIntent().getExtras();
        String readyflag = extras.getString("AREYOUREADY", "Yes");

        if(readyflag.equals("No")){
            showalert();
        }
        else{
            write("YOUREADY?");
        }
    }

    private void showalert() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Are you ready?");
        builder.setCancelable(true);
        builder.setPositiveButton("Yes",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                        write("START");
                        myball = 0;
                        setContentView(new BallView(ctx));
                    }
                });
        builder.setNegativeButton("No",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });

        AlertDialog alert1 = builder.create();
        alert1.show();
    }

    private void actionbarhide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }

    }

    private void processMsg(String msg) {
        switch (msg) {
            case "YOUREADY?":

                break;
            case "START":
                startGame();
                break;
            default:
                if(msg.contains("BALL")){
                    String[] parts = msg.split(":");
                    xcoordinate = Math.round(Float.parseFloat(parts[1]) * screenwidth);
                    ycoordinate = Math.round(Float.parseFloat(parts[2]) * screenheight);
                    xvelocity = (int) Math.round(Float.parseFloat(parts[3]) * screenwidth);
                    yvelocity = (int) Math.round(Float.parseFloat(parts[4]) * screenheight);
                    myball = 1;
                }
                break;
        }
    }

    private static void write (String msg) {
        byte[] bytes = msg.getBytes();
        btS.write(bytes);
    }

    public void startGame() {
        myball = 1;
        setContentView(new BallView(this));
    }



    //this inner class draws the ball
    private static class BallView extends View {

        // CONSTRUCTOR
        public BallView(Context context) {
            super(context);
            setFocusable(true);
        }

        @Override
        protected void onDraw(Canvas canvas) {

            invalidate(); //invalidates the drawing
            try {
                Thread.sleep(10);
            } catch (InterruptedException ex) {
            }
            canvas.drawColor(Color.WHITE);

            //state 1 means the ball is in the field of this device
            if(myball==1) {
                Paint p = new Paint();
                // smooths
                p.setAntiAlias(true);
                p.setColor(Color.BLUE);

                //calculate the new coordinates, always add 1 %

                int x = xcoordinate + xvelocity;
                int y = ycoordinate + yvelocity;
                xcoordinate = x;
                ycoordinate = y;

                if (x + radius > screenwidth) {
                    xvelocity = -xvelocity;
                    return;
                } else if (x - radius < 0 && xvelocity < 0) {
                    xvelocity = -xvelocity;
                    return;
                } else if (y + radius > screenheight && yvelocity > 0) {
                    yvelocity = -yvelocity;
                    return;
                } else if (y - radius < 0 && yvelocity < 0) {
                    myball = 2;
                    float xper = 1.0f - (new Float(x)/screenwidth);
                    float yper = 0;
                    float xvPer = new Float(-xvelocity)/screenwidth;
                    float yvPer = new Float(-yvelocity)/screenheight;
                    write("BALL:" + xper + ":" + yper + ":" + xvPer + ":" + yvPer);
                    return;
                }

                canvas.drawCircle(x, y, radius, p);
                canvas.drawText("X: " + xcoordinate, 10.0f, 10.0f, p);
                canvas.drawText("Y: " + ycoordinate, 10.0f, 30.0f, p);
                canvas.drawText("Mode: " + myball, 10.0f, 50.0f, p);
            }

            //this state means the ball is in a transistion between the devices
            else if(myball==2) {
                Paint p = new Paint();
                // smooths
                p.setAntiAlias(true);
                p.setColor(Color.BLUE);

                int x = xcoordinate + xvelocity;
                int y = ycoordinate + yvelocity;
                xcoordinate = x;
                ycoordinate = y;

                if (x - radius >= screenwidth) {
                    myball = 0;
                    return;
                } else if (x + radius <= 0) {
                    myball = 0;
                    return;
                } else if (y - radius >= screenheight) {
                    myball = 0;
                    return;
                } else if (y + radius <= 0) {
                    myball = 0;
                    return;
                }

                canvas.drawCircle(x, y, radius, p);

                canvas.drawText("X: " + xcoordinate, 10.0f, 10.0f, p);
                canvas.drawText("Y: " + ycoordinate, 10.0f, 30.0f, p);
                canvas.drawText("Mode: " + myball, 10.0f, 50.0f, p);
            }

        }

    }


}
