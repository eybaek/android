package com.mobile.mediaplayer.lib.dlna;


import java.util.ArrayList;

import android.util.Log;

import com.mxlib.common.data.core.DEVICE_TINY;
import com.mxlib.dlna.DMC;
import com.mxlib.dlna.data.dmc.USER_RENDERER_INFO;
import com.mxlib.dlna.data.dmc.event.DeviceAddRemoveParam;
import com.mxlib.dlna.data.dmc.event.DeviceEventParam;
import com.mxlib.dlna.data.dmc.event.DeviceResponseParam;



public class RendererManager  {

	// ------------------------------------------------------------------
	protected 	DlnaApi							api;
	
	private 	RendererList 	rendererList;
	private 	String 			mMime = "";
	
	
	protected	ArrayList<Renderer>	 mPlayableRendererList = new ArrayList<Renderer>();
	
	int DEVICE_REMOVE = 0;
	
	
	final static String Tag = RendererManager.class.getSimpleName();
	// ------------------------------------------------------------------
	
	public RendererManager() {
		api = DlnaApi.getSharedInstance();
	}
	
	public int	getRendererListCount() {
		return rendererList.getCount();
	}
	
	public int getPlayableRendererCount() {
		return mPlayableRendererList.size();
	}
	
	private boolean checkMime( String protocolInfo , String mime )
	{
		String[] lines = protocolInfo.split(",");
		mime = mime.toLowerCase();
		for (int i = 0 ; i < lines.length ; i ++ ) {
			
			String line = lines[i];
			
			String[] words  = line.split( ":");
			
			String word = "";
			
			if ( words.length >=2 ) word = words[2].toLowerCase();
			
			
			// Log.e(Tag , "mime=" + word + ":" + mime);
			
			if ( word.contains( mime ) ) {
				Log.d(RendererManager.class.getSimpleName(), "______ select mime : " + word);
				return true;
			}
			
		}
		return false;
	}
	
	
	public void initilize(String mime) {
		mMime = mime;
		
		rendererList = new RendererList(); 
		
		DMC.addEventDeviceAddRemove( handlerEventDeviceAddRemove );
		DMC.addEventDeviceResponse( hanlderEventDeviceResponse ) ;
		DMC.addEventDeviceEvent( handlerEventDeviceEvent );
		
		rendererList.addRenderers( api.getRendererList() , mListener);
		
		if ( mListener != null ) {
			mListener.onChangeRenderList( rendererList );
		}
	}
	
	public void rendererSearch() {
		api.msearch();
	}
	
	@Override
	public void finalize() {
		
		mListener = null;
		
		DMC.removeEventDeviceAddRemove( handlerEventDeviceAddRemove );
		DMC.removeEventDeviceResponse( hanlderEventDeviceResponse);
		DMC.removeEventDeviceEvent(handlerEventDeviceEvent);
		
		api.changeRenderer( "" );
	}

	private void checkPlayableRendererList(String UDN) {
		for (int i=0; i<mPlayableRendererList.size(); i++) {
			Renderer device = mPlayableRendererList.get(i);
			
			if (device.udn.equals(UDN))
				mPlayableRendererList.remove(i);
		}
	}

	
	DMC.EventDeviceAddRemove handlerEventDeviceAddRemove = new DMC.EventDeviceAddRemove() {
		@Override
		public void onDeviceAddRemove(DeviceAddRemoveParam param) {
			
			if (param.device.nDeviceType == com .mxlib.common.Common.MXDLNA_RENDERER_TYPE){
				Log.d(Tag, "______onDeviceAddRemove Event :" + param.added + " / " + param.device.FriendlyName);
			
				checkPlayableRendererList(param.device.UDN);
			
				ArrayList<DEVICE_TINY> devices ;
				
				if (param != null) {
					if ( param.device != null )
						devices = api.getRendererList(param.device.UDN, param.added);
					else
						devices = api.getRendererList();
					
					rendererList.addRenderers(  devices , mListener);
				}
			if ( mListener != null ) {
				mListener.onChangeRenderList( rendererList );
			}
		}
		}
	};
	
	DMC.EventDeviceResponse hanlderEventDeviceResponse = new DMC.EventDeviceResponse() {
		
		@Override
		public void onDeviceResponse(DeviceResponseParam param) {
			
			Renderer renderer = rendererList.getRenderer( param.actionData.udn );
			
			if ( renderer == null ) return;
			
			if (param.error != 0) {
				renderer.onResponseError( param.error );
				
			} else {

				switch (param.code) {
				

					case DMC.MXDLNA_MRCP_SELECTED_RENDERER_SINK : {
				
						USER_RENDERER_INFO rendererInfo = param.ri_data;
						String title = rendererInfo.MediaTitle;
						
						if ( title == null ) title = "";
//						Log.e( Tag , "MXDLNA_MRCP_SELECTED_RENDERER_SINK " + rendererInfo.MediaTitle );
//						Log.e( Tag , "MXDLNA_MRCP_SELECTED_RENDERER_SINK " + rendererInfo.ProtocolInfo );
						
						boolean playable = false;
//						Log.d("TAG", "___________renderer ( " + renderer.title + " ),  protocolInfo ( "+ rendererInfo.ProtocolInfo + " )");
						if ( rendererInfo.ProtocolInfo != null && rendererInfo.ProtocolInfo.length() > 0 ) {
							if ( checkMime( rendererInfo.ProtocolInfo , mMime) ){
								playable = true;

							} else if (renderer.getManufacturerName().equalsIgnoreCase("humax") && mMime.equalsIgnoreCase("video/x-mpegurl")) {
								playable = true;
							}
								
						} else {
							if (renderer.getManufacturerName().equalsIgnoreCase("humax") && mMime.equalsIgnoreCase("video/x-mpegurl")) {
							playable = true;
						
							} else {
								playable = false;
							}
						}
						
						if (playable)
							mPlayableRendererList.add(renderer);
							
						int state = rendererInfo.PlayState;
						renderer.onResponseInitialize( state , playable ,  title );
						
						if ( mListener != null ) 
							mListener.onChangeRenderList( rendererList );


					} break;
						

					case DMC.MXDLNA_MRCP_SETURL_SINK: 
						
						renderer.onResponseSetUri();
						
						break;
					
					case DMC.MXDLNA_MRCP_PLAY_SINK:
						
						renderer.onResponsePlay();
						break;
						
					case DMC.MXDLNA_MRCP_STOP_SINK:
						renderer.onResponseStop();
						break;
						
					case DMC.MXDLNA_MRCP_PAUSE_SINK:
						renderer.onResponsePause();
						break;
				}
			}
		}
	};

	
	DMC.EventDeviceEvent handlerEventDeviceEvent = new DMC.EventDeviceEvent() {
			
		@Override
		public void onDeviceEvent(DeviceEventParam param) {

			Renderer renderer = rendererList.getRenderer( param.UDN );
			
			if ( renderer == null ) return;

			if (param.service == DMC.MXDLNA_MRCP_AVT_EVENT_LASTCHANGE ) {
				
				if (( param.flag & DMC.AVT_EVENT_TRANSPORT_STATE) == DMC.AVT_EVENT_TRANSPORT_STATE ) {
					int playState = param.avtData.PlayState;
					renderer.onEventPlaystate( playState );
				}
				
				if (( param.flag & DMC.AVT_EVENT_CURRENT_METADATA_TRACK_OBJECT) == DMC.AVT_EVENT_CURRENT_METADATA_TRACK_OBJECT ) {
					
					String title = param.avtData.MediaTitle ;
					
					if ( title == null ) title = "";
					
					renderer.onEventPlayingTitle( title );
					
				}

			}
		
			if (param.service == DMC.MXDLNA_MRCP_RCS_EVENT_LASTCHANGE) {

			}
		}
	};
		
	EventRendererListener mListener = null;
	public void setEventRendererListener ( EventRendererListener e ) {
		mListener = e;
	}
	
	
	public interface EventRendererListener {
		public void onChangeRenderList( RendererList rendererList ) ;
		public void onChangeRender( Renderer renderer);
	}
	
	// ------------------------------------------------------
}
