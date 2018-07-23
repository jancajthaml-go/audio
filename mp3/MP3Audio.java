 package app.audio;

import javax.sound.sampled.*;

import decoder.MP3Decoder;
import exceptions.JavaLayerException;
import exceptions.UnsupportedFormatException;
import format.mp3.MP3Frame;
import java.io.*;
import meta.mp3.ID3v1;
import meta.mp3.ID3v2;
import meta.mp3.Mp3File;
import app.Tag;

public class MP3Audio extends Audio
{
	
	private	int			frames		= 0;
	private	int			pos			= 0;
	private	int			current		= 0;
	private	MP3Decoder	dec			= null;
	public	String		res			= null;
	private	Tag			t			= null;
	private	float		seconds		= 0f;
	
	public void resource(String res) throws Exception
	{
		this.res	= res;
		this.t		= new Tag(res);
		
		try
		{
			Mp3File f = new Mp3File(res, false);
				
			if(f.hasId3v1Tag())
			{
				//System.err.println("ID31: "+f.getSampleRate()+" "+super.getSampleRate()+" "+f.getBitrate());
			
				ID3v1 tag = f.getId3v1Tag(); 
			
				t.title( tag.getTitle() );
				t.artist( tag.getArtist() );
				t.album( tag.getAlbum() );
				t.track( tag.getTrack() );
				t.genre( tag.getGenreDescription() );
				//t.lyrics( tag.getSongLyric() );
				t.year( tag.getYear() );
				//t.composer( tag.getAuthorComposer() );
				t.comment( tag.getComment() );		
			}
			if(f.hasId3v2Tag())
			{
				ID3v2 tag = f.getId3v2Tag();
			
				//System.err.println("ID32: "+f.getSampleRate()+" "+super.getSampleRate()+" "+f.getBitrate());
				t.title(tag.getTitle());
				//t.title( tag.getTitle() );
				t.artist(tag.getArtist());
				t.album(tag.getAlbum());	
				t.track(tag.getTrack());
				//t.artist( tag.getArtist() );
				//t.album( tag.getAlbum() );
				//t.track( tag.getTrack() );

				//FIXME
				//tag.setGenre(t.genre());
				t.year(tag.getYear());
					
				//t.genre( tag.getGenreDescription() );
				//t.lyrics( tag.getSongLyric() );
				//t.year( tag.getYear() );
				//t.composer( tag.getAuthorComposer() );
				//t.comment( tag.getComment() );
				t.composer(tag.getComposer());
				t.image(tag.getAlbumImage());
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
			//System.err.println(e.getMessage());
		}
	
		frames		= 0;
		dec			= new MP3Decoder(new FileInputStream(res));
		
		while(!dec.isEOF())
		{
			dec.readNextFrame();
			frames++;
		}

		super.setAudioFormat(dec.getFormat());
		
		seconds				= ((float)dec.getSamplesDecoded()) / getFrameRate();
		
		dec					= null;
		
		t.frames			= frames;
		t.seconds			= seconds;
		
		System.out.println(timePosition()+" "+this.timeLength()+" "+frames+" "+seconds+" "+getFrameRate());
	}
	
	public void resource(Tag cachedTag)
	{
		res		= cachedTag.resource();
		frames	= cachedTag.frames;
		seconds	= cachedTag.seconds;
		t		= cachedTag;
	}

	public void run()
	{
		try
		{
			while(runner==Thread.currentThread())
			{
				try
				{
					if(dec==null || current > pos || super.playContinuously())
					{
						super.setPlayContinuously(false);
						dec = new MP3Decoder(new FileInputStream(res));
						current = 0;
					}
					while(pos > current)
					{
						dec.readNextFrame();
						current++;
					}

					MP3Frame f = dec.readNextFrame();

					current++;

					if(f==null)
					{
						pause=true;

						runner.interrupt();

						runner=null;
						
						super.playbackCompleted();
					}
					else
					{
						process(f.getData(), f.getOffset(), f.getLength());
					}
				
					while(pause && runner!=null) synchronized(this) {wait();}
				}
				catch(IOException e)
				{/* ignore for now */}
				finally
				{
					if(super.playContinuously())
					{
						play();
						return;
					}
				}
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
			super.error(e,"Playback error");
		}
	}

	public void process(byte[] data, int offset, int read)
	{
		
		try
		{
			if (!super.lineReady())
				createSource();
			
			pos++;
			super.processAudio(data,offset,read);
			
			super.frameCompleted(pos);
		}
		catch(Exception e){ e.printStackTrace();}
    }

	
	public synchronized void play()
	{
		if(runner==null)
		{
			pos		= 0;
			dec		= null;
			pause	= false;
			runner	= new Thread(this);
			runner.start();
		}
		else if(pause)
		{
			pause = false;
			notify();
		}
		//else //pos = 0;
	}

	public synchronized void stop()
	{
		if(runner==null);
		else if(pause)
		{
			runner = null;
			notify();
		}
		else runner = null;
		
		pos		= 0;
		pause	= true;
		dec		= null;
		
		super.stopProcessor();
		super.closeLine();
		super.destroyLine();
		
		super.frameCompleted(0);		
	}
	
	public synchronized void seek(int frame)
	{ this.pos = frame; }
	
	public int framePosition()
	{ return pos; }
	
	public int frameLength()
	{ return frames; }
	
	public int timePosition()
	{ return (dec==null)?0:((int)((float)dec.getSamplesDecoded() / getFrameRate())); }

	public int timeLength()
	{ return (int)seconds; }
	
	public boolean isPlaying()
	{ return !pause; }
	
	public String formatName()
	{ return "mp3"; }
	
	public String[] getExtensions()
	{ return new String[]{".mp3"}; }

	protected void createSource() throws JavaLayerException, UnsupportedFormatException
    {
        Throwable t = null;
        try
        {
        	super.setAudioFormat(dec.getFormat());
        	super.createLine();
        	super.openLine();
            super.openSignalProcessor();
            super.initVolume();
			super.audioStarted();
        }
        catch (RuntimeException ex)			{ t = ex; }
        catch (LinkageError ex)				{ t = ex; }
        catch (LineUnavailableException ex)	{ t = ex; }
        
        
		if (!super.lineReady()) throw new JavaLayerException("cannot obtain source audio line", t);
    }

	public boolean isSeekable()
	{ return true; }
	
	public boolean isTaggable()
	{ return true; }

	public Tag tag()
	{ return t; }

	public void tag(Tag t) throws Exception
	{
		Mp3File f = new Mp3File(res, false);
		
		try
		{

			if(f.hasId3v1Tag())
			{
				System.err.println("tag(Tag t): "+f.getSampleRate()+" "+super.getSampleRate());
				ID3v1 tag = f.getId3v1Tag();
				tag.setTitle(t.title());
				
				//t.title( tag.getTitle() );
				tag.setArtist(t.artist());
				tag.setAlbum(t.album());
				tag.setTrack(t.track());
				
				//t.artist( tag.getArtist() );
				//t.album( tag.getAlbum() );
				//t.track( tag.getTrack() );

				//FIXME
				//tag.setGenre(t.genre());
				
				tag.setYear(t.year());
				//t.genre( tag.getGenreDescription() );
				//t.lyrics( tag.getSongLyric() );
				//t.year( tag.getYear() );
				tag.setComment(t.comment());
				//t.composer( tag.getAuthorComposer() );
				//t.comment( tag.getComment() );
				
			}
			if(f.hasId3v2Tag())
			{
				ID3v2 tag = f.getId3v2Tag();
				tag.setComposer(t.composer());
				
				//tag.setAlbumImage(t.image(), t.mime());
				//t.composer(tag.getComposer());
				//t.image(tag.getAlbumImage());
			}
		}
		catch(Exception e){}
		
		f.save(res);
		this.t = t;
	}

	public String resource() { return res; }
}