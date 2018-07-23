package format.mp3;

public final class BitReserve
{

	private static final int		BUFSIZE			= 4096*8;
	private static final int		BUFSIZE_MASK	= BUFSIZE-1;

	private int 			offset			= 0;
	private int				totbit			= 0;
	private int				buf_byte_idx	= 0;
	private final int[] 	buf				= new int[BUFSIZE];

	public BitReserve()
	{}
	
	public int hsstell()
	{ return(totbit); }
	
	public int hgetbits(int N)
	{
		totbit	+= N;
		int val	=  0;
		int pos	=  buf_byte_idx;
		
		if (pos+N < BUFSIZE)
		{
			while (N-- > 0)
			{
				val <<= 1;
				val |= ((buf[pos++]!=0) ? 1 : 0);		 
			}
		}
		else
		{
			while (N-- > 0)
			{
				val <<= 1;			 
				val |=  ((buf[pos]!=0) ? 1 : 0);
				pos =   (pos+1) & BUFSIZE_MASK;
			}
		}
		
		buf_byte_idx = pos;
		
		return val;
	}
	
	public int hget1bit()
	{
		totbit++;
		
		int val			= buf[buf_byte_idx];
		buf_byte_idx	= (buf_byte_idx+1) & BUFSIZE_MASK;
		
		return val;
	}
	
	public void hputbuf(int val)
	{   	  
		int ofs		= offset;
		buf[ofs++]	= val & 0x80;
		buf[ofs++]	= val & 0x40;
		buf[ofs++]	= val & 0x20;
		buf[ofs++]	= val & 0x10;
		buf[ofs++]	= val & 0x08;
		buf[ofs++]	= val & 0x04;
		buf[ofs++]	= val & 0x02;
		buf[ofs++]	= val & 0x01;
		offset		= (ofs==BUFSIZE)?0:ofs;
	}
	
	public void rewindNbits(int N)
	{
		totbit			-= N;	  	  
		buf_byte_idx	-= N;
		
		if (buf_byte_idx<0) buf_byte_idx += BUFSIZE;
	}
	
	public void rewindNbytes(int N)
	{
		int bits		= (N << 3);
		totbit			-= bits;
		buf_byte_idx	-= bits;
		
		if (buf_byte_idx<0) buf_byte_idx += BUFSIZE;
	}

}