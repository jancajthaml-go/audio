package decoder;

import io.util.Constants;

import java.io.InputStream;
import java.io.IOException;
import java.io.EOFException;
import java.util.Arrays;
import javax.sound.sampled.AudioFormat;
import exceptions.StreamFormatException;
import format.mp3.BitReservoir;
import format.mp3.HuffmanTables;
import format.mp3.HybridFilter;
import format.mp3.InputBitStream;
import format.mp3.MP3Frame;
import format.mp3.PolyphaseFilter;

public class MP3Decoder implements Constants
{

	private InputBitStream bitStream				= null;
	private final Object finishBlocker				= new Object();
	private boolean eof    							= false;
	private long samplesDecoded						= 0;
	private final float[][]       outputF			= new float[2][32];
	private final PolyphaseFilter synth0			= new PolyphaseFilter(outputF[0]);
	private final PolyphaseFilter synth1			= new PolyphaseFilter(outputF[1]);
	private final float[] roTmp						= new float[576];
	private final byte[] outputBuffer				= new byte[1152<<2];
	private int     layer							= 0;
	private boolean protection						= false;
	private int     bitrateIndex					= 15;
	private int     samplingFreq					= 3;
	private int     padding							= 0;
	private int     mode							= 0;
	private int     modeExtension					= 0;
	private boolean copyright						= false;
	private boolean original						= false;
	private int     emphasis						= EMPH_UNKNOWN;
	private int     frameSize						= 0;
	private int     channels						= 2;
	private int badFrames							= 0;
	private int bound								= 0;
	private final int[][]     allocation			= new int[2][32];
	private final float[][][] scalefactor12			= new float[2][32][3];
	private final int[][][]   sample        		= new int[2][36][32];
	private final float[][][] sampleF   	    	= new float[2][36][32];
	private int oldZps12							= 0;
	private int zeroPartStart12						= 0;
	private BitReservoir     reservoir				= null;
	private final HybridFilter[][] hybrid			= new HybridFilter[2][32];
	private int                mainDataBegin		= 0;
	private final boolean[][]  scfsi3				= new boolean[2][4];
	private final int[][]      part23Length			= new int[2][2];
	private final int[][]      bigValues			= new int[2][2];
	private final int[][]      globalGain			= new int[2][2];
	private final int[][]      scalefacCompress		= new int[2][2];
	private final boolean[][]  windowSwitchingFlag	= new boolean[2][2];
	private final int[][]      blockType			= new int[2][2];
	private final boolean[][]  mixedBlockFlag		= new boolean[2][2];
	private final int[][][]    subblockGain			= new int[2][2][3];
	private final int[][][]    tableSelect			= new int[2][2][3];
	private final int[][]      region0Count			= new int[2][2];
	private final int[][]      region1Count			= new int[2][2];
	private final int[][]      preflag				= new int[2][2];
	private final int[][]      scalefacScale		= new int[2][2];
	private final int[][]      count1TableSelect	= new int[2][2];
	private final int[][]      part2Length			= new int[2][2];
	private final int[][] scfsi2					= new int[2][32];
	
	public MP3Decoder(InputStream in)
	{
		if (in == null)
			throw new NullPointerException();
		
		samplesDecoded	= 0;
		bitStream		= new InputBitStream(in);
		reservoir		= new BitReservoir(bitStream);
		
		for (int ch = 0; ch < 2; ++ch)
		for (int sb = 0; sb < 32; ++sb)
			hybrid[ch][sb] = new HybridFilter(sampleF[ch], sb);
	}
	
	public AudioFormat getFormat()
	{ return new AudioFormat(getSamplingFreq(), 16, channels, true, false) ; }
	
	synchronized public int getLayer()
	{ return layer; }
	
	synchronized public int getBitrate()
	{ return layer != 0 && bitrateIndex != 15 ? MP3_BITRATE[layer][bitrateIndex] : 0; }
	
	synchronized public int getSamplingFreq()
	{ return MP3_FREQ_HZ[samplingFreq]; }
	
	synchronized public boolean isCopyrighted()
	{ return copyright; }
	
	synchronized public boolean isOriginal()
	{ return original; }

	synchronized public int getEmphasis()
	{ return emphasis; }
	
	public boolean isEOF()
	{
		synchronized (finishBlocker)
		{ return eof; }
	}

	public MP3Frame readNextFrame() throws IOException
    {
    	try
    	{
    		while(true)
    		{
    			try
    			{
    				decodeHeader();
    			
    				if (protection)
    				{
    					bitStream.readNextByte();
    					bitStream.readNextByte();
    				}
    			
    				switch (layer)
    				{
    					case MPEG_LAYER_1	:	decodeLayer1();	break;
    					case MPEG_LAYER_2	:	decodeLayer2();	break;
    					case MPEG_LAYER_3	:	decodeLayer3();	break;
    					default				:	throw new StreamFormatException("layer == '00'");
    			}

    			badFrames = 0;

    			return new MP3Frame(outputBuffer,0,(layer == 1 ? 12*32*4 : 36*32*4));
    		}
    		catch (StreamFormatException e)
    		{
    			++badFrames;

    			if (badFrames >= MAX_BAD_FRAMES)
    				throw new EOFException();

    			clearSamples();  // The rest is silence.
    			continue;
    		}
    		}
        }
    	catch (EOFException e) { eof = true; }
    	
        return null;
    }
	
	private void clearSamples()
	{
		for (int ch = 0; ch < channels; ++ch)
		for (int s = 0; s < 36; ++s)
			Arrays.fill(sampleF[ch][s], 0.0f);
	}

	private int findSyncword() throws IOException, StreamFormatException
	{
		int state		= 0;
		int syncword	= 0;
		int n			= 0;
		
		while (state != 2)
		{
			if (n++ == MAX_SYNC_SEARCH) throw new StreamFormatException("No syncword found");
			
			syncword = bitStream.readNextInt();
			state = (state == 1)?(((syncword & 0xf8) == 0xf8)?2:0):((syncword == 0xff ? 1 : 0));
		}
		
		return syncword;
	}
	
	private void decodeHeader() throws IOException, StreamFormatException
	{
		int header0   = findSyncword();
		int header1   = bitStream.readNextInt();
		int header2   = bitStream.readNextInt();

		synchronized (this)
		{
			layer         = 4 - ((header0 >> 1) & 0x03);
			protection    = ((header0 & 0x01) == 0);
		
			bitrateIndex  = header1 >> 4;
			samplingFreq  = (header1 >> 2) & 0x03;
			padding       = (header1 >> 1) & 0x01;
		
			mode          = header2 >> 6;
			modeExtension = (header2 >> 4) & 0x03;
			copyright     = ((header2 & 0x08) != 0);
			original      = ((header2 & 0x04) != 0);
			emphasis      = header2 & 0x03;
		}
		
		if (layer == 4)				throw new StreamFormatException("layer == '00'");
		if (bitrateIndex == 15)		throw new StreamFormatException("bitrate_index == '1111'");
		if (samplingFreq == 3)		throw new StreamFormatException("sampling_frequency == '11'");
		
		channels	= (mode == 3 ? 1 : 2);
		frameSize	= (layer == 1 ? 12 : 144) * MP3_BITRATE[layer][bitrateIndex] / MP3_FREQ_HZ[samplingFreq] + padding;
		
		if (layer == 1)
			frameSize <<= 2;
		
		samplesDecoded += frameSize;
	}
	
	private void convertSamples(float[] f, int inx)
	{
		byte[] buffer = outputBuffer;
			
		for (int tt = 0; tt < 32; ++tt)
		{
			float sampF = f[tt];
			int   sampI	= (sampF > 32767.0)?32767:((sampF < -32768.0)?-32768:(int) (sampF + (sampF < 0 ? -0.5 : 0.5)));

			buffer[inx  ] = (byte) sampI;
			buffer[inx+1] = (byte) (sampI >> 8);

			inx += 4;
		}
	}
	
	private void synthetizeOutput()
	{
		int sampPerSb = (layer == 1 ? 12 : 36);

		// Channel 0
		float[][]       sampF = sampleF[0];
		float[]         outF  = outputF[0];
		PolyphaseFilter synth = synth0;

		int inx = 0;
		
		for (int s = 0; s < sampPerSb; ++s)
		{
			synth.filter(sampF[s]);
			convertSamples(outF, inx);
			inx += 128;
		}

		// Channel 1
		sampF = sampleF[1];
		outF  = outputF[1];
		synth = synth1;
		
		if (channels > 1)
		{
			inx = 2;
		
			for (int s = 0; s < sampPerSb; ++s)
			{
				synth.filter(sampF[s]);
				convertSamples(outF, inx);
				inx += 128;
			}
		}
		else
		{
			byte[] out = outputBuffer;

			while (inx > 0)
			{
				inx -= 4;
				out[inx + 2] = out[inx + 0];
				out[inx + 3] = out[inx + 1];
			}
		}
	}
	
	private void dequantize12(int parts)
	{
		int maxZps = (oldZps12 > zeroPartStart12 ? oldZps12 : zeroPartStart12);

		for (int ch = 0; ch < channels; ++ch) {
			int[][]   sampleCh  = sample[ch];
			float[][] sampleFCh = sampleF[ch];
			float[][] sfCh      = scalefactor12[ch];
			int[][]   alloc     = allocation;
			
			for (int sb = 0; sb < maxZps; ++sb) {
				int n = alloc[ch][sb];
				float[] sfChSb = sfCh[sb];

				if (n != 0) {
					int k = (n > 0 ? n : (n == -5 ? 2 : (n == -7 ? 3 : 4)));
					
					for (int p = 0; p < parts; ++p) {
						int   inx = 12*p;
						float sf = sfChSb[p];
						
						for (int s = 0; s < 12; ++s) {
							float f;
							f  = ((sampleCh[inx][sb] >> (k - 1)) != 0) ?
								0.0f : -1.0f;
							
							f += (sampleCh[inx][sb] & ((1 << k - 1) - 1)) /
								(float) (1 << k - 1);
							
							if (n > 0) {
								f = dequantD[n] * (f + dequantC[n]) * sf;
							}
							else {
								// Grouping
								float D = (n == -5 ? 4.0f/3.0f :
									(n == -7 ? 8.0f/5.0f : 16.0f/9.0f));

								f = (D * (f + 0.5f)) * sf;
							}
							sampleFCh[inx][sb] = f;
							++inx;
						}
					}
				}
				else {
					// No bits for this subband, all samples are zero
					for (int s = 12*parts-1; s >= 0; --s) {
						sampleFCh[s][sb] = 0.0f;
					}
				}
			}
		}
	}

	// Layer 1
	
	private void decodeLayer1() throws IOException, StreamFormatException
	{
		
		bound = (mode == 1 ? (modeExtension + 1) << 2 : 32);
		
		//Decode Allocation
		
						oldZps12	= zeroPartStart12;
		int				zps			= -1;
		InputBitStream	bs			= bitStream;
		int[][]			bitAlloc	= allocation;
		int				bnd			= bound;
		
		for (int sb = 0; sb < bnd; ++sb)
		{
			for (int ch = 0; ch < channels; ++ch)
			{
				int alloc = bs.readBits(4);
				
				if (alloc == 15)
					throw new StreamFormatException("alloc == '1111'");
				
				if (alloc > 0)
				{
					++alloc;
					zps = sb;
				}
				
				bitAlloc[ch][sb] = alloc;
			}
		}
		
		for (int sb = bnd; sb < 32; ++sb)
		{
			int alloc = bs.readBits(4);
			
			if (alloc == 15)
				throw new StreamFormatException("alloc == '1111'");
			
			if (alloc > 0)
			{
				++alloc;
				zps = sb;
			}

			bitAlloc[0][sb] = alloc;
			bitAlloc[1][sb] = alloc;
		}

		zeroPartStart12 = zps + 1;
		
		//Decode Scale Factors
		
					bs			= bitStream;
					bitAlloc	= allocation;
		float[][][] scalefac	= scalefactor12;
		int			chans		= channels;

		for (int sb = 0; sb < 32; ++sb)
		for (int ch = 0; ch < chans; ++ch)
		{
			if (bitAlloc[ch][sb] != 0)
				scalefac[ch][sb][0] = scalefactors12[bs.readBits(6)];
		}
		
		//Decode Samples
		
		bs	= bitStream;
		zps	= zeroPartStart12;
		
		if (channels == 1)
		{
			int[]	alloc0	= allocation[0];
			int[][]	samp0	= sample[0];

			for (int s = 0; s < 12; ++s)
			{
				int[] samp0S  = samp0[s];

				for (int sb = 0; sb < zps; ++sb)
				{
					if (alloc0[sb] != 0)
						samp0S[sb] = bs.readBits(alloc0[sb]);
				}
			}
		}
		else
		{
			int[]	alloc0		= allocation	[0];
			int[]	alloc1		= allocation	[1];
			int[][]	samp0		= sample		[0];
			int[][]	samp1		= sample		[1];
			int		boundedZps	= (bound < zps ? bound : zps);

			for (int s = 0; s < 12; ++s)
			{
				int[] samp0S  = samp0[s];
				int[] samp1S  = samp1[s];

				for (int sb = 0; sb < boundedZps; ++sb)
				{
					if (alloc0[sb] != 0)
						samp0S[sb] = bs.readBits(alloc0[sb]);
					
					if (alloc1[sb] != 0)
						samp1S[sb] = bs.readBits(alloc1[sb]);
				}
			
				for (int sb = bound; sb < zps; ++sb)
				{
					int samp = 0;
					if (alloc0[sb] != 0)
					{
						samp		= bs.readBits(alloc0[sb]);
						samp0S[sb]	= samp;
						samp1S[sb]	= samp;
					}
				}
			}
		}
		
		dequantize12(1);
		
		synthetizeOutput();
	}

	//Layer 2
	
	private void decodeLayer2() throws IOException, StreamFormatException
	{
		bound = (mode == 1 ? (modeExtension + 1) << 2 : 32);
		
		//Decode Allocation
		
		int descInx = aTabIndex[3*(channels - 1) + samplingFreq][bitrateIndex];

		if (descInx == -1)  {
			throw new StreamFormatException(
					"Illegal combination of bitrate and mode");
		}

		int[] desc			= aTabDesc[descInx];
		int[] tabRows		= aTabRows[desc[0]];
		int rowsBase		= -1;
		int descPtr			= 0;
		int reps			= 0;
		int nbal			= 0;
		int     zps			= 0;
		int     bnd			= bound;
		int     chans		= channels;
		int[][] alloc		= allocation;
		InputBitStream bs	= bitStream;

		for (int sb = 0; sb < 32; ++sb)
		{
			if (reps == 0)
			{
				rowsBase += (1 << nbal);
				reps = desc[++descPtr];
				
				if (reps > 0)
					nbal = tabRows[rowsBase];
				else
				{
					nbal = 0;
					reps = 32;
				}
			}
			--reps;
			
			if (sb < bnd)
			{
				for (int ch = 0; ch < chans; ++ch)
				{
					int index = bs.readBits(nbal);

					if (index == 0)
						alloc[ch][sb] = 0;
					
					else
					{
						alloc[ch][sb] = tabRows[rowsBase + index];
						zps = sb;
					}
				}
			}
			else
			{
				int index = bs.readBits(nbal);

				if (index == 0)
					alloc[0][sb] = 0;
				
				else
				{
					alloc[0][sb] = tabRows[rowsBase+index];
					zps = sb;
				}

				if (chans > 1)
					alloc[1][sb] = alloc[0][sb];
			}
		}

		oldZps12 = zeroPartStart12;
		zeroPartStart12 = zps + 1;
		
		//Decode Scale Factors
		
		bs = bitStream;		
		alloc = allocation;
		chans = channels;
		int[][] scfsi = scfsi2;

		for (int sb = 0; sb < 32; ++sb)
		for (int ch = 0; ch < chans; ++ch)
		{
			if (alloc[ch][sb] != 0)
				scfsi[ch][sb] = bs.readBits(2);
		}
		
		// Decode scalefactors
		for (int sb = 0; sb < 32; ++sb)
		for (int ch = 0; ch < chans; ++ch)
		{
			if (alloc[ch][sb] != 0)
			{
				float sf = scalefactors12[bs.readBits(6)];
				scalefactor12[ch][sb][0] = sf;
					
				if (scfsi[ch][sb] == 0 || scfsi[ch][sb] == 3)
					sf = scalefactors12[bs.readBits(6)];
					
				scalefactor12[ch][sb][1] = sf;
					
				if (scfsi[ch][sb] <= 1)
					sf = scalefactors12[bs.readBits(6)];
					
				scalefactor12[ch][sb][2] = sf;
			}
		}
		
		//Decode Samples
		
		bs					= bitStream;
		int[][][] samp      = sample;
		int[][]   samp0     = samp[0];
		int[][]   samp1     = samp[1];
		int[][]   bitAlloc  = allocation;
		chans				= channels;
		zps					= zeroPartStart12;
		int boundedZps		= (bound < zps ? bound : zps);

		for (int s = 0; s < 36; s += 3)
		{
			for (int sb = 0; sb < boundedZps; ++sb)
			{
				for (int ch = 0; ch < chans; ++ch)
				{
					int[][] sampleCh = samp[ch];

					int alocation = bitAlloc[ch][sb];
					if (alocation >= 0)
					{
						sampleCh[s + 0][sb] = bs.readBits(alocation);
						sampleCh[s + 1][sb] = bs.readBits(alocation);
						sampleCh[s + 2][sb] = bs.readBits(alocation);
					}
					else
					{
						int sampleCode = bs.readBits(-alocation);
						int nlevels = (alocation == -5 ? 3 : (alocation == -7 ? 5 : 9));
						
						sampleCh[s + 0][sb] = sampleCode % nlevels;
						sampleCode /= nlevels;
						sampleCh[s + 1][sb] = sampleCode % nlevels;
						sampleCh[s + 2][sb] = sampleCode / nlevels;
					}
				}
			}
			
			for (int sb = bound; sb < zps; ++sb)
			{
				int alocation = bitAlloc[0][sb];

				if (alocation >= 0)
				{
					samp0[s + 0][sb] = bs.readBits(alocation);
					samp0[s + 1][sb] = bs.readBits(alocation);
					samp0[s + 2][sb] = bs.readBits(alocation);
				}
				else
				{
					int sampleCode = bs.readBits(-alocation);
					int nlevels = (alocation == -5 ? 3 : (alocation == -7 ? 5 : 9));

					samp0[s + 0][sb] = sampleCode % nlevels;
					sampleCode /= nlevels;
					samp0[s + 1][sb] = sampleCode % nlevels;
					samp0[s + 2][sb] = sampleCode / nlevels;
				}
				
				samp1[s + 0][sb] = samp0[s + 0][sb];
				samp1[s + 1][sb] = samp0[s + 1][sb];
				samp1[s + 2][sb] = samp0[s + 2][sb];
			}
		}
		
		dequantize12(3);
		synthetizeOutput();
	}
	
	private void decodeLayer3() throws IOException, StreamFormatException
	{
		decodeSideInformation();
		
		int mainDataBytes = frameSize - 4 - (protection ? 2 : 0) - (mode == 3 ? 17 : 32);
		
		if (!((bitrateIndex != 0) ? reservoir.loadBytes(mainDataBegin, mainDataBytes) : reservoir.loadBytes(mainDataBegin)))
			throw new StreamFormatException("Not enough main data");
		
		for (int gr = 0; gr < 2; ++gr)
		{
			for (int ch = 0; ch < channels; ++ch)
			{
				decodeScalefactors3(gr, ch);
				decodeHuffmanData(gr, ch);
				dequantize3(gr, ch);
			}
			
			
			if (mode == JOINT_STEREO)
			{ // 1 == joint_stereo
				stereo(gr);
			}
			
			for (int ch = 0;  ch < channels; ++ch)
			{
				reorder(gr, ch);
				antialias(gr, ch);
				hybrid(gr, ch);
			}
		}
		
		synthetizeOutput();
	}
	
	
	private void hybrid(int gr, int ch) {
	
		int            type        = blockType[gr][ch];
		int            firstZeroSb = (zeroPartStart3[gr][ch] + 17) / 18;
		float[]        hdata       = huffmanData[gr][ch];
		HybridFilter[] hybridCh    = hybrid[ch];
		
		// Transform the upper 30 subbands

		for (int sb = 2; sb < firstZeroSb; ++sb) {
			hybridCh[sb].transform(hdata, type, gr);
		}
		
		for (int sb = firstZeroSb; sb < 32; ++sb) {
			hybridCh[sb].zeros(gr);
		}
		
		// Transform the lowest 2 subbands
		
		if (windowSwitchingFlag[gr][ch] && mixedBlockFlag[gr][ch]) {
			type = 0;
		}
		
		if (firstZeroSb > 2) {
			firstZeroSb = 2;
		}

		for (int sb = 0; sb < firstZeroSb; ++sb) {
			hybridCh[sb].transform(hdata, type, gr);
		}
	}
	
	
	private void decodeSideInformation()
		throws IOException, StreamFormatException
	{
		InputBitStream bs = bitStream;
		boolean[][] scfsi = scfsi3;

		mainDataBegin = bs.readBits(9);
		
		// private_bits (ignored)
		bs.readBits(mode == 3 ? 5 : 3);
		
		for (int ch = 0; ch < channels; ++ch) {
			for (int scfsi_band = 0; scfsi_band < 4; ++scfsi_band) {
				scfsi[ch][scfsi_band] = (bs.readBits(1) == 1);
			}
		}
		
		for (int gr = 0; gr < 2; ++gr) {
			for (int ch = 0; ch < channels; ++ch) {
				
				part23Length[gr][ch]        = bs.readBits(12);
				bigValues[gr][ch]           = bs.readBits(9);
				globalGain[gr][ch]          = bs.readBits(8);
				scalefacCompress[gr][ch]    = bs.readBits(4);
				windowSwitchingFlag[gr][ch] = (bs.readBits(1) == 1);
				
				if (windowSwitchingFlag[gr][ch]) {
					
					blockType[gr][ch] = bs.readBits(2);
					
					if (blockType[gr][ch] == 0) {
						throw new StreamFormatException(
							"window_switching && block_type == 0");
					}
					
					mixedBlockFlag[gr][ch] = (bs.readBits(1) == 1);
					
					for (int region = 0; region < 2; ++region) {
						tableSelect[gr][ch][region] = bs.readBits(5);
					}
					
					for (int window = 0; window < 3; ++window) {
						subblockGain[gr][ch][window] = bs.readBits(3);
					}
				}
				else {
					blockType[gr][ch] = 0;
					
					for (int region = 0; region < 3; ++region) {
						tableSelect[gr][ch][region] = bs.readBits(5);
					}
					
					region0Count[gr][ch] = bs.readBits(4);
					region1Count[gr][ch] = bs.readBits(3);
				}
				
				preflag[gr][ch]           = bs.readBits(1);
				scalefacScale[gr][ch]     = bs.readBits(1);
				count1TableSelect[gr][ch] = bs.readBits(1);
			}
		}   
	}
	
	
	private final int[][][]   scalefactor3L  = new int[2][2][21];
	private final int[][][][] scalefactor3S  = new int[2][2][12][3];
	private final int[][]     zeroPartStart3  = new int[2][2];
	

	private void decodeScalefactors3(int gr, int ch)
		throws StreamFormatException
	{
		int slen1 = slen1Tab[scalefacCompress[gr][ch]];
		int slen2 = slen2Tab[scalefacCompress[gr][ch]];

		BitReservoir res = reservoir;
		
		int[] scfLGrCh = scalefactor3L[gr][ch];

		int p2l;

		if (blockType[gr][ch] == 2) {
			int[][] scfSGrCh = scalefactor3S[gr][ch];
			
			// Short blocks (Type 2)
			if (mixedBlockFlag[gr][ch]) {
				// Mixed
				for (int sfb = 0; sfb < 8; ++sfb)
					scfLGrCh[sfb] = res.readBits(slen1);
				
				p2l = 17 * slen1 + 18 * slen2;
			}
			
			else {
				// Pure short
				for (int sfb = 0; sfb < 3; ++sfb) {
					for (int window = 0; window < 3; ++window) {
						scfSGrCh[sfb][window] = res.readBits(slen1);
					}
				}
				
				p2l = 18 * (slen1 + slen2);
			}
			
			for (int sfb = 3; sfb < 6; ++sfb) {
				for (int window = 0; window < 3; ++window) {
					scfSGrCh[sfb][window] = res.readBits(slen1);
				}
			}
			
			for (int sfb = 6; sfb < 12; ++sfb) {
				for (int window = 0; window < 3; ++window) {
					scfSGrCh[sfb][window] = res.readBits(slen2);
				}
			}
		}
		
		else {
			// Long blocks (Type 0, 1, 3)
			p2l = 11 * slen1 + 10 * slen2;
			boolean[][] scfsi = scfsi3;
			int[] scfL0Ch = scalefactor3L[0][ch];

			int sfb = 0;
			for (int scfsiBand = 0; scfsiBand < 4; ++scfsiBand) {
				int slen = (scfsiBand < 2 ? slen1 : slen2);

				int boundary = scfsiBoundaryL[scfsiBand];
				for (; sfb < boundary; ++sfb) {
					if (gr == 0 || !scfsi[ch][scfsiBand]) {
						scfLGrCh[sfb] = res.readBits(slen);
					}
					else {
						scfLGrCh[sfb] = scfL0Ch[sfb];
						p2l -= slen;
					}
				}
			}
		}

		part2Length[gr][ch] = p2l;
	}
	
	
	private static final int[] linbits = {
		 0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,
		 1,  2,  3,  4,  6,  8, 10, 13,  4,  5,  6,  7,  8,  9, 11, 13,
	};
	
	private final float[][][] huffmanData = new float[2][2][576];
	
	private final int[] regionBoundary = new int[3]; // region boundaries

	
	private void decodeHuffmanData(int gr, int ch)
		throws StreamFormatException, IOException
	{
		int     n     = 0;
		float[] hdata = huffmanData[gr][ch];
		int     bits  = part23Length[gr][ch] - part2Length[gr][ch];
		int     bv    = bigValues[gr][ch] << 1;
		int[]   regb  = this.regionBoundary;
		
		if (bv > 576) {
			//System.err.println("big_values too large (" + bv + ")");
			bv = 576;
		}
		
		if (windowSwitchingFlag[gr][ch]) {
			regb[0] = 36;
			regb[1] = 576;
		}
		else {
			regb[0] = scfBandsL[samplingFreq][region0Count[gr][ch] + 1];
			regb[1] = scfBandsL[samplingFreq][region0Count[gr][ch]
				+ region1Count[gr][ch] + 2];
			// region0_count + region1_count should be at most 20.
		}
		regb[2] = bv;
		
		if (regb[1] > bv) {
			regb[1] = bv;
			if (regb[0] > bv) regb[0] = bv;
		}


		BitReservoir res = reservoir;
		
		// big_values
		for (int region = 0; region < 3; ++region) {
			int regionEnd = regb[region];
			int ts = tableSelect[gr][ch][region];
			
			if (ts == 0) {
				// Table 0
				Arrays.fill(hdata, n, regionEnd, 0.0f);
				n = regionEnd;
				continue;
			}
			
			int[] codeTable = HuffmanTables.tables[ts];			
			if (codeTable == null) {
				throw new StreamFormatException("Invalid huffman table: " + ts);
			}
			
			int lb = linbits[ts];
			
			while (n < regionEnd) {
				int data = res.readCode(codeTable);

				bits -= (data >>> 16) & 0xff;
				
				int x = (data >>> 8) & 0xff;
				if (x == 15) {
					x += res.readBits(lb);
					bits -= lb;
				}
				float xF = pow43[x]; // the first step of dequantization
				if (x != 0 && res.readBit() == 1) xF = -xF;
				
				int y = data & 0xff;
				if (y == 15) {
					y += res.readBits(lb);
					bits -= lb;
				}
				float yF = pow43[y]; // dequant.
				if (y != 0 && res.readBit() == 1) yF = -yF;
				
				hdata[n]   = xF;
				hdata[n+1] = yF;
				n += 2;
			}
		}
		
		
		// count1
		int[] codeTable;
		codeTable = (count1TableSelect[gr][ch] == 0 ?
			HuffmanTables.tabA : HuffmanTables.tabB);

		/* If big_values is odd and part_2_3_length is large enough (e.g. the
		 * granule has some stuffing), the count1 partition could overflow.
		 * To avoid range errors, we quit the loop when n >= 572, i.e. there's
		 * no room for another 4 values.
		 */
		while (n < 572 && bits > 0) {
			int data = res.readCode(codeTable);
			bits -= (data & 0xff00) >> 8;
			
			hdata[n+0] = ((data & 0x08) == 0 ? 0.0f :
				(res.readBit() == 0 ? 1.0f : -1.0f));

			hdata[n+1] = ((data & 0x04) == 0 ? 0.0f :
				(res.readBit() == 0 ? 1.0f : -1.0f));

			hdata[n+2] = ((data & 0x02) == 0 ? 0.0f :
				(res.readBit() == 0 ? 1.0f : -1.0f));
			
			hdata[n+3] = ((data & 0x01) == 0 ? 0.0f :
				(res.readBit() == 0 ? 1.0f : -1.0f));
			
			n += 4;
		}
		
		
		// Find out where the zero_part starts.
		// We'll need that for stereo processing and some minor optimizations.
		int  m = n;
		
		while ((m > 0) && (hdata[m-1] == 0.0)) {
			--m;
		}
		
		int oldZps = zeroPartStart3[gr][ch];
		zeroPartStart3[gr][ch] = m;
		
		
		// rzero
		// Frequency lines above oldZps are already zero.
		if (n < oldZps) {
			Arrays.fill(hdata, n, oldZps, 0.0f);
		}

		// Adjust the stream
		if (bits > 0) {
			res.skipBits(bits);
		}
		else if (bits < 0) {
			// Anyway, this is an error.
			//System.err.println("Rewinding reservoir (" +(-bits) + " bits)");
			res.rewind(-bits);
		}
	}
	
	
	private static final float[] pow43 = initPow43();
	
	private static float[] initPow43() {
		float[] r = new float[8192+15];
		
		for (int k = 0; k < 8192; ++k)
			r[k] = (float) Math.pow(k, 4.0/3.0);
		
		for (int k = 8192; k < 8192+15; ++k)
			r[k] = r[8191];
		
		return r;
	}
	
	
	private static final float[] exp2 = initExp2();
	
	private static float[] initExp2() {
		float[] r = new float[436];
		
		for (int k = -390; k <= 45; ++k) {
			r[k + 390] = (float) Math.pow(2.0, k / 4.0);
		}
		
		return r;
	}
	
	
	private void dequantize3(int gr, int ch) {
		int sfb = 0;
		int n   = 0;
		
		float   hdata[]   = huffmanData[gr][ch];
		int     ssShift   = scalefacScale[gr][ch]+1;
		int     ggOffs    = globalGain[gr][ch] - 210  +390; // exp2[] offset
		int     zps       = zeroPartStart3[gr][ch];
		int[]   scfbLF    = scfBandsL[samplingFreq];
		int[]   scfbSF    = scfBandsS[samplingFreq];
		
		if ((blockType[gr][ch] != 2) || mixedBlockFlag[gr][ch]) {
			
			int   pf  = preflag[gr][ch];
			int[] sfL = scalefactor3L[gr][ch];
			
			int lastLongSfb = (blockType[gr][ch] == 2 ? 8 : 22);
			
			// Long bands

			for (sfb = 0; sfb < lastLongSfb; ++sfb) {
				
				if (n >= zps) {
					return;
				}
				
				// exponent (in quarters)
				int exp;
				
				if (sfb < 21) {
					exp = -sfL[sfb];
					if (pf != 0) {
						exp -= pretab[sfb];
					}
					exp <<= ssShift;
					exp += ggOffs;
				}
				else exp = ggOffs;
				
				// multiplier
				float mul = exp2[exp];
				
				int end = scfbLF[sfb + 1];
				
				while (n < end) {
					hdata[n] *= mul;
					++n;
				}
			}
			
			if (lastLongSfb == 8) {
				sfb = 3;
			}
			else {
				return;
			}
		}
		
		int[][] sfS = scalefactor3S[gr][ch];
		int[]   sbG = subblockGain[gr][ch];
		
		// Short bands

		for (; sfb < 13; ++sfb) {
			
			if (n >= zps) {
				return;
			}
			
			int len = scfbSF[sfb+1] - scfbSF[sfb];

			for (int window = 0; window < 3; ++window) {
				
				// exponent (in quarters)
				int exp = ggOffs - (sbG[window] << 3);
				
				if (sfb < 12) {
					exp -= (sfS[sfb][window] << ssShift);
				}
				
				// multiplier
				float mul = exp2[exp];

				for (int k = len ; k > 0; --k) {
					hdata[n] *= mul;
					++n;
				}
			}
		}
	}
	

	
	private void stereo(int gr) {

		int me = modeExtension;

		if (me == 0) {
			return;
		}
		
		float[] hdata0 = huffmanData[gr][0];
		float[] hdata1 = huffmanData[gr][1];
		int[]   scfbLF = scfBandsL[samplingFreq];
		int[]   scfbSF = scfBandsS[samplingFreq];
		
		
		// Find first zero scalefactor band
		
		int switchSfb = 0; // First sfb with long blocks
		int switchN   = 0;
		
		boolean longBlocksOnly = (blockType[gr][0] != 2);
		int[] zpsA = zeroPartStart3[gr];
		int   zps  = zpsA[1];
		
		
		boolean switchLb =
				longBlocksOnly ||
				(mixedBlockFlag[gr][0] && (zps <= 30));
		
		if (switchLb) {
			while (switchN < zps) {
				++switchSfb;
				switchN = scfbLF[switchSfb];
			}
		}
		else {
			while (switchN < zps) {
				++switchSfb;
				switchN  = 3*scfbSF[switchSfb];
			}
		}
		
		// Update zeroPartStart
		
		if (zpsA[0] > zpsA[1]) {
			zps = zpsA[1] = zpsA[0];
		}
		else {
			zps = zpsA[0] = zpsA[1];
		}
		
		
		// Stereo processing
		
		int n = 0;
		
		// M/S stereo
		
		if ((me & 2) != 0) {
			if ((me & 1) == 0) {
				switchN = zps;
			}
			
			for (; n < switchN; ++n) {
				float tmp0 = hdata0[n];
				float tmp1 = hdata1[n];
				hdata0[n] = (tmp0 + tmp1) / 1.41421356237f;
				hdata1[n] = (tmp0 - tmp1) / 1.41421356237f;
			}
		}
		
		
		// Intensity stereo
		
		if ((me & 1) != 0) {
			int sfb = switchSfb;
			n = switchN;
			
			if (switchLb) { // Some long bands are in intensity stereo
				int nLongBands = (longBlocksOnly ? 22 : 8);
				int[] isPositions = scalefactor3L[gr][1];
				
				while (sfb < nLongBands) {
					
					if (n >= zps) {
						return;
					}
					
					// Use previous is_pos for the highest scalefactor band
					int isPos = (sfb != 21? isPositions[sfb] : isPositions[20]);
					++sfb;
					
					if (isPos < 7) {
						float isM0 = isMul0[isPos];
						float isM1 = isMul1[isPos];
						
						while (n < scfbLF[sfb]) {
							float tmp = hdata0[n];
							hdata0[n] = tmp*isM0;
							hdata1[n] = tmp*isM1;
							++n;
						}
					}
					else if ((me & 2) != 0) {
						// Invalid isPos, use M/S stereo if enabled
						while (n < scfbLF[sfb]) {
							float tmp0 = hdata0[n];
							float tmp1 = hdata1[n];
							hdata0[n] = (tmp0 + tmp1) / 1.41421356237f;
							hdata1[n] = (tmp0 - tmp1) / 1.41421356237f;
							++n;
						}
					}
					else {
						n = scfbLF[sfb];
					}
					
				}
				
				sfb = (longBlocksOnly ? 13 : 3);
			}
			
			// Short bands in intensity stereo
			int[][] isPositions = scalefactor3S[gr][1];
			
			while (sfb < 13) {
				if (n >= zps) {
					return;
				}
				
				int len = scfbSF[sfb+1] - scfbSF[sfb];
				
				for (int window = 0; window < 3; ++window) {
					// Use previous is_pos for the highest scalefactor band
					int isPos = (sfb != 12 ?
						isPositions[sfb][window] :
						isPositions[11][window]);
					
					if (isPos < 7) {
						float isM0 = isMul0[isPos];
						float isM1 = isMul1[isPos];
						
						for (int k = 0; k < len; ++k) {
							float tmp = hdata0[n];
							hdata0[n] = tmp*isM0;
							hdata1[n] = tmp*isM1;
							++n;
						}
					}
					else if ((me & 2) != 0) {
						// Invalid isPos, use M/S stereo if enabled
						for (int k = 0; k < len; ++k) {
							float tmp0 = hdata0[n];
							float tmp1 = hdata1[n];
							hdata0[n] = (tmp0 + tmp1) / 1.41421356237f;
							hdata1[n] = (tmp0 - tmp1) / 1.41421356237f;
							++n;
						}
					}
					else {
						n += len;
					}
				}
				
				++sfb;
			}
		}
		
	}
	
	private void antialias(int gr, int ch) {
	
		float[] hdata = huffmanData[gr][ch];
		
		int uLimit;
		
		if (blockType[gr][ch] != 2) {
			uLimit = 576;
		}
		else if (mixedBlockFlag[gr][ch]) {
			uLimit = 36;
		}
		else {
			return;
		}
		
		// The indices affected by the inner loop range from X - 8 to X + 7,
		// where X is the initial value of 'u'. If X - 8 >= ZPS, we can quit
		// the outer loop, as the remaining butterflies would not change
		// anything.

		int zps = zeroPartStart3[gr][ch];
		
		if (uLimit - 8 > zps) {
			uLimit = zps + 8;
		}
		
		int l = 17;  // Lower wing of the butterfly
		int u = 18;  // Upper wing of the butterfly

		while (u < uLimit) {
			for (int i = 0; i < 8; ++i) {
				float tmpL = hdata[l];
				float tmpU = hdata[u];
				hdata[l] = tmpL*Cs[i] - tmpU*Ca[i];
				hdata[u] = tmpU*Cs[i] + tmpL*Ca[i];
				--l;
				++u;
			}
			
			l += 18 + 8;
			u += 18 - 8;
		}
		
		if (zps < (u - (18 - 8))) {
			zeroPartStart3[gr][ch] = u - (18 - 8);
		}
	}
	
	
	private void reorder(int gr, int ch)
	{
		float[] hdata = huffmanData[gr][ch];
		
		if (blockType[gr][ch] != 2)
			return;
		
		int[] bands     = scfBandsS[samplingFreq];
		int   zps       = zeroPartStart3[gr][ch];
		
		int   sfb       =  mixedBlockFlag[gr][ch] ? 3 : 0;
		int   start     = 3*bands[sfb];
		int   firstFreq = start;
		float[] tmp = roTmp;
		
		while (firstFreq < zps) {
			int len = bands[sfb+1] - bands[sfb];
			
			for (int w = 0;  w < 3; ++w) {
				for (int f = 0; f < len; ++f) {
					tmp[firstFreq + w + 3*f] = hdata[firstFreq + len*w + f];
				}
			}
			
			++sfb;
			
			firstFreq = 3*bands[sfb];
		}
		
		if (firstFreq > zps) {
			zeroPartStart3[gr][ch] = firstFreq;
		}

		System.arraycopy(tmp, start, hdata, start,
			zeroPartStart3[gr][ch] - start);
	}

	public float getSamplesDecoded()
	{ return samplesDecoded; }

	public int getChannels()
	{ return this.channels; }
	
	/*
	 * 
	public void waitFinish() throws InterruptedException
	{
		synchronized (finishBlocker)
		{ if (!eof) finishBlocker.wait(); }
	}
	 */
}