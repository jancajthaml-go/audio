package format.mp3;

import exceptions.BitstreamException;
import exceptions.SoundErrorCodes;
import io.util.CRC16;


public final class Header
{
	public  static final int[][]	frequencies					= {{22050, 24000, 16000, 1}, {44100, 48000, 32000, 1}, {11025, 12000, 8000, 1}};
	public static final int			bitrates[][][]				= { {{0 , 32000, 48000, 56000, 64000, 80000, 96000, 112000, 128000, 144000, 160000, 176000, 192000 ,224000, 256000, 0}, {0 , 8000, 16000, 24000, 32000, 40000, 48000, 56000, 64000, 80000, 96000, 112000, 128000, 144000, 160000, 0}, {0 , 8000, 16000, 24000, 32000, 40000, 48000, 56000, 64000, 80000, 96000, 112000, 128000, 144000, 160000, 0}}, { {0 , 32000, 64000, 96000, 128000, 160000, 192000, 224000, 256000, 288000, 320000, 352000, 384000, 416000, 448000, 0}, {0 , 32000, 48000, 56000, 64000, 80000, 96000, 112000, 128000, 160000, 192000, 224000, 256000, 320000, 384000, 0}, {0 , 32000, 40000, 48000, 56000, 64000, 80000, 96000, 112000, 128000, 160000, 192000, 224000, 256000, 320000, 0}}, {{0 , 32000, 48000, 56000, 64000, 80000, 96000, 112000, 128000, 144000, 160000, 176000, 192000 ,224000, 256000, 0}, {0 , 8000, 16000, 24000, 32000, 40000, 48000, 56000, 64000, 80000, 96000, 112000, 128000, 144000, 160000, 0}, {0 , 8000, 16000, 24000, 32000, 40000, 48000, 56000, 64000, 80000, 96000, 112000, 128000, 144000, 160000, 0}} };
	public static final String		bitrate_str[][][]			= {{{"free format", "32 kbit/s", "48 kbit/s", "56 kbit/s", "64 kbit/s", "80 kbit/s", "96 kbit/s", "112 kbit/s", "128 kbit/s", "144 kbit/s", "160 kbit/s", "176 kbit/s", "192 kbit/s", "224 kbit/s", "256 kbit/s", "forbidden"}, {"free format", "8 kbit/s", "16 kbit/s", "24 kbit/s", "32 kbit/s", "40 kbit/s", "48 kbit/s", "56 kbit/s", "64 kbit/s", "80 kbit/s", "96 kbit/s", "112 kbit/s", "128 kbit/s", "144 kbit/s", "160 kbit/s", "forbidden"}, {"free format", "8 kbit/s", "16 kbit/s", "24 kbit/s", "32 kbit/s", "40 kbit/s", "48 kbit/s", "56 kbit/s", "64 kbit/s", "80 kbit/s", "96 kbit/s", "112 kbit/s", "128 kbit/s", "144 kbit/s", "160 kbit/s", "forbidden"}},{{"free format", "32 kbit/s", "64 kbit/s", "96 kbit/s", "128 kbit/s", "160 kbit/s", "192 kbit/s", "224 kbit/s", "256 kbit/s", "288 kbit/s", "320 kbit/s", "352 kbit/s", "384 kbit/s", "416 kbit/s", "448 kbit/s", "forbidden"},{"free format", "32 kbit/s", "48 kbit/s", "56 kbit/s", "64 kbit/s", "80 kbit/s", "96 kbit/s", "112 kbit/s", "128 kbit/s", "160 kbit/s", "192 kbit/s", "224 kbit/s", "256 kbit/s", "320 kbit/s", "384 kbit/s", "forbidden"},{"free format", "32 kbit/s", "40 kbit/s", "48 kbit/s", "56 kbit/s", "64 kbit/s", "80 kbit/s" , "96 kbit/s", "112 kbit/s", "128 kbit/s", "160 kbit/s", "192 kbit/s", "224 kbit/s", "256 kbit/s", "320 kbit/s", "forbidden"}},{{"free format", "32 kbit/s", "48 kbit/s", "56 kbit/s", "64 kbit/s", "80 kbit/s", "96 kbit/s", "112 kbit/s", "128 kbit/s", "144 kbit/s", "160 kbit/s", "176 kbit/s", "192 kbit/s", "224 kbit/s", "256 kbit/s", "forbidden"},{"free format", "8 kbit/s", "16 kbit/s", "24 kbit/s", "32 kbit/s", "40 kbit/s", "48 kbit/s", "56 kbit/s", "64 kbit/s", "80 kbit/s", "96 kbit/s", "112 kbit/s", "128 kbit/s", "144 kbit/s", "160 kbit/s", "forbidden"},{"free format", "8 kbit/s", "16 kbit/s", "24 kbit/s", "32 kbit/s", "40 kbit/s", "48 kbit/s", "56 kbit/s", "64 kbit/s", "80 kbit/s", "96 kbit/s", "112 kbit/s", "128 kbit/s", "144 kbit/s", "160 kbit/s", "forbidden"}}};

	//FIXME enum
	public static final int			MPEG2_LSF					= 0;
	public static final int			MPEG25_LSF					= 2;
	public static final int			MPEG1						= 1;
	public static final int			STEREO						= 0;
	public static final int			JOINT_STEREO				= 1;
	public static final int			DUAL_CHANNEL				= 2;
	public static final int			SINGLE_CHANNEL				= 3;
	public static final int			FOURTYFOUR_POINT_ONE		= 0;
	public static final int			FOURTYEIGHT					= 1;
	public static final int			THIRTYTWO					= 2;

	private int						h_layer						= 0;
	private int						h_protection_bit			= 0;
	private int						h_bitrate_index				= 0;
	private int						h_padding_bit				= 0;
	private int						h_mode_extension			= 0;
	private int						h_version					= 0;
	private int						h_mode						= 0;
	private int						h_sample_frequency			= 0;
	private int						h_number_of_subbands		= 0;
	private int						h_intensity_stereo_bound	= 0;
	private boolean					h_copyright, h_original		= false;
	private double[] 				h_vbr_time_per_frame		= {-1, 384, 1152, 1152};
	private boolean					h_vbr						= false;
	private int						h_vbr_frames				= 0;
	private int						h_vbr_scale					= 0;
	private int						h_vbr_bytes					= 0;
	private byte[]					h_vbr_toc					= null;
	private byte					syncmode					= Bitstream.INITIAL_SYNC;
	private CRC16					crc							= null;
	public short					checksum					= 0;
	public int						framesize					= 0;
	public int						nSlots						= 0;
	private int						_headerstring				= -1;

	public Header()
	{}
	
	public String toString()
	{
		StringBuffer buffer = new StringBuffer(200);
		buffer.append("Layer ");
		buffer.append(layer_string());
		buffer.append(" frame ");
		buffer.append(mode_string());
		buffer.append(' ');
		buffer.append(version_string());
		if (!checksums())
			buffer.append(" no");
		buffer.append(" checksums");
		buffer.append(' ');
		buffer.append(sample_frequency_string());
		buffer.append(',');
		buffer.append(' ');
		buffer.append(bitrate_string());

		String s =  buffer.toString();
		return s;
	}

	void read_header(Bitstream stream, CRC16[] crcp) throws BitstreamException
	{
		int headerstring	= 0;
		int channel_bitrate	= 0;
		boolean sync		= false;
		
		do
		{
			headerstring	= stream.syncHeader(syncmode);
			_headerstring	= headerstring;
			
			if (syncmode == Bitstream.INITIAL_SYNC)
			{
				h_version = ((headerstring >>> 19) & 1);
				
				if (((headerstring >>> 20) & 1) == 0)
					if (h_version == MPEG2_LSF)	h_version = MPEG25_LSF;
					else						throw stream.newBitstreamException(SoundErrorCodes.UNKNOWN_BITSTREAM_ERROR);
				
				if ((h_sample_frequency = ((headerstring >>> 10) & 3)) == 3)
					throw stream.newBitstreamException(SoundErrorCodes.UNKNOWN_BITSTREAM_ERROR);
			}
			
			h_layer						= 4 - (headerstring >>> 17) & 3;
			h_protection_bit			= (headerstring >>> 16) & 1;
			h_bitrate_index				= (headerstring >>> 12) & 0xF;
			h_padding_bit				= (headerstring >>> 9) & 1;
			h_mode						= ((headerstring >>> 6) & 3);
			h_mode_extension			= (headerstring >>> 4) & 3;
			h_intensity_stereo_bound	= (h_mode == JOINT_STEREO)?((h_mode_extension << 2) + 4):0;
			
			if (((headerstring >>> 3) & 1) == 1)	h_copyright = true;
			if (((headerstring >>> 2) & 1) == 1)	h_original = true;
			
			if (h_layer == 1)
				h_number_of_subbands = 32;
			else
			{
				channel_bitrate = h_bitrate_index;
				if (h_mode != SINGLE_CHANNEL)
					channel_bitrate = (channel_bitrate == 4)?1:(channel_bitrate-4);
			
				h_number_of_subbands = ((channel_bitrate == 1) || (channel_bitrate == 2))?((h_sample_frequency == THIRTYTWO)?12:8):(((h_sample_frequency == FOURTYEIGHT) || ((channel_bitrate >= 3) && (channel_bitrate <= 5)))?27:30);
			}
			
			if (h_intensity_stereo_bound > h_number_of_subbands)
				h_intensity_stereo_bound = h_number_of_subbands;
			
			calculate_framesize();
			
			int framesizeloaded = stream.read_frame_data(framesize);
			
			if ((framesize >=0) && (framesizeloaded != framesize))
				throw stream.newBitstreamException(SoundErrorCodes.INVALID_FRAME);
			
			if (stream.isSyncCurrentPosition(syncmode))
			{
				if (syncmode == Bitstream.INITIAL_SYNC)
				{
					syncmode = Bitstream.STRICT_SYNC;
					stream.set_syncword(headerstring & 0xFFF80CC0);
				}
				sync = true;
			}
			else stream.unreadFrame();
		}
		while (!sync);
		
		stream.parse_frame();
		
		if (h_protection_bit == 0)
		{
			checksum = (short) stream.get_bits(16);
			
			if(crc == null) crc = new CRC16();
			
			crc.add_bits(headerstring, 16);
			crcp[0] = crc;
		}
		else crcp[0] = null;
	}

	void parseVBR(byte[] firstframe) throws BitstreamException
	{
		byte tmp[]	= new byte[4];
		int offset	= (h_version == MPEG1)?((h_mode == SINGLE_CHANNEL)?17:32):((h_mode == SINGLE_CHANNEL)?9:17);
		
		try
		{
			System.arraycopy(firstframe, offset, tmp, 0, 4);
			if ("Xing".equals(new String(tmp)))
			{
				h_vbr			= true;
				h_vbr_frames	= -1;
				h_vbr_bytes		= -1;
				h_vbr_scale		= -1;
				h_vbr_toc		= new byte[100];
				int length		= 4;
				byte flags[]	= new byte[4];
				
				System.arraycopy(firstframe, offset + length, flags, 0, flags.length);

				length += flags.length;
				
				if ((flags[3] & (byte) (1 << 0)) != 0)
				{
					System.arraycopy(firstframe, offset + length, tmp, 0, tmp.length);
				
					h_vbr_frames	= (tmp[0] << 24)&0xFF000000 | (tmp[1] << 16)&0x00FF0000 | (tmp[2] << 8)&0x0000FF00 | tmp[3]&0x000000FF;
					length			+= 4;	
				}
				if ((flags[3] & (byte) (1 << 1)) != 0)
				{
					System.arraycopy(firstframe, offset + length, tmp, 0, tmp.length);
					
					h_vbr_bytes	= (tmp[0] << 24)&0xFF000000 | (tmp[1] << 16)&0x00FF0000 | (tmp[2] << 8)&0x0000FF00 | tmp[3]&0x000000FF;
					length		+= 4;	
				}
				if ((flags[3] & (byte) (1 << 2)) != 0)
				{
					System.arraycopy(firstframe, offset + length, h_vbr_toc, 0, h_vbr_toc.length);
					
					length		+= h_vbr_toc.length;	
				}
				if ((flags[3] & (byte) (1 << 3)) != 0)
				{
					System.arraycopy(firstframe, offset + length, tmp, 0, tmp.length);
					
					h_vbr_scale	= (tmp[0] << 24)&0xFF000000 | (tmp[1] << 16)&0x00FF0000 | (tmp[2] << 8)&0x0000FF00 | tmp[3]&0x000000FF;
					length		+= 4;	
				}			
			}				
		}
		catch (ArrayIndexOutOfBoundsException e)
		{ throw new BitstreamException("XingVBRHeader Corrupted",e); }
		
		offset		= 32;
		try
		{
			System.arraycopy(firstframe, offset, tmp, 0, 4);
			if ("VBRI".equals(new String(tmp)))
			{
				h_vbr			= true;
				h_vbr_frames	= -1;
				h_vbr_bytes		= -1;
				h_vbr_scale		= -1;
				h_vbr_toc		= new byte[100];		
				int length		= 10;
				
				System.arraycopy(firstframe, offset + length, tmp, 0, tmp.length);
				
				h_vbr_bytes		= (tmp[0] << 24)&0xFF000000 | (tmp[1] << 16)&0x00FF0000 | (tmp[2] << 8)&0x0000FF00 | tmp[3]&0x000000FF;
				length			+= 4;	
				
				System.arraycopy(firstframe, offset + length, tmp, 0, tmp.length);
				
				h_vbr_frames	= (tmp[0] << 24)&0xFF000000 | (tmp[1] << 16)&0x00FF0000 | (tmp[2] << 8)&0x0000FF00 | tmp[3]&0x000000FF;
				length			+= 4;
			}
		}
		catch (ArrayIndexOutOfBoundsException e)
		{ throw new BitstreamException("VBRIVBRHeader Corrupted",e); }
	}
	
	public int version()
	{ return h_version; }

	public int layer()
	{ return h_layer; }

	public int bitrate_index()
	{ return h_bitrate_index; }

	public int sample_frequency()
	{ return h_sample_frequency; }

	public int frequency()
	{ return frequencies[h_version][h_sample_frequency]; }

	public int mode()
	{ return h_mode; }

	public boolean checksums()
	{ return h_protection_bit == 0; }

	public boolean copyright()
	{ return h_copyright; }

	public boolean original()
	{ return h_original; }

	public boolean vbr()
	{ return h_vbr; }

	public int vbr_scale()
	{ return h_vbr_scale; }

	public byte[] vbr_toc()
	{ return h_vbr_toc; }

	public boolean checksum_ok()
	{ return checksum == crc.checksum(); }

	public boolean padding()
	{ return h_padding_bit != 0; }

	public int slots()
	{ return nSlots; }

	public int mode_extension()
	{ return h_mode_extension; }

	public int calculate_framesize()
	{
		if (h_layer == 1)
		{
			framesize = (12 * bitrates[h_version][0][h_bitrate_index]) / frequencies[h_version][h_sample_frequency];
			if (h_padding_bit != 0 ) framesize++;
			
			framesize	<<= 2;
			nSlots		= 0;
		}
		else
		{
			framesize = (144 * bitrates[h_version][h_layer - 1][h_bitrate_index]) / frequencies[h_version][h_sample_frequency];
		
			if (h_version == MPEG2_LSF || h_version == MPEG25_LSF)	framesize >>= 1;
			if (h_padding_bit != 0)									framesize++;

			nSlots = (h_layer == 3)?((h_version == MPEG1)?(framesize - ((h_mode == SINGLE_CHANNEL) ? 17 : 32) -  ((h_protection_bit!=0) ? 0 : 2) - 4):(framesize - ((h_mode == SINGLE_CHANNEL) ?  9 : 17) -  ((h_protection_bit!=0) ? 0 : 2) - 4)):0;
		}
		
		framesize -= 4;
		
		return framesize;
	}

	public int max_number_of_frames(int streamsize)
	{ return h_vbr?h_vbr_frames:(((framesize + 4 - h_padding_bit) == 0)?0:(streamsize / (framesize + 4 - h_padding_bit))); }

	public int min_number_of_frames(int streamsize)
	{ return h_vbr?h_vbr_frames:((framesize + 5 - h_padding_bit) == 0)?0:(streamsize / (framesize + 5 - h_padding_bit)); }

	public float ms_per_frame()
	{ return h_vbr?(((float) (((h_version == MPEG2_LSF) || (h_version == MPEG25_LSF))?((h_vbr_time_per_frame[layer()] / frequency()) / 2):(h_vbr_time_per_frame[layer()] / frequency()) * 1000))):(new float[][]{{8.707483f,8.0f,12.0f},{26.12245f,24.0f,36.0f},{26.12245f,24.0f,36.0f}}[h_layer-1][h_sample_frequency]); }
	
	public float total_ms(int streamsize)
	{ return(max_number_of_frames(streamsize) * ms_per_frame()); }

	public int getSyncHeader()
	{ return _headerstring; }

	public String layer_string()
	{
		switch (h_layer)
		{
			case 1	:	return "I";
			case 2	:	return "II";
			case 3	:	return "III";
		}
		return null;
	}

	public String bitrate_string()
	{ return (h_vbr == true)?(Integer.toString(bitrate()/1000)+" kb/s"):bitrate_str[h_version][h_layer - 1][h_bitrate_index]; }

	public int bitrate()
	{ return (h_vbr == true)?(((int) ((h_vbr_bytes<<3) / (ms_per_frame() * h_vbr_frames)))*1000):(bitrates[h_version][h_layer - 1][h_bitrate_index]); }

	public int bitrate_instant()
	{ return bitrates[h_version][h_layer - 1][h_bitrate_index]; }

	public String sample_frequency_string()
	{
		switch (h_sample_frequency)
		{
	    	case THIRTYTWO				:	return (h_version == MPEG1)?"32 kHz":((h_version == MPEG2_LSF)?"16 kHz":"8 kHz");
	    	case FOURTYFOUR_POINT_ONE	:	return (h_version == MPEG1)?"44.1 kHz":((h_version == MPEG2_LSF)?"22.05 kHz":"11.025 kHz");
	    	case FOURTYEIGHT			:	return (h_version == MPEG1)?"48 kHz":((h_version == MPEG2_LSF)?"24 kHz":"12 kHz");
	    	default						:	return null;
		}
	}

	public String mode_string()
	{
		switch (h_mode)
		{
			case STEREO			:	return "Stereo";
			case JOINT_STEREO	:	return "Joint stereo";
			case DUAL_CHANNEL	:	return "Dual channel";
			case SINGLE_CHANNEL	:	return "Single channel";
			default				:	return null;
	   }
	}

	public String version_string()
	{
		switch (h_version)
		{
			case MPEG1		:	return "MPEG-1";
			case MPEG2_LSF	:	return "MPEG-2 LSF";
			case MPEG25_LSF	:	return "MPEG-2.5 LSF";
			default			:	return null;
		}
	}

	public int number_of_subbands()
	{ return h_number_of_subbands; }

	public int intensity_stereo_bound()
	{ return h_intensity_stereo_bound; }
	
}