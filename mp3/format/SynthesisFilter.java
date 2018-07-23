package format.mp3;

//Don't bother to read this code, its optimised for speed but highly unreadable
public final class SynthesisFilter
{
	private float[]	v1					= null;
	private float[]	v2					= null;
	private float[]	actual_v			= null;
	private int 	actual_write_pos	= 0;
	private float[]	samples				= null;
	private int		channel				= 0;
	private float 	scalefactor			= 0.0f;
	private float[]	eq					= null;
	private float[] _tmpOut				= new float[32];
	private static float d16[][]		= splitArray(new float[]{0.0f,-4.42505E-4f,0.003250122f,-0.007003784f,0.031082153f,-0.07862854f,0.10031128f,-0.57203674f,1.144989f,0.57203674f,0.10031128f,0.07862854f,0.031082153f,0.007003784f,0.003250122f,4.42505E-4f,-1.5259E-5f,-4.73022E-4f,0.003326416f,-0.007919312f,0.030517578f,-0.08418274f,0.090927124f,-0.6002197f,1.1442871f,0.54382324f,0.1088562f,0.07305908f,0.03147888f,0.006118774f,0.003173828f,3.96729E-4f,-1.5259E-5f,-5.34058E-4f,0.003387451f,-0.008865356f,0.029785156f,-0.08970642f,0.08068848f,-0.6282959f,1.1422119f,0.51560974f,0.11657715f,0.06752014f,0.03173828f,0.0052948f,0.003082275f,3.66211E-4f,-1.5259E-5f,-5.79834E-4f,0.003433228f,-0.009841919f,0.028884888f,-0.09516907f,0.06959534f,-0.6562195f,1.1387634f,0.48747253f,0.12347412f,0.06199646f,0.031845093f,0.004486084f,0.002990723f,3.20435E-4f,-1.5259E-5f,-6.2561E-4f,0.003463745f,-0.010848999f,0.027801514f,-0.10054016f,0.057617188f,-0.6839142f,1.1339264f,0.45947266f,0.12957764f,0.056533813f,0.031814575f,0.003723145f,0.00289917f,2.89917E-4f,-1.5259E-5f,-6.86646E-4f,0.003479004f,-0.011886597f,0.026535034f,-0.1058197f,0.044784546f,-0.71131897f,1.1277466f,0.43165588f,0.1348877f,0.051132202f,0.031661987f,0.003005981f,0.002792358f,2.59399E-4f,-1.5259E-5f,-7.47681E-4f,0.003479004f,-0.012939453f,0.02508545f,-0.110946655f,0.031082153f,-0.7383728f,1.120224f,0.40408325f,0.13945007f,0.045837402f,0.03138733f,0.002334595f,0.002685547f,2.44141E-4f,-3.0518E-5f,-8.08716E-4f,0.003463745f,-0.014022827f,0.023422241f,-0.11592102f,0.01651001f,-0.7650299f,1.1113739f,0.37680054f,0.14326477f,0.040634155f,0.03100586f,0.001693726f,0.002578735f,2.13623E-4f,-3.0518E-5f,-8.8501E-4f,0.003417969f,-0.01512146f,0.021575928f,-0.12069702f,0.001068115f,-0.791214f,1.1012115f,0.34986877f,0.1463623f,0.03555298f,0.030532837f,0.001098633f,0.002456665f,1.98364E-4f,-3.0518E-5f,-9.61304E-4f,0.003372192f,-0.016235352f,0.01953125f,-0.1252594f,-0.015228271f,-0.816864f,1.0897827f,0.32331848f,0.1487732f,0.03060913f,0.029937744f,5.49316E-4f,0.002349854f,1.67847E-4f,-3.0518E-5f,-0.001037598f,0.00328064f,-0.017349243f,0.01725769f,-0.12956238f,-0.03237915f,-0.84194946f,1.0771179f,0.2972107f,0.15049744f,0.025817871f,0.029281616f,3.0518E-5f,0.002243042f,1.52588E-4f,-4.5776E-5f,-0.001113892f,0.003173828f,-0.018463135f,0.014801025f,-0.1335907f,-0.050354004f,-0.8663635f,1.0632172f,0.2715912f,0.15159607f,0.0211792f,0.028533936f,-4.42505E-4f,0.002120972f,1.37329E-4f,-4.5776E-5f,-0.001205444f,0.003051758f,-0.019577026f,0.012115479f,-0.13729858f,-0.06916809f,-0.89009094f,1.0481567f,0.24650574f,0.15206909f,0.016708374f,0.02772522f,-8.69751E-4f,0.00201416f,1.2207E-4f,-6.1035E-5f,-0.001296997f,0.002883911f,-0.020690918f,0.009231567f,-0.14067078f,-0.088775635f,-0.9130554f,1.0319366f,0.22198486f,0.15196228f,0.012420654f,0.02684021f,-0.001266479f,0.001907349f,1.06812E-4f,-6.1035E-5f,-0.00138855f,0.002700806f,-0.02178955f,0.006134033f,-0.14367676f,-0.10916138f,-0.9351959f,1.0146179f,0.19805908f,0.15130615f,0.00831604f,0.025909424f,-0.001617432f,0.001785278f,1.06812E-4f,-7.6294E-5f,-0.001480103f,0.002487183f,-0.022857666f,0.002822876f,-0.1462555f,-0.13031006f,-0.95648193f,0.99624634f,0.17478943f,0.15011597f,0.004394531f,0.024932861f,-0.001937866f,0.001693726f,9.1553E-5f,-7.6294E-5f,-0.001586914f,0.002227783f,-0.023910522f,-6.86646E-4f,-0.14842224f,-0.15220642f,-0.9768524f,0.9768524f,0.15220642f,0.14842224f,6.86646E-4f,0.023910522f,-0.002227783f,0.001586914f,7.6294E-5f,-9.1553E-5f,-0.001693726f,0.001937866f,-0.024932861f,-0.004394531f,-0.15011597f,-0.17478943f,-0.99624634f,0.95648193f,0.13031006f,0.1462555f,-0.002822876f,0.022857666f,-0.002487183f,0.001480103f,7.6294E-5f,-1.06812E-4f,-0.001785278f,0.001617432f,-0.025909424f,-0.00831604f,-0.15130615f,-0.19805908f,-1.0146179f,0.9351959f,0.10916138f,0.14367676f,-0.006134033f,0.02178955f,-0.002700806f,0.00138855f,6.1035E-5f,-1.06812E-4f,-0.001907349f,0.001266479f,-0.02684021f,-0.012420654f,-0.15196228f,-0.22198486f,-1.0319366f,0.9130554f,0.088775635f,0.14067078f,-0.009231567f,0.020690918f,-0.002883911f,0.001296997f,6.1035E-5f,-1.2207E-4f,-0.00201416f,8.69751E-4f,-0.02772522f,-0.016708374f,-0.15206909f,-0.24650574f,-1.0481567f,0.89009094f,0.06916809f,0.13729858f,-0.012115479f,0.019577026f,-0.003051758f,0.001205444f,4.5776E-5f,-1.37329E-4f,-0.002120972f,4.42505E-4f,-0.028533936f,-0.0211792f,-0.15159607f,-0.2715912f,-1.0632172f,0.8663635f,0.050354004f,0.1335907f,-0.014801025f,0.018463135f,-0.003173828f,0.001113892f,4.5776E-5f,-1.52588E-4f,-0.002243042f,-3.0518E-5f,-0.029281616f,-0.025817871f,-0.15049744f,-0.2972107f,-1.0771179f,0.84194946f,0.03237915f,0.12956238f,-0.01725769f,0.017349243f,-0.00328064f,0.001037598f,3.0518E-5f,-1.67847E-4f,-0.002349854f,-5.49316E-4f,-0.029937744f,-0.03060913f,-0.1487732f,-0.32331848f,-1.0897827f,0.816864f,0.015228271f,0.1252594f,-0.01953125f,0.016235352f,-0.003372192f,9.61304E-4f,3.0518E-5f,-1.98364E-4f,-0.002456665f,-0.001098633f,-0.030532837f,-0.03555298f,-0.1463623f,-0.34986877f,-1.1012115f,0.791214f,-0.001068115f,0.12069702f,-0.021575928f,0.01512146f,-0.003417969f,8.8501E-4f,3.0518E-5f,-2.13623E-4f,-0.002578735f,-0.001693726f,-0.03100586f,-0.040634155f,-0.14326477f,-0.37680054f,-1.1113739f,0.7650299f,-0.01651001f,0.11592102f,-0.023422241f,0.014022827f,-0.003463745f,8.08716E-4f,3.0518E-5f,-2.44141E-4f,-0.002685547f,-0.002334595f,-0.03138733f,-0.045837402f,-0.13945007f,-0.40408325f,-1.120224f,0.7383728f,-0.031082153f,0.110946655f,-0.02508545f,0.012939453f,-0.003479004f,7.47681E-4f,1.5259E-5f,-2.59399E-4f,-0.002792358f,-0.003005981f,-0.031661987f,-0.051132202f,-0.1348877f,-0.43165588f,-1.1277466f,0.71131897f,-0.044784546f,0.1058197f,-0.026535034f,0.011886597f,-0.003479004f,6.86646E-4f,1.5259E-5f,-2.89917E-4f,-0.00289917f,-0.003723145f,-0.031814575f,-0.056533813f,-0.12957764f,-0.45947266f,-1.1339264f,0.6839142f,-0.057617188f,0.10054016f,-0.027801514f,0.010848999f,-0.003463745f,6.2561E-4f,1.5259E-5f,-3.20435E-4f,-0.002990723f,-0.004486084f,-0.031845093f,-0.06199646f,-0.12347412f,-0.48747253f,-1.1387634f,0.6562195f,-0.06959534f,0.09516907f,-0.028884888f,0.009841919f,-0.003433228f,5.79834E-4f,1.5259E-5f,-3.66211E-4f,-0.003082275f,-0.0052948f,-0.03173828f,-0.06752014f,-0.11657715f,-0.51560974f,-1.1422119f,0.6282959f,-0.08068848f,0.08970642f,-0.029785156f,0.008865356f,-0.003387451f,5.34058E-4f,1.5259E-5f,-3.96729E-4f,-0.003173828f,-0.006118774f,-0.03147888f,-0.07305908f,-0.1088562f,-0.54382324f,-1.1442871f,0.6002197f,-0.090927124f,0.08418274f,-0.030517578f,0.007919312f,-0.003326416f,4.73022E-4f,1.5259E-5f}, 16);
	
	public SynthesisFilter(int channelnumber, float factor, float[] eq)
	{
		v1			= new float[512];
		v2			= new float[512];
		samples		= new float[32];
		channel		= channelnumber;
		scalefactor	= factor;
		setEQ(eq);
		reset();
	}

	public void setEQ(float[] eq0)
	{
		this.eq = (eq==null)?new float[]{1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,}:eq0;
		if (eq.length<32)	throw new IllegalArgumentException("eq0");
	}
  
	public void reset()
	{
		for (int i=0; i<512; i++)	v1[i] = v2[i] = 0.0f;
		for (int i=0; i<32; i++) 	samples[i] = 0.0f;
		
		actual_v			= v1;
		actual_write_pos	= 15;
	}

	public void input_sample(float sample, int subbandnumber)
	{ samples[subbandnumber] = eq[subbandnumber]*sample; }
	
	public void input_samples(float[] s)
	{
		samples[31]	= s[31]*eq[31];
		samples[30]	= s[30]*eq[30];
		samples[29]	= s[29]*eq[29];
		samples[28]	= s[28]*eq[28];
		samples[27]	= s[27]*eq[27];
		samples[26]	= s[26]*eq[26];
		samples[25]	= s[25]*eq[25];
		samples[24]	= s[24]*eq[24];
		samples[23]	= s[23]*eq[23];
		samples[22]	= s[22]*eq[22];
		samples[21]	= s[21]*eq[21];
		samples[20]	= s[20]*eq[20];
		samples[19]	= s[19]*eq[19];
		samples[18]	= s[18]*eq[18];
		samples[17]	= s[17]*eq[17];
		samples[16]	= s[16]*eq[16];
		samples[15]	= s[15]*eq[15];
		samples[14]	= s[14]*eq[14];
		samples[13]	= s[13]*eq[13];
		samples[12]	= s[12]*eq[12];
		samples[11]	= s[11]*eq[11];
		samples[10]	= s[10]*eq[10];
		samples[9]	= s[9]*eq[9];
		samples[8]	= s[8]*eq[8];
		samples[7]	= s[7]*eq[7];
		samples[6]	= s[6]*eq[6];
		samples[5]	= s[5]*eq[5];
		samples[4]	= s[4]*eq[4];
		samples[3]	= s[3]*eq[3];
		samples[2]	= s[2]*eq[2];
		samples[1]	= s[1]*eq[1];
		samples[0]	= s[0]*eq[0];
	}
	
	private void compute_new_v()
	{
		//rewritten recursion (hard to read but pretty damn fast)
		
		float[] s		= samples;
		
		float s0		= s[0];
		float s1		= s[1];
		float s2		= s[2];
		float s3		= s[3];
		float s4		= s[4];
		float s5		= s[5];
		float s6		= s[6];
		float s7		= s[7];
		float s8		= s[8];
		float s9		= s[9];
		float s10		= s[10];	
		float s11		= s[11];
		float s12		= s[12];
		float s13		= s[13];
		float s14		= s[14];
		float s15		= s[15];
		float s16		= s[16];
		float s17		= s[17];
		float s18		= s[18];
		float s19		= s[19];
		float s20		= s[20];	
		float s21		= s[21];
		float s22		= s[22];
		float s23		= s[23];
		float s24		= s[24];
		float s25		= s[25];
		float s26		= s[26];
		float s27		= s[27];
		float s28		= s[28];
		float s29		= s[29];
		float s30		= s[30];	
		float s31		= s[31];

		float p0		= s0 + s31;
		float p1		= s1 + s30;
		float p2		= s2 + s29;
		float p3		= s3 + s28;
		float p4		= s4 + s27;
		float p5		= s5 + s26;
		float p6		= s6 + s25;
		float p7		= s7 + s24;
		float p8		= s8 + s23;
		float p9		= s9 + s22;
		float p10		= s10 + s21;
		float p11		= s11 + s20;
		float p12		= s12 + s19;
		float p13		= s13 + s18;
		float p14		= s14 + s17;
		float p15		= s15 + s16;
		float pp0		= p0 + p15;
		float pp1		= p1 + p14;
		float pp2		= p2 + p13;
		float pp3		= p3 + p12;
		float pp4		= p4 + p11;
		float pp5		= p5 + p10;
		float pp6		= p6 + p9;
		float pp7		= p7 + p8;
		float pp8		= (p0 - p15) * 0.5024193f;
		float pp9		= (p1 - p14) * 0.5224986f;
		float pp10		= (p2 - p13) * 0.56694406f;
		float pp11		= (p3 - p12) * 0.6468218f;
		float pp12		= (p4 - p11) * 0.7881546f;
		float pp13		= (p5 - p10) * 1.0606776f;
		float pp14		= (p6 - p9) * 1.7224472f;
		float pp15		= (p7 - p8) * 5.1011486f;

		p0				= pp0 + pp7;
		p1				= pp1 + pp6;
		p2				= pp2 + pp5;
		p3				= pp3 + pp4;
		p4				= (pp0 - pp7) * 0.5097956f;
		p5				= (pp1 - pp6) * 0.6013449f;
		p6				= (pp2 - pp5) * 0.8999762f;
		p7				= (pp3 - pp4) * 2.5629156f;
		p8				= pp8 + pp15;
		p9				= pp9 + pp14;
		p10				= pp10 + pp13;
		p11				= pp11 + pp12;
		p12				= (pp8 - pp15) * 0.5097956f;
		p13				= (pp9 - pp14) * 0.6013449f;
		p14				= (pp10 - pp13) * 0.8999762f;
		p15				= (pp11 - pp12) * 2.5629156f;
		pp0				= p0 + p3;
		pp1				= p1 + p2;
		pp2				= (p0 - p3) * 0.5411961f;
		pp3				= (p1 - p2) * 1.306563f;
		pp4				= p4 + p7;
		pp5				= p5 + p6;
		pp6				= (p4 - p7) * 0.5411961f;
		pp7				= (p5 - p6) * 1.306563f;
		pp8				= p8 + p11;
		pp9				= p9 + p10;
		pp10			= (p8 - p11) * 0.5411961f;
		pp11			= (p9 - p10) * 1.306563f;
		pp12			= p12 + p15;
		pp13			= p13 + p14;
		pp14			= (p12 - p15) * 0.5411961f;
		pp15			= (p13 - p14) * 1.306563f;
		p0				= pp0 + pp1;
		p1				= (pp0 - pp1) * 0.70710677f;
		p2				= pp2 + pp3;
		p3				= (pp2 - pp3) * 0.70710677f;
		p4				= pp4 + pp5;
		p5				= (pp4 - pp5) * 0.70710677f;
		p6				= pp6 + pp7;
		p7				= (pp6 - pp7) * 0.70710677f;
		p8				= pp8 + pp9;
		p9				= (pp8 - pp9) * 0.70710677f;
		p10				= pp10 + pp11;
		p11				= (pp10 - pp11) * 0.70710677f;
		p12				= pp12 + pp13;
		p13				= (pp12 - pp13) * 0.70710677f;
		p14				= pp14 + pp15;
		p15				= (pp14 - pp15) * 0.70710677f;

		float new_v12	= p7;
		float new_v4	= new_v12 + p5;
		float new_v19	= -new_v4 - p6;
		float new_v27	= -p6 - p7 - p4;
		float new_v14	= p15;
		float new_v10	= new_v14 + p11;
		float new_v6	= new_v10 + p13;
		float new_v2	= p15 + p13 + p9;
		float new_v17	= -new_v2 - p14;
		float tmp1		= -p14 - p15 - p10 - p11;
		float new_v21	= tmp1 - p13;
		float new_v29	= -p14 - p15 - p12 - p8;
		float new_v25	= tmp1 - p12;
		float new_v31	= -p0;
		float new_v0	= p1;
		float new_v8	= p3;
		float new_v23	= -new_v8 - p2;

		p0				= (s0 - s31) * 0.500603f;
		p1				= (s1 - s30) * 0.50547093f;
		p2				= (s2 - s29) * 0.5154473f;
		p3				= (s3 - s28) * 0.5310426f;
		p4				= (s4 - s27) * 0.5531039f;
		p5				= (s5 - s26) * 0.582935f;
		p6				= (s6 - s25) * 0.6225041f;
		p7				= (s7 - s24) * 0.6748083f;
		p8				= (s8 - s23) * 0.7445363f;
		p9				= (s9 - s22) * 0.8393496f;
		p10				= (s10 - s21) * 0.9725682f;
		p11				= (s11 - s20) * 1.1694399f;
		p12				= (s12 - s19) * 1.4841646f;
		p13				= (s13 - s18) * 2.057781f;
		p14				= (s14 - s17) * 3.4076085f;
		p15				= (s15 - s16) * 10.190008f;
		pp0				= p0 + p15;
		pp1				= p1 + p14;
		pp2				= p2 + p13;
		pp3				= p3 + p12;
		pp4				= p4 + p11;
		pp5				= p5 + p10;
		pp6				= p6 + p9;
		pp7				= p7 + p8;
		pp8				= (p0 - p15) * 0.5024193f;
		pp9				= (p1 - p14) * 0.5224986f;
		pp10			= (p2 - p13) * 0.56694406f;
		pp11			= (p3 - p12) * 0.6468218f;
		pp12			= (p4 - p11) * 0.7881546f;
		pp13			= (p5 - p10) * 1.0606776f;
		pp14			= (p6 - p9) * 1.7224472f;
		pp15			= (p7 - p8) * 5.1011486f;
		p0				= pp0 + pp7;
		p1				= pp1 + pp6;
		p2				= pp2 + pp5;
		p3				= pp3 + pp4;
		p4				= (pp0 - pp7) * 0.5097956f;
		p5				= (pp1 - pp6) * 0.6013449f;
		p6				= (pp2 - pp5) * 0.8999762f;
		p7				= (pp3 - pp4) * 2.5629156f;
		p8				= pp8 + pp15;
		p9				= pp9 + pp14;
		p10				= pp10 + pp13;
		p11				= pp11 + pp12;
		p12				= (pp8 - pp15) * 0.5097956f;
		p13				= (pp9 - pp14) * 0.6013449f;
		p14				= (pp10 - pp13) * 0.8999762f;
		p15				= (pp11 - pp12) * 2.5629156f;
		pp0				= p0 + p3;
		pp1				= p1 + p2;
		pp2				= (p0 - p3) * 0.5411961f;
		pp3				= (p1 - p2) * 1.306563f;
		pp4				= p4 + p7;
		pp5				= p5 + p6;
		pp6				= (p4 - p7) * 0.5411961f;
		pp7				= (p5 - p6) * 1.306563f;
		pp8				= p8 + p11;
		pp9				= p9 + p10;
		pp10			= (p8 - p11) * 0.5411961f;
		pp11			= (p9 - p10) * 1.306563f;
		pp12			= p12 + p15;
		pp13			= p13 + p14;
		pp14			= (p12 - p15) * 0.5411961f;
		pp15			= (p13 - p14) * 1.306563f;
		p0				= pp0 + pp1;
		p1				= (pp0 - pp1) * 0.70710677f;
		p2				= pp2 + pp3;
		p3				= (pp2 - pp3) * 0.70710677f;
		p4				= pp4 + pp5;
		p5				= (pp4 - pp5) * 0.70710677f;
		p6				= pp6 + pp7;
		p7				= (pp6 - pp7) * 0.70710677f;
		p8				= pp8 + pp9;
		p9				= (pp8 - pp9) * 0.70710677f;
		p10				= pp10 + pp11;
		p11				= (pp10 - pp11) * 0.70710677f;
		p12				= pp12 + pp13;
		p13				= (pp12 - pp13) * 0.70710677f;
		p14				= pp14 + pp15;
		p15				= (pp14 - pp15) * 0.70710677f;

		float new_v15	= p15;
		float new_v13	= new_v15 + p7;
		float new_v11	= new_v13 + p11;
		float new_v5	= new_v11 + p5 + p13;
		float new_v9	= p15 + p11 + p3;
		float new_v7	= new_v9 + p13;
		float new_v1	= (tmp1 = p13 + p15 + p9) + p1;
		float new_v16	= -new_v1 - p14;
		float new_v3	= tmp1 + p5 + p7;
		float new_v18	= -new_v3 - p6 - p14;
		float new_v22	= (tmp1 = -p10 - p11 - p14 - p15) - p13 - p2 - p3;
		float new_v20	= tmp1 - p13 - p5 - p6 - p7;
		float new_v24	= tmp1 - p12 - p2 - p3;
		float tmp2		= p4 + p6 + p7;
		float new_v26	= tmp1 - p12 - tmp2;
		float new_v30	= (tmp1 = -p8 - p12 - p14 - p15) - p0;
		float new_v28	= tmp1 - tmp2;
		float dest[]	= actual_v;	
		int pos			= actual_write_pos;

		dest[0 + pos]	= new_v0;
		dest[16 + pos]	= new_v1;
		dest[32 + pos]	= new_v2;
		dest[48 + pos]	= new_v3;
		dest[64 + pos]	= new_v4;
		dest[80 + pos]	= new_v5;
		dest[96 + pos]	= new_v6;
		dest[112 + pos]	= new_v7;
		dest[128 + pos]	= new_v8;
		dest[144 + pos]	= new_v9;
		dest[160 + pos]	= new_v10;
		dest[176 + pos]	= new_v11;
		dest[192 + pos]	= new_v12;
		dest[208 + pos]	= new_v13;
		dest[224 + pos]	= new_v14;
		dest[240 + pos]	= new_v15;
		dest[256 + pos] = 0.0f;
		dest[272 + pos] = -new_v15;
		dest[288 + pos] = -new_v14;
		dest[304 + pos] = -new_v13;
		dest[320 + pos] = -new_v12;
		dest[336 + pos] = -new_v11;
		dest[352 + pos] = -new_v10;
		dest[368 + pos] = -new_v9;
		dest[384 + pos] = -new_v8;
		dest[400 + pos] = -new_v7;
		dest[416 + pos] = -new_v6;
		dest[432 + pos] = -new_v5;
		dest[448 + pos] = -new_v4;
		dest[464 + pos] = -new_v3;
		dest[480 + pos] = -new_v2;
		dest[496 + pos] = -new_v1;

		dest			= (actual_v==v1) ? v2 : v1;
	
		dest[0 + pos]	= -new_v0;
		dest[16 + pos]	= new_v16;
		dest[32 + pos]	= new_v17;
		dest[48 + pos]	= new_v18;
		dest[64 + pos]	= new_v19;
		dest[80 + pos]	= new_v20;
		dest[96 + pos]	= new_v21;
		dest[112 + pos]	= new_v22;
		dest[128 + pos]	= new_v23;
		dest[144 + pos]	= new_v24;
		dest[160 + pos]	= new_v25;
		dest[176 + pos]	= new_v26;
		dest[192 + pos]	= new_v27;
		dest[208 + pos]	= new_v28;
		dest[224 + pos]	= new_v29;
		dest[240 + pos]	= new_v30;
		dest[256 + pos]	= new_v31;
		dest[272 + pos]	= new_v30;
		dest[288 + pos]	= new_v29;
		dest[304 + pos]	= new_v28;
		dest[320 + pos]	= new_v27;
		dest[336 + pos]	= new_v26;
		dest[352 + pos]	= new_v25;
		dest[368 + pos]	= new_v24;
		dest[384 + pos]	= new_v23;
		dest[400 + pos]	= new_v22;
		dest[416 + pos]	= new_v21;
		dest[432 + pos]	= new_v20;
		dest[448 + pos]	= new_v19;
		dest[464 + pos]	= new_v18;
		dest[480 + pos]	= new_v17;
		dest[496 + pos]	= new_v16; 			
	}

	private void compute_pcm_samples(Buffer buffer)
	{
		final float[] vp		= actual_v;
		final float[] tmpOut	= _tmpOut;
		int dvp					= 0;
		
		switch (actual_write_pos)
		{
			case 0	:
			{
				for( int i=0; i<32; i++)
				{
					final float[] dp	= d16[i];
		            tmpOut[i]			= (float)(((vp[0 + dvp] * dp[0]) + (vp[15 + dvp] * dp[1]) + (vp[14 + dvp] * dp[2]) + (vp[13 + dvp] * dp[3]) + (vp[12 + dvp] * dp[4]) + (vp[11 + dvp] * dp[5]) + (vp[10 + dvp] * dp[6]) + (vp[9 + dvp] * dp[7]) + (vp[8 + dvp] * dp[8]) + (vp[7 + dvp] * dp[9]) + (vp[6 + dvp] * dp[10]) + (vp[5 + dvp] * dp[11]) + (vp[4 + dvp] * dp[12]) + (vp[3 + dvp] * dp[13]) + (vp[2 + dvp] * dp[14]) + (vp[1 + dvp] * dp[15]) ) * scalefactor);
					dvp					+= 16;
				}
			}break;
			case 1	:
			{
				for(int i=0; i<32; i++)
				{
					final float[] dp	= d16[i];
		            tmpOut[i]			= (float)(((vp[1 + dvp] * dp[0]) + (vp[0 + dvp] * dp[1]) + (vp[15 + dvp] * dp[2]) + (vp[14 + dvp] * dp[3]) + (vp[13 + dvp] * dp[4]) + (vp[12 + dvp] * dp[5]) + (vp[11 + dvp] * dp[6]) + (vp[10 + dvp] * dp[7]) + (vp[9 + dvp] * dp[8]) + (vp[8 + dvp] * dp[9]) + (vp[7 + dvp] * dp[10]) + (vp[6 + dvp] * dp[11]) + (vp[5 + dvp] * dp[12]) + (vp[4 + dvp] * dp[13]) + (vp[3 + dvp] * dp[14]) + (vp[2 + dvp] * dp[15]) ) * scalefactor);
					dvp					+= 16;
				}
			}break;
			case 2	:
			{
				for(int i=0; i<32; i++)
				{
					final float[] dp	= d16[i];
		            tmpOut[i]			= (float)(((vp[2 + dvp] * dp[0]) + (vp[1 + dvp] * dp[1]) + (vp[0 + dvp] * dp[2]) + (vp[15 + dvp] * dp[3]) + (vp[14 + dvp] * dp[4]) + (vp[13 + dvp] * dp[5]) + (vp[12 + dvp] * dp[6]) + (vp[11 + dvp] * dp[7]) + (vp[10 + dvp] * dp[8]) + (vp[9 + dvp] * dp[9]) + (vp[8 + dvp] * dp[10]) + (vp[7 + dvp] * dp[11]) + (vp[6 + dvp] * dp[12]) + (vp[5 + dvp] * dp[13]) + (vp[4 + dvp] * dp[14]) + (vp[3 + dvp] * dp[15]) ) * scalefactor);
					dvp					+= 16;
				}
			}break;
			case 3	:
			{
				for(int i=0; i<32; i++)
				{
					final float[] dp	= d16[i];
		            tmpOut[i]			= (float)(((vp[3 + dvp] * dp[0]) + (vp[2 + dvp] * dp[1]) + (vp[1 + dvp] * dp[2]) + (vp[0 + dvp] * dp[3]) + (vp[15 + dvp] * dp[4]) + (vp[14 + dvp] * dp[5]) + (vp[13 + dvp] * dp[6]) + (vp[12 + dvp] * dp[7]) + (vp[11 + dvp] * dp[8]) + (vp[10 + dvp] * dp[9]) + (vp[9 + dvp] * dp[10]) + (vp[8 + dvp] * dp[11]) + (vp[7 + dvp] * dp[12]) + (vp[6 + dvp] * dp[13]) + (vp[5 + dvp] * dp[14]) + (vp[4 + dvp] * dp[15]) ) * scalefactor);
					dvp					+= 16;
				}	
			}break;
			case 4	:
			{
				for(int i=0; i<32; i++)
				{
					final float[] dp	= d16[i];
		            tmpOut[i]			= (float)(((vp[4 + dvp] * dp[0]) + (vp[3 + dvp] * dp[1]) + (vp[2 + dvp] * dp[2]) + (vp[1 + dvp] * dp[3]) + (vp[0 + dvp] * dp[4]) + (vp[15 + dvp] * dp[5]) + (vp[14 + dvp] * dp[6]) + (vp[13 + dvp] * dp[7]) + (vp[12 + dvp] * dp[8]) + (vp[11 + dvp] * dp[9]) + (vp[10 + dvp] * dp[10]) + (vp[9 + dvp] * dp[11]) + (vp[8 + dvp] * dp[12]) + (vp[7 + dvp] * dp[13]) + (vp[6 + dvp] * dp[14]) + (vp[5 + dvp] * dp[15]) ) * scalefactor);
					dvp					+= 16;
				}
			}break;
			case 5	:
			{
				for(int i=0; i<32; i++)
				{
					final float[] dp	= d16[i];
		            tmpOut[i]			= (float)(((vp[5 + dvp] * dp[0]) + (vp[4 + dvp] * dp[1]) + (vp[3 + dvp] * dp[2]) + (vp[2 + dvp] * dp[3]) + (vp[1 + dvp] * dp[4]) + (vp[0 + dvp] * dp[5]) + (vp[15 + dvp] * dp[6]) + (vp[14 + dvp] * dp[7]) + (vp[13 + dvp] * dp[8]) + (vp[12 + dvp] * dp[9]) + (vp[11 + dvp] * dp[10]) + (vp[10 + dvp] * dp[11]) + (vp[9 + dvp] * dp[12]) + (vp[8 + dvp] * dp[13]) + (vp[7 + dvp] * dp[14]) + (vp[6 + dvp] * dp[15]) ) * scalefactor);
					dvp					+= 16;
				}
			}break;
			case 6	:
			{
				for(int i=0; i<32; i++)
				{
					final float[] dp	= d16[i];
					tmpOut[i]			= (float)(((vp[6 + dvp] * dp[0]) + (vp[5 + dvp] * dp[1]) + (vp[4 + dvp] * dp[2]) + (vp[3 + dvp] * dp[3]) + (vp[2 + dvp] * dp[4]) + (vp[1 + dvp] * dp[5]) + (vp[0 + dvp] * dp[6]) + (vp[15 + dvp] * dp[7]) + (vp[14 + dvp] * dp[8]) + (vp[13 + dvp] * dp[9]) + (vp[12 + dvp] * dp[10]) + (vp[11 + dvp] * dp[11]) + (vp[10 + dvp] * dp[12]) + (vp[9 + dvp] * dp[13]) + (vp[8 + dvp] * dp[14]) + (vp[7 + dvp] * dp[15]) ) * scalefactor);
					dvp					+= 16;
				}
			}break;
			case 7	:
			{
				for(int i=0; i<32; i++)
				{
					final float[] dp	= d16[i];
		            tmpOut[i]			= (float)(((vp[7 + dvp] * dp[0]) + (vp[6 + dvp] * dp[1]) + (vp[5 + dvp] * dp[2]) + (vp[4 + dvp] * dp[3]) + (vp[3 + dvp] * dp[4]) + (vp[2 + dvp] * dp[5]) + (vp[1 + dvp] * dp[6]) + (vp[0 + dvp] * dp[7]) + (vp[15 + dvp] * dp[8]) + (vp[14 + dvp] * dp[9]) + (vp[13 + dvp] * dp[10]) + (vp[12 + dvp] * dp[11]) + (vp[11 + dvp] * dp[12]) + (vp[10 + dvp] * dp[13]) + (vp[9 + dvp] * dp[14]) + (vp[8 + dvp] * dp[15]) ) * scalefactor);
					dvp					+= 16;
				}		
			}break;
			case 8	:
			{
				for(int i=0; i<32; i++)
				{
					final float[] dp	= d16[i];
		            tmpOut[i]			= (float)(((vp[8 + dvp] * dp[0]) + (vp[7 + dvp] * dp[1]) + (vp[6 + dvp] * dp[2]) + (vp[5 + dvp] * dp[3]) + (vp[4 + dvp] * dp[4]) + (vp[3 + dvp] * dp[5]) + (vp[2 + dvp] * dp[6]) + (vp[1 + dvp] * dp[7]) + (vp[0 + dvp] * dp[8]) + (vp[15 + dvp] * dp[9]) + (vp[14 + dvp] * dp[10]) + (vp[13 + dvp] * dp[11]) + (vp[12 + dvp] * dp[12]) + (vp[11 + dvp] * dp[13]) + (vp[10 + dvp] * dp[14]) + (vp[9 + dvp] * dp[15]) ) * scalefactor);
					dvp					+= 16;
				}
			}break;
			case 9	:
			{
				for(int i=0; i<32; i++)
				{
					final float[] dp	= d16[i];
		            tmpOut[i]			= (float)(((vp[9 + dvp] * dp[0]) + (vp[8 + dvp] * dp[1]) + (vp[7 + dvp] * dp[2]) + (vp[6 + dvp] * dp[3]) + (vp[5 + dvp] * dp[4]) + (vp[4 + dvp] * dp[5]) + (vp[3 + dvp] * dp[6]) + (vp[2 + dvp] * dp[7]) + (vp[1 + dvp] * dp[8]) + (vp[0 + dvp] * dp[9]) + (vp[15 + dvp] * dp[10]) + (vp[14 + dvp] * dp[11]) + (vp[13 + dvp] * dp[12]) + (vp[12 + dvp] * dp[13]) + (vp[11 + dvp] * dp[14]) + (vp[10 + dvp] * dp[15]) ) * scalefactor);
					dvp					+= 16;
				}
			}break;
			case 10	:
			{
				for(int i=0; i<32; i++)
				{
					final float[] dp	= d16[i];
		            tmpOut[i]			= (float)(((vp[10 + dvp] * dp[0]) + (vp[9 + dvp] * dp[1]) + (vp[8 + dvp] * dp[2]) + (vp[7 + dvp] * dp[3]) + (vp[6 + dvp] * dp[4]) + (vp[5 + dvp] * dp[5]) + (vp[4 + dvp] * dp[6]) + (vp[3 + dvp] * dp[7]) + (vp[2 + dvp] * dp[8]) + (vp[1 + dvp] * dp[9]) + (vp[0 + dvp] * dp[10]) + (vp[15 + dvp] * dp[11]) + (vp[14 + dvp] * dp[12]) + (vp[13 + dvp] * dp[13]) + (vp[12 + dvp] * dp[14]) + (vp[11 + dvp] * dp[15]) ) * scalefactor);
					dvp					+= 16;
				}		
			}break;
			case 11	:
			{
				for(int i=0; i<32; i++)
				{
					final float[] dp	= d16[i];
		            tmpOut[i]			= (float)(((vp[11 + dvp] * dp[0]) + (vp[10 + dvp] * dp[1]) + (vp[9 + dvp] * dp[2]) + (vp[8 + dvp] * dp[3]) + (vp[7 + dvp] * dp[4]) + (vp[6 + dvp] * dp[5]) + (vp[5 + dvp] * dp[6]) + (vp[4 + dvp] * dp[7]) + (vp[3 + dvp] * dp[8]) + (vp[2 + dvp] * dp[9]) + (vp[1 + dvp] * dp[10]) + (vp[0 + dvp] * dp[11]) + (vp[15 + dvp] * dp[12]) + (vp[14 + dvp] * dp[13]) + (vp[13 + dvp] * dp[14]) + (vp[12 + dvp] * dp[15]) ) * scalefactor);
					dvp					+= 16;
				}
			}break;
			case 12	:
			{
				for(int i=0; i<32; i++)
				{
					final float[] dp	= d16[i];
		            tmpOut[i]			= (float)(((vp[12 + dvp] * dp[0]) + (vp[11 + dvp] * dp[1]) + (vp[10 + dvp] * dp[2]) + (vp[9 + dvp] * dp[3]) + (vp[8 + dvp] * dp[4]) + (vp[7 + dvp] * dp[5]) + (vp[6 + dvp] * dp[6]) + (vp[5 + dvp] * dp[7]) + (vp[4 + dvp] * dp[8]) + (vp[3 + dvp] * dp[9]) + (vp[2 + dvp] * dp[10]) + (vp[1 + dvp] * dp[11]) + (vp[0 + dvp] * dp[12]) + (vp[15 + dvp] * dp[13]) + (vp[14 + dvp] * dp[14]) + (vp[13 + dvp] * dp[15]) ) * scalefactor);
					dvp					+= 16;
				}
			}break;
			case 13	:
			{
				for(int i=0; i<32; i++)
				{
					final float[] dp	= d16[i];
		            tmpOut[i]			= (float)(((vp[13 + dvp] * dp[0]) + (vp[12 + dvp] * dp[1]) + (vp[11 + dvp] * dp[2]) + (vp[10 + dvp] * dp[3]) + (vp[9 + dvp] * dp[4]) + (vp[8 + dvp] * dp[5]) + (vp[7 + dvp] * dp[6]) + (vp[6 + dvp] * dp[7]) + (vp[5 + dvp] * dp[8]) + (vp[4 + dvp] * dp[9]) + (vp[3 + dvp] * dp[10]) + (vp[2 + dvp] * dp[11]) + (vp[1 + dvp] * dp[12]) + (vp[0 + dvp] * dp[13]) + (vp[15 + dvp] * dp[14]) + (vp[14 + dvp] * dp[15]) ) * scalefactor);
					dvp					+= 16;
				}		
			}break;
			case 14	:
			{
				for(int i=0; i<32; i++)
				{
					final float[] dp	= d16[i];
		            tmpOut[i]			= (float)(((vp[14 + dvp] * dp[0]) + (vp[13 + dvp] * dp[1]) + (vp[12 + dvp] * dp[2]) + (vp[11 + dvp] * dp[3]) + (vp[10 + dvp] * dp[4]) + (vp[9 + dvp] * dp[5]) + (vp[8 + dvp] * dp[6]) + (vp[7 + dvp] * dp[7]) + (vp[6 + dvp] * dp[8]) + (vp[5 + dvp] * dp[9]) + (vp[4 + dvp] * dp[10]) + (vp[3 + dvp] * dp[11]) + (vp[2 + dvp] * dp[12]) + (vp[1 + dvp] * dp[13]) + (vp[0 + dvp] * dp[14]) + (vp[15 + dvp] * dp[15]) ) * scalefactor);
					dvp					+= 16;
				}
			}break;
			case 15	:
			{
				for(int i=0; i<32; i++)
				{
					final float dp[]	= d16[i];
		            tmpOut[i]			= (float)(((vp[15 + dvp] * dp[0]) + (vp[14 + dvp] * dp[1]) + (vp[13 + dvp] * dp[2]) + (vp[12 + dvp] * dp[3]) + (vp[11 + dvp] * dp[4]) + (vp[10 + dvp] * dp[5]) + (vp[9 + dvp] * dp[6]) + (vp[8 + dvp] * dp[7]) + (vp[7 + dvp] * dp[8]) + (vp[6 + dvp] * dp[9]) + (vp[5 + dvp] * dp[10]) + (vp[4 + dvp] * dp[11]) + (vp[3 + dvp] * dp[12]) + (vp[2 + dvp] * dp[13]) + (vp[1 + dvp] * dp[14]) + (vp[0 + dvp] * dp[15]) ) * scalefactor);			
					dvp					+= 16;
				}
			}break;
		}

		if (buffer!=null)
			appendSamples(buffer, channel, _tmpOut);
	}

	public void appendSamples(Buffer buffer, int channel, float[] f)
	{ for (int i=0; i<32;) buffer.append(channel, clip(f[i++])); }

	private final short clip(float sample)
	{ return ((sample > 32767.0f) ? 32767 : ((sample < -32768.0f) ? -32768 : (short) sample)); }
	
	public void calculate_pcm_samples(Buffer buffer)
	{
		compute_new_v();	
		compute_pcm_samples(buffer);

		actual_write_pos	= (actual_write_pos + 1) & 0xf;
		actual_v			= (actual_v == v1) ? v2 : v1;

		for (int p=0;p<32;p++) 
			samples[p] = 0.0f;
	}
  
	//FIXME use in Core
	static private float[][] splitArray(final float[] array, final int blockSize)
	{
		int size		= array.length / blockSize;
		float[][] split	= new float[size][];
		
		for (int i=0; i<size; i++)
			split[i] = subArray(array, i*blockSize, blockSize);
		
		return split;
	}

	//FIXME use in Core
	static private float[] subArray(final float[] array, final int offs, int len)
	{
		if (offs+len > array.length)	len = array.length-offs;
		if (len < 0)					len = 0;
		
		float[] subarray = new float[len];
		
		for (int i=0; i<len; i++)
			subarray[i] = array[offs+i];
		
		return subarray;
	}

}