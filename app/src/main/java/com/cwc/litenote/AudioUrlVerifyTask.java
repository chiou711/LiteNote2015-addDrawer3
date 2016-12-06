package com.cwc.litenote;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.pm.ActivityInfo;
import android.os.AsyncTask;
import android.widget.Toast;

//A class that will run progress bar in the main GUI context
//
// audio Url verification task
//
class AudioUrlVerifyTask extends AsyncTask<String,Integer,String>
{
	 ProgressDialog mUrlVerifyDialog;
	 Activity mActivity;
	 AudioPrepareTask mAudioPrepareTask;

	 public AudioUrlVerifyTask(Activity act)
	 {
		 mActivity = act;
	 }	 
	 
	 @Override
	 protected void onPreExecute() 
	 {
	 	super.onPreExecute();
		// disable rotation
	 	mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
	 	
	 	
	 	System.out.println("AudioUrlVerifyTask / onPreExecute" );
	 	mUrlVerifyDialog = new ProgressDialog(mActivity);
        mUrlVerifyDialog.setMessage(mActivity.getResources().getText(R.string.audio_message_searching_media));
        mUrlVerifyDialog.setCancelable(true); // set true for enabling Back button 
        mUrlVerifyDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER); //ProgressDialog.STYLE_HORIZONTAL
        mUrlVerifyDialog.show();
        AudioPlayer.mIsPrepared = false;
	 } 
	 
	 static boolean mIsOkUrl;
	 static int mProgress =0;
	 @Override
	 protected String doInBackground(String... params) 
	 {
		 System.out.println("AudioUrlVerifyTask / doInBackground / params[0] = " + params[0] );
		 mProgress =0;
 		 // check if audio file exists or not
		 String audioStr = AudioPlayer.mAudioInfo.getAudioAt(AudioPlayer.mAudioIndex);
 		 mIsOkUrl = false;
 		 String scheme  = Util.getUriScheme(audioStr);
 		 System.out.println("scheme = " + scheme + " / path = " + audioStr);
 		
 		 // if scheme is https or http
 		 boolean isUriExisted = false;
 		 if(scheme.equalsIgnoreCase("http")|| scheme.equalsIgnoreCase("https") )						
 		 {
			 if(Util.isNetworkConnected(AudioPlayer.mAct))
			 {
		 		 isUriExisted = Util.isUriExisted(audioStr, AudioPlayer.mAct );
		 		 System.out.println("AudioUrlVerifyTask / isUriExisted  = " + isUriExisted);
		 		 if(isUriExisted)
		 		 {
		 			 try 
		 			 {
		 				 boolean isEnd = false;
		 				 int i = 0;
		 				 while(!isEnd)
		 				 {
		 					 // check if network connection is OK
		 					 publishProgress(Integer.valueOf(mProgress));
		 					 mProgress =+ 20;
		 					 if(mProgress >= 100)
		 						 mProgress = 0;
 				         
		 					 Util.tryUrlConnection(audioStr,AudioPlayer.mAct);
		 					 // wait for response
		 					 Thread.sleep(Util.oneSecond); //??? better idea?
 						
		 					 // check response
		 					 if(200 <= Util.mResponseCode && Util.mResponseCode <= 399)
		 						 mIsOkUrl =  true;
		 					 else
		 						 mIsOkUrl =  false;
 						
		 					 System.out.println("mIsOkUrl = " + mIsOkUrl +
		 							 			" / count = " + i);
		 					 if(mIsOkUrl)
		 						 isEnd = true;
		 					 else
		 					 {
		 						 i++;
		 						 if(i==5) //??? better idea?
		 							 isEnd = true; // no more try
		 					 }
		 				 }
		 			 } 
		 			 catch (Exception e1) 
		 			 {
		 				 e1.printStackTrace();
		 			 }
		 		}
			 }
 		 } 
 		 // if scheme is content 
 		 else if(scheme.equalsIgnoreCase("content") ||
 				scheme.equalsIgnoreCase("file")    )
 		 {
 			 String strName = null;
 			 isUriExisted = Util.isUriExisted(audioStr, AudioPlayer.mAct );
	 		 
 			 if(isUriExisted)
	 			 strName = Util.getDisplayNameByUriString(audioStr, AudioPlayer.mAct);
 			 
	 		 if(!Util.isEmptyString(strName))
 				mIsOkUrl = true;
 			 else
 				mIsOkUrl = false;
 		 }
 		
 		 System.out.println("Url mIsOkUrl = " + mIsOkUrl);	    	 

 		 if(mIsOkUrl)
 			 return "ok";
 		 else
 			 return "ng";
	 }
	
	 @Override
	 protected void onProgressUpdate(Integer... progress) 
	 { 
//		 System.out.println("AudioUrlVerifyTask / OnProgressUpdate / progress[0] " + progress[0] );
	     super.onProgressUpdate(progress);
	     if(mUrlVerifyDialog != null)
	    	 mUrlVerifyDialog.setProgress(progress[0]);
	 }
	 
	 // This is executed in the context of the main GUI thread
	 protected void onPostExecute(String result)
	 {
	 	System.out.println("AudioUrlVerifyTask / onPostExecute / result = " + result);
		
	 	// dialog off
		if((mUrlVerifyDialog != null) && mUrlVerifyDialog.isShowing() )
			mUrlVerifyDialog.dismiss();

 		mUrlVerifyDialog = null;

 		// task for audio prepare
	 	if(mIsOkUrl)
	 	{
	 		mAudioPrepareTask = new AudioPrepareTask(mActivity); 
	 		mAudioPrepareTask.execute("Preparing to play ...");
	 	}
 		
	 	// call runnable
	 	if(AudioPlayer.mPlayerState != AudioPlayer.PLAYER_AT_STOP)
	 	{
			if(AudioPlayer.mPlayMode == AudioPlayer.ONE_TIME_MODE)
			 	AudioPlayer.mAudioHandler.postDelayed(AudioPlayer.mRunOneTimeMode,Util.oneSecond/4); 
			else if(AudioPlayer.mPlayMode == AudioPlayer.CONTINUE_MODE)
			 	AudioPlayer.mAudioHandler.postDelayed(AudioPlayer.mRunContinueMode,Util.oneSecond/4);
	 	}	 	
	 }
}

/***************************************************************
 * 
 * audio prepare task
 * 
 */
class AudioPrepareTask extends AsyncTask<String,Integer,String>
{
	 Activity mActivity;
	 ProgressDialog mPrepareDialog;
	 int mProgress;
	 public AudioPrepareTask(Activity act)
	 {
		 mActivity = act;
	 }	 
	 
	 @Override
	 protected void onPreExecute() 
	 {
	 	super.onPreExecute();
	 	mPrepareDialog = new ProgressDialog(mActivity);

	 	System.out.println("AudioPrepareTask / onPreExecute" );
        mPrepareDialog.setCancelable(true); // set true for enabling Back button 
        mPrepareDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER); //ProgressDialog.STYLE_HORIZONTAL
        mPrepareDialog.show();
	 	mPrepareDialog.setMessage(mActivity.getResources().getText(R.string.audio_message_preparing_to_play));
        AudioPlayer.mIsPrepared = false;
	 } 
	 
	 @Override
	 protected String doInBackground(String... params) 
	 {
		 boolean isTimeOut = false;
		 mProgress = 0;
		 System.out.println("AudioPrepareTask / doInBackground / params[0] = " + params[0] );
		 int count = 0;
		 while(!AudioPlayer.mIsPrepared && !isTimeOut )
		 {
			 System.out.println("AudioPrepareTask / doInBackground / count = " + count);
			 count++;
			 
			 if(count >= 40) // 10 seconds, 1/4 * 40
				 isTimeOut = true;
			 
			 publishProgress(Integer.valueOf(mProgress));
			 
			 mProgress =+ 20;
			 if(mProgress >= 100)
				 mProgress = 0;	
			 
			 try {
				Thread.sleep(Util.oneSecond/4); //??? java.lang.InterruptedException
			 } catch (InterruptedException e) {
				e.printStackTrace();
			 } 
		 }
		 
		 if(isTimeOut)
			return "timeout";
		 else
			return "ok";
	 }
	
	 @Override
	 protected void onProgressUpdate(Integer... progress) 
	 { 
//		 System.out.println("AudioPrepareTask / OnProgressUpdate / progress[0] " + progress[0] );
	     super.onProgressUpdate(progress);
	     
	     if((mPrepareDialog != null) && mPrepareDialog.isShowing())
	    	 mPrepareDialog.setProgress(progress[0]);
	 }
	 
	 // This is executed in the context of the main GUI thread
	 protected void onPostExecute(String result)
	 {
	 	System.out.println("AudioPrepareTask / onPostExecute / result = " + result);
	 	
	 	// dialog off
		if((mPrepareDialog != null) && mPrepareDialog.isShowing())
			mPrepareDialog.dismiss();
		
		mPrepareDialog = null;

		// show time out
		if(result.equalsIgnoreCase("timeout"))
		{
			Toast toast = Toast.makeText(mActivity.getApplicationContext(), R.string.audio_message_preparing_time_out, Toast.LENGTH_SHORT);
			toast.show();
		}
		
		// disable rotation
	 	mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
	 }
	 
}