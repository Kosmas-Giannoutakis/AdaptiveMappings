+ Object {

	specify {
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
		arg timeWindow=5, lagTime=0.1;

		var in=this.specify;
		var numChannels=in.size;
		var sampNum=(timeWindow/ControlDur.ir).round; //calculate number of sumples
		var pitch=numChannels.collect{arg i; A2K.kr(ZeroCrossing.ar(in[i])).cpsmidi}; //track raw pitch, convert to midi
		var ringBuf=numChannels.collect{LocalBuf(sampNum)}; // create buffers for each channel
		var phasor=Phasor.kr(0,1,0,sampNum);

		//circularly write raw pitch in correspinding buffers
		var writeBuf=numChannels.collect{arg i; BufWr.kr(pitch[i], ringBuf[i], phasor)};

		// extract min values from the ring buffers, substract infinitesimal value for preventing nan in stable signals
		var min=numChannels.collect{arg i; (BufMin.kr(ringBuf[i])[0]-1e-5).clip(0,127).lag};
		// extract max values from the ring buffers, add infinitesimal value for preventing nan in stable signals
		var max=numChannels.collect{arg i; (BufMax.kr(ringBuf[i])[0]+1e-5).clip(0,127).lag};

		// unamp pitch to the range 0-1 for each channel
		var unmapedPitch=numChannels.collect{arg i; [min[i], max[i]].asSpec.unmap(pitch[i])};

		^unmapedPitch.lag(lagTime);

	}

	dAmp {
		arg timeWindow=5, lagTime=0.1;

		var in=this.specify;
		var numChannels=in.size;
		var sampNum=(timeWindow/ControlDur.ir).round;
		var amps=numChannels.collect{arg i; Amplitude.kr(in[i]).ampdb};
		var ringBuf=numChannels.collect{LocalBuf(sampNum)};
		var phasor=Phasor.kr(0,1,0,sampNum);

		var writeBuf=numChannels.collect{arg i; BufWr.kr(amps[i], ringBuf[i], phasor)};

		var min=numChannels.collect{arg i; (BufMin.kr(ringBuf[i])[0]-1e-5).clip(-100,0).lag};
		var max=numChannels.collect{arg i; (BufMax.kr(ringBuf[i])[0]+1e-5).clip(-100,0).lag};

		var unmapedAmp=numChannels.collect{arg i; [min[i], max[i]].asSpec.unmap(amps[i])};

		^unmapedAmp.lag(lagTime);

	}

	dRms {
		arg timeWindow=5, lagTime=0.1;

		var in=this.specify;
		var numChannels=in.size;
		var sampNum=(timeWindow/ControlDur.ir).round;
		var rms=numChannels.collect{arg i; (RunningSum.kr(in[i].squared)/40).sqrt.ampdb};
		var ringBuf=numChannels.collect{LocalBuf(sampNum)};
		var phasor=Phasor.kr(0,1,0,sampNum);

		var writeBuf=numChannels.collect{arg i; BufWr.kr(rms[i], ringBuf[i], phasor)};

		var min=numChannels.collect{arg i; (BufMin.kr(ringBuf[i])[0]-1e-5).clip(-100,0).lag};
		var max=numChannels.collect{arg i; (BufMax.kr(ringBuf[i])[0]+1e-5).clip(-100,0).lag};

		var unmapedRms=numChannels.collect{arg i; [min[i], max[i]].asSpec.unmap(rms[i])};

		^unmapedRms.lag(lagTime);

	}

	dLoud {
		arg timeWindow=5, lagTime=0.1;

		var in=this.specify;
		var numChannels=in.size;
		var sampNum=(timeWindow/ControlDur.ir).round;
		var chain=in.collect{arg item; FFT(LocalBuf(1024), item)};
		var loudness=numChannels.collect{arg i; Loudness.kr(chain[i])};
		var ringBuf=numChannels.collect{LocalBuf(sampNum)};
		var phasor=Phasor.kr(0,1,0,sampNum);

		var writeBuf=numChannels.collect{arg i; BufWr.kr(loudness[i], ringBuf[i], phasor)};

		var min=numChannels.collect{arg i; (BufMin.kr(ringBuf[i])[0]-1e-5).clip(0,64).lag};
		var max=numChannels.collect{arg i; (BufMax.kr(ringBuf[i])[0]+1e-5).clip(0,64).lag};

		var unmapedLoudness=numChannels.collect{arg i; [min[i], max[i]].asSpec.unmap(loudness[i])};

		^unmapedLoudness.lag(lagTime);

	}

	dFlat {
		arg timeWindow=5, lagTime=0.1;

		var in=this.specify;
		var numChannels=in.size;
		var sampNum=(timeWindow/ControlDur.ir).round;
		var chain=in.collect{arg item; FFT(LocalBuf(1024), item)};
		var flat=numChannels.collect{arg i; SpecFlatness.kr(chain[i])};
		var ringBuf=numChannels.collect{LocalBuf(sampNum)};
		var phasor=Phasor.kr(0,1,0,sampNum);

		var writeBuf=numChannels.collect{arg i; BufWr.kr(flat[i], ringBuf[i], phasor)};

		var min=numChannels.collect{arg i; (BufMin.kr(ringBuf[i])[0]-1e-5).clip(0,0.8).lag};
		var max=numChannels.collect{arg i; (BufMax.kr(ringBuf[i])[0]+1e-5).clip(0,0.8).lag};

		var unmapedFlatness=numChannels.collect{arg i; [min[i], max[i]].asSpec.unmap(flat[i])};

		^unmapedFlatness.lag(lagTime);

	}

	dPcile {
		arg timeWindow=5, lagTime=0.1;

		var in=this.specify;
		var numChannels=in.size;
		var sampNum=(timeWindow/ControlDur.ir).round;
		var chain=in.collect{arg item; FFT(LocalBuf(1024), item)};
		var pcile=numChannels.collect{arg i; SpecPcile.kr(chain[i])};
		var ringBuf=numChannels.collect{LocalBuf(sampNum)};
		var phasor=Phasor.kr(0,1,0,sampNum);

		var writeBuf=numChannels.collect{arg i; BufWr.kr(pcile[i], ringBuf[i], phasor)};

		var min=numChannels.collect{arg i; (BufMin.kr(ringBuf[i])[0]-1e-5).clip(0,15000).lag};
		var max=numChannels.collect{arg i; (BufMax.kr(ringBuf[i])[0]+1e-5).clip(0,15000).lag};

		var unmapedPcile=numChannels.collect{arg i; [min[i], max[i]].asSpec.unmap(pcile[i])};

		^unmapedPcile.lag(lagTime);

	}

	dCentroid {
		arg timeWindow=5, lagTime=0.1;

		var in=this.specify;
		var numChannels=in.size;
		var sampNum=(timeWindow/ControlDur.ir).round;
		var chain=in.collect{arg item; FFT(LocalBuf(1024), item)};
		var centroid=numChannels.collect{arg i; SpecCentroid.kr(chain[i])};
		var ringBuf=numChannels.collect{LocalBuf(sampNum)};
		var phasor=Phasor.kr(0,1,0,sampNum);

		var writeBuf=numChannels.collect{arg i; BufWr.kr(centroid[i], ringBuf[i], phasor)};

		var min=numChannels.collect{arg i; (BufMin.kr(ringBuf[i])[0]-1e-5).clip(0,15000).lag};
		var max=numChannels.collect{arg i; (BufMax.kr(ringBuf[i])[0]+1e-5).clip(0,15000).lag};

		var unmapedCentroid=numChannels.collect{arg i; [min[i], max[i]].asSpec.unmap(centroid[i])};

		^unmapedCentroid.lag(lagTime);

	}

	dSpread {
		arg timeWindow=5, lagTime=0.1;

		var in=this.specify;
		var numChannels=in.size;
		var sampNum=(timeWindow/ControlDur.ir).round;
		var chain=in.collect{arg item; FFT(LocalBuf(1024), item)};
		var spread=numChannels.collect{arg i; FFTSpread.kr(chain[i])};
		var ringBuf=numChannels.collect{LocalBuf(sampNum)};
		var phasor=Phasor.kr(0,1,0,sampNum);

		var writeBuf=numChannels.collect{arg i; BufWr.kr(spread[i], ringBuf[i], phasor)};

		var min=numChannels.collect{arg i; (BufMin.kr(ringBuf[i])[0]-1e-5).clip(1e+4,7e+7).lag};
		var max=numChannels.collect{arg i; (BufMax.kr(ringBuf[i])[0]+1e-5).clip(1e+4,7e+7).lag};

		var unmapedSpread=numChannels.collect{arg i; [min[i], max[i]].asSpec.unmap(spread[i])};

		^unmapedSpread.lag(lagTime);

	}

	dSlope {
		arg timeWindow=5, lagTime=0.1;

		var in=this.specify;
		var numChannels=in.size;
		var sampNum=(timeWindow/ControlDur.ir).round;
		var chain=in.collect{arg item; FFT(LocalBuf(1024), item)};
		var slope=numChannels.collect{arg i; FFTSlope.kr(chain[i])};
		var ringBuf=numChannels.collect{LocalBuf(sampNum)};
		var phasor=Phasor.kr(0,1,0,sampNum);

		var writeBuf=numChannels.collect{arg i; BufWr.kr(slope[i], ringBuf[i], phasor)};

		var min=numChannels.collect{arg i; (BufMin.kr(ringBuf[i])[0]-1e-5).clip(0,15000).lag};
		var max=numChannels.collect{arg i; (BufMax.kr(ringBuf[i])[0]+1e-5).clip(0,15000).lag};

		var unmapedSlope=numChannels.collect{arg i; [min[i], max[i]].asSpec.unmap(slope[i])};

		^unmapedSlope.lag(lagTime);

	}

	dCrest {
		arg timeWindow=5, lagTime=0.1;

		var in=this.specify;
		var numChannels=in.size;
		var sampNum=(timeWindow/ControlDur.ir).round;
		var chain=in.collect{arg item; FFT(LocalBuf(1024), item)};
		var crest=numChannels.collect{arg i; FFTCrest.kr(chain[i])};
		var ringBuf=numChannels.collect{LocalBuf(sampNum)};
		var phasor=Phasor.kr(0,1,0,sampNum);

		var writeBuf=numChannels.collect{arg i; BufWr.kr(crest[i], ringBuf[i], phasor)};

		var min=numChannels.collect{arg i; (BufMin.kr(ringBuf[i])[0]-1e-5).clip(0,15000).lag};
		var max=numChannels.collect{arg i; (BufMax.kr(ringBuf[i])[0]+1e-5).clip(0,15000).lag};

		var unmapedCrest=numChannels.collect{arg i; [min[i], max[i]].asSpec.unmap(crest[i])};

		^unmapedCrest.lag(lagTime);

	}

}