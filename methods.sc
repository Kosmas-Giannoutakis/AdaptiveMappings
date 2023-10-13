+ Object {

	audify {
		^if(
			this.isArray,
			{
				if(this[0].isUGen,
					{this},
					{this.collect{arg i; i.ar}
					}
				)
			},
			{
				if(this.isUGen,
					{[this]},
					{if(this.ar.isArray,
						{this.ar},
						{[this.ar]})}
				)
			}
		);

	}

	dPitch {
		arg lagTime=0.1, timeWindow=5;

		var in=this.audify;
		var numChannels=in.size;
		var sampNum=(timeWindow/ControlDur.ir).round; //calculate number of sumples
		var pitch=numChannels.collect{arg i; A2K.kr(ZeroCrossing.ar(in[i])).cpsmidi}; //track raw pitch, convert to midi
		var ringBuf=numChannels.collect{LocalBuf(sampNum)}; // create buffers for each channel
		var phasor=Phasor.kr(0,1,0,sampNum);

		//circularly write raw pitch in correspinding buffers
		var writeBuf=numChannels.collect{arg i; BufWr.kr(pitch[i], ringBuf[i], phasor)};

		// extract min values from the ring buffers, substract infinitesimal value for preventing nan in stable signals
		var min=numChannels.collect{arg i; (BufMin.kr(ringBuf[i])[0].clip(0,127)-1e-5).lag};
		// extract max values from the ring buffers, add infinitesimal value for preventing nan in stable signals
		var max=numChannels.collect{arg i; (BufMax.kr(ringBuf[i])[0].clip(0,127)+1e-5).lag};

		// unamp pitch to the range 0-1 for each channel
		var unmapedPitch=numChannels.collect{arg i; [min[i], max[i]].asSpec.unmap(pitch[i])};

		^unmapedPitch.lag(lagTime);

	}

	mdPitch {
		arg low=0, high=1, warp=\lin, lagTime=0.1, timeWindow=5;
		^[low, high, warp].asSpec.map(this.dPitch(lagTime, timeWindow))
	}

	sPitch {

		arg lagTime=0.1, lowFreq=40, highFreq=20000, warp=\exp;
		var in=this.audify;
		var unmaping=[lowFreq,highFreq,warp].asSpec.unmap(ZeroCrossing.ar(in));
		^unmaping.lag(lagTime);

	}

	msPitch {
		arg low=0, high=1, warp=\lin, lagTime=0.1, lowFreq=40, highFreq=20000, warpFreq=\exp;
		^[low, high, warp].asSpec.map(this.sPitch(lagTime, lowFreq, highFreq, warpFreq))
	}

	dAmp {
		arg lagTime=0.1, timeWindow=5;

		var in=this.audify;
		var numChannels=in.size;
		var sampNum=(timeWindow/ControlDur.ir).round;
		var amps=numChannels.collect{arg i; Amplitude.kr(in[i]).ampdb};
		var ringBuf=numChannels.collect{LocalBuf(sampNum)};
		var phasor=Phasor.kr(0,1,0,sampNum);

		var writeBuf=numChannels.collect{arg i; BufWr.kr(amps[i], ringBuf[i], phasor)};

		var min=numChannels.collect{arg i; (BufMin.kr(ringBuf[i])[0].clip(-100,0)-1e-5).lag};
		var max=numChannels.collect{arg i; (BufMax.kr(ringBuf[i])[0].clip(-100,0)+1e-5).lag};

		var unmapedAmp=numChannels.collect{arg i; [min[i], max[i]].asSpec.unmap(amps[i])};

		^unmapedAmp.lag(lagTime);

	}

	mdAmp {
		arg low=0, high=1, warp=\lin, lagTime=0.1, timeWindow=5;
		^[low, high, warp].asSpec.map(this.dAmp(lagTime, timeWindow))
	}

	sAmp {

		arg lagTime=0.1, lowAmp= -60, highAmp=0, warp=\lin;
		var in=this.audify;
		var unmaping=[lowAmp,highAmp,warp].asSpec.unmap(Amplitude.ar(in).ampdb);
		^unmaping.lag(lagTime);

	}

	msAmp {
		arg low=0, high=1, warp=\lin, lagTime=0.1, lowAmp= -60, highAmp=0, warpAmp=\lin;
		^[low, high, warp].asSpec.map(this.sAmp(lagTime, lowAmp, highAmp, warpAmp))
	}

	dRms {
		arg lagTime=0.1, timeWindow=5;

		var in=this.audify;
		var numChannels=in.size;
		var sampNum=(timeWindow/ControlDur.ir).round;
		var rms=numChannels.collect{arg i; (RunningSum.kr(in[i].squared)/40).sqrt.ampdb};
		var ringBuf=numChannels.collect{LocalBuf(sampNum)};
		var phasor=Phasor.kr(0,1,0,sampNum);

		var writeBuf=numChannels.collect{arg i; BufWr.kr(rms[i], ringBuf[i], phasor)};

		var min=numChannels.collect{arg i; (BufMin.kr(ringBuf[i])[0].clip(-100,0)-1e-5).lag};
		var max=numChannels.collect{arg i; (BufMax.kr(ringBuf[i])[0].clip(-100,0)+1e-5).lag};

		var unmapedRms=numChannels.collect{arg i; [min[i], max[i]].asSpec.unmap(rms[i])};

		^unmapedRms.lag(lagTime);

	}

	mdRms {
		arg low=0, high=1, warp=\lin, lagTime=0.1, timeWindow=5;
		^[low, high, warp].asSpec.map(this.dRms(lagTime, timeWindow))
	}

	sRms {

		arg lagTime=0.1, lowRms= -60, highRms=0, warp=\lin;
		var in=this.audify;
		var unmaping=[lowRms,highRms,warp].asSpec.unmap(RunningSum.rms(in).ampdb);
		^unmaping.lag(lagTime);

	}

	msRms {
		arg low=0, high=1, warp=\lin, lagTime=0.1, lowRms= -60, highRms=0, warpRms=\lin;
		^[low, high, warp].asSpec.map(this.sRms(lagTime, lowRms, highRms, warpRms))
	}

	dLoud {
		arg lagTime=0.1, timeWindow=5;

		var in=this.audify;
		var numChannels=in.size;
		var sampNum=(timeWindow/ControlDur.ir).round;
		var chain=in.collect{arg item; FFT(LocalBuf(1024), item)};
		var loudness=numChannels.collect{arg i; Loudness.kr(chain[i])};
		var ringBuf=numChannels.collect{LocalBuf(sampNum)};
		var phasor=Phasor.kr(0,1,0,sampNum);

		var writeBuf=numChannels.collect{arg i; BufWr.kr(loudness[i], ringBuf[i], phasor)};

		var min=numChannels.collect{arg i; (BufMin.kr(ringBuf[i])[0].clip(0,64)-1e-5).lag};
		var max=numChannels.collect{arg i; (BufMax.kr(ringBuf[i])[0].clip(0,64)+1e-5).lag};

		var unmapedLoudness=numChannels.collect{arg i; [min[i], max[i]].asSpec.unmap(loudness[i])};

		^unmapedLoudness.lag(lagTime);

	}

	dFlat {
		arg lagTime=0.1, timeWindow=5;

		var in=this.audify;
		var numChannels=in.size;
		var sampNum=(timeWindow/ControlDur.ir).round;
		var chain=in.collect{arg item; FFT(LocalBuf(1024), item)};
		var flat=numChannels.collect{arg i; SpecFlatness.kr(chain[i])};
		var ringBuf=numChannels.collect{LocalBuf(sampNum)};
		var phasor=Phasor.kr(0,1,0,sampNum);

		var writeBuf=numChannels.collect{arg i; BufWr.kr(flat[i], ringBuf[i], phasor)};

		var min=numChannels.collect{arg i; (BufMin.kr(ringBuf[i])[0].clip(0,0.8)-1e-5).lag};
		var max=numChannels.collect{arg i; (BufMax.kr(ringBuf[i])[0].clip(0,0.8)+1e-5).lag};

		var unmapedFlatness=numChannels.collect{arg i; [min[i], max[i]].asSpec.unmap(flat[i])};

		^unmapedFlatness.lag(lagTime);

	}

	mdFlat {
		arg low=0, high=1, warp=\lin, lagTime=0.1, timeWindow=5;
		^[low, high, warp].asSpec.map(this.dFlat(lagTime, timeWindow))
	}

	sFlat {

		arg lagTime=0.1, lowFlat=0, highFlat=0.8, warp=\lin;
		var in=this.audify;
		var numChannels=in.size;
		var chain=in.collect{arg item; FFT(LocalBuf(1024), item)};
		var flat=numChannels.collect{arg i; SpecFlatness.kr(chain[i])};
		var unmaping=[lowFlat,highFlat,warp].asSpec.unmap(flat);
		^unmaping.lag(lagTime);

	}

	msFlat {
		arg low=0, high=1, warp=\lin, lagTime=0.1, lowFlat= -60, highFlat=0, warpFlat=\lin;
		^[low, high, warp].asSpec.map(this.sFlat(lagTime, lowFlat, highFlat, warpFlat))
	}

	dPcile {
		arg lagTime=0.1, timeWindow=5;

		var in=this.audify;
		var numChannels=in.size;
		var sampNum=(timeWindow/ControlDur.ir).round;
		var chain=in.collect{arg item; FFT(LocalBuf(1024), item)};
		var pcile=numChannels.collect{arg i; SpecPcile.kr(chain[i])};
		var ringBuf=numChannels.collect{LocalBuf(sampNum)};
		var phasor=Phasor.kr(0,1,0,sampNum);

		var writeBuf=numChannels.collect{arg i; BufWr.kr(pcile[i], ringBuf[i], phasor)};

		var min=numChannels.collect{arg i; (BufMin.kr(ringBuf[i])[0].clip(0,15000)-1e-5).lag};
		var max=numChannels.collect{arg i; (BufMax.kr(ringBuf[i])[0].clip(0,15000)+1e-5).lag};

		var unmapedPcile=numChannels.collect{arg i; [min[i], max[i]].asSpec.unmap(pcile[i])};

		^unmapedPcile.lag(lagTime);

	}

	mdPcile {
		arg low=0, high=1, warp=\lin, lagTime=0.1, timeWindow=5;
		^[low, high, warp].asSpec.map(this.dPcile(lagTime, timeWindow))
	}

	sPcile {

		arg lagTime=0.1, lowPcile=40, highPcile=20000, warp=\exp;
		var in=this.audify;
		var numChannels=in.size;
		var chain=in.collect{arg item; FFT(LocalBuf(1024), item)};
		var pcile=numChannels.collect{arg i; SpecPcile.kr(chain[i])};
		var unmaping=[lowPcile,highPcile,warp].asSpec.unmap(pcile);
		^unmaping.lag(lagTime);

	}


	msPcile {
		arg low=0, high=1, warp=\lin, lagTime=0.1, lowPcile= -60, highPcile=0, warpPcile=\exp;
		^[low, high, warp].asSpec.map(this.sPcile(lagTime, lowPcile, highPcile, warpPcile))
	}

	dCent {
		arg lagTime=0.1, timeWindow=5;

		var in=this.audify;
		var numChannels=in.size;
		var sampNum=(timeWindow/ControlDur.ir).round;
		var chain=in.collect{arg item; FFT(LocalBuf(1024), item)};
		var centroid=numChannels.collect{arg i; SpecCentroid.kr(chain[i])};
		var ringBuf=numChannels.collect{LocalBuf(sampNum)};
		var phasor=Phasor.kr(0,1,0,sampNum);

		var writeBuf=numChannels.collect{arg i; BufWr.kr(centroid[i], ringBuf[i], phasor)};

		var min=numChannels.collect{arg i; (BufMin.kr(ringBuf[i])[0].clip(0,15000)-1e-5).lag};
		var max=numChannels.collect{arg i; (BufMax.kr(ringBuf[i])[0].clip(0,15000)+1e-5).lag};

		var unmapedCentroid=numChannels.collect{arg i; [min[i], max[i]].asSpec.unmap(centroid[i])};

		^unmapedCentroid.lag(lagTime);

	}

	mdCent {
		arg low=0, high=1, warp=\lin, lagTime=0.1, timeWindow=5;
		^[low, high, warp].asSpec.map(this.dCent(lagTime, timeWindow))
	}

	sCent {

		arg lagTime=0.1, lowCentroid=40, highCentroid=20000, warp=\exp;
		var in=this.audify;
		var numChannels=in.size;
		var chain=in.collect{arg item; FFT(LocalBuf(1024), item)};
		var centroid=numChannels.collect{arg i; SpecCentroid.kr(chain[i])};
		var unmaping=[lowCentroid,highCentroid,warp].asSpec.unmap(centroid);
		^unmaping.lag(lagTime);

	}

	msCent {
		arg low=0, high=1, warp=\lin, lagTime=0.1, lowCentroid= -60, highCentroid=0, warpCentroid=\exp;
		^[low, high, warp].asSpec.map(this.sCent(lagTime, lowCentroid, highCentroid, warpCentroid))
	}

	dSpread {
		arg lagTime=0.1, timeWindow=5;

		var in=this.audify;
		var numChannels=in.size;
		var sampNum=(timeWindow/ControlDur.ir).round;
		var chain=in.collect{arg item; FFT(LocalBuf(1024), item)};
		var spread=numChannels.collect{arg i; FFTSpread.kr(chain[i])};
		var ringBuf=numChannels.collect{LocalBuf(sampNum)};
		var phasor=Phasor.kr(0,1,0,sampNum);

		var writeBuf=numChannels.collect{arg i; BufWr.kr(spread[i], ringBuf[i], phasor)};

		var min=numChannels.collect{arg i; (BufMin.kr(ringBuf[i])[0].clip(1e+4,7e+7)-1e-5).lag};
		var max=numChannels.collect{arg i; (BufMax.kr(ringBuf[i])[0].clip(1e+4,7e+7)+1e-5).lag};

		var unmapedSpread=numChannels.collect{arg i; [min[i], max[i]].asSpec.unmap(spread[i])};

		^unmapedSpread.lag(lagTime);

	}

	mdSpread {
		arg low=0, high=1, warp=\lin, lagTime=0.1, timeWindow=5;
		^[low, high, warp].asSpec.map(this.dSpread(lagTime, timeWindow))
	}

	sSpread {

		arg lagTime=0.1, lowSpread=40, highSpread=20000, warp=\exp;
		var in=this.audify;
		var numChannels=in.size;
		var chain=in.collect{arg item; FFT(LocalBuf(1024), item)};
		var spread=numChannels.collect{arg i; FFTSpread.kr(chain[i])};
		var unmaping=[lowSpread,highSpread,warp].asSpec.unmap(spread);
		^unmaping.lag(lagTime);

	}

	msSpread {
		arg low=0, high=1, warp=\lin, lagTime=0.1, lowSpread= -60, highSpread=0, warpSpread=\exp;
		^[low, high, warp].asSpec.map(this.sSpread(lagTime, lowSpread, highSpread, warpSpread))
	}

	dSlope {
		arg lagTime=0.1, timeWindow=5;

		var in=this.audify;
		var numChannels=in.size;
		var sampNum=(timeWindow/ControlDur.ir).round;
		var chain=in.collect{arg item; FFT(LocalBuf(1024), item)};
		var slope=numChannels.collect{arg i; FFTSlope.kr(chain[i])};
		var ringBuf=numChannels.collect{LocalBuf(sampNum)};
		var phasor=Phasor.kr(0,1,0,sampNum);

		var writeBuf=numChannels.collect{arg i; BufWr.kr(slope[i], ringBuf[i], phasor)};

		var min=numChannels.collect{arg i; (BufMin.kr(ringBuf[i])[0].clip(-1,1)-1e-5).lag};
		var max=numChannels.collect{arg i; (BufMax.kr(ringBuf[i])[0].clip(-1,1)+1e-5).lag};

		var unmapedSlope=numChannels.collect{arg i; [min[i], max[i]].asSpec.unmap(slope[i])};

		^unmapedSlope.lag(lagTime);

	}

	mdSlope {
		arg low=0, high=1, warp=\lin, lagTime=0.1, timeWindow=5;
		^[low, high, warp].asSpec.map(this.dSlope(lagTime, timeWindow))
	}

	sSlope {

		arg lagTime=0.1, lowSlope= -1, highSlope=1, warp=\lin;
		var in=this.audify;
		var numChannels=in.size;
		var chain=in.collect{arg item; FFT(LocalBuf(1024), item)};
		var flat=numChannels.collect{arg i; FFTSlope.kr(chain[i])};
		var unmaping=[lowSlope,highSlope,warp].asSpec.unmap(flat);
		^unmaping.lag(lagTime);

	}

	msSlope {
		arg low=0, high=1, warp=\lin, lagTime=0.1, lowSlope= -60, highSlope=0, warpSlope=\lin;
		^[low, high, warp].asSpec.map(this.sSlope(lagTime, lowSlope, highSlope, warpSlope))
	}

	dCrest {
		arg lagTime=0.1, timeWindow=5;

		var in=this.audify;
		var numChannels=in.size;
		var sampNum=(timeWindow/ControlDur.ir).round;
		var chain=in.collect{arg item; FFT(LocalBuf(1024), item)};
		var crest=numChannels.collect{arg i; FFTCrest.kr(chain[i])};
		var ringBuf=numChannels.collect{LocalBuf(sampNum)};
		var phasor=Phasor.kr(0,1,0,sampNum);

		var writeBuf=numChannels.collect{arg i; BufWr.kr(crest[i], ringBuf[i], phasor)};

		var min=numChannels.collect{arg i; (BufMin.kr(ringBuf[i])[0].clip(0,15000)-1e-5).lag};
		var max=numChannels.collect{arg i; (BufMax.kr(ringBuf[i])[0].clip(0,15000)+1e-5).lag};

		var unmapedCrest=numChannels.collect{arg i; [min[i], max[i]].asSpec.unmap(crest[i])};

		^unmapedCrest.lag(lagTime);

	}

	mdCrest {
		arg low=0, high=1, warp=\lin, lagTime=0.1, timeWindow=5;
		^[low, high, warp].asSpec.map(this.dCrest(lagTime, timeWindow))
	}

	sCrest {

		arg lagTime=0.1, lowCrest=40, highCrest=20000, warp=\exp;
		var in=this.audify;
		var numChannels=in.size;
		var chain=in.collect{arg item; FFT(LocalBuf(1024), item)};
		var crest=numChannels.collect{arg i; FFTCrest.kr(chain[i])};
		var unmaping=[lowCrest,highCrest,warp].asSpec.unmap(crest);
		^unmaping.lag(lagTime);

	}

	msCrest {
		arg low=0, high=1, warp=\lin, lagTime=0.1, lowCrest= -60, highCrest=0, warpCrest=\exp;
		^[low, high, warp].asSpec.map(this.sCrest(lagTime, lowCrest, highCrest, warpCrest))
	}

	dOnsets {

		var in=this.audify;
		var numChannels=in.size;
		var amps=numChannels.collect{arg i; Amplitude.kr(in[i])};
		var chain=in.collect{arg item; FFT(LocalBuf(1024), item)};
		var onsets=numChannels.collect{arg i; Onsets.kr(chain[i], amps[i]-0.01)};
		^onsets;

	}

	dOnsetsJA {

		var in=this.audify;
		var numChannels=in.size;
		var amps=numChannels.collect{arg i; Amplitude.ar(in[i])};
		var chain=in.collect{arg item; FFT(LocalBuf(2048), item)};
		var onsets=numChannels.collect{arg i; PV_JensenAndersen.ar(chain[i], threshold: amps[i])};
		^onsets;

	}

	dOnsetsHF {

		var in=this.audify;
		var numChannels=in.size;
		var amps=numChannels.collect{arg i; Amplitude.ar(in[i])};
		var chain=in.collect{arg item; FFT(LocalBuf(2048), item)};
		var onsets=numChannels.collect{arg i; PV_HainsworthFoote.ar(chain[i], threshold: amps[i]-0.001)};
		^onsets;

	}

}
