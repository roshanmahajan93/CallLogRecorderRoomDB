package com.prishan.calllogrecorderroomdb;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

import androidx.room.Room;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class CallReceiver extends BroadcastReceiver {
    private static final String TAG = "CallReceiver";

    public Context context;
    public static String callerPhoneNumber="";

    private MediaRecorder rec = null;
    public static boolean recoderstarted = false;

    ArrayList<Todo> todoArrayList = new ArrayList<>();
    MyDatabase myDatabase;

    public static String callerStartTime="";
    public static String callerEndTime="";

    @Override
    public void onReceive(final Context context, Intent intent) {
        this.context = context;

        TelephonyManager manager = (TelephonyManager) context.getSystemService(context.TELEPHONY_SERVICE);
        manager.listen(new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                super.onCallStateChanged(state, incomingNumber);
                Log.e(TAG,"incomingNumber===>"+incomingNumber);
                if(!incomingNumber.equalsIgnoreCase("")&&incomingNumber!=null)
                {
                    callerPhoneNumber = ""+incomingNumber;
                }

                if (TelephonyManager.CALL_STATE_IDLE == state ){
                    try {
                        Log.e(TAG,"CALL_STATE_IDLE===>");
                        Log.e(TAG,"callerPhoneNumber===>"+callerPhoneNumber);
                        if(recoderstarted && !callerPhoneNumber.equalsIgnoreCase(""))
                        {
                            Log.e(TAG,"CALL_STATE_IDLE===>recoderstarted");
                            Thread.sleep(2000);
                            recoderstarted = false;

                            Date date = new Date();
                            String stringTime = ""+DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM).format(date);
                            callerEndTime = stringTime;

                            Log.e(TAG,"callerEndTime===>"+callerEndTime);

                            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/M/yyyy hh:mm:ss");

                            Date date1 = simpleDateFormat.parse(""+callerStartTime);
                            Date date2 = simpleDateFormat.parse(""+callerEndTime);

                            Log.e(TAG,"callerStartTime===>After"+DateFormat.getDateTimeInstance().format(date1));
                            Log.e(TAG,"callerEndTime===>After"+DateFormat.getDateTimeInstance().format(date2));

                            String dateDiff = ""+printDifference(date1, date2);

                            myDatabase = Room.databaseBuilder(context, MyDatabase.class, MyDatabase.DB_NAME).fallbackToDestructiveMigration().build();

                            Todo todo = new Todo();
                            todo.name = ""+callerPhoneNumber;
                            todo.description = ""+callerStartTime;
                            todo.category = ""+dateDiff;

                            todoArrayList.add(todo);
                            insertList(todoArrayList);

                            if (Networkstate.haveNetworkConnection(context)) {
                                checkFolderAndUploadFile();
                            }

                            Log.e(TAG,"CALL_STATE_IDLE===>Last");
                        }
                    }
                    catch(Exception e) {
                        Log.e("Exception","CALL_STATE_IDLE===>"+e.getMessage());
                        e.printStackTrace();
                    }
                }
                else if(TelephonyManager.CALL_STATE_OFFHOOK==state){
                    try {
                        Log.e(TAG,"CALL_STATE_OFFHOOK===>");
                        if(recoderstarted==false)
                        {

                            Log.e(TAG,"CALL_STATE_OFFHOOK===>First");
                            recoderstarted=true;

                            Date date = new Date();
                            String stringTime = ""+DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM).format(date);
                            callerStartTime = stringTime;
                            Log.e(TAG,"callerStartTime===>"+callerStartTime);

                        }
                    } catch (Exception e) {
                        Log.e("Exception","CALL_STATE_OFFHOOK===>"+e.getMessage());
                        e.printStackTrace();
                    }
                }
                else if(TelephonyManager.CALL_STATE_RINGING==state){
                    try {
                        Log.e(TAG,"CALL_STATE_RINGING===>");
                    } catch (Exception e) {
                        Log.e("Exception","CALL_STATE_RINGING===>"+e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
        }, PhoneStateListener.LISTEN_CALL_STATE);
    }

    @SuppressLint("StaticFieldLeak")
    private void insertList(List<Todo> todoList) {
        new AsyncTask<List<Todo>, Void, Void>() {
            @Override
            protected Void doInBackground(List<Todo>... params) {
                myDatabase.daoAccess().insertTodoList(params[0]);

                return null;

            }

            @Override
            protected void onPostExecute(Void voids) {
                super.onPostExecute(voids);
            }
        }.execute(todoList);

    }

    private void checkFolderAndUploadFile() {

        File[] datefolder = Constant.getCallRecordingDir().listFiles();

        if (datefolder != null) {
            Arrays.sort(datefolder, (f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));
            if (datefolder.length != 0) {
                //new uploadFile(datefolder).execute("");
            } else {
                Toast toast = Toast.makeText(context, "NO FILE AVAILABLE", Toast.LENGTH_LONG);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
                context.sendBroadcast(
                        new Intent().setAction("MANUAL_FILE_UPLOAD_COMPLETE")
                );
            }
        }
    }

    public String printDifference(Date startDate, Date endDate) {
        //milliseconds
        long different = endDate.getTime() - startDate.getTime();

        long secondsInMilli = 1000;
        long minutesInMilli = secondsInMilli * 60;
        long hoursInMilli = minutesInMilli * 60;
        long daysInMilli = hoursInMilli * 24;

        long elapsedDays = different / daysInMilli;
        different = different % daysInMilli;

        long elapsedHours = different / hoursInMilli;
        different = different % hoursInMilli;

        long elapsedMinutes = different / minutesInMilli;
        different = different % minutesInMilli;

        long elapsedSeconds = different / secondsInMilli;

        Log.e(TAG,""+elapsedHours+":"+elapsedMinutes+":"+elapsedSeconds);

        return ""+elapsedHours+":"+elapsedMinutes+":"+elapsedSeconds;
    }


}
