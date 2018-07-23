package format.mp3;
import java.io.InputStream;
import java.io.IOException;
import java.io.EOFException;

public class InputBitStream {
	private static final int BUFFER_SIZE = 65536;
	
	private final InputStream stream;
	
	private final byte[] buffer  = new byte[BUFFER_SIZE];
	private int bufferHead;
	private int bytesLeft  = 0;
	private int bitsLeft   = 0;
	private int byteBuffer;
	
	public InputBitStream(InputStream stream) {
		this.stream = stream;
		
	}


	public int readBits(int n) throws IOException {
		int r = 0;
		int bl = bitsLeft;
		int bb = byteBuffer;

		try {
			while (n > 0) {
				if (bl == 0) {
					if (bytesLeft > 0) {
						--bytesLeft;
						bb = buffer[++bufferHead];
					}
					else {
						bytesLeft = stream.read(buffer, 0, BUFFER_SIZE) - 1;
						if (bytesLeft == -2) {
							throw new EOFException();
						}
						bufferHead = 0;
						bb = buffer[0];
					}
					
					bb <<= 24;
					bl = 8;
				}

				int bits = (bl < n ? bl : n);
				n -= bits;
				bl -= bits;

				r = (r << bits) | (bb >>> (32 - bits));
				bb <<= bits;				
			}
		}
		finally {
			bitsLeft   = bl;
			byteBuffer = bb;
		}

		return r;
	}

	
	public byte readNextByte() throws IOException {
		bitsLeft = 0;

		if (bytesLeft > 0) {
			--bytesLeft;
			++bufferHead;
			return buffer[bufferHead];
		}
		else {
			bytesLeft = stream.read(buffer, 0, BUFFER_SIZE) - 1;
			if (bytesLeft == -2) {
				throw new EOFException();
			}
			bufferHead = 0;
			return buffer[0];
		}
	}


	public int readNextInt() throws IOException {
		return readNextByte() & 0xff;
	}
	
	
		// start <= end, end - start < BUFFER_SIZE
		int readNextBytes(byte[] dst, int start, int end)
				throws IOException
		{
			bitsLeft = 0;

			int len1 = end - start;
			int len2;
			
			if (len1 > bytesLeft) {
				len2 = len1 - bytesLeft;
				len1 = bytesLeft;
			}
			else len2 = 0;
			
			// read first part
			System.arraycopy(buffer, bufferHead + 1, dst, start, len1);
			start += len1;
			bufferHead += len1;

			bytesLeft -= len1;
			
			// refill buffer if needed
			if (bytesLeft == 0) {
				bytesLeft = stream.read(buffer, 0, BUFFER_SIZE);
				if (bytesLeft == -1) {
					bytesLeft = 0;
				}
				bufferHead = -1;
			}
			
			// read second part
			if (len2 > bytesLeft) {
				len2 = bytesLeft;
				end  = start + len2;
			}
			
			System.arraycopy(buffer, bufferHead + 1, dst, start, len2);
			bufferHead += len2;			
			bytesLeft -= len2;
			
			return len1 + len2;
		}
	
	
	// MPEG-specific stuff
	
	boolean noSyncWord3() throws IOException {
		if (bytesLeft < 2) {
			buffer[0] = buffer[bufferHead];
			if (bytesLeft == 1) {
				buffer[1] = buffer[bufferHead + 1];
			}
			bufferHead = 0;
			
			int k = stream.read(buffer, bytesLeft + 1, BUFFER_SIZE-bytesLeft-1);
			if (k > 0) {
				bytesLeft += k;
			}
			
			if (bytesLeft < 2) {
				return false;
			}
		}
		
		return (buffer[bufferHead + 1] != -1) ||
			((buffer[bufferHead + 2] & 0xfe) != 0xfa);
	}


	
}
