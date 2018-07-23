package format.mp3;


public class Buffer 
{

	public static final int	OBUFFERSIZE = 2 * 1152;	// max. 2 * 1152 samples per frame
	public static final int MAXCHANNELS = 2;        // max. number of channels

	
	private short[] 	buffer		= new short[2304];
	private int[] 		bufferp		= new int[2];
	private int 		channels	= 0;
	private int			frequency	= 0;

	public Buffer(int sample_frequency, int number_of_channels)
	{
		channels	= number_of_channels;
		frequency	= sample_frequency;

		for (int i = 0; i < number_of_channels; ++i) 
			bufferp[i] = (short)i;
	}

	public int getChannelCount()
	{ return this.channels; }
  
	public int getSampleFrequency()
	{ return this.frequency; }
  
	public short[] getBuffer()
	{ return this.buffer; }
  
	public int getBufferLength()
	{ return bufferp[0]; }
	
	public void append(int channel, short value)
	{
		buffer[bufferp[channel]]	= value;
		bufferp[channel]			+= channels;	  	
	}
	
	public void appendSamples(int channel, float[] f)
	{
		int pos		= bufferp[channel];
		float fs	= 0.0f;
	    
		for (int i=0; i<32;)
	    {
			fs			= f[i++];
			buffer[pos]	= (short)((fs>32767.0f ? 32767.0f : (fs < -32767.0f ? -32767.0f : fs)));
			pos			+= channels;
	    }
		
		bufferp[channel] = pos;
	}
	
	public void write_buffer(int val)
	{}
	
	public void close()
	{}

	public void clear_buffer()
	{
		for (int i = 0; i < channels; ++i)
			bufferp[i] = (short)i;
	}
	
	public void set_stop_flag()
	{}

}	   