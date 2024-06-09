+ Object {

	audify {
		^if(
			this.isArray,
			{
				// If `this` is an array
				if(this[0].isUGen,
					{ this },  // If first element is a UGen, return the array as is
					{ this.collect { |i| i.ar } }  // Otherwise, convert each element to audio-rate UGens
				)
			},
			{
				// If `this` is not an array
				if(this.isUGen,
					{ [this] },  // If `this` is already a UGen, wrap it in an array
					{
						// Otherwise, convert `this` to an audio-rate UGen
						if(this.ar.isArray,
							{ this.ar },  // If the result is an array, return it
							{ [this.ar] }  // Otherwise, wrap the result in an array
						)
					}
				)
			}
		);
	}


	dPitch {
		arg lagTime = 0.1, timeWindow = 5;

		var in = this.audify;
		var numChannels = in.size;
		var sampNum = (timeWindow / ControlDur.ir).round;
		var pitch, ringBuf, phasor, writeBuf, min, max, unmapedPitch;

		// Ensure input is valid
		if (in.isEmpty) { ^Signal(0)!numChannels };

		// Track raw pitch using ZeroCrossing
		pitch = numChannels.collect { |i|
			var p = A2K.kr(ZeroCrossing.ar(in[i])).cpsmidi;
			Clip.kr(p,0,127);// Clip pitch values within the range 0-127
		};

		// Create buffers for each channel
		ringBuf = numChannels.collect { LocalBuf(sampNum) };
		// Phasor for circular buffer writing
		phasor = Phasor.kr(0, 1, 0, sampNum);

		// Write raw pitch values to corresponding buffers
		writeBuf = numChannels.collect { |i|
			BufWr.kr(pitch[i], ringBuf[i], phasor)
		};

		// Extract min values from the ring buffers with smaller infinitesimal adjustment
		min = numChannels.collect { |i|
			(BufMin.kr(ringBuf[i])[0].clip(0, 127) - 1e-10).lag(lagTime);
		};

		// Extract max values from the ring buffers with smaller infinitesimal adjustment
		max = numChannels.collect { |i|
			(BufMax.kr(ringBuf[i])[0].clip(0, 127) + 1e-10).lag(lagTime);
		};

		// Normalize pitch to the range 0-1 for each channel
		unmapedPitch = numChannels.collect { |i|
			[min[i], max[i]].asSpec.unmap(pitch[i])
		};

		// Return the normalized pitch values with applied lag
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
		arg lagTime = 0.1, timeWindow = 5;

		var in = this.audify;
		var numChannels = in.size;
		var sampNum = (timeWindow / ControlDur.ir).round;
		var amps, ringBuf, phasor, writeBuf, min, max, unmapedAmp;

		// Ensure input is valid
		if (in.isEmpty) { ^Signal(0)!numChannels };

		// Track amplitude using Amplitude.kr
		amps = numChannels.collect { |i|
			Amplitude.kr(in[i]).ampdb.clip(-100, 0);  // Clip amplitude values within a reasonable range
		};

		// Create buffers for each channel
		ringBuf = numChannels.collect { LocalBuf(sampNum) };
		// Phasor for circular buffer writing
		phasor = Phasor.kr(0, 1, 0, sampNum);

		// Write amplitude values to corresponding buffers
		writeBuf = numChannels.collect { |i|
			BufWr.kr(amps[i], ringBuf[i], phasor)
		};

		// Extract min values from the ring buffers with infinitesimal adjustment
		min = numChannels.collect { |i|
			(BufMin.kr(ringBuf[i])[0] - 1e-10).clip(-100, 0).lag(lagTime);
		};

		// Extract max values from the ring buffers with infinitesimal adjustment
		max = numChannels.collect { |i|
			(BufMax.kr(ringBuf[i])[0] + 1e-10).clip(-100, 0).lag(lagTime);
		};

		// Normalize amplitude to the range 0-1 for each channel
		unmapedAmp = numChannels.collect { |i|
			[min[i], max[i]].asSpec.unmap(amps[i])
		};

		// Return the normalized amplitude values with applied lag
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
		arg lagTime = 0.1, timeWindow = 5;

		var in = this.audify;
		var numChannels = in.size;
		var sampNum = (timeWindow / ControlDur.ir).round;
		var rms, ringBuf, phasor, writeBuf, min, max, unmapedRms;

		// Ensure input is valid
		if (in.isEmpty) { ^Signal(0)!numChannels };

		// Track RMS amplitude using RunningSum.kr
		rms = numChannels.collect { |i|
			(RunningSum.kr(in[i].squared) / 40).sqrt.ampdb.clip(-100, 0);  // RMS calculation and clipping
		};

		// Create buffers for each channel
		ringBuf = numChannels.collect { LocalBuf(sampNum) };
		// Phasor for circular buffer writing
		phasor = Phasor.kr(0, 1, 0, sampNum);

		// Write RMS amplitude values to corresponding buffers
		writeBuf = numChannels.collect { |i|
			BufWr.kr(rms[i], ringBuf[i], phasor)
		};

		// Extract min values from the ring buffers with infinitesimal adjustment
		min = numChannels.collect { |i|
			(BufMin.kr(ringBuf[i])[0] - 1e-10).clip(-100, 0).lag(lagTime);
		};

		// Extract max values from the ring buffers with infinitesimal adjustment
		max = numChannels.collect { |i|
			(BufMax.kr(ringBuf[i])[0] + 1e-10).clip(-100, 0).lag(lagTime);
		};

		// Normalize RMS amplitude to the range 0-1 for each channel
		unmapedRms = numChannels.collect { |i|
			[min[i], max[i]].asSpec.unmap(rms[i])
		};

		// Return the normalized RMS amplitude values with applied lag
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
		arg lagTime = 0.1, timeWindow = 5;

		var in = this.audify;
		var numChannels = in.size;
		var sampNum = (timeWindow / ControlDur.ir).round;
		var chain, loudness, ringBuf, phasor, writeBuf, min, max, unmapedLoudness;

		// Ensure input is valid
		if (in.isEmpty) { ^Signal(0)!numChannels };

		// Compute FFT chains for each channel
		chain = in.collect { |item| FFT(LocalBuf(1024), item) };

		// Calculate loudness for each channel
		loudness = numChannels.collect { |i|
			Loudness.kr(chain[i]).clip(0, 64);  // Loudness calculation and clipping
		};

		// Create buffers for each channel
		ringBuf = numChannels.collect { LocalBuf(sampNum) };
		// Phasor for circular buffer writing
		phasor = Phasor.kr(0, 1, 0, sampNum);

		// Write loudness values to corresponding buffers
		writeBuf = numChannels.collect { |i|
			BufWr.kr(loudness[i], ringBuf[i], phasor)
		};

		// Extract min values from the ring buffers with infinitesimal adjustment
		min = numChannels.collect { |i|
			(BufMin.kr(ringBuf[i])[0] - 1e-10).clip(0, 64).lag(lagTime);
		};

		// Extract max values from the ring buffers with infinitesimal adjustment
		max = numChannels.collect { |i|
			(BufMax.kr(ringBuf[i])[0] + 1e-10).clip(0, 64).lag(lagTime);
		};

		// Normalize loudness to the range 0-1 for each channel
		unmapedLoudness = numChannels.collect { |i|
			[min[i], max[i]].asSpec.unmap(loudness[i])
		};

		// Return the normalized loudness values with applied lag
		^unmapedLoudness.lag(lagTime);
	}

	mdLoud {
		arg low=0, high=1, warp=\lin, lagTime=0.1, timeWindow=5;
		^[low, high, warp].asSpec.map(this.dLoud(lagTime, timeWindow))
	}

	sLoud {
		arg lagTime=0.1, lowLoud=0, highLoud=64, warp=\lin;
		var in = this.audify;
		var chain = in.collect { |item| FFT(LocalBuf(1024), item) };
		var loudness = chain.collect { |c| Loudness.kr(c).clip(0, 64) };
		var unmaping = [lowLoud, highLoud, warp].asSpec.unmap(loudness);
		^unmaping.lag(lagTime);
	}


	msLoud {
		arg low=0, high=1, warp=\lin, lagTime=0.1, lowLoud=0, highLoud=64, warpLoud=\lin;
		^[low, high, warp].asSpec.map(this.sLoud(lagTime, lowLoud, highLoud, warpLoud))
	}


	dFlat {
		arg lagTime = 0.1, timeWindow = 5;

		var in = this.audify;
		var numChannels = in.size;
		var sampNum = (timeWindow / ControlDur.ir).round;
		var chain, flat, ringBuf, phasor, writeBuf, min, max, unmapedFlatness;

		// Ensure input is valid
		if (in.isEmpty) { ^Signal(0)!numChannels };

		// Compute FFT chains for each channel
		chain = in.collect { |item| FFT(LocalBuf(1024), item) };

		// Calculate spectral flatness for each channel
		flat = numChannels.collect { |i|
			SpecFlatness.kr(chain[i]).clip(0, 0.8);  // Spectral flatness calculation and clipping
		};

		// Create buffers for each channel
		ringBuf = numChannels.collect { LocalBuf(sampNum) };
		// Phasor for circular buffer writing
		phasor = Phasor.kr(0, 1, 0, sampNum);

		// Write spectral flatness values to corresponding buffers
		writeBuf = numChannels.collect { |i|
			BufWr.kr(flat[i], ringBuf[i], phasor)
		};

		// Extract min values from the ring buffers with infinitesimal adjustment
		min = numChannels.collect { |i|
			(BufMin.kr(ringBuf[i])[0] - 1e-10).clip(0, 0.8).lag(lagTime);
		};

		// Extract max values from the ring buffers with infinitesimal adjustment
		max = numChannels.collect { |i|
			(BufMax.kr(ringBuf[i])[0] + 1e-10).clip(0, 0.8).lag(lagTime);
		};

		// Normalize spectral flatness to the range 0-1 for each channel
		unmapedFlatness = numChannels.collect { |i|
			[min[i], max[i]].asSpec.unmap(flat[i])
		};

		// Return the normalized spectral flatness values with applied lag
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
		arg lagTime = 0.1, timeWindow = 5;

		var in = this.audify;
		var numChannels = in.size;
		var sampNum = (timeWindow / ControlDur.ir).round;
		var chain, pcile, ringBuf, phasor, writeBuf, min, max, unmapedPcile;

		// Ensure input is valid
		if (in.isEmpty) { ^Signal(0)!numChannels };

		// Compute FFT chains for each channel
		chain = in.collect { |item| FFT(LocalBuf(1024), item) };

		// Calculate spectral percentile for each channel
		pcile = numChannels.collect { |i|
			var p = SpecPcile.kr(chain[i]);  // Spectral percentile calculation and clipping
			Clip.kr(p,0,20000);
		};

		// Create buffers for each channel
		ringBuf = numChannels.collect { LocalBuf(sampNum) };
		// Phasor for circular buffer writing
		phasor = Phasor.kr(0, 1, 0, sampNum);

		// Write spectral percentile values to corresponding buffers
		writeBuf = numChannels.collect { |i|
			BufWr.kr(pcile[i], ringBuf[i], phasor)
		};

		// Extract min values from the ring buffers with infinitesimal adjustment
		min = numChannels.collect { |i|
			(BufMin.kr(ringBuf[i])[0] - 1e-10).clip(0, 20000).lag(lagTime);
		};

		// Extract max values from the ring buffers with infinitesimal adjustment
		max = numChannels.collect { |i|
			(BufMax.kr(ringBuf[i])[0] + 1e-10).clip(0, 20000).lag(lagTime);
		};

		// Normalize spectral percentile to the range 0-1 for each channel
		unmapedPcile = numChannels.collect { |i|
			[min[i], max[i]].asSpec.unmap(pcile[i])
		};

		// Return the normalized spectral percentile values with applied lag
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
		arg lagTime = 0.1, timeWindow = 5;

		var in = this.audify;
		var numChannels = in.size;
		var sampNum = (timeWindow / ControlDur.ir).round;
		var chain, centroid, ringBuf, phasor, writeBuf, min, max, unmapedCentroid;

		// Ensure input is valid
		if (in.isEmpty) { ^Signal(0)!numChannels };

		// Compute FFT chains for each channel
		chain = in.collect { |item| FFT(LocalBuf(1024), item) };

		// Calculate spectral centroid for each channel
		centroid = numChannels.collect { |i|
			var c = SpecCentroid.kr(chain[i]).cpsmidi;  // Spectral centroid calculation and clipping
			Clip.kr(c, 0, 127);
		};

		// Create buffers for each channel
		ringBuf = numChannels.collect { LocalBuf(sampNum) };
		// Phasor for circular buffer writing
		phasor = Phasor.kr(0, 1, 0, sampNum);

		// Write spectral centroid values to corresponding buffers
		writeBuf = numChannels.collect { |i|
			BufWr.kr(centroid[i], ringBuf[i], phasor)
		};

		// Extract min values from the ring buffers with infinitesimal adjustment
		min = numChannels.collect { |i|
			(BufMin.kr(ringBuf[i])[0] - 1e-10).clip(0, 127).lag(lagTime);
		};

		// Extract max values from the ring buffers with infinitesimal adjustment
		max = numChannels.collect { |i|
			(BufMax.kr(ringBuf[i])[0] + 1e-10).clip(0, 127).lag(lagTime);
		};


		// Normalize spectral centroid MIDI to the range 0-1 for each channel
		unmapedCentroid = numChannels.collect { |i|
			[min[i], max[i]].asSpec.unmap(centroid[i])
		};

		// Return the normalized spectral centroid MIDI values with applied lag
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
		arg lagTime = 0.1, timeWindow = 5;

		var in = this.audify;
		var numChannels = in.size;
		var sampNum = (timeWindow / ControlDur.ir).round;
		var chain, spread, centroids, ringBuf, phasor, writeBuf, min, max, unmapedSpread;

		// Ensure input is valid
		if (in.isEmpty) { ^Signal(0)!numChannels };

		// Compute FFT chains for each channel
		chain = in.collect { |item| FFT(LocalBuf(1024), item) };

		centroids = numChannels.collect { |i|
			SpecCentroid.kr(chain[i]);
		};

		// Calculate spread for each channel
		spread = numChannels.collect { |i|
			FFTSpread.kr(chain[i], centroids[i]).clip(1e+4, 7e+7);  // Spread calculation and clipping
		};

		// Create buffers for each channel
		ringBuf = numChannels.collect { LocalBuf(sampNum) };
		// Phasor for circular buffer writing
		phasor = Phasor.kr(0, 1, 0, sampNum);

		// Write spread values to corresponding buffers
		writeBuf = numChannels.collect { |i|
			BufWr.kr(spread[i], ringBuf[i], phasor)
		};

		// Extract min values from the ring buffers with infinitesimal adjustment
		min = numChannels.collect { |i|
			(BufMin.kr(ringBuf[i])[0] - 1e-10).clip(1e+4, 7e+7).lag(lagTime);
		};

		// Extract max values from the ring buffers with infinitesimal adjustment
		max = numChannels.collect { |i|
			(BufMax.kr(ringBuf[i])[0] + 1e-10).clip(1e+4, 7e+7).lag(lagTime);
		};

		// Normalize spread values to the range 0-1 for each channel
		unmapedSpread = numChannels.collect { |i|
			[min[i], max[i]].asSpec.unmap(spread[i])
		};

		// Return the normalized spread values with applied lag
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

		var in = this.audify;
		var numChannels = in.size;
		var sampNum = (timeWindow / ControlDur.ir).round;
		var chain, slope, ringBuf, phasor, writeBuf, min, max, unmapedSlope;

		// Ensure input is valid
		if (in.isEmpty) { ^Signal(0)!numChannels };

		// Compute FFT chains for each channel
		chain = in.collect { |item| FFT(LocalBuf(1024), item) };

		// Calculate spectral slope for each channel
		slope = numChannels.collect { |i| FFTSlope.kr(chain[i]).clip(-1, 1); };

		// Create buffers for each channel
		ringBuf = numChannels.collect { LocalBuf(sampNum) };

		// Phasor for circular buffer writing
		phasor = Phasor.kr(0, 1, 0, sampNum);

		// Write spectral slope values to corresponding buffers
		writeBuf = numChannels.collect { |i| BufWr.kr(slope[i], ringBuf[i], phasor) };

		// Extract min values from the ring buffers with infinitesimal adjustment
		min = numChannels.collect { |i| (BufMin.kr(ringBuf[i])[0] - 1e-5).clip(-1, 1).lag(lagTime) };

		// Extract max values from the ring buffers with infinitesimal adjustment
		max = numChannels.collect { |i| (BufMax.kr(ringBuf[i])[0] + 1e-5).clip(-1, 1).lag(lagTime) };

		// Normalize spectral slope to the range 0-1 for each channel
		unmapedSlope = numChannels.collect { |i| [min[i], max[i]].asSpec.unmap(slope[i]) };

		// Return the normalized spectral slope values with applied lag
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
		arg lagTime = 0.1, timeWindow = 5;

		var in = this.audify;
		var numChannels = in.size;
		var sampNum = (timeWindow / ControlDur.ir).round;
		var chain, crest, ringBuf, phasor, writeBuf, min, max, unmapedCrest;

		// Ensure input is valid
		if (in.isEmpty) { ^Signal(0)!numChannels };

		// Compute FFT chains for each channel
		chain = in.collect { |item| FFT(LocalBuf(1024), item) };

		// Calculate spectral crest factor for each channel
		crest = numChannels.collect { |i|
			FFTCrest.kr(chain[i]).clip(0, 20000); // Clip to a reasonable range to avoid extreme values

		};

		// Create buffers for each channel
		ringBuf = numChannels.collect { LocalBuf(sampNum) };

		// Phasor for circular buffer writing
		phasor = Phasor.kr(0, 1, 0, sampNum);

		// Write spectral crest values to corresponding buffers
		writeBuf = numChannels.collect { |i| BufWr.kr(crest[i], ringBuf[i], phasor) };

		// Extract min values from the ring buffers with infinitesimal adjustment
		min = numChannels.collect { |i|
			var m = BufMin.kr(ringBuf[i])[0] - 1e-10;
			m.clip(0,20000).lag(lagTime);
		};

		// Extract max values from the ring buffers with infinitesimal adjustment
		max = numChannels.collect { |i|
			var m = BufMax.kr(ringBuf[i])[0] + 1e-10; // Replace extreme values
			m.clip(0,20000).lag(lagTime);
		};

		// Normalize spectral crest to the range 0-1 for each channel
		unmapedCrest = numChannels.collect { |i|
			[min[i], max[i]].asSpec.unmap(crest[i]);
		};

		// Return the normalized spectral crest values with applied lag
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
