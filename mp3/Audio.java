package app.audio;

import io.util.ByteData;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import event.AudioListener;
import exceptions.UnsupportedFormatException;
import java.util.LinkedList;
import test.DigitalSignalConsumer;
import app.Tag;

public abstract class Audio implements Runnable
{
	
	private		FloatControl			volume		= null;
	public		DigitalSignalConsumer	consumer	= null;
	private		AudioOutputStream		out			= null;
	private		AudioFormat				format		= null;
	private		boolean					loop		= false;
	protected	boolean					pause		= true;
	protected	Thread					runner		= null;
	
	Audio()
	{}
	
	public String resource()
	{ return "";}
	
	public void resource(String res) throws Exception
	{} //count frames, read tag; throw exception if cannot play resource
	
	public void resource(Tag cachedTag)
	{} 
		
	public void play()
	{
		System.err.println("play in audio");
	} //if stopped, play; if paused, unpause; if playing, restart //Audios should acquire resource on play
	
	
	public void stop()
	{} //stop //Audios are expected to release resources on stop

	public void seek(int frame)
	{} 
	
	public int framePosition()
	{ return 0; }
	
	public int frameLength()
	{ return 0; } 
	
	public int timePosition()
	{ return 0; } //in seconds
	
	public int timeLength()
	{ return 0; }
	
	public boolean isPlaying()
	{ return false; }
	
	public boolean isSeekable()
	{ return false; }
	
	public boolean isTaggable()
	{ return false; }
	
	public Tag tag()
	{ return new Tag(resource());}
	
	public void tag(Tag t) throws Exception {}

	String[] formatExtensions()
	{ return new String[]{""}; }
	
	public String formatName()
	{ return "Undefined"; }
	
	public String toString()
	{ return new java.io.File(resource()).getName(); }

	public void processor(DigitalSignalConsumer dsp)
	{ consumer = dsp; }
	
	public void stopProcessor()
	{
		if(consumer!=null)
			consumer.stop();
	}
	
	public void openSignalProcessor()
	{ consumer.start( out ); }

	public void processAudio(ByteData pcm)
	{ processAudio(pcm.getData(), 0, pcm.getLen()); }


	public void processAudio( byte[] data, int offset, int length )
	{
		out.write(data, offset, length);
		
		//FIXME use NIL consumer
		if(consumer!=null)
			consumer.writeAudioData(data, offset, length);
	}
	
	public String displayString()
	{ return tag().toString(); }

	public void destroyLine()
	{
		out=null;
	}

	public boolean lineReady()
	{ return out!=null; }

	public void initVolume()
	{
		if(out!=null)
			volume = out.getVolume();
	}
	
	public void volume(int level)
	{
		if(volume!=null)
			volume.setValue(level);
	}

	public void closeLine()
	{
		try{		out.close();} catch(Exception e){}
	}

	public void setAudioFormat(AudioFormat format)
	{ this.format=format; }

	
	public float getFrameRate()
	{ return format==null?1:format.getFrameRate(); }
	
	public int getFrameSize()
	{ return format==null?0:format.getFrameSize(); }

	public int getSampleRate()
	{ return (int) (format==null?1:format.getSampleRate());	}
	
	public int getChannels()
	{ return format==null?2:format.getChannels(); }

	public void createLine() throws UnsupportedFormatException
	{
		try
		{
			out=new AudioOutputStream((SourceDataLine) AudioSystem.getLine(new DataLine.Info( SourceDataLine.class, format)));
		}
		catch(Exception e)
		{
			throw new UnsupportedFormatException("Format: "+format.toString()+", Throwable: "+e.getMessage());
		}
	}

	public void openLine() throws LineUnavailableException
	{
		if(out==null) throw new LineUnavailableException("DataLine was NOT created - cannot open NIL line");
		out.play();
	}

	public void reset()
	{ loop=true; }

	public boolean playContinuously()
	{ return loop; }

	public void setPlayContinuously(boolean loop)
	{ this.loop=loop; }

	public synchronized void pause()//if stoped, ignore; if paused, unpause; if playing, pause  //Audios in pause may keep resources open
	{
		if(runner==null) return;
		
		if(pause)
		{
			
			if(out!=null) out.play();
			pause = false;
			notify();
		}
		else
		{
			if(out!=null && out.isPlaying()) out.pause();
			pause = true;
		}
	}
	
	private final LinkedList<AudioListener> listeners = new LinkedList<AudioListener>();
	
	public void removeListener(AudioListener listener)
	{
		listeners.remove(listener);
	}
	
	public void addListener(AudioListener listener)
	{
		listeners.add(listener);
	}

	public void frameCompleted(int pos)
	{
	
		for(AudioListener listener : listeners)
			listener.frameCompleted(pos, this);
	}

	public void playbackCompleted()
	{
		for(AudioListener listener : listeners)
			listener.playbackCompleted(this);
		
	}

	public void error(Exception e, String string)
	{
		for(AudioListener listener : listeners)
			listener.error(e,string);
	}

	public void audioStarted()
	{
		for(AudioListener listener : listeners)
			listener.audioStarted(this);
		
	}
	
	public void corrupted()
	{
		for(AudioListener listener : listeners)
			listener.corrupted(this);
	}

}