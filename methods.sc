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

	// Enhanced bulletproof version with multiple safety layers
	prDynamicAnalysis {
		arg analysisFunc, clipLo, clipHi, low=0, high=1, warp=\lin, lagTime=0.1, timeWindow=5;
		var in = this;
		var sampNum = (timeWindow / ControlDur.ir).round.max(2); // Ensure minimum buffer size
		var ringBuf = LocalBuf(sampNum, in.size);
		var phasor = Phasor.kr(0, 1, 0, sampNum);
		var feature, min, max, safeMax, unmapped, safeFeature, result;

		// 1. Get the raw feature with multiple safety layers
		feature = analysisFunc.value(in);

		// 2. Multiple sanitization passes - some NaN values are persistent
		safeFeature = feature;
		safeFeature = Sanitize.kr(safeFeature, clipLo); // First pass
		safeFeature = Select.kr(CheckBadValues.kr(safeFeature, 0, 0) > 0, [safeFeature, clipLo]); // Replace bad values
		safeFeature = safeFeature.clip(clipLo, clipHi); // Final clipping

		// 3. Write the clean feature to buffer BEFORE reading min/max
		BufWr.kr(safeFeature, ringBuf, phasor);

		// 4. Read min/max with additional safety
		min = BufMin.kr(ringBuf);
		max = BufMax.kr(ringBuf);

		// 5. Sanitize min/max and provide fallbacks
		min = Sanitize.kr(min, clipLo);
		max = Sanitize.kr(max, clipHi);
		min = Select.kr(CheckBadValues.kr(min, 0, 0) > 0, [min, clipLo]);
		max = Select.kr(CheckBadValues.kr(max, 0, 0) > 0, [max, clipHi]);

		// 6. Ensure min <= max and prevent division by zero
		min = min.min(max - 1e-6);
		safeMax = max.max(min + 1e-6);

		// 7. Apply lag to min/max for stability
		min = min.lag(lagTime);
		safeMax = safeMax.lag(lagTime);

		// 8. Normalize with safety check
		unmapped = (safeFeature - min) / (safeMax - min);
		unmapped = unmapped.clip(0, 1); // Ensure [0,1] range
		unmapped = Sanitize.kr(unmapped, 0.5); // Default to middle if NaN

		// 9. Map to output range with final safety
		result = [low, high, warp].asSpec.map(unmapped).lag(lagTime);

		// 10. Return single value if input was mono, array if input was multi-channel
		^if(in.size == 1, { result[0] }, { result });
	}

	// Enhanced static analysis with better safety
	prStaticAnalysis {
		arg analysisFunc, inputLow, inputHigh, inputWarp, outputLow, outputHigh, outputWarp, lagTime;
		var in = this.prAudify;
		var feature, safeFeature, unmapped, result;

		feature = analysisFunc.value(in);
		safeFeature = Sanitize.kr(feature, (inputLow + inputHigh) * 0.5);
		safeFeature = Select.kr(CheckBadValues.kr(safeFeature, 0, 0) > 0, [safeFeature, (inputLow + inputHigh) * 0.5]);
		safeFeature = safeFeature.clip(inputLow, inputHigh);
		unmapped = [inputLow, inputHigh, inputWarp].asSpec.unmap(safeFeature);
		unmapped = unmapped.clip(0, 1);
		unmapped = Sanitize.kr(unmapped, 0.5);
		result = [outputLow, outputHigh, outputWarp].asSpec.map(unmapped);
		result = Sanitize.kr(result, (outputLow + outputHigh) * 0.5);

		// THE FIX: Check the size of the *original* audified input.
		// If it was mono, return the single result. Otherwise, return the array.
		^if(in.size == 1, { result }, { result }).lag(lagTime);
	}

	/*
	====================================================================
	PUBLIC LIVE CODING METHODS
	====================================================================
	*/

	//--- Pitch (Fundamental Frequency) ---
	dPitch { arg low=0, high=1, warp=\lin, lagTime=0.1, timeWindow=5;
		^this.prAudify.prDynamicAnalysis(
			{ |in|
				var freq = if(in.size == 1,
					{ Tartini.kr(in[0])[0] }, // Single channel
					{ in.collect { |chan| Tartini.kr(chan)[0] } } // Multi-channel
				);
				freq = freq.max(20).min(20000); // Clip to reasonable range first
				freq.cpsmidi.max(0).min(127) // Convert and clip MIDI range
			},
			0, 127, low, high, warp, lagTime, timeWindow
		)
	}

	sPitch { arg low=0, high=1, warp=\lin, lagTime=0.1, lowFreq=40, highFreq=20000, warpFreq=\exp;
		^this.prAudify.prStaticAnalysis(
			{ |in|
				var freq = if(in.size == 1,
					{ Tartini.kr(in[0])[0] }, // Single channel
					{ in.collect { |chan| Tartini.kr(chan)[0] } } // Multi-channel
				);
				freq.max(20).min(20000) // Clip to reasonable range
			},
			lowFreq, highFreq, warpFreq, low, high, warp, lagTime
		)
	}

	//--- Amplitude (Peak) ---
	dAmp { arg low=0, high=1, warp=\lin, lagTime=0.1, timeWindow=5;
		^this.prAudify.prDynamicAnalysis(
			{ |in|
				var amp = if(in.size == 1,
					{ Amplitude.kr(in[0], 0.01, 0.1) },
					{ in.collect { |chan| Amplitude.kr(chan, 0.01, 0.1) } }
				);
				amp.max(1e-6).ampdb.max(-100).min(0) // Prevent log(0) and clip
			},
			-100, 0, low, high, warp, lagTime, timeWindow
		)
	}

	sAmp { arg low=0, high=1, warp=\lin, lagTime=0.1, lowAmp= -60, highAmp=0, warpAmp=\lin;
		^this.prAudify.prStaticAnalysis(
			{ |in|
				var amp = if(in.size == 1,
					{ Amplitude.kr(in[0], 0.01, 0.1) },
					{ in.collect { |chan| Amplitude.kr(chan, 0.01, 0.1) } }
				);
				amp.max(1e-6).ampdb.max(-100).min(0)
			},
			lowAmp, highAmp, warpAmp, low, high, warp, lagTime
		)
	}

	//--- RMS (Root Mean Square Power) ---
	dRms { arg low=0, high=1, warp=\lin, lagTime=0.1, timeWindow=5;
		^this.prAudify.prDynamicAnalysis(
			{ |in|
				var rms = if(in.size == 1,
					{ RunningSum.kr(in[0].squared, 40) / 40 },
					{ in.collect { |chan| RunningSum.kr(chan.squared, 40) / 40 } }
				);
				rms = rms.max(1e-12).sqrt; // Prevent sqrt of negative/zero
				rms.max(1e-6).ampdb.max(-100).min(0)
			},
			-100, 0, low, high, warp, lagTime, timeWindow
		)
	}

	sRms { arg low=0, high=1, warp=\lin, lagTime=0.1, lowRms= -60, highRms=0, warpRms=\lin;
		^this.prAudify.prStaticAnalysis(
			{ |in|
				var rms = if(in.size == 1,
					{ RunningSum.kr(in[0].squared, 40) / 40 },
					{ in.collect { |chan| RunningSum.kr(chan.squared, 40) / 40 } }
				);
				rms = rms.max(1e-12).sqrt;
				rms.max(1e-6).ampdb.max(-100).min(0)
			},
			lowRms, highRms, warpRms, low, high, warp, lagTime
		)
	}

	//--- Loudness (Perceptual) ---
	dLoud { arg low=0, high=1, warp=\lin, lagTime=0.1, timeWindow=5;
		var chain = this.prAudify.collect { |item| FFT(LocalBuf(1024), item) };
		^chain.prDynamicAnalysis(
			{ |ch|
				var loud = Loudness.kr(ch);
				loud.max(0).min(64) // Clip to expected range
			},
			0, 64, low, high, warp, lagTime, timeWindow
		)
	}

	sLoud { arg low=0, high=1, warp=\lin, lagTime=0.1, lowLoud=0, highLoud=64, warpLoud=\lin;
		var chain = this.prAudify.collect { |item| FFT(LocalBuf(1024), item) };
		^chain.prStaticAnalysis(
			{ |ch|
				var loud = Loudness.kr(ch);
				loud.max(0).min(64)
			},
			lowLoud, highLoud, warpLoud, low, high, warp, lagTime
		)
	}

	//--- Spectral Flatness ---
	dFlat { arg low=0, high=1, warp=\lin, lagTime=0.1, timeWindow=5;
		var chain = this.prAudify.collect { |item| FFT(LocalBuf(1024), item) };
		^chain.prDynamicAnalysis(
			{ |ch|
				var flat = SpecFlatness.kr(ch);
				flat.max(0).min(1) // Clip to [0,1] range
			},
			0, 1, low, high, warp, lagTime, timeWindow
		)
	}

	sFlat { arg low=0, high=1, warp=\lin, lagTime=0.1, lowFlat=0, highFlat=1, warpFlat=\lin;
		var chain = this.prAudify.collect { |item| FFT(LocalBuf(1024), item) };
		^chain.prStaticAnalysis(
			{ |ch|
				var flat = SpecFlatness.kr(ch);
				flat.max(0).min(1)
			},
			lowFlat, highFlat, warpFlat, low, high, warp, lagTime
		)
	}

	//--- Spectral Percentile ---
	dPcile { arg low=0, high=1, warp=\lin, lagTime=0.1, timeWindow=5;
		var chain = this.prAudify.collect { |item| FFT(LocalBuf(1024), item) };
		^chain.prDynamicAnalysis(
			{ |ch|
				var pcile = SpecPcile.kr(ch);
				pcile.max(20).min(20000) // Clip to reasonable frequency range
			},
			20, 20000, low, high, warp, lagTime, timeWindow
		)
	}

	sPcile { arg low=0, high=1, warp=\lin, lagTime=0.1, lowPcile=40, highPcile=20000, warpPcile=\exp;
		var chain = this.prAudify.collect { |item| FFT(LocalBuf(1024), item) };
		^chain.prStaticAnalysis(
			{ |ch|
				var pcile = SpecPcile.kr(ch);
				pcile.max(20).min(20000)
			},
			lowPcile, highPcile, warpPcile, low, high, warp, lagTime
		)
	}

	//--- Spectral Centroid ---
	dCent { arg low=0, high=1, warp=\lin, lagTime=0.1, timeWindow=5;
		var chain = this.prAudify.collect { |item| FFT(LocalBuf(1024), item) };
		^chain.prDynamicAnalysis(
			{ |ch|
				var cent = SpecCentroid.kr(ch);
				cent.max(20).min(20000).cpsmidi.max(0).min(127)
			},
			0, 127, low, high, warp, lagTime, timeWindow
		)
	}

	sCent { arg low=0, high=1, warp=\lin, lagTime=0.1, lowCent=40, highCent=20000, warpCent=\exp;
		var chain = this.prAudify.collect { |item| FFT(LocalBuf(1024), item) };
		^chain.prStaticAnalysis(
			{ |ch|
				var cent = SpecCentroid.kr(ch);
				cent.max(20).min(20000)
			},
			lowCent, highCent, warpCent, low, high, warp, lagTime
		)
	}

	//--- Spectral Spread ---
	dSpread { arg low=0, high=1, warp=\lin, lagTime=0.1, timeWindow=5;
		var in = this.prAudify;
		var chain = in.collect { |item| FFT(LocalBuf(1024), item) };
		^chain.prDynamicAnalysis(
			{ |ch|
				var centroids = SpecCentroid.kr(ch).max(20).min(20000);
				var spread = FFTSpread.kr(ch, centroids);
				spread.max(1e+3).min(1e+8) // Clip to reasonable range
			},
			1e+3, 1e+8, low, high, warp, lagTime, timeWindow
		)
	}

	sSpread { arg low=0, high=1, warp=\lin, lagTime=0.1, lowSpread=1e+3, highSpread=1e+8, warpSpread=\exp;
		var chain = this.prAudify.collect { |item| FFT(LocalBuf(1024), item) };
		^chain.prStaticAnalysis(
			{ |ch|
				var centroids = SpecCentroid.kr(ch).max(20).min(20000);
				var spread = FFTSpread.kr(ch, centroids);
				spread.max(1e+3).min(1e+8)
			},
			lowSpread, highSpread, warpSpread, low, high, warp, lagTime
		)
	}

	//--- Spectral Slope ---
	dSlope { arg low=0, high=1, warp=\lin, lagTime=0.1, timeWindow=5;
		var chain = this.prAudify.collect { |item| FFT(LocalBuf(1024), item) };
		^chain.prDynamicAnalysis(
			{ |ch|
				var slope = FFTSlope.kr(ch);
				slope.max(-10).min(10) // Clip to reasonable range
			},
			-10, 10, low, high, warp, lagTime, timeWindow
		)
	}

	sSlope { arg low=0, high=1, warp=\lin, lagTime=0.1, lowSlope= -1, highSlope=1, warpSlope=\lin;
		var chain = this.prAudify.collect { |item| FFT(LocalBuf(1024), item) };
		^chain.prStaticAnalysis(
			{ |ch|
				var slope = FFTSlope.kr(ch);
				slope.max(-10).min(10)
			},
			lowSlope, highSlope, warpSlope, low, high, warp, lagTime
		)
	}

	//--- Spectral Crest ---
	dCrest { arg low=0, high=1, warp=\lin, lagTime=0.1, timeWindow=5;
		var chain = this.prAudify.collect { |item| FFT(LocalBuf(1024), item) };
		^chain.prDynamicAnalysis(
			{ |ch|
				var crest = FFTCrest.kr(ch);
				crest.max(0).min(100) // Clip to reasonable range
			},
			0, 100, low, high, warp, lagTime, timeWindow
		)
	}

	sCrest { arg low=0, high=1, warp=\lin, lagTime=0.1, lowCrest=0, highCrest=100, warpCrest=\exp;
		var chain = this.prAudify.collect { |item| FFT(LocalBuf(1024), item) };
		^chain.prStaticAnalysis(
			{ |ch|
				var crest = FFTCrest.kr(ch);
				crest.max(0).min(100)
			},
			lowCrest, highCrest, warpCrest, low, high, warp, lagTime
		)
	}

	/*
	====================================================================
	ONSET DETECTORS
	====================================================================
	*/
	dOnsets {
		var in = this.prAudify;
		var amps = Amplitude.kr(in).max(1e-6);
		var chain = in.collect { |item| FFT(LocalBuf(1024), item) };
		^Onsets.kr(chain, threshold: (amps - 0.01).max(0.001));
	}

	dOnsetsJA {
		var in = this.prAudify;
		var amps = Amplitude.ar(in).max(1e-6);
		var chain = in.collect { |item| FFT(LocalBuf(2048), item) };
		^PV_JensenAndersen.ar(chain, threshold: amps.max(0.001));
	}

	dOnsetsHF {
		var in = this.prAudify;
		var amps = Amplitude.ar(in).max(1e-6);
		var chain = in.collect { |item| FFT(LocalBuf(2048), item) };
		^PV_HainsworthFoote.ar(chain, threshold: (amps - 0.001).max(0.0001));
	}
}