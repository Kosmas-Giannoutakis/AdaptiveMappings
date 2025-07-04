+ Object {

	/*
	====================================================================
	INTERNAL HELPER METHODS
	====================================================================
	*/

	prAudify {
		^if(
			this.isArray,
			{ if(this[0].isUGen, { this }, { this.collect(_.ar) }) },
			{ if(this.isUGen, { [this] }, { if(this.ar.isArray, { this.ar }, { [this.ar] }) }) }
		)
	}

	prDynamicAnalysis {
		arg analysisFunc, clipLo, clipHi, low=0, high=1, warp=\lin, lagTime=0.1, timeWindow=5;
		var in = this;
		var sampNum = (timeWindow / ControlDur.ir).round;
		var feature = analysisFunc.value(in).clip(clipLo, clipHi);
		var ringBuf = LocalBuf(sampNum, in.size);
		var phasor = Phasor.kr(0, 1, 0, sampNum);
		var min = (BufMin.kr(ringBuf) - 1e-9).lag(lagTime).clip(clipLo, clipHi);
		var max = (BufMax.kr(ringBuf) + 1e-9).lag(lagTime).clip(clipLo, clipHi);
		var safeMax = max.max(min + 1e-6);
		var unmapped = [min, safeMax].asSpec.unmap(feature);
		BufWr.kr(feature, ringBuf, phasor);
		^[low, high, warp].asSpec.map(unmapped).lag(lagTime);
	}


	/*
	====================================================================
	PUBLIC LIVE CODING METHODS
	====================================================================
	*/

	//--- Pitch (Fundamental Frequency) ---
	dPitch { arg low=0, high=1, warp=\lin, lagTime=0.1, timeWindow=5;
		^this.prAudify.prDynamicAnalysis(
			{ |in| A2K.kr(ZeroCrossing.ar(in)).cpsmidi },
			0, 127, low, high, warp, lagTime, timeWindow
		)
	}
	sPitch { arg low=0, high=1, warp=\lin, lagTime=0.1, lowFreq=40, highFreq=20000, warpFreq=\exp;
		var sig = ZeroCrossing.ar(this.prAudify);
		var unmapped = [lowFreq, highFreq, warpFreq].asSpec.unmap(sig);
		^Lag.ar(unmapped, lagTime).range(low, high, warp);
	}

	//--- Amplitude (Peak) ---
	dAmp { arg low=0, high=1, warp=\lin, lagTime=0.1, timeWindow=5;
		^this.prAudify.prDynamicAnalysis(
			{ |in| Amplitude.kr(in).ampdb },
			-100, 0, low, high, warp, lagTime, timeWindow
		)
	}
	sAmp { arg low=0, high=1, warp=\lin, lagTime=0.1, lowAmp= -60, highAmp=0, warpAmp=\lin;
		var sig = Amplitude.ar(this.prAudify).ampdb;
		var unmapped = [lowAmp, highAmp, warpAmp].asSpec.unmap(sig);
		^Lag.ar(unmapped, lagTime).range(low, high, warp);
	}

	//--- RMS (Root Mean Square Power) ---
	dRms { arg low=0, high=1, warp=\lin, lagTime=0.1, timeWindow=5;
		^this.prAudify.prDynamicAnalysis(
			{ |in| (RunningSum.kr(in.squared) / 40).sqrt.ampdb },
			-100, 0, low, high, warp, lagTime, timeWindow
		)
	}
	sRms { arg low=0, high=1, warp=\lin, lagTime=0.1, lowRms= -60, highRms=0, warpRms=\lin;
		var sig = RunningSum.rms(this.prAudify).ampdb;
		var unmapped = [lowRms, highRms, warpRms].asSpec.unmap(sig);
		^Lag.ar(unmapped, lagTime).range(low, high, warp);
	}

	//--- Loudness (Perceptual) ---
	dLoud { arg low=0, high=1, warp=\lin, lagTime=0.1, timeWindow=5;
		var chain = this.prAudify.collect { |item| FFT(LocalBuf(1024), item) };
		^chain.prDynamicAnalysis(
			{ |ch| Loudness.kr(ch) },
			0, 64, low, high, warp, lagTime, timeWindow
		)
	}
	sLoud { arg low=0, high=1, warp=\lin, lagTime=0.1, lowLoud=0, highLoud=64, warpLoud=\lin;
		var chain = this.prAudify.collect { |item| FFT(LocalBuf(1024), item) };
		var sig = Loudness.kr(chain);
		var unmapped = [lowLoud, highLoud, warpLoud].asSpec.unmap(sig); // FIXED
		^unmapped.lag(lagTime).range(low, high, warp);
	}

	//--- Spectral Flatness ---
	dFlat { arg low=0, high=1, warp=\lin, lagTime=0.1, timeWindow=5;
		var chain = this.prAudify.collect { |item| FFT(LocalBuf(1024), item) };
		^chain.prDynamicAnalysis(
			{ |ch| SpecFlatness.kr(ch) },
			0, 0.8, low, high, warp, lagTime, timeWindow
		)
	}
	sFlat { arg low=0, high=1, warp=\lin, lagTime=0.1, lowFlat=0, highFlat=0.8, warpFlat=\lin;
		var chain = this.prAudify.collect { |item| FFT(LocalBuf(1024), item) };
		var sig = SpecFlatness.kr(chain);
		var unmapped = [lowFlat, highFlat, warpFlat].asSpec.unmap(sig); // FIXED
		^unmapped.lag(lagTime).range(low, high, warp);
	}

	//--- Spectral Percentile ---
	dPcile { arg low=0, high=1, warp=\lin, lagTime=0.1, timeWindow=5;
		var chain = this.prAudify.collect { |item| FFT(LocalBuf(1024), item) };
		^chain.prDynamicAnalysis(
			{ |ch| SpecPcile.kr(ch) },
			0, 20000, low, high, warp, lagTime, timeWindow
		)
	}
	sPcile { arg low=0, high=1, warp=\lin, lagTime=0.1, lowPcile=40, highPcile=20000, warpPcile=\exp;
		var chain = this.prAudify.collect { |item| FFT(LocalBuf(1024), item) };
		var sig = SpecPcile.kr(chain);
		var unmapped = [lowPcile, highPcile, warpPcile].asSpec.unmap(sig); // FIXED
		^unmapped.lag(lagTime).range(low, high, warp);
	}

	//--- Spectral Centroid ---
	dCent { arg low=0, high=1, warp=\lin, lagTime=0.1, timeWindow=5;
		var chain = this.prAudify.collect { |item| FFT(LocalBuf(1024), item) };
		^chain.prDynamicAnalysis(
			{ |ch| SpecCentroid.kr(ch).cpsmidi },
			0, 127, low, high, warp, lagTime, timeWindow
		)
	}
	sCent { arg low=0, high=1, warp=\lin, lagTime=0.1, lowCent=40, highCent=20000, warpCent=\exp;
		var chain = this.prAudify.collect { |item| FFT(LocalBuf(1024), item) };
		var sig = SpecCentroid.kr(chain);
		var unmapped = [lowCent, highCent, warpCent].asSpec.unmap(sig); // FIXED
		^unmapped.lag(lagTime).range(low, high, warp);
	}

	//--- Spectral Spread ---
	dSpread { arg low=0, high=1, warp=\lin, lagTime=0.1, timeWindow=5;
		var in = this.prAudify;
		var chain = in.collect { |item| FFT(LocalBuf(1024), item) };
		var centroids = SpecCentroid.kr(chain);
		^chain.prDynamicAnalysis(
			{ |ch| FFTSpread.kr(ch, centroids) },
			1e+4, 7e+7, low, high, warp, lagTime, timeWindow
		)
	}
	sSpread { arg low=0, high=1, warp=\lin, lagTime=0.1, lowSpread=40, highSpread=20000, warpSpread=\exp;
		var chain = this.prAudify.collect { |item| FFT(LocalBuf(1024), item) };
		var sig = FFTSpread.kr(chain);
		var unmapped = [lowSpread, highSpread, warpSpread].asSpec.unmap(sig); // FIXED
		^unmapped.lag(lagTime).range(low, high, warp);
	}

	//--- Spectral Slope ---
	dSlope { arg low=0, high=1, warp=\lin, lagTime=0.1, timeWindow=5;
		var chain = this.prAudify.collect { |item| FFT(LocalBuf(1024), item) };
		^chain.prDynamicAnalysis(
			{ |ch| FFTSlope.kr(ch) },
			-1, 1, low, high, warp, lagTime, timeWindow
		)
	}
	sSlope { arg low=0, high=1, warp=\lin, lagTime=0.1, lowSlope= -1, highSlope=1, warpSlope=\lin;
		var chain = this.prAudify.collect { |item| FFT(LocalBuf(1024), item) };
		var sig = FFTSlope.kr(chain);
		var unmapped = [lowSlope, highSlope, warpSlope].asSpec.unmap(sig); // FIXED
		^unmapped.lag(lagTime).range(low, high, warp);
	}

	//--- Spectral Crest ---
	dCrest { arg low=0, high=1, warp=\lin, lagTime=0.1, timeWindow=5;
		var chain = this.prAudify.collect { |item| FFT(LocalBuf(1024), item) };
		^chain.prDynamicAnalysis(
			{ |ch| FFTCrest.kr(ch) },
			0, 20000, low, high, warp, lagTime, timeWindow
		)
	}
	sCrest { arg low=0, high=1, warp=\lin, lagTime=0.1, lowCrest=40, highCrest=20000, warpCrest=\exp;
		var chain = this.prAudify.collect { |item| FFT(LocalBuf(1024), item) };
		var sig = FFTCrest.kr(chain);
		var unmapped = [lowCrest, highCrest, warpCrest].asSpec.unmap(sig); // FIXED
		^unmapped.lag(lagTime).range(low, high, warp);
	}

	/*
	====================================================================
	ONSET DETECTORS
	====================================================================
	*/
	dOnsets {
		var in = this.prAudify;
		var amps = Amplitude.kr(in);
		var chain = in.collect { |item| FFT(LocalBuf(1024), item) };
		^Onsets.kr(chain, threshold: amps - 0.01);
	}

	dOnsetsJA {
		var in = this.prAudify;
		var amps = Amplitude.ar(in);
		var chain = in.collect { |item| FFT(LocalBuf(2048), item) };
		^PV_JensenAndersen.ar(chain, threshold: amps);
	}

	dOnsetsHF {
		var in = this.prAudify;
		var amps = Amplitude.ar(in);
		var chain = in.collect { |item| FFT(LocalBuf(2048), item) };
		^PV_HainsworthFoote.ar(chain, threshold: amps - 0.001);
	}
}