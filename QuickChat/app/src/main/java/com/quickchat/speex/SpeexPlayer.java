/**
 * 
 */
package com.quickchat.speex;

import com.gauss.speex.encode.SpeexDecoder;

import java.io.File;

/**
 * @author Gauss
 * 
 */
public class SpeexPlayer {
	private String fileName = null;
	private SpeexDecoder speexdec = null;
	private Thread playThread = null;

	public SpeexPlayer(String fileName) {
		this.fileName = fileName;
		System.out.println(this.fileName);
		try {
			speexdec = new SpeexDecoder(new File(this.fileName));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void startPlay() {
		if(speexdec.isPaused()){
			speexdec.setPaused(false);
		}
		else{
			RecordPlayThread rpt = new RecordPlayThread();
			playThread = new Thread(rpt);
			playThread.start();
		}
	}

	public boolean isPlaying(){
		return speexdec.isDecoding();
	}

	public boolean isPaused(){
		return speexdec.isPaused();
	}

	public void pause(){
		if(!speexdec.isPaused() && speexdec.isDecoding()){
			speexdec.setPaused(true);
		}
	}

	public void stop(){
		if(playThread!=null && playThread.isAlive() && speexdec.isDecoding()){
			playThread.interrupt();
		}
	}

	class RecordPlayThread extends Thread {
		public void run() {
			try {
				if (speexdec != null)
					speexdec.decode();
			} catch (Exception t) {
				t.printStackTrace();
			}
		}
	};
}
