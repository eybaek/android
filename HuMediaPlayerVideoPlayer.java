package com.mobile.mediaplayer.activities.player;

import java.util.Formatter;
import java.util.Locale;

import kr.co.iplayer.control.PlayControl.PlayerEvent;
import kr.co.iplayer.control.VideoControl;
import kr.co.iplayer.factory.PlayFactory;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.humax.mxlib.ra.RA;
import com.mobile.mediaplayer.AppConfig;
import com.mobile.mediaplayer.activities.HuMediaPlayerHubActivity;
import com.mobile.mediaplayer.data.ContentItem;
import com.mobile.mediaplayer.data.ContentList;
import com.mobile.mediaplayer.data.PlayList;
import com.mobile.mediaplayer.data.factory.AppDataDefine;
import com.mobile.mediaplayer.data.item.WoonContentItem;
import com.mobile.mediaplayer.player.factory.AVPlayListController;
import com.mobile.mediaplayer.receiver.HuMediaPlayerReceiver;
import com.mobile.mediaplayer.widget.HuLoadingLayout;
import com.mobile.mediaplayer.widget.HuSeekBar;
import com.mobile.mediaplayer.widget.dialog.HuDMCDialog;
import com.mobile.mediaplayer.widget.dialog.contextmenu.HuLaunchDefaultAppDialog;
import com.mobile.mediaplayer.widget.dialog.contextmenu.HuLaunchDefaultAppDialog.DialogEventListener;
import com.mobile.mediaplayer.widget.dialog.contextmenu.HuToastMessage;
import com.mobile.mediaplayer.widget.dialog.contextmenu.MessageDialog_TwoButton;
import com.mobile.mediaplayer.widget.event.HuDialogClickListener;
import com.mobile.mediaplayerphone.R;

public class HuMediaPlayerVideoPlayer extends Activity {


	private static Activity			mActivity;
	private static ContentList		mContentList;
	private static PlayList			mPlayList;
	private static int				mIndex;

	private AVPlayListController	mPlayController;
	private SurfaceView				mSurfaceView;

	// UI ------------------------------------
	private RelativeLayout		mRootView;
	private RelativeLayout		mButtonLayout,
								mProgressLayout;

	private RelativeLayout.LayoutParams	mButtonLayout_RelativeLayoutParams;

	private HuLoadingLayout		mLoadingLayout;
	private View				mPauseView;
	private ProgressBar			mProgressBar;

	private ImageButton 		mListBtn,
								mRendererBtn,
								mHomeBtn;

	private Button				mPrevBtn,
								mPlayBtn,
								mNextBtn;
	private TextView			mTitleView,
								mPlayingTimeView,
								mCurrentPositionView,
								mDurationView;

	StringBuilder               mFormatBuilder;
	Formatter                   mFormatter;

	private static final int    sDefaultTimeout = 3000;
	private int					TRANSCODING_MAX_COUNT = 15;
    private static final int    FINISH_ACTIVITY = -3,
    							FADE_OUT		= -2,
    							STOP_LOADING	= -1,
    							SHOW_LOADING	= 1,
    							SHOW_PROGRESS 	= 2,
    							SHOW_CENTER_PAUSE = 3,
    							HIDE_CENTER_PAUSE = 4,
    							CHECK_HLS_PLAYBACK = 5;

	private boolean				mShowing = true;
	private boolean				mDragging;
	private boolean				mIsTranscoding;
	private boolean 			mLoadingShowing = true;
	// -----------------------------------------------

	String	TAG = HuMediaPlayerVideoPlayer.class.getSimpleName();


	// ------------------------------------------------
	public static void startActivity(final Context ctx, ContentList list, int idx) {
		mActivity = (Activity)ctx;
		mContentList = list;
		mPlayList = null;
		mIndex = idx;

		Intent intent = new Intent(mActivity, HuMediaPlayerVideoPlayer.class);
		mActivity.startActivity(intent);
	}
	// ------------------------------------------------
		public static void startActivity(final Context ctx, PlayList list, int idx) {
			mActivity = (Activity)ctx;
			mContentList = null;
			mPlayList = list;
			mIndex = idx;

			Intent intent = new Intent(mActivity, HuMediaPlayerVideoPlayer.class);
			mActivity.startActivity(intent);
		}
	// ------------------------------------------------
	BroadcastReceiver mReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {

			Log.d(TAG, "________Receiver Video :" + intent.getAction());

			if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
				HuMediaPlayerVideoPlayer.this.finish();
			}
		}
	};

	// ------------------------------------------------
	// Activity
	// ------------------------------------------------
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView (R.layout.mp_activity_player_video);
		mActivity = HuMediaPlayerVideoPlayer.this;

		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		getControlByXml();
		VideoControl mPlayControl = (VideoControl) PlayFactory.createLocalMediaPlayer(HuMediaPlayerVideoPlayer.this, PlayFactory.ItemTypeVideo);
		mPlayControl.setVideoView(mSurfaceView);

		if(mContentList != null) {
			mPlayController = new AVPlayListController(mPlayControl, mPlayerEvent, mContentList, mIndex, AppDataDefine.ItemClassVideo);
		} else {
			mPlayController = new AVPlayListController(mPlayControl, mPlayerEvent, mPlayList, mIndex, AppDataDefine.ItemClassVideo);
		}
		mPlayController.start();

		setControlEvent();

		// ---------- Receiver
		IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
		filter.setPriority(100000);
		registerReceiver(mReceiver, filter);
		// ----------
	}

	@Override
	protected void onNewIntent(Intent intent) {

		mPlayController.start();

		super.onNewIntent(intent);
	}

	@Override
	protected void onStart() {
		super.onStart();
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onStop() {
		release();
		try {
			unregisterReceiver(mReceiver);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		}
		finish();
		super.onStop();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {

            if (mShowing) {
            	if  ( event.getY() > 50 && event.getY() < 700 )
            		hide();
            } else {
            	show(sDefaultTimeout);
            }
        }
        return true;
	}

	// ------------------------------------------------
	// UI Control
	// ------------------------------------------------
	private void getControlByXml() {
		mLoadingLayout = (HuLoadingLayout) findViewById(R.id.video_loading_layout);
		mRootView = (RelativeLayout) findViewById(R.id.video_root_view);
		mButtonLayout = (RelativeLayout) findViewById(R.id.video_button_layout);
		mProgressLayout = (RelativeLayout) findViewById(R.id.video_progress_layout);

		mSurfaceView = (SurfaceView) findViewById(R.id.video_surface_view);
		mPauseView = (View) findViewById(R.id.video_control_center_pause_view);	setEventListener(mPauseView);
		mListBtn = (ImageButton) findViewById(R.id.video_control_list_btn);			setEventListener(mListBtn);
		mRendererBtn = (ImageButton) findViewById(R.id.video_control_renderer_btn);	setEventListener(mRendererBtn);
		mHomeBtn = (ImageButton) findViewById(R.id.video_control_home_btn);			setEventListener(mHomeBtn);
		mPrevBtn = (Button) findViewById(R.id.video_control_prev_btn);			setEventListener(mPrevBtn);
		mPlayBtn = (Button) findViewById(R.id.video_control_play_btn);			setEventListener(mPlayBtn);
		mNextBtn = (Button) findViewById(R.id.video_control_next_btn);			setEventListener(mNextBtn);

		mTitleView = (TextView) findViewById(R.id.video_control_title_view);
		mPlayingTimeView = (TextView) findViewById(R.id.video_control_playing_time);
		mDurationView = (TextView) findViewById(R.id.video_control_duration_time);

		mRootView.bringChildToFront(mSurfaceView);
//		mRootView.invalidate();

		mTitleView.setSelected(true);


		mProgressBar = (ProgressBar) findViewById(R.id.video_control_progress);
		if (mProgressBar != null) {
			if(mProgressBar instanceof HuSeekBar) {
				HuSeekBar seeker = (HuSeekBar) mProgressBar;
				seeker.setOnSeekBarChangeListener(mSeekbarListener);
				setEventListener(seeker);
				seeker.requestFocus();
			}
			mProgressBar.setMax(1000);
		}

		mFormatBuilder = new StringBuilder();
		mFormatter = new Formatter(mFormatBuilder, Locale.getDefault());
	}

	private void setControlEvent() {
		// Send To
		if (mRendererBtn != null) {

			ContentItem item = mPlayController.getCurrentItem();
			int networkType = HuMediaPlayerReceiver.getSharedInstance().getNetworkType();

			if (item instanceof WoonContentItem) {
				if (((WoonContentItem) item).getNetworkArea() == RA.RA_NETWORK_INTERNAL) {
					mRendererBtn.setClickable(true);
					mRendererBtn.setVisibility(View.VISIBLE);

					if (networkType == HuMediaPlayerReceiver.TYPE_3G_OR_LTE) {
						Log.d(TAG, "============ Network ( 3G / LTE ) ============");
						mRendererBtn.setClickable(false);
						mRendererBtn.setVisibility(View.INVISIBLE);
					}

				} else {
					mRendererBtn.setClickable(false);
					mRendererBtn.setVisibility(View.INVISIBLE);
				}
			}
		}

		// Control View hidden
		if (mPlayController.getItemUrl().contains("m3u8"))
			mIsTranscoding = true;
		else
			mIsTranscoding = false;

		mButtonLayout_RelativeLayoutParams = (RelativeLayout.LayoutParams) mButtonLayout.getLayoutParams();
		if (mIsTranscoding) {
			mProgressLayout.setVisibility(View.INVISIBLE);
			RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			params.addRule(RelativeLayout.CENTER_HORIZONTAL);
			mButtonLayout.setLayoutParams(params);
		}

		mHandler.sendEmptyMessage(SHOW_LOADING);
	}

	// ------------------------------------------------
	// Handler & Event
	public final Handler mStatusHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
        	if (msg.what == FINISH_ACTIVITY){
        		HuMediaPlayerVideoPlayer.this.finish();

        	} else {
        		updatePausePlay();
        	}
        }
	};

	private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

        	int pos;
            switch (msg.what) {
	            case STOP_LOADING : {
	            	if (mLoadingLayout != null) {
	            		mLoadingLayout.setVisibility(View.INVISIBLE);
	            		mLoadingShowing = false;
	            	}
	            } break;

	            case SHOW_LOADING : {
	            	hide();
	            	if (mLoadingLayout != null) {
	            		mLoadingLayout.setVisibility(View.VISIBLE);
	            		mLoadingShowing = true;
	            	}
	            } break;

                case FADE_OUT: {
                    hide();
                } break;

                case SHOW_PROGRESS: {
                    pos = setProgress();
                    if (!mDragging && mShowing && mPlayController.isPlaying()) {
                        msg = obtainMessage(SHOW_PROGRESS);
                        sendMessageDelayed(msg, 1000 - (pos % 1000));
                    }
                } break;

                case SHOW_CENTER_PAUSE : {
                	mPauseView.setVisibility(View.VISIBLE);
                } break;

                case HIDE_CENTER_PAUSE : {
                	mPauseView.setVisibility(View.INVISIBLE);
                } break;

                case CHECK_HLS_PLAYBACK : {
                	setProgress();
                	if (mPlayController.isPlaying()) {
                		removeMessages(CHECK_HLS_PLAYBACK);
                		msg = obtainMessage(CHECK_HLS_PLAYBACK);
                        sendMessageDelayed(msg, 1000);
                    }
                }
            }
        }
    };

    private void setEventListener(View v) {
		if (v != null)
			v.setOnClickListener(mClickListener);
	}

	private OnSeekBarChangeListener mSeekbarListener = new OnSeekBarChangeListener() {
		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {
//            show(3600000); // why..?
            show(sDefaultTimeout);

            mDragging = true;
            mHandler.removeMessages(SHOW_PROGRESS);

            if (mProgressBar instanceof HuSeekBar) {
            	HuSeekBar seeker = (HuSeekBar) mProgressBar;
                seeker.setShowTime(true);
            }
		}

		@Override
		public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (!fromUser) {
                return;
            }

            long duration = mPlayController.getDuration();
            long newposition = (duration * progress) / 1000L;
            mPlayController.seekTo( (int) newposition);

            if (mProgressBar instanceof HuSeekBar) {
            	HuSeekBar seeker = (HuSeekBar) mProgressBar;
                seeker.setCurPosition((int)newposition);
            }
		}

		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {
            mDragging = false;
            setProgress();
            updatePausePlay();
            show(sDefaultTimeout);

            mHandler.sendEmptyMessage(SHOW_PROGRESS);

            if (mProgressBar instanceof HuSeekBar) {
            	HuSeekBar seeker = (HuSeekBar) mProgressBar;
                seeker.setShowTime(false);
            }
		}
	};

	private View.OnClickListener	mClickListener = new OnClickListener() { @Override
		public void onClick(View v) {
			switch (v.getId()) {
				case R.id.video_control_list_btn : {
					finish();
				} break;

				case R.id.video_control_renderer_btn : {
					ContentItem item = mPlayController.getCurrentItem();
					final boolean bRefresh = item.getType() == AppDataDefine.ItemContentDlna;
					HuDMCDialog dlg = new HuDMCDialog( HuMediaPlayerVideoPlayer.this , item  , new HuDMCDialog.ExitEventListener() {
						@Override
						public void onResult(int error) {
						}
					});
					dlg.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
					dlg.setCurrentPlayTime(mPlayController.getCurrentPosition());
					dlg.showDialog();
				} break;

				case R.id.video_control_home_btn: {
					HuMediaPlayerHubActivity.startActivityFullscreen(HuMediaPlayerVideoPlayer.this, mStatusHandler);
				} break;

				case R.id.video_control_prev_btn : {
					boolean isPlayable = mPlayController.prevPlay();
					if (!isPlayable){
						showToastMessage(getString(R.string.str_mobile_0076_id));
					}else {
						mHandler.sendEmptyMessage(SHOW_LOADING);
						//HuMediaPlayerVideoPlayer.this.finish();
						//mPlayController.setStatus(AVPlayListController.STATE_LOAD);
					}
				} break;

				case R.id.video_control_next_btn : {
					boolean isPlayable = mPlayController.nextPlay();
					if (!isPlayable) {
						showToastMessage(getString(R.string.str_mobile_0076_id));
					} else {
						mHandler.sendEmptyMessage(SHOW_LOADING);
						//HuMediaPlayerVideoPlayer.this.finish();
						//mPlayController.setStatus(AVPlayListController.STATE_LOAD);
					}
				} break;

				case R.id.video_control_play_btn : {
					if (mPlayController.isPlaying())
						mPlayController.pause();
					else {
						if (mPlayController.getStatus() != AVPlayListController.STATE_LOAD) {

							if (mPlayController.getStatus() == AVPlayListController.STATE_STOP) {
								mPlayController.setStatus(AVPlayListController.STATE_LOAD);
								mHandler.sendEmptyMessage(SHOW_LOADING);
								mPlayController.resume();

							} else {
								mPlayController.setStatus(AVPlayListController.STATE_LOAD);
								mPlayController.play();
							}
						}
					}
				} break;
			}
		}
	};

	PlayerEvent		mPlayerEvent = new PlayerEvent() {

		@Override
		public void onStop() {
			mPlayController.setStatus(mPlayController.STATE_STOP);
			callUpdateStatusHandler();
		}

		@Override
		public void onSeekTo(int position) {
			// TODO Auto-generated method stub
		}

		@Override
		public String onRequestUri(int position) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void onPrepared(boolean prepare) {
			if (prepare) {
				mPlayController.play();
			}
			setTitle(mPlayController.getTitle());
		}

		@Override
		public void onPlay() {

			// Control View hidden
			if (mPlayController.getItemUrl().contains("m3u8"))
				mIsTranscoding = true;
			else
				mIsTranscoding = false;

			Log.d(TAG, "onPlay() mIsTranscoding:"+mIsTranscoding + " getStatus:"+mPlayController.getStatus());

			// MediaController Layout 초기화
			mProgressLayout.setVisibility(View.VISIBLE);
			mButtonLayout.setLayoutParams(mButtonLayout_RelativeLayoutParams);

			if (!mIsTranscoding) {
				mPlayController.setStatus(AVPlayListController.STATE_PLAY);

				Log.d(TAG, "onPlay() mPlayController.getDuration():"+mPlayController.getDuration());
				if (mPlayController.getDuration() < 0) {
					mProgressLayout.setVisibility(View.INVISIBLE);
					RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
					params.addRule(RelativeLayout.CENTER_HORIZONTAL);
					mButtonLayout.setLayoutParams(params);
				}

				callUpdateStatusHandler();
			} else {
				//HLS일때는 progressbar layout invisible
				mProgressLayout.setVisibility(View.INVISIBLE);
				RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
				params.addRule(RelativeLayout.CENTER_HORIZONTAL);
				mButtonLayout.setLayoutParams(params);

				// 20130426 pause 후 play 되는 시점에서 HLS 인 경우 (STATE_LOAD임) button모양을 갱신토록 한다.
				if (mPlayController.getStatus() == AVPlayListController.STATE_LOAD) {
					mPlayController.setStatus(AVPlayListController.STATE_PLAY);
					callUpdateStatusHandler();
				}
			}
		}

		@Override
		public void onPause() {
			mPlayController.setStatus(mPlayController.STATE_PAUSE);
			callUpdateStatusHandler();
		}

		@Override
		public void onError() {
			ResolveInfo resolveInfo = AppConfig.getSharedInstance().getDefualtLaunchMediaPlayerApp(mPlayController.getCurrentItem().getMediaUrl());
			if(resolveInfo == null){
				MessageDialog_TwoButton dialog = new MessageDialog_TwoButton(AppConfig.getSharedInstance().getContext());
				dialog.setDialogEvent(new HuDialogClickListener() {
					@Override
					public void onDialogClick(boolean ret) {
						if (ret) {
							try {
								AppConfig.getSharedInstance().launchExternalAppLink(mPlayController.getCurrentItem().getMediaUrl());
							} catch(Exception e) {
								e.printStackTrace();
							}
						}
					}
				});
				String msg = getString(R.string.str_mesg_4689_id);
				msg += "\n"+ getString(R.string.str_mesg_4690_id);
				dialog.showDialog(msg);
			} else {
				HuLaunchDefaultAppDialog dialog = new HuLaunchDefaultAppDialog(AppConfig.getSharedInstance().getContext());
				dialog.setResolveInfo(resolveInfo);
				dialog.setEventListener(new DialogEventListener() {

					@Override
					public void onDialogClick(boolean result) {
						if(result) {
							AppConfig.getSharedInstance().launchDefaultExternalApp(mPlayController.getCurrentItem().getMediaUrl());
						}
					}
				});
				dialog.show();
			}
		}

		@Override
		public void onCurrentVolume(int volume) {
			// TODO Auto-generated method stub
		}

		@Override
		public void onComplete() {
//			Log.d(TAG, "________onComplete");
			mPlayController.setStatus(mPlayController.STATE_STOP);
			callUpdateStatusHandler();
			finish();
		}

		@Override
		public void onBuffering(int percent) {
			// TODO Auto-generated method stub
		}

		@Override
		public void onVideoSizeChanged(int width, int height) {
			if (width > 0 && height > 0) {
				Log.d(TAG, "_____onVideoSizeChanged : " + width + " / " + height);
				mPlayController.setStatus(mPlayController.STATE_PLAY);
				callUpdateStatusHandler();

				show(sDefaultTimeout);
			}
		}

		@Override
		public void onVideoViewChanged(SurfaceHolder arg0, int arg1, int arg2,
				int arg3) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onSeekCompleted() {
			// TODO Auto-generated method stub

		}
	};


	// ------------------------------------------------
	// set UI control state..
	// ------------------------------------------------
	public void show(int timeout){
		Log.d(TAG, "________showing (" + mShowing + "), RootView (" + mRootView + ")");
		if (!mShowing && mRootView != null) {
			setProgress();

			if (mProgressBar != null && mProgressBar.isFocused() == false)
				mProgressBar.requestFocus();

		    mShowing = true;
		    mRootView.setVisibility(View.VISIBLE);
		    mRootView.bringChildToFront(mSurfaceView);

		    Log.d(TAG, "________showing (" + mShowing + "), RootView (" + mRootView.getVisibility() + ")");
		}

		mHandler.sendEmptyMessage(SHOW_PROGRESS);

		Message msg = mHandler.obtainMessage(FADE_OUT);
		if (timeout != 0) {
			mHandler.removeMessages(FADE_OUT);
			mHandler.sendMessageDelayed(msg, timeout);
		}
	}

	private void hide() {
        if (mRootView == null)
            return;

        if (mShowing) {
            try {
            	if (mHandler != null)
            		mHandler.removeMessages(SHOW_PROGRESS);
                mRootView.setVisibility(View.INVISIBLE);

            } catch (IllegalArgumentException ex) {
//                Log.w("MediaController", "already removed");

            } catch (NullPointerException e) {
//            	e.printStackTrace();
            }
            mShowing = false;
        }
	}

	private void callUpdateStatusHandler() {
		if (mStatusHandler != null)
			mStatusHandler.sendEmptyMessage(0);
	}

	// ------------------------------------------------
	// set UI control state..
	// ------------------------------------------------
	public void release() {
		mPlayController.finish();
		if(mHandler != null) {
			mHandler.removeMessages(SHOW_PROGRESS);
			if (mIsTranscoding)
				mHandler.removeMessages(CHECK_HLS_PLAYBACK);
			mHandler = null;
		}
	}

	private void updatePausePlay() {
		Log.d(TAG, "updatePausePlay isPlaying:" +mPlayController.isPlaying());

		if (mRootView == null || mPlayBtn == null)
			return;

		if (mPlayController.getStatus() == AVPlayListController.STATE_START) {
			mHandler.sendEmptyMessage(STOP_LOADING);

		} else {
			if (mPlayController.isPlaying()) {
				mHandler.sendEmptyMessage(SHOW_PROGRESS);
				mHandler.sendEmptyMessage(STOP_LOADING);
				mHandler.sendEmptyMessage(HIDE_CENTER_PAUSE);
//				mPauseView.setVisibility(View.INVISIBLE);
				mPlayBtn.setBackgroundResource(R.drawable.selector_button_play_pause);

				if (mIsTranscoding)
					mHandler.sendEmptyMessage(CHECK_HLS_PLAYBACK);

			} else {
				if (mHandler != null) {
					mHandler.sendEmptyMessage(SHOW_CENTER_PAUSE);
					mHandler.sendEmptyMessageDelayed(HIDE_CENTER_PAUSE, 3000);
	//				mPauseView.setVisibility(View.VISIBLE);
				}
				mPlayBtn.setBackgroundResource(R.drawable.selector_button_play_play);

			}
		}
	}


	// -------------------------------------------------------

	public void setTitle(String title){
		if (mTitleView != null) {
			mTitleView.setText(title);
			mTitleView.setSelected(true);
		}
	}

	int  	failCount = 0,
			prevPosition = 0;
	private int setProgress() {
		if (mPlayController == null || mDragging) {
			return 0;
		}

		int position = mPlayController.getCurrentPosition();
		int duration = mPlayController.getDuration();

		if (mProgressBar != null) {
			if (duration > 0) {
				// use long to avoid overflow
				long pos = 1000L * position / duration;
				mProgressBar.setProgress( (int) pos);
			} else {
			}

			// ---------------------------------------------------
			if (mIsTranscoding) {
				Log.d(TAG, "_________position info : " + prevPosition + "/" + position + " fail(" + failCount + ")" + " isPlaying:"+mPlayController.isPlaying());
				if (/*!*/mPlayController.isPlaying() && prevPosition == position && !mLoadingShowing) {
					failCount++;

					if (failCount > TRANSCODING_MAX_COUNT) {
						Log.d(TAG, "________ HLS Finished ________");
						finish();
					}
				} else {
					failCount = 0;
				}
			}
			// ---------------------------------------------------


			mProgressBar.setSecondaryProgress(100 * 10);

			if(mProgressBar instanceof HuSeekBar) {
				HuSeekBar seeker = (HuSeekBar) mProgressBar;
				seeker.setCurPosition(position);
			}
		}

		if (mDurationView != null)
			mDurationView.setText(stringForTime(duration));
		if (mPlayingTimeView != null)
			mPlayingTimeView.setText(stringForTime(mPlayController.getCurrentPosition()));

		prevPosition = position;

		return position;
	}

	private String stringForTime(int timeMs) {
        int totalSeconds = timeMs / 1000;

        int seconds = totalSeconds % 60;
        int minutes = (totalSeconds / 60) % 60;
        int hours   = totalSeconds / 3600;

        mFormatBuilder.setLength(0);
        return mFormatter.format("%02d:%02d:%02d", hours, minutes, seconds).toString();
    }

	private HuToastMessage toast;
	private void showToastMessage(String msg) {
		if (toast == null) {
			toast = new HuToastMessage(HuMediaPlayerVideoPlayer.this, msg);

		} else {
			toast.setText(msg);
		}

		toast.show();
	}
	// ------------------------------------------------
}