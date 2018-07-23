package format.mp3;
import java.io.IOException;
import java.io.EOFException;
import java.util.Arrays;

import exceptions.StreamFormatException;


public class BitReservoir {
	
	private final InputBitStream stream;

	// Max. framesize - min. header size + reservoir size
	private static final int CAPACITY = 1440 - 17 + 511;
	
	private final byte[] buffer = new byte[CAPACITY];
	
	private int byteBuffer = 0x100;
	private int bufferHead = -1;
	private int bytesLeft  = 0;


	private static final int[] bits2cnt = initBits2Cnt();

	private static int[] initBits2Cnt() {
		int[] r = new int[256];
		Arrays.fill(r, 0);
		for (int i = 0; i < 8; ++i)
			r[1 << i] = 8-i;
		return r;
	}
	
	
	public BitReservoir(InputBitStream stream) {
		this.stream = stream;
	}
	

	public int readBit() throws StreamFormatException {
		int bb = byteBuffer;

		if (bb == 0x100) {
			loadBits();
			bb = byteBuffer;
		}
		
		byteBuffer = bb << 1;
		return bb >>> 31;
	}

	
	public int readBits(int n) throws StreamFormatException {
		int r = 0;

		while (n > 0) {
			--n;
			r <<= 1;
			r |= readBit();
		}
		
		return r;
	}


	private void loadBits() throws StreamFormatException {
		if (bytesLeft == 0) {
			throw new StreamFormatException("Bit reservoir empty");
		}
		
		++bufferHead;
		if (bufferHead == CAPACITY) {
			bufferHead = 0;
		}
		byteBuffer = (buffer[bufferHead] << 24) + 1;
		--bytesLeft;
	}
	
	
	// skipBits must not be negative
	public void skipBits(int skippedBits) throws StreamFormatException {
		int skippedBytes = skippedBits >> 3;		
		skippedBits &= 0x07;

		int bitsLeft = bits2cnt[byteBuffer & 0xff] - skippedBits;
		if (bitsLeft < 0) {
			bitsLeft += 8;
			++skippedBytes;
		}
		
		bytesLeft -= skippedBytes;
		if (bytesLeft >= 0) {
			bufferHead = (bufferHead + skippedBytes) % CAPACITY;
			byteBuffer = ((buffer[bufferHead] << 24) + 1) << (8 - bitsLeft);
		}
		else {
			// More data needs to be skipped then actually present.
			// Skip what we have in the buffer.
			bytesLeft  = 0;
			byteBuffer = 0x100;
		}
	}
	
	
	// rwBits must not be any greater than the number of bits read since the
	// last call to loadBytes().
	public void rewind(int rwBits) {
		int rwBytes = (rwBits >> 3);
		rwBits &= 0x07;

		int bitsLeft = bits2cnt[byteBuffer & 0xff] + rwBits;
		if (bitsLeft >= 8) {
			++rwBytes;
			bitsLeft -= 8;
		}
		
		bufferHead  = (bufferHead + CAPACITY - rwBytes) % CAPACITY;
		byteBuffer  = ((buffer[bufferHead] << 24) + 1) << (8 - bitsLeft);
		bytesLeft  += rwBytes;
	}
	
	
	// 0 <= size
	private boolean setSize(int size) {
		int n = bytesLeft - size;
		
		if (n < 0) {
			return false;
		}
		
		bufferHead  = (bufferHead + n) % CAPACITY;
		bytesLeft  -= n;
		byteBuffer  = 0x100;
		
		return true;
	}
	
	
	public boolean loadBytes(int size, int n) throws IOException {
		boolean success = setSize(size);
		
		int i = (bufferHead + bytesLeft + 1) % CAPACITY;
		try {
			
			int n1 = i + n;
			int n2;
			
			if (n1 > CAPACITY) {
				n1 = CAPACITY;
				n2 = i + n - CAPACITY;
			}
			else n2 = 0;
			
			bytesLeft += stream.readNextBytes(buffer, i, n1);
			bytesLeft += stream.readNextBytes(buffer, 0, n2);
			
		}
		catch (EOFException e) {
		}
		
		return success;
	}
	
	
	public boolean loadBytes(int size) throws IOException {
		boolean success = setSize(size);
		
		int i = (bufferHead + bytesLeft + 1) % CAPACITY;
		
		try {
			while (stream.noSyncWord3()) {
				buffer[i] = stream.readNextByte();
				
				++i;
				if (i == CAPACITY) i = 0;
				
				++bytesLeft;
			}
		}
		catch (EOFException e) {
		}
		
		return success;
	}
	
	
	public int readCode(int[] codeTable)
		throws StreamFormatException, IOException
	{
		int state = 0;
		int bb = byteBuffer;

		try {
			do {
				if (bb == 0x100) {
					loadBits();
					bb = byteBuffer;
				}

				state = codeTable[state + (bb >>> 31)];
				bb <<= 1;
			} while (codeTable[state] != 0);
		}
		finally {
			byteBuffer = bb;
		}
		
		return codeTable[state + 2];
	}

}
