package format.mp3;

import io.util.CRC16;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;

import exceptions.BitstreamException;
import exceptions.SoundErrorCodes;


public final class Bitstream implements SoundErrorCodes
{

	//FIXME enum
	public static byte		INITIAL_SYNC	= 0;
	public static byte		STRICT_SYNC		= 1;
	private static final int	BUFFER_INT_SIZE = 433;

	private final int[]					framebuffer		= new int[BUFFER_INT_SIZE];
	private int							framesize		= 0;
	private byte[]						frame_bytes		= new byte[BUFFER_INT_SIZE*4];
	private int							wordpointer		= 0;
	private int							bitindex		= 0;
	private int							syncword		= 0;
	private int							header_pos		= 0;
	private boolean						single_ch_mode	= false;
	private final int					bitmask[]		= {0, 0x00000001, 0x00000003, 0x00000007, 0x0000000F, 0x0000001F, 0x0000003F, 0x0000007F, 0x000000FF, 0x000001FF, 0x000003FF, 0x000007FF, 0x00000FFF, 0x00001FFF, 0x00003FFF, 0x00007FFF, 0x0000FFFF, 0x0001FFFF};
	private final Header				header			= new Header();
	private final byte					syncbuf[]		= new byte[4];
	private CRC16[]						crc				= new CRC16[1];
	private byte[]						rawid3v2		= null;
	private boolean						firstframe		= true;
	private final PushbackInputStream	source;

	public Bitstream(InputStream in)
	{
		if (in==null) throw new NullPointerException("in");
		in = new BufferedInputStream(in);		
		
		loadID3v2(in);
		
		source		= new PushbackInputStream(in, BUFFER_INT_SIZE*4);
		closeFrame();
	}
	
	public int header_pos()
	{ return header_pos; }
	
	private void loadID3v2(InputStream in)
	{		
		int size = -1;
		
		try
		{
			in.mark(10);
			byte[] id3header	= new byte[4];
			size				= -10;
			
			in.read(id3header,0,3);
		
			if ( (id3header[0]=='I') && (id3header[1]=='D') && (id3header[2]=='3'))
			{
				in.read(id3header,0,3);
				in.read(id3header,0,4);
				size = (int) (id3header[0] << 21) + (id3header[1] << 14) + (id3header[2] << 7) + (id3header[3]);
			}
			
			header_pos	= size+10;			
		}
		catch (IOException e)
		{}
		finally
		{
			try						{ in.reset();	}
			catch (IOException e)	{				}
		}
		try
		{
			if (size > 0)
			{
				rawid3v2 = new byte[size];
				in.read(rawid3v2,0,rawid3v2.length);
			}			
		}
		catch (IOException e)
		{}
	}
	
	public void close() throws BitstreamException
	{
		try
		{ source.close(); }
		catch (IOException ex)
		{ throw newBitstreamException(STREAM_ERROR, ex); }
	}

	public Header readFrame() throws BitstreamException
	{
		Header result = null;
		try
		{
			result = readNextFrame();
			if (firstframe == true)
			{
				result.parseVBR(frame_bytes);
				firstframe = false;
			}			
		}
		catch (BitstreamException ex)
		{
			if ((ex.getErrorCode()==INVALID_FRAME))
			{
				try
				{
					closeFrame();
					result = readNextFrame();
				}
				catch (BitstreamException e)
				{
					if ((e.getErrorCode()!=STREAM_EOF))
						throw newBitstreamException(e.getErrorCode(), e);
				}
			}
			else if ((ex.getErrorCode()!=STREAM_EOF))
				throw newBitstreamException(ex.getErrorCode(), ex);
		}
		return result;
	}

	private Header readNextFrame() throws BitstreamException
	{
		if (framesize == -1)
			nextFrame();
		return header;
	}

	private void nextFrame() throws BitstreamException
	{ header.read_header(this, crc); }

	public void unreadFrame() throws BitstreamException
	{
		if (wordpointer==-1 && bitindex==-1 && (framesize>0))
		{
			try
			{ source.unread(frame_bytes, 0, framesize); }
			catch (IOException ex)
			{ throw newBitstreamException(STREAM_ERROR); }
		}
	}

	public void closeFrame()
	{
		framesize	= -1;
		wordpointer	= -1;
		bitindex	= -1;
	}

	public boolean isSyncCurrentPosition(int syncmode) throws BitstreamException
	{
		int read			= readBytes(syncbuf, 0, 4);
		int headerstring	= ((syncbuf[0] << 24) & 0xFF000000) | ((syncbuf[1] << 16) & 0x00FF0000) | ((syncbuf[2] << 8) & 0x0000FF00) | ((syncbuf[3] << 0) & 0x000000FF);

		try
		{ source.unread(syncbuf, 0, read); }
		catch (IOException ex)
		{}

		switch (read)
		{
			case 0	: return true;
			case 4	: return isSyncMark(headerstring, syncmode, syncword);
			default	: return false; 
		}
	}

	public int readBits(int n)
	{ return get_bits(n); }

	public int readCheckedBits(int n)
	{ return get_bits(n); }

	protected BitstreamException newBitstreamException(int errorcode)
	{ return new BitstreamException(errorcode, null); }
	
	protected BitstreamException newBitstreamException(int errorcode, Throwable throwable)
	{ return new BitstreamException(errorcode, throwable); }

	int syncHeader(byte syncmode) throws BitstreamException
	{
		int bytesRead		= readBytes(syncbuf, 0, 3);
		
		if (bytesRead!=3) throw newBitstreamException(STREAM_EOF, null);

		boolean sync		= false;
		int headerstring	= ((syncbuf[0] << 16) & 0x00FF0000) | ((syncbuf[1] << 8) & 0x0000FF00) | ((syncbuf[2] << 0) & 0x000000FF);

		do
		{
			headerstring <<= 8;

			if (readBytes(syncbuf, 3, 1)!=1)
				throw newBitstreamException(STREAM_EOF, null);

			headerstring |= (syncbuf[3] & 0x000000FF);

			sync = isSyncMark(headerstring, syncmode, syncword);
		}
		while(!sync);

		return headerstring;
	}

	public boolean isSyncMark(int headerstring, int syncmode, int word)
	{
		boolean sync = (syncmode == INITIAL_SYNC)?((headerstring & 0xFFE00000) == 0xFFE00000):(((headerstring & 0xFFF80C00) == word) && (((headerstring & 0x000000C0) == 0x000000C0) == single_ch_mode));

		if (sync)	sync = (((headerstring >>> 10) & 3)!=3);
		if (sync)	sync = (((headerstring >>> 17) & 3)!=0);
		if (sync)	sync = (((headerstring >>> 19) & 3)!=1);

		return sync;
	}

	int read_frame_data(int bytesize) throws BitstreamException
	{
 		int	numread	= readFully(frame_bytes, 0, bytesize);
		framesize	= bytesize;
		wordpointer	= -1;
	    bitindex	= -1;
	    
	    return numread;
	}

	void parse_frame() throws BitstreamException
	{
		int b			= 0;
		byte[] byteread	= frame_bytes;
		int bytesize	= framesize;

		for (int k=0;k<bytesize;k=k+4)
		{
			byte b0	= byteread[k];;
			byte b1	= 0;
			byte b2	= 0;
			byte b3	= 0;

			if (k+1<bytesize) b1 = byteread[k+1];
			if (k+2<bytesize) b2 = byteread[k+2];
			if (k+3<bytesize) b3 = byteread[k+3];

			framebuffer[b++] = ((b0 << 24) &0xFF000000) | ((b1 << 16) & 0x00FF0000) | ((b2 << 8) & 0x0000FF00) | (b3 & 0x000000FF);
		}
		
		wordpointer	= 0;
		bitindex	= 0;
	}

	public int get_bits(int number_of_bits)
	{
		int returnvalue	= 0;
		int sum			= bitindex + number_of_bits;

		if (wordpointer < 0) wordpointer = 0;
    
		if (sum <= 32)
		{
			returnvalue = (framebuffer[wordpointer] >>> (32 - sum)) & bitmask[number_of_bits];
			
			if ((bitindex += number_of_bits) == 32)
			{
				bitindex = 0;
				wordpointer++;
			}
			return returnvalue;
		}

		returnvalue	= ((((framebuffer[wordpointer++] & 0x0000FFFF)) << 16) & 0xFFFF0000) | ((((framebuffer[wordpointer] & 0xFFFF0000)) >>> 16)& 0x0000FFFF);
		returnvalue	>>>= 48 - sum;
		returnvalue	&= bitmask[number_of_bits];
		bitindex	= sum - 32;
    
		return returnvalue;
	}

	void set_syncword(int syncword0)
	{
		syncword		= syncword0 & 0xFFFFFF3F;
		single_ch_mode	= ((syncword0 & 0x000000C0) == 0x000000C0);
	}
	
	private int readFully(byte[] b, int offs, int len) throws BitstreamException
	{		
		int nRead = 0;
		
		try
		{
			while (len > 0)
			{
				int bytesread = source.read(b, offs, len);
				
				if (bytesread == -1)
				{
					while (len-->0) b[offs++] = 0;
					break;
				}
				nRead	= nRead + bytesread;
				offs	+= bytesread;
				len		-= bytesread;
			}
		}
		catch (IOException ex)
		{ throw newBitstreamException(STREAM_ERROR, ex); }
		return nRead;
	}

	private int readBytes(byte[] b, int offs, int len) throws BitstreamException
	{
		int totalBytesRead = 0;
		
		try
		{
			while (len > 0)
			{
				int bytesread = source.read(b, offs, len);
				if (bytesread == -1) break;
				
				totalBytesRead	+= bytesread;
				offs			+= bytesread;
				len				-= bytesread;
			}
		}
		catch (IOException ex)
		{ throw newBitstreamException(STREAM_ERROR, ex); }
		
		return totalBytesRead;
	}
		
}