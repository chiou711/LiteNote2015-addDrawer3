package com.cwc.litenote;

import java.util.ArrayList;
import java.util.List;

import com.cwc.litenote.lib.DragSortController;
import com.cwc.litenote.lib.DragSortListView;
import com.cwc.litenote.lib.SimpleDragSortCursorAdapter;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.ImageLoadingProgressListener;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnShowListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

public class NoteFragment extends UilListViewBaseFragment 
						  implements LoaderManager.LoaderCallbacks<List<String>> 
{
	private static Cursor mNotesCursor;
	private static DB mDb;
    SharedPreferences mPref_delete_warn;
    SharedPreferences mPref_style;
	SharedPreferences mPref_show_note_attribute;

    private static Long mNoteNumber1 = (long) 1;
	private static String mNoteTitle1;
	private static String mNotePictureUri1;
	private static String mNoteAudioUri1;
	private static String mNoteBodyString1;
	private static int mMarkingIndex1;
	private static Long mCreateTime1;
	private static Long mNoteNumber2 ;
	private static String mNotePictureUri2;
	private static String mNoteAudioUri2;
	private static String mNoteTitle2;
	private static String mNoteBodyString2;
	private static int mMarkingIndex2;
	private static Long mCreateTime2;
	private List<Boolean> mSelectedList = new ArrayList<Boolean>();
	
	// This is the Adapter being used to display the list's data.
	NoteListAdapter mAdapter;
	DragSortListView mDndListView;
	private DragSortController mController;
	int MOVE_TO = 0;
	int COPY_TO = 1;
    int mStyle = 0;
    int mCount;
    
	public NoteFragment(){}
	static Activity mAct;
	String mClassName;
	
	static SeekBar seekBarProgress;
	static int mediaFileLengthInMilliseconds; // this value contains the song duration in milliseconds. Look at getDuration() method in MediaPlayer class
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) 
	{
		super.onActivityCreated(savedInstanceState);
		System.out.println("NoteFragment / onActivityCreated");  
		mAct = getActivity();
		
		mClassName = getClass().getSimpleName();
		
		// recover scroll Y
		mFirstVisibleIndex = Util.getPref_lastTimeView_list_view_first_visible_index(getActivity());
		mFirstVisibleIndexTop = Util.getPref_lastTimeView_list_view_first_visible_index_top(getActivity());
		
		listView = (DragSortListView)getActivity().findViewById(R.id.list1);
		mDndListView = listView;
    	mDb = new DB(getActivity()); 
    	
    	mStyle = Util.getCurrentPageStyle(getActivity());

    	UilCommon.init();
    	
    	//listener: view note 
    	mDndListView.setOnItemClickListener(new OnItemClickListener()
    	{   @Override
			public void onItemClick(AdapterView<?> arg0, View view, int position, long id) 
			{
    			System.out.println("NoteFragment / onItemClick");
	    		mDb.doOpenByDrawerTabsTableId(DB.getFocus_DrawerTabsTableId());
	    		mCount = mDb.getNotesCount();
	    		mDb.doClose();
				if(position < mCount) // avoid footer error
				{
					Intent intent;
					intent = new Intent(getActivity(), Note_view_pager.class);
			        intent.putExtra("POSITION", position);
			        startActivity(intent);
				}
			}
    	});
    	
    	// listener: edit note 
    	mDndListView.setOnItemLongClickListener(new OnItemLongClickListener()
    	{
            public boolean onItemLongClick(AdapterView<?> parent, View v, int position, long id)
             {	
		        Intent i = new Intent(getActivity(), Note_edit.class);
				mDb.doOpenByDrawerTabsTableId(DB.getFocus_DrawerTabsTableId());
				Long rowId = mDb.getNoteId(position);
		        i.putExtra("list_view_position", position);
		        i.putExtra(DB.KEY_NOTE_ID, mDb.getNoteId(position));
		        i.putExtra(DB.KEY_NOTE_TITLE, mDb.getNoteTitleById(rowId));
		        i.putExtra(DB.KEY_NOTE_PICTURE_URI , mDb.getNotePictureUriById(rowId));
		        i.putExtra(DB.KEY_NOTE_AUDIO_URI , mDb.getNoteAudioUriById(rowId));
		        i.putExtra(DB.KEY_NOTE_BODY, mDb.getNoteBodyById(rowId));
		        i.putExtra(DB.KEY_NOTE_CREATED, mDb.getNoteCreatedTimeById(rowId));
				mDb.doClose();
		        startActivity(i);
            	return true;
             }
	    });
    	
        mController = buildController(mDndListView);
        mDndListView.setFloatViewManager(mController);
        mDndListView.setOnTouchListener(mController);
  		mDndListView.setDragEnabled(true);
	  	
		// We have a menu item to show in action bar.
		setHasOptionsMenu(true);

		// Create an empty adapter we will use to display the loaded data.
		mAdapter = new NoteListAdapter(getActivity());

		setListAdapter(mAdapter);

		// Start out with a progress indicator.
		setListShown(true); //set progress indicator

		// Prepare the loader. Either re-connect with an existing one or start a new one.
		getLoaderManager().initLoader(0, null, this);
		
	}
	
    // list view listener: on drag
    private DragSortListView.DragListener onDrag = new DragSortListView.DragListener() 
    {
                @Override
                public void drag(int startPosition, int endPosition) {
                	//add highlight boarder
//                    View v = mDndListView.mFloatView;
//                    v.setBackgroundColor(Color.rgb(255,128,0));
//                	v.setBackgroundResource(R.drawable.listview_item_shape_dragging);
//                    v.setPadding(0, 4, 0,4);
                }
    };
	
    // list view listener: on drop
    private DragSortListView.DropListener onDrop = new DragSortListView.DropListener() 
    {
        @Override
        public void drop(int startPosition, int endPosition) {
        	
        	int oriStartPos = startPosition;
        	int oriEndPos = endPosition;
        	
			mDb.doOpenByDrawerTabsTableId(DB.getFocus_DrawerTabsTableId());
			if(startPosition >= mDb.getNotesCount()) // avoid footer error
				return;
	    	mDb.doClose();
			
			mSelectedList.set(startPosition, true);
			mSelectedList.set(endPosition, true);
			
			
			//reorder data base storage
			int loop = Math.abs(startPosition-endPosition);
			for(int i=0;i< loop;i++)
			{
				swapRows(startPosition,endPosition);
				if((startPosition-endPosition) >0)
					endPosition++;
				else
					endPosition--;
			}
			
			if( (AudioPlayer.mMediaPlayer != null) )
			{
				if( (mHighlightPosition == oriEndPos)  && (oriStartPos > oriEndPos))      
				{
					mHighlightPosition = oriEndPos+1;
				}
				else if( (mHighlightPosition == oriEndPos) && (oriStartPos < oriEndPos))
				{
					mHighlightPosition = oriEndPos-1;
				}
				else if( (mHighlightPosition == oriStartPos)  && (oriStartPos > oriEndPos))      
				{
					mHighlightPosition = oriEndPos;
				}
				else if( (mHighlightPosition == oriStartPos) && (oriStartPos < oriEndPos))
				{
					mHighlightPosition = oriEndPos;
				}				
				else if(  (mHighlightPosition < oriEndPos) && 
						  (mHighlightPosition > oriStartPos)   )    
				{
					mHighlightPosition--;
				}
				else if( (mHighlightPosition > oriEndPos) && 
						 (mHighlightPosition < oriStartPos)  )
				{
					mHighlightPosition++;
				}
				

				AudioPlayer.mAudioIndex = (int)mHighlightPosition;
				AudioPlayer.prepareAudioInfo(getActivity());
			}
			mItemAdapter.notifyDataSetChanged();
			setFooter();
        }
    };
	
    /**
     * Called in onCreateView. Override this to provide a custom
     * DragSortController.
     */
    public DragSortController buildController(DragSortListView dslv)
    {
        // defaults are
        DragSortController controller = new DragSortController(dslv);
        controller.setSortEnabled(true);
        
        //drag
	  	mPref_show_note_attribute = getActivity().getSharedPreferences("show_note_attribute", 0);
	  	if(mPref_show_note_attribute.getString("KEY_ENABLE_DRAGGABLE", "yes").equalsIgnoreCase("yes"))
	  		controller.setDragInitMode(DragSortController.ON_DOWN); // click
	  	else
	        controller.setDragInitMode(DragSortController.MISS); 

	  	controller.setDragHandleId(R.id.img_dragger);// handler
//        controller.setDragInitMode(DragSortController.ON_LONG_PRESS); //long click to drag
	  	controller.setBackgroundColor(Color.argb(128,128,64,0));// background color when dragging
//        controller.setBackgroundColor(Util.mBG_ColorArray[mStyle]);// background color when dragging
        
	  	// mark
        controller.setMarkEnabled(true);
        controller.setClickMarkId(R.id.img_check);
        controller.setMarkMode(DragSortController.ON_DOWN);
        // audio
        controller.setAudioEnabled(true);
//        controller.setClickAudioId(R.id.img_audio);
        controller.setClickAudioId(R.id.audio_block);
        controller.setAudioMode(DragSortController.ON_DOWN);

        return controller;
    }        

    @Override
    public void onResume() {
    	super.onResume();
    	System.out.println(mClassName + " / onResume");
    }
    
    @Override
    public void onSaveInstanceState(Bundle outState) {
    	super.onSaveInstanceState(outState);
    	System.out.println(mClassName + " / onSaveInstanceState");
    }
    
	@Override
	public Loader<List<String>> onCreateLoader(int id, Bundle args) 
	{
		// This is called when a new Loader needs to be created. 
		return new NoteListLoader(getActivity());
	}
	

	@Override
	public void onLoadFinished(Loader<List<String>> loader,
							   List<String> data) 
	{
		System.out.println("== NoteFragment / onLoadFinished");
		// Set the new data in the adapter.
		mAdapter.setData(data);

		// The list should now be shown.
		if (isResumed()) 
			setListShown(true);
		else 
			setListShownNoAnimation(true);
		
		fillData();
		
		getLoaderManager().destroyLoader(0); // add for fixing callback twice
	}

	@Override
	public void onLoaderReset(Loader<List<String>> loader) {
		// Clear the data in the adapter.
		mAdapter.setData(null);
	}

	int mFirstVisibleIndex;
	int mFirstVisibleIndexTop;
	/**
	 * fill data
	 */
	static ItemAdapter mItemAdapter;
    public void fillData()
    {
    	System.out.println("NoteFragment / fillData");
    	
    	// save index and top position
//    	int index = mDndListView.getFirstVisiblePosition();
//      View v = mDndListView.getChildAt(0);
//      int top = (v == null) ? 0 : v.getTop();
        
        // set background color of list view
        mDndListView.setBackgroundColor(Util.mBG_ColorArray[mStyle]);

    	//show divider color
        if(mStyle%2 == 0)
	    	mDndListView.setDivider(new ColorDrawable(0xFFffffff));//for dark
        else
           	mDndListView.setDivider(new ColorDrawable(0xff000000));//for light

        mDndListView.setDividerHeight(1);
        
    	mDb.doOpenByDrawerTabsTableId(DB.getFocus_DrawerTabsTableId());
    	mNotesCursor = mDb.mNoteCursor;
    	int count = mDb.getNotesCount();
        mDb.doClose();
        // set adapter
        String[] from = new String[] { DB.KEY_NOTE_TITLE};
        int[] to = new int[] { R.id.text_whole };
        mItemAdapter = new ItemAdapter(
				getActivity(),
				R.layout.activity_main_list_row,
				mNotesCursor,
				from,
				to,
				0
				);
        
         mDndListView.setAdapter(mItemAdapter);
        
		// selected list
		for(int i=0; i< count ; i++ )
		{
			mSelectedList.add(true);
			mSelectedList.set(i,true);
		}
		
        // restore index and top position
        mDndListView.setSelectionFromTop(mFirstVisibleIndex, mFirstVisibleIndexTop);
        
        mDndListView.setDropListener(onDrop);
        mDndListView.setDragListener(onDrag);
        mDndListView.setMarkListener(onMark);
        mDndListView.setAudioListener(onAudio);
		mDndListView.setOnScrollListener(onScroll);
        
        setFooter();
    }

    OnScrollListener onScroll = new OnScrollListener() {
		
		@Override
		public void onScrollStateChanged(AbsListView view, int scrollState) {
			System.out.println("_onScrollStateChanged");
	        mFirstVisibleIndex = mDndListView.getFirstVisiblePosition();
	        View v = mDndListView.getChildAt(0);
	        mFirstVisibleIndexTop = (v == null) ? 0 : v.getTop();
            // keep index and top position
	        Util.setPref_lastTimeView_list_view_first_visible_index(getActivity(),mFirstVisibleIndex);
	        Util.setPref_lastTimeView_list_view_first_visible_index_top(getActivity(),mFirstVisibleIndexTop);
	        
//	        System.out.println("getCurrentListViewLocation() = " + Util.getCurrentListViewLocation(getActivity()));
		}
		
		@Override
		public void onScroll(AbsListView view, int firstVisibleItem,
				int visibleItemCount, int totalItemCount) {
//			System.out.println("_onScroll");
		}
	};

    
    // swap rows
	protected static void swapRows(int startPosition, int endPosition) 
	{
		mDb.doOpenByDrawerTabsTableId(DB.getFocus_DrawerTabsTableId());

		mNoteNumber1 = mDb.getNoteId(startPosition);
        mNoteTitle1 = mDb.getNoteTitle(startPosition);
        mNotePictureUri1 = mDb.getNotePictureUri(startPosition);
        mNoteAudioUri1 = mDb.getNoteAudioUri(startPosition);
        mNoteBodyString1 = mDb.getNoteBody(startPosition);
        mMarkingIndex1 = mDb.getNoteMarking(startPosition);
    	mCreateTime1 = mDb.getNoteCreatedTime(startPosition); 

		mNoteNumber2 = mDb.getNoteId(endPosition);
        mNoteTitle2 = mDb.getNoteTitle(endPosition);
        mNotePictureUri2 = mDb.getNotePictureUri(endPosition);
        mNoteAudioUri2 = mDb.getNoteAudioUri(endPosition);
        mNoteBodyString2 = mDb.getNoteBody(endPosition);
        mMarkingIndex2 = mDb.getNoteMarking(endPosition);
    	mCreateTime2 = mDb.getNoteCreatedTime(endPosition); 
		
        mDb.updateNote(mNoteNumber2,
				 mNoteTitle1,
				 mNotePictureUri1,
				 mNoteAudioUri1, 
				 "", //???
				 mNoteBodyString1,
				 mMarkingIndex1,
				 mCreateTime1);		        
		
		mDb.updateNote(mNoteNumber1,
		 		 mNoteTitle2,
		 		 mNotePictureUri2,
		 		 mNoteAudioUri2, 
				 "", //???
		 		 mNoteBodyString2,
		 		 mMarkingIndex2,
		 		 mCreateTime2);	
    	mDb.doClose();
	}

    // list view listener: on mark
    private DragSortListView.MarkListener onMark =
    new DragSortListView.MarkListener() 
	{   @Override
        public void mark(int position) 
		{
			System.out.println("NoteFragment / onMark");

            mDb.doOpenByDrawerTabsTableId(DB.getFocus_DrawerTabsTableId());
            if(position >= mNotesCursor.getCount()) //end of list
            	return ;
            
            String strNote = mDb.getNoteTitle(position);
            String strPictureUri = mDb.getNotePictureUri(position);
            String strAudioUri = mDb.getNoteAudioUri(position);
            String strNoteBody = mDb.getNoteBody(position);
            Long idNote =  mDb.getNoteId(position);
		
            // toggle the marking
            if(mDb.getNoteMarking(position) == 0)                
          	  mDb.updateNote(idNote, strNote, strPictureUri, strAudioUri , "", strNoteBody , 1, 0); //???
            else
          	  mDb.updateNote(idNote, strNote, strPictureUri, strAudioUri , "", strNoteBody ,0, 0); //???
            
            mDb.doClose();
          
            if( (DrawerActivity.mCurrentPlaying_NotesTblId == 
    			 TabsHostFragment.mCurrentNotesTableId        ) &&
    			(DrawerActivity.mCurrentPlaying_TabIndex == 
    			 TabsHostFragment.mCurrentTabIndex            )      )
            {
            	AudioPlayer.prepareAudioInfo(getActivity());
            }
            
            // Stop if unmarked item is at playing state
            if(AudioPlayer.mAudioIndex == position) 
            	UtilAudio.stopAudioIfNeeded();

            mItemAdapter.notifyDataSetChanged();
            setFooter();
            
            return;
        }
    };    
    
    // list view listener: on audio
    private DragSortListView.AudioListener onAudio = new DragSortListView.AudioListener() 
	{   @Override
        public void audio(int position) 
		{
			System.out.println("NoteFragment / onAudio");
			AudioPlayer.mPlayMode = AudioPlayer.CONTINUE_MODE;
			
			mDb.doOpenByDrawerTabsTableId(DB.getFocus_DrawerTabsTableId());
            if(position >= mNotesCursor.getCount()) //end of list
            	return ;
            
    		mCount = mDb.getNotesCount();
    		
    		boolean isAudioUri = false;
    		String uriString = mDb.getNoteAudioUri(position);
    		if(uriString != null)
    		{
    			isAudioUri = ((uriString.length() > 0)&&
    						   (mDb.getNoteMarking(position) == 1)		  	  )?true:false;
    		}
    		System.out.println("isAudioUri = " + isAudioUri);
    		mDb.doClose();
	
			mDb.doOpenByDrawerTabsTableId(DB.getFocus_DrawerTabsTableId());
			boolean bItemIsMarked = (mDb.getNoteMarking(position) == 1?true:false);
			mDb.doClose();

			if(position < mCount) // avoid footer error
			{
				if(isAudioUri && bItemIsMarked)
				{
					// cancel playing
					if(AudioPlayer.mMediaPlayer != null)
					{
						if(AudioPlayer.mMediaPlayer.isPlaying())
		   			   	{
		   					AudioPlayer.mMediaPlayer.pause();
		   			   	}
						AudioPlayer.mAudioHandler.removeCallbacks(AudioPlayer.mRunOneTimeMode);     
						AudioPlayer.mAudioHandler.removeCallbacks(AudioPlayer.mRunContinueMode);
						AudioPlayer.mMediaPlayer.release();
						AudioPlayer.mMediaPlayer = null;
					}
					
					// create new Intent to play audio
					AudioPlayer.mAudioIndex = (int)position; 
					UtilAudio.startAudioPlayer(getActivity()); //??? what to do when double click?
					setFooter();// call _setFooterMessage at AudioPlayer _onPrepared stage
					
					// update notes table Id
					DrawerActivity.mCurrentPlaying_NotesTblId = TabsHostFragment.mCurrentNotesTableId;
					// update playing tab index
					DrawerActivity.mCurrentPlaying_TabIndex = TabsHostFragment.mCurrentTabIndex;
					// update playing drawer index
				    DrawerActivity.mCurrentPlaying_DrawerIndex = DrawerActivity.mFocusDrawerPos;	
				}
			}
			
            mItemAdapter.notifyDataSetChanged();
            return;
        }
	};            

    static TextView footerTextView;
    static TextView footerAudioTextView;
    static TextView footerAudioNumberTextView;
    static ImageView footerAudioPlayAndPause;
    static ImageView footerAudioPlayPrevious;
    static ImageView footerAudioPlayNext;
    static View footer_audio_control;
    static String strFooterAudioMessage;
    
	// set footer
    static void setFooter()
    {
    	System.out.println("NoteFragment / setFooter ");
    	strFooterAudioMessage = null;
	    mDb.doOpenByDrawerTabsTableId(DB.getFocus_DrawerTabsTableId());
	    footerTextView = (TextView) mAct.findViewById(R.id.footerText);
	    footerTextView.setTextColor(Color.rgb(255,255,255));
	    if(footerTextView != null) //add this for avoiding null exception when after e-Mail action
	    {
	    	footerTextView.setText(getFooterMessage());
	    }
	    mDb.doClose();

	    footer_audio_control = mAct.findViewById(R.id.footer_audio_control);
	    footer_audio_control.setVisibility(View.GONE);
	    
	    // init footer audio image
	    footerAudioPlayAndPause = (ImageView) mAct.findViewById(R.id.footer_img_audio_play);
	    footerAudioPlayAndPause.setImageResource(R.drawable.ic_audio_selected);
	    footerAudioPlayPrevious = (ImageView) mAct.findViewById(R.id.footer_img_audio_previous);
	    footerAudioPlayPrevious.setImageResource(R.drawable.ic_media_previous);
	    footerAudioPlayNext = (ImageView) mAct.findViewById(R.id.footer_img_audio_next);
	    footerAudioPlayNext.setImageResource(R.drawable.ic_media_next);
	    footerAudioTextView = (TextView) mAct.findViewById(R.id.footer_audio_info);
	    footerAudioNumberTextView = (TextView) mAct.findViewById(R.id.footer_audio_number);
	    
	    // init audio seek bar
		seekBarProgress = (SeekBar)mAct.findViewById(R.id.footer_img_audio_seek_bar);	
		seekBarProgress.setMax(99); // It means 100% .0-99
		seekBarProgress.setProgress(mProgress);
		
		// set seek bar listener
		seekBarProgress.setOnSeekBarChangeListener(new OnSeekBarChangeListener() 
		{
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) 
			{
				if( AudioPlayer.mMediaPlayer != null  )
				{
					int mPlayPosition = (int) (((float)(mediaFileLengthInMilliseconds / 100)) * seekBar.getProgress());
					AudioPlayer.mMediaPlayer.seekTo(mPlayPosition);
				}
				
			}
			
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {}
			
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {}
		});

        // for showing footer when playing audio
        if( (AudioPlayer.mPlayerState != AudioPlayer.PLAYER_AT_STOP) &&
        	!Util.isEmptyString(AudioPlayer.mAudioStr)                  )
        {
    		setFooterAudioControl(AudioPlayer.mAudioStr);
        }
    }
    
    static String getFooterMessage()
    {
    	String str = mAct.getResources().getText(R.string.footer_checked).toString() + 
			        "/" +
			        mAct.getResources().getText(R.string.footer_total).toString() +
				       ": " +
			        mDb.getCheckedNotesCount() + 
					   "/" +
					mDb.getNotesCount();
    	return str;
    }
    
    static void setFooterAudioControl(String string)
    {
    	// show audio icon
    	footer_audio_control.setVisibility(View.VISIBLE);
    	
    	// seek bar behavior is not like other control item
    	//, it is seen when changing drawer, so set invisible at xml 
    	seekBarProgress.setVisibility(View.VISIBLE); 
    	
    	// show playing item
    	strFooterAudioMessage =	mAct.getResources().getString(R.string.menu_button_play) +
			    				": " + "#" +
			    				(AudioPlayer.mAudioIndex +1);	    	
    	
    	footerAudioNumberTextView.setText(strFooterAudioMessage);
    	
	    // set footer message with audio name
    	footerAudioTextView.setText(Util.getDisplayNameByUriString(string, mAct));
		// set marquee
    	footerAudioTextView.setSelected(true);
		
		// update status
	    UtilAudio.updateFooterAudioState(footerAudioPlayAndPause,footerAudioTextView);
	    
	    // set audio play and pause control image
	    footerAudioPlayAndPause.setOnClickListener(new View.OnClickListener() 
	    {
			@Override
			public void onClick(View v) 
			{
				UtilAudio.startAudioPlayer(DrawerActivity.mDrawerActivity);
				// update status
			    UtilAudio.updateFooterAudioState(footerAudioPlayAndPause,footerAudioTextView);
			}
		});
	    
	    // play previous audio 
	    footerAudioPlayPrevious.setOnClickListener(new View.OnClickListener() 
	    {
			@Override
			public void onClick(View v) 
			{
				AudioPlayer.willPlayNext = false;
				AudioPlayer.playPreviousAudio();
				// update status
			    UtilAudio.updateFooterAudioState(footerAudioPlayAndPause,footerAudioTextView);
			}
		});
	    
	    // play next audio
	    footerAudioPlayNext.setOnClickListener(new View.OnClickListener() 
	    {
			@Override
			public void onClick(View v) 
			{
				AudioPlayer.willPlayNext = true;
				AudioPlayer.playNextAudio();
				// update status
			    UtilAudio.updateFooterAudioState(footerAudioPlayAndPause,footerAudioTextView);
			}
		});	    
    }
	
    static int mHighlightPosition;
    
	class ItemAdapter extends SimpleDragSortCursorAdapter 
	{
		public ItemAdapter(Context context, int layout, Cursor c,
							String[] from, int[] to, int flags) 
		{
			super(context, layout, c, from, to, flags);
		}

		private class ViewHolder {
			public TextView audioName;
			public TextView textTitle;
			public TextView textBody;
			public TextView textTime;
			public View audioBlock;
			public ImageView imageAudio;
			public ImageView imagePicture;
			public ImageView imageCheck;
			public ImageView imageDragger;
			public ProgressBar progressBar;
		}

		@Override
		public int getCount() {
			mDb.doOpenByDrawerTabsTableId(DB.getFocus_DrawerTabsTableId());
			int count = mDb.getNotesCount();
			mDb.doClose();
			return count;
		}

		@Override
		public Object getItem(int position) {
			return position;
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {
			View view = convertView;
			final ViewHolder holder;
			if (convertView == null) 
			{
				view = getActivity().getLayoutInflater().inflate(R.layout.activity_main_list_row, parent, false);
				//04-23 23:00:02.226: E/AndroidRuntime(27214): java.lang.NullPointerException: Attempt to invoke virtual method 'android.view.LayoutInflater android.support.v4.app.FragmentActivity.getLayoutInflater()' on a null object reference

				holder = new ViewHolder();
				holder.audioName = (TextView) view.findViewById(R.id.row_audio_name);
				holder.textTitle = (TextView) view.findViewById(R.id.row_title);
				holder.textBody = (TextView) view.findViewById(R.id.row_body);
				holder.textTime = (TextView) view.findViewById(R.id.row_time);
				holder.imageCheck= (ImageView) view.findViewById(R.id.img_check);
				holder.audioBlock = view.findViewById(R.id.audio_block);
				holder.imageAudio = (ImageView) view.findViewById(R.id.img_audio);
				holder.imagePicture = (ImageView) view.findViewById(R.id.img_picture);
				holder.imageDragger = (ImageView) view.findViewById(R.id.img_dragger);
				holder.progressBar = (ProgressBar) view.findViewById(R.id.img_progress);
				view.setTag(holder);
			} 
			else 
			{
				holder = (ViewHolder) view.getTag();
			}

			mDb.doOpenByDrawerTabsTableId(DB.getFocus_DrawerTabsTableId());
			
			// show check box, title , picture
			holder.textTitle.setText(mDb.getNoteTitle(position));
			holder.textTitle.setTextColor(Util.mText_ColorArray[mStyle]);

			// set audio name
			String audio_name = null;
			if(!Util.isEmptyString(mDb.getNoteAudioUri(position)))
			{
				String uriString = mDb.getNoteAudioUri(position);
				System.out.println("NoteFragment / ItemAdapter / _getView" );
//				System.out.println("    audio uriString = " + uriString);
				audio_name = Util.getDisplayNameByUriString(uriString, getActivity());
			}
			
//			System.out.println("    audio_name = " + audio_name);
			holder.audioName.setText(audio_name);
			holder.audioName.setTextSize(12.0f);
			
			// show audio highlight
			if( (DrawerActivity.mCurrentPlaying_DrawerIndex == DrawerActivity.mFocusDrawerPos) && 
				(DrawerActivity.mCurrentPlaying_NotesTblId == 
				 	Integer.valueOf(Util.getPref_lastTimeView_NotesTableId(getActivity())) ) &&
				(position == AudioPlayer.mAudioIndex)  &&
				(AudioPlayer.mMediaPlayer != null) &&
				(AudioPlayer.mPlayerState != AudioPlayer.PLAYER_AT_STOP) &&
				(AudioPlayer.mPlayMode == AudioPlayer.CONTINUE_MODE))
			{
				mHighlightPosition = position;
//				holder.textTitle.setTextColor(Color.argb(0xff,0xff,0x80,0x00));
				holder.audioBlock.setBackgroundColor(Color.argb(0x80,0xff,0x80,0x00));
//				holder.audioName.setTextColor(Color.argb(0xff,0xff,0x80,0x00));
				holder.audioName.setTextColor(Util.mText_ColorArray[mStyle]);
				holder.audioBlock.setVisibility(View.VISIBLE);
				holder.imageAudio.setVisibility(View.VISIBLE);
				holder.imageAudio.setImageResource(R.drawable.ic_audio_selected);
			}
			else
			{
//				holder.audioBlock.setBackgroundColor(Util.mBG_ColorArray[mStyle]);
				if(!Util.isEmptyString(mDb.getNoteAudioUri(position)))
				{
					holder.audioBlock.setBackgroundColor(Color.argb(0x80,0x80,0x80,0x80));
					holder.audioName.setTextColor(Util.mText_ColorArray[mStyle]);
				}
//				holder.audioName.setTextColor(Color.argb(0xff,0xff,0xff,0xff));
				holder.audioBlock.setVisibility(View.VISIBLE);
				holder.imageAudio.setVisibility(View.VISIBLE);
				holder.imageAudio.setImageResource(R.drawable.ic_lock_ringer_on);
			}
			
			// audio icon and block
			if(Util.isEmptyString(mDb.getNoteAudioUri(position)))
			{
				holder.imageAudio.setVisibility(View.INVISIBLE);
				holder.audioBlock.setVisibility(View.INVISIBLE);
			}
			
			// image icon
			System.out.println("-");
			System.out.println("-mDb.getNotePictureUri(position) = " + mDb.getNotePictureUri(position));
			System.out.println("-");
			holder.imagePicture.setVisibility(View.VISIBLE);
			UilCommon.imageLoader.displayImage(mDb.getNotePictureUri(position) ,
									 holder.imagePicture,
									 mStyle%2 == 1 ? 
										 UilCommon.optionsForRounded_light:
										 UilCommon.optionsForRounded_dark,
									 new SimpleImageLoadingListener() 
									 {
										 @Override
										 public void onLoadingStarted(String imageUri, View view) {
											 holder.imagePicture.setVisibility(View.GONE);
											 holder.progressBar.setProgress(0);
											 holder.progressBar.setVisibility(View.VISIBLE);
										 }

										 @Override
										 public void onLoadingFailed(String imageUri, View view,
												 FailReason failReason) {
											 holder.progressBar.setVisibility(View.GONE);
											 holder.imagePicture.setVisibility(View.VISIBLE);
										 }

										 @Override
										 public void onLoadingComplete(String imageUri, View view, 
												 Bitmap loadedImage) {
											 holder.progressBar.setVisibility(View.GONE);
											 holder.imagePicture.setVisibility(View.VISIBLE);
										 }
									 }, 
									 new ImageLoadingProgressListener() 
									 {
										 @Override
										 public void onProgressUpdate(String imageUri, View view, int current,
												 int total) {
											 holder.progressBar.setProgress(Math.round(100.0f * current / total));
										 }
									 }	);									 
        
			
			// show body or not
		  	mPref_show_note_attribute = getActivity().getSharedPreferences("show_note_attribute", 0);
		  	if(mPref_show_note_attribute.getString("KEY_SHOW_BODY", "yes").equalsIgnoreCase("yes"))
		  	{
		  		holder.textBody.setText(mDb.getNoteBody(position));
		  		
		  		// test only: enabled for showing picture path
//				if(!Util.isEmptyString(mDb.getNoteBody(position)))
//					holder.textBody.setText(mDb.getNoteBody(position));
//				else
//					holder.textBody.setText(mDb.getNotePictureUri(position));

		  		// test only: enabled for showing audio path
//				if(!Util.isEmptyString(mDb.getNoteBody(position)))
//				{
//					holder.textBody.setText(mDb.getNoteBody(position));
//				}
//				else
//				{
//					holder.textBody.setTextSize(12);
//					holder.textBody.setText(mDb.getNoteAudioUri(position));
//				}
		  		
				holder.textBody.setTextColor(Util.mText_ColorArray[mStyle]);
				holder.textTime.setText(Util.getTimeString(mDb.getNoteCreatedTime(position)));
				holder.textTime.setTextColor(Util.mText_ColorArray[mStyle]);
		  	}
		  	else
		  	{
		  		holder.textBody.setVisibility(View.GONE);
		  		holder.textTime.setVisibility(View.GONE);
		  	}
		  	
		  	// dragger
		  	mPref_show_note_attribute = getActivity().getSharedPreferences("show_note_attribute", 0);
		  	if(mPref_show_note_attribute.getString("KEY_ENABLE_DRAGGABLE", "yes").equalsIgnoreCase("yes"))
		  		holder.imageDragger.setVisibility(View.VISIBLE); 
		  	else
		  		holder.imageDragger.setVisibility(View.GONE); 
			
		  	// marking
			if( mDb.getNoteMarking(position) == 1)
				holder.imageCheck.setBackgroundResource(mStyle%2 == 1 ?
		    			R.drawable.btn_check_on_holo_light:
		    			R.drawable.btn_check_on_holo_dark);	
			else
				holder.imageCheck.setBackgroundResource(mStyle%2 == 1 ?
						R.drawable.btn_check_off_holo_light:
						R.drawable.btn_check_off_holo_dark);
			
			mDb.doClose();
			
			return view;
		}
	}

	/*******************************************
	 * 					menu
	 *******************************************/
    // Menu identifiers
    static final int CHECK_ALL = R.id.CHECK_ALL;
    static final int UNCHECK_ALL = R.id.UNCHECK_ALL;
    static final int MOVE_CHECKED_NOTE = R.id.MOVE_CHECKED_NOTE;
    static final int COPY_CHECKED_NOTE = R.id.COPY_CHECKED_NOTE;
    static final int MAIL_CHECKED_NOTE = R.id.MAIL_CHECKED_NOTE;
    static final int DELETE_CHECKED_NOTE = R.id.DELETE_CHECKED_NOTE;
    
	int noteCount;
    
    @Override public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) 
        {
	        case CHECK_ALL:
	        	checkAll(1); 
	            return true;
	        case UNCHECK_ALL:
	        	checkAll(0); 
	            return true;
	        case MOVE_CHECKED_NOTE:
	        case COPY_CHECKED_NOTE:
	    		if(!noItemChecked())
	        	{
		    		mDb.doOpenByDrawerTabsTableId(DB.getFocus_DrawerTabsTableId());
		    		String copyItems[] = new String[mDb.getCheckedNotesCount()];
		    		String copyItemsPicture[] = new String[mDb.getCheckedNotesCount()];
		    		String copyItemsAudio[] = new String[mDb.getCheckedNotesCount()];
		    		String copyItemsBody[] = new String[mDb.getCheckedNotesCount()];
		    		Long copyItemsTime[] = new Long[mDb.getCheckedNotesCount()];
		    		int cCopy = 0;
		    		for(int i=0;i<mDb.getNotesCount();i++)
		    		{
		    			if(mDb.getNoteMarking(i) == 1)
		    			{
		    				copyItems[cCopy] = mDb.getNoteTitle(i);
		    				copyItemsPicture[cCopy] = mDb.getNotePictureUri(i);
		    				copyItemsAudio[cCopy] = mDb.getNoteAudioUri(i);
		    				copyItemsBody[cCopy] = mDb.getNoteBody(i);
		    				copyItemsTime[cCopy] = mDb.getNoteCreatedTime(i);
		    				cCopy++;
		    			}
		    		}
		    		mDb.doClose();
		           
		    		if(item.getItemId() == MOVE_CHECKED_NOTE)
		    			operateCheckedTo(copyItems, copyItemsPicture, copyItemsAudio, copyItemsBody, copyItemsTime, MOVE_TO); // move to
		    		else if(item.getItemId() == COPY_CHECKED_NOTE)
			    		operateCheckedTo(copyItems, copyItemsPicture, copyItemsAudio, copyItemsBody, copyItemsTime, COPY_TO);// copy to
		    			
	        	}
	        	else
	    			Toast.makeText(getActivity(),
							   R.string.delete_checked_no_checked_items,
							   Toast.LENGTH_SHORT)
					     .show();
	            return true;
	            
	        case MAIL_CHECKED_NOTE:
	    		if(!noItemChecked())
	        	{
		        	// set Sent string Id
					List<Long> rowArr = new ArrayList<Long>();
					List<String> pictureFileNameList = new ArrayList<String>();
	            	int j=0;
		    		mDb.doOpenByDrawerTabsTableId(DB.getFocus_DrawerTabsTableId());
		    		for(int i=0;i<mDb.getNotesCount();i++)
		    		{
		    			if(mDb.getNoteMarking(i) == 1)
		    			{
		    				rowArr.add(j,(long) mDb.getNoteId(i));
		    				j++;
		    				
		    				String picFile = mDb.getNotePictureUriById((long) mDb.getNoteId(i));
		    				if((picFile != null) && (picFile.length() > 0))
		    					pictureFileNameList.add(picFile);
		    			}
		    		}
		    		mDb.doClose();
		    		
		    		// message
		    		Intent intentMail = new Intent(getActivity(), SendMailAct.class);
		    		String extraStr = Util.getStringWithXmlTag(rowArr);
		    		extraStr = Util.addXmlTag(extraStr);
		    		intentMail.putExtra("SentString", extraStr);
		    		
		    		// picture array
		    		int cnt = pictureFileNameList.size();
		    		String pictureFileNameArr[] = new String[cnt];
		    		for(int i=0; i < cnt ; i++ )
		    		{
		    			pictureFileNameArr[i] = pictureFileNameList.get(i);
		    		}
		    		intentMail.putExtra("SentPictureFileNameArray", pictureFileNameArr );
		    		
					startActivity(intentMail);
	        	}
	        	else
	    			Toast.makeText(getActivity(),
							   R.string.delete_checked_no_checked_items,
							   Toast.LENGTH_SHORT)
						 .show();
	        	return true;
	        	
	        case DELETE_CHECKED_NOTE:
	        	if(!noItemChecked())
	        		deleteCheckedNotes();
	        	else
	    			Toast.makeText(getActivity(),
	    						   R.string.delete_checked_no_checked_items,
	    						   Toast.LENGTH_SHORT)
	    				 .show();
	            return true;     

            default:
                return super.onOptionsItemSelected(item);
        }
    }
    
	
	static public void swap()
	{
        mDb.doOpenByDrawerTabsTableId(DB.getFocus_DrawerTabsTableId());
        int startCursor = mDb.getNotesCount()-1;
        mDb.doClose();
        int endCursor = 0;
		
		//reorder data base storage for ADD_NEW_TO_TOP option
		int loop = Math.abs(startCursor-endCursor);
		for(int i=0;i< loop;i++)
		{
			swapRows(startCursor,endCursor);
			if((startCursor-endCursor) >0)
				endCursor++;
			else
				endCursor--;
		}
	}
    
	/**
	 *  check all or uncheck all
	 */
	public void checkAll(int action) 
	{
		mDb.doOpenByDrawerTabsTableId(DB.getFocus_DrawerTabsTableId());
		for(int i=0;i<mDb.getNotesCount();i++)
		{
			Long rowId = mDb.getNoteId(i);
			String noteTitle = mDb.getNoteTitle(i);
			String pictureUri = mDb.getNotePictureUri(i);
			String audioUri = mDb.getNoteAudioUri(i);
			String noteBody = mDb.getNoteBody(i);
			mDb.updateNote(rowId, noteTitle, pictureUri, audioUri, "", noteBody , action, 0);//??? // action 1:check all, 0:uncheck all
		}
		mDb.doClose();
		mItemAdapter.notifyDataSetChanged();
		setFooter();
	}
	
    /**
     *   operate checked to: move to, copy to
     * 
     */
	void operateCheckedTo(final String[] copyItems, final String[] copyItemsPicture, 
						  final String[] copyItemsAudio, final String[] copyItemsBody,
						  final Long[] copyItemsTime, final int action)
	{
		//list all tabs
		mDb.doOpenByDrawerTabsTableId(DB.getFocus_DrawerTabsTableId());
		int tabCount = DB.getTabsCount();
		final String[] tabNames = new String[tabCount];
		final int[] tableIds = new int[tabCount];
		for(int i=0;i<tabCount;i++)
		{
			tabNames[i] = DB.getTabTitle(i);
			tableIds[i] = DB.getTab_NotesTableId(i);
		}
		tabNames[TabsHostFragment.mCurrentTabIndex] = tabNames[TabsHostFragment.mCurrentTabIndex] + " *"; // add mark to current page 
		mDb.doClose();
		   
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which) {
				
				//keep original table id
				String curTableNum = DB.getNotes_TableId();

				//copy checked item to destination tab
				String destTableNum = String.valueOf(tableIds[which]);
				DB.setFocus_NotesTableId(destTableNum);
				mDb.doOpenByDrawerTabsTableId(DB.getFocus_DrawerTabsTableId());
				for(int i=0;i< copyItems.length;i++)
				{
					int marking = 0;
					// default marking of picture or audio is 1
					if( (!Util.isEmptyString(copyItemsPicture[i])) || (!Util.isEmptyString(copyItemsAudio[i])))
						marking = 1;
					
					mDb.insertNote(copyItems[i],copyItemsPicture[i], copyItemsAudio[i], "", copyItemsBody[i],marking, copyItemsTime[i]); //???
				}
				mDb.doClose();
				
				//recover to original table id
				if(action == MOVE_TO)
				{
					DB.setFocus_NotesTableId(curTableNum);
					mDb.doOpenByDrawerTabsTableId(DB.getFocus_DrawerTabsTableId());
					//delete checked
					for(int i=0;i< mDb.getNotesCount() ;i++)
					{
						if(mDb.getNoteMarking(i) == 1)
							mDb.deleteNote(mDb.getNoteId(i));
					}
					mDb.doClose();
					mItemAdapter.notifyDataSetChanged();
					setFooter();
				}
				else if(action == COPY_TO)
				{
					DB.setFocus_NotesTableId(curTableNum);
					if(destTableNum.equalsIgnoreCase(curTableNum))
					{
						mItemAdapter.notifyDataSetChanged();
						setFooter();
					}
				}
				
				dialog.dismiss();
			}
		};
		
		if(action == MOVE_TO)
			builder.setTitle(R.string.checked_notes_move_to_dlg);
		else if(action == COPY_TO)
			builder.setTitle(R.string.checked_notes_copy_to_dlg);
		
		builder.setSingleChoiceItems(tabNames, -1, listener)
		  	.setNegativeButton(R.string.btn_Cancel, null);
		
		// override onShow to mark current page status
		AlertDialog alertDlg = builder.create();
		alertDlg.setOnShowListener(new OnShowListener() {
			@Override
			public void onShow(DialogInterface dlgInterface) {
				// add mark for current page
				Util util = new Util(getActivity());
				util.addMarkToCurrentPage(dlgInterface);
			}
		});
		alertDlg.show();
	}
	
	
	/**
	 * delete checked notes
	 */
	public void deleteCheckedNotes()
	{
		final Context context = getActivity();

		mPref_delete_warn = context.getSharedPreferences("delete_warn", 0);
    	if(mPref_delete_warn.getString("KEY_DELETE_WARN_MAIN","enable").equalsIgnoreCase("enable") &&
           mPref_delete_warn.getString("KEY_DELETE_CHECKED_WARN","yes").equalsIgnoreCase("yes"))
    	{
			Util util = new Util(getActivity());
			util.vibrate();
    		
    		// show warning dialog
			Builder builder = new Builder(context);
			builder.setTitle(R.string.delete_checked_note_title)
					.setMessage(R.string.delete_checked_message)
					.setNegativeButton(R.string.btn_Cancel, 
							new OnClickListener() 
					{	@Override
						public void onClick(DialogInterface dialog, int which) 
						{/*cancel*/} })
					.setPositiveButton(R.string.btn_OK, 
							new OnClickListener() 
					{	@Override
						public void onClick(DialogInterface dialog, int which) 
						{
							mDb.doOpenByDrawerTabsTableId(DB.getFocus_DrawerTabsTableId());
							for(int i=0;i< mDb.getNotesCount() ;i++)
							{
								if(mDb.getNoteMarking(i) == 1)
									mDb.deleteNote(mDb.getNoteId(i));
							}
							mDb.doClose();
							
							// Stop Play/Pause if current tab's item is played and is not at Stop state
							if(AudioPlayer.mAudioIndex == NoteFragment.mHighlightPosition)
								UtilAudio.stopAudioIfNeeded();
							
							mItemAdapter.notifyDataSetChanged();
							setFooter();
						}
					});
			
	        AlertDialog d = builder.create();
	        d.show();
    	}
    	else
    	{
    		// not show warning dialog
			mDb.doOpenByDrawerTabsTableId(DB.getFocus_DrawerTabsTableId());
			for(int i=0;i< mDb.getNotesCount() ;i++)
			{
				if(mDb.getNoteMarking(i) == 1)
					mDb.deleteNote(mDb.getNoteId(i));
			}
			mDb.doClose();
			mItemAdapter.notifyDataSetChanged();
			setFooter();
    	}
	}
    
	@Override
	public void onDestroy() {
		mDb.doClose();
		super.onDestroy();
	}
	
	boolean noItemChecked()
	{
		mDb.doOpenByDrawerTabsTableId(DB.getFocus_DrawerTabsTableId());
		int checkedItemCount = mDb.getCheckedNotesCount(); 
		mDb.doClose();
		return (checkedItemCount == 0);
	}
	
	/*
	 * inner class for note list loader
	 */
	public static class NoteListLoader extends AsyncTaskLoader<List<String>> 
	{
		List<String> mApps;

		public NoteListLoader(Context context) {
			super(context);
		}

		@Override
		public List<String> loadInBackground() {
			List<String> entries = new ArrayList<String>();
			return entries;
		}

		@Override
		protected void onStartLoading() {
			forceLoad();
		}
	}

	/*
	 * 	inner class for note list adapter
	 */
	public static class NoteListAdapter extends ArrayAdapter<String> 
	{
		public NoteListAdapter(Context context) {
			super(context, android.R.layout.simple_list_item_1);
		}
		public void setData(List<String> data) {
			clear();
			if (data != null) {		
					addAll(data);
			}
		}
	}

	
	// set seek bar progress
	static int mProgress;
    static void primarySeekBarProgressUpdater() 
    {
    	mProgress = (int)(((float)AudioPlayer.mMediaPlayer.getCurrentPosition()/mediaFileLengthInMilliseconds)*100);
    	seekBarProgress.setProgress(mProgress); // This math construction give a percentage of "was playing"/"song length"
    }
}
