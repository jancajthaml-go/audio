package format.mp3;

import io.util.Constants;

public class MP3Frame implements Constants
{
	private byte[] data			= null;
    private int offset			= 0;
    private int length			= 0;
    
    
    public byte[] getData()
    {
    	return data;
    }
    
    public int getLength()
    {
    	return length;
    }
    
    public int getOffset()
    {
    	return offset;
    }
    
    public MP3Frame(byte[] data, int offset, int length)
    {
    	this.data=data;
    	this.offset=offset;
    	this.length=length;
    }
    
}