# Adaptive Mappings for SuperCollider

A custom method library for real-time feature analysis and intuitive signal mapping in SuperCollider, designed specifically for the speed and fluidity of live coding.

This library provides a suite of powerful tools to extract musical features (like pitch, loudness, and spectral shape) from any audio signal and map them to control parameters. The core innovation is its dual-system approach, offering both **dynamic** and **static** mapping for every feature.

- **Dynamic Mapping (`d...`)**: Automatically adapts to the signal's recent dynamic range. A quiet, delicate passage can have just as much expressive control over a parameter as a loud, aggressive one. This is ideal for improvisational and responsive control.
- **Static Mapping (`s...`)**: Maps the signal against a fixed, absolute range. This is perfect for predictable, repeatable control where specific values matter.

## Table of Contents

- [Why Use This Library?](#why-use-this-library)
- [Installation](#installation)
- [Quick Start](#quick-start)
- [Core Concepts](#core-concepts)
  - [Dynamic vs. Static Mapping](#dynamic-vs-static-mapping)
  - [Method Naming Convention](#method-naming-convention)
- [Usage Examples](#usage-examples)
  - [Example 1: Dynamic Loudness Control](#example-1-dynamic-loudness-control)
  - [Example 2: Static Pitch to Pan Mapping](#example-2-static-pitch-to-pan-mapping)
  - [Example 3: Triggering Events with Onsets](#example-3-triggering-events-with-onsets)
- [API Reference](#api-reference)
  - [Common Arguments](#common-arguments)
  - [Time-Domain Features](#time-domain-features)
  - [Frequency-Domain (FFT) Features](#frequency-domain-fft-features)
  - [Onset Detectors](#onset-detectors)
- [Contributing](#contributing)

## Why Use This Library?

In live coding, creating meaningful control signals from audio is often a hassle. You might find yourself constantly tweaking thresholds to get a loudness follower to work for both quiet and loud sections.

This library solves that by offering two distinct modes of interaction:

The **dynamic mapping** (`d...`) methods learn from the incoming audio in real-time. Here’s how they work:

1.  **Listen**: The method finds the lowest and highest values of a feature (like amplitude or brightness) that have occurred over a recent time window.
2.  **Normalize**: Internally, it uses this dynamic min/max range to scale the current signal to a normalized `0..1` value. This represents *where* the current signal falls within its own recent history.
3.  **Map**: It immediately maps this normalized `0..1` value to the final output range you specify with the `low` and `high` arguments.

The result is an expressive control signal mapped directly to your desired range, which adapts "musically" to the performance.

Conversely, the **static mapping** (`s...`) methods map the feature against a fixed, absolute range you pre-define, which is ideal for signals whose properties stay within a known and stable range.

## Installation

1.  **Clone the Repository**:
    ```bash
    git clone https://github.com/your-username/your-repo-name.git
    ```
2.  **Find your SuperCollider Extensions Directory**: In SuperCollider, run the line `Platform.userExtensionDir` and press `Cmd/Ctrl+Enter`. This will post the path to your extensions folder.
3.  **Move the Folder**: Move the cloned repository folder into the SuperCollider extensions directory you just found.
4.  **Recompile**: Restart SuperCollider, or open the Language menu and select **"Recompile Class Library"** (`Cmd/Ctrl+Shift+L`).

## Quick Start

```supercollider
// Start a noise source
(
Ndef(\noise, {
	PinkNoise.ar(0.3)
}).play;
)

// Use the dynamic loudness of the noise to control a filter's cutoff frequency.
// dLoud maps the recent min/max loudness to the range 500-4000 Hz.
(
Ndef(\filter, {
	var loudControl = Ndef(\noise).dLoud(low: 500, high: 4000, warp: \exp);
	LPF.ar(Ndef(\noise), loudControl, 0.1)
}).play;
)

// Stop everything
Ndef(\filter).free;
Ndef(\noise).free;
```

## Core Concepts

### Dynamic vs. Static Mapping

This is the central idea of the library.

-   **`d...` methods (Dynamic)**:
    -   Analyzes the signal over a sliding `timeWindow` (default 5 seconds).
    -   Finds the minimum and maximum values of the feature that have occurred within that window.
    -   Internally scales the current feature value to a normalized `0..1` range based on that dynamic min/max.
    -   Maps this `0..1` value to your desired output range, specified by the `low` and `high` arguments.
    
-   **`s...` methods (Static)**:
    -   Compares the signal against a fixed, absolute input range that you define (e.g., `-60` to `0` dB for amplitude).
    -   Internally scales the current feature value to a normalized `0..1` range based on these absolute boundaries.
    -   Maps this `0..1` value to your desired output range, specified by the `low` and `high` arguments.

### Method Naming Convention

The API is designed for speed:

-   The prefix `d` stands for **D**ynamic.
-   The prefix `s` stands for **S**tatic.
-   The suffix is the feature name (e.g., `Amp`, `Pitch`, `Cent`).

So, `dAmp` is dynamic amplitude, and `sPitch` is static pitch.

## Usage Examples

### Example 1: Dynamic Loudness Control

Here, `dFlat` (dynamic spectral flatness) controls the mix between a noisy and a tonal sound. When the input is more noise-like (high flatness), we hear more of the sine wave. The mapping adapts automatically.

```supercollider
(
Ndef(\source, {
	// A sound that varies between tonal and noisy
	var sound = SinOsc.ar(440, 0, 0.2) + (BrownNoise.ar(0.2) * Dust.kr(3));
	// Add a filter to make it more interesting
	RLPF.ar(sound, LFTri.kr(0.2).range(500, 2500), 0.1)
}).play;
)

(
Ndef(\effect, {
	var flatness = Ndef(\source).dFlat(low: 0, high: 1, lagTime: 0.5);
	var noisySound = PinkNoise.ar(0.3);
	var tonalSound = SinOsc.ar(220, 0, 0.4);

	// Crossfade based on spectral flatness
	XFade2.ar(tonalSound, noisySound, flatness.linlin(0, 1, -1, 1))
}).play;
)
```

### Example 2: Static Pitch to Pan Mapping

We want to pan a sound left if its pitch is low (220 Hz) and right if it's high (880 Hz). `sPitch` is perfect because the boundaries are absolute.

```supercollider
(
Ndef(\pitchPan, {
	var freq = LFSaw.kr(0.3).range(220, 880);
	var sig = Saw.ar(freq, 0.3);
	// Map the absolute frequency range 220-880 Hz to the pan range -1 to 1
	var pan = sig.sPitch(low: -1, high: 1, lowFreq: 220, highFreq: 880);
	Pan2.ar(sig, pan)
}).play;
)
```

### Example 3: Triggering Events with Onsets

The onset detectors are special cases that return a trigger signal. Here, we use `dOnsets` to trigger a percussive synth.

```supercollider
(
Ndef(\mic, { SoundIn.ar(0) }).play; // Use your microphone
)

(
Ndef(\drum, {
	var trig = Ndef(\mic).dOnsets;
	var env = EnvGen.kr(Env.perc(0.01, 0.5), trig);
	var sound = SinOsc.ar(TExpRand.kr(100, 800, trig), 0, env * 0.5);
	sound + BPF.ar(PinkNoise.ar(1.5), 5000, 0.3, env);
}).play;
)
```

## API Reference

### Common Arguments

All `d...` and `s...` methods share these final mapping arguments:

-   `low` (float): The minimum output value. Defaults to `0`.
-   `high` (float): The maximum output value. Defaults to `1`.
-   `warp` (symbol): The mapping curve. Can be `\lin`, `\exp`, `\log`, etc. Defaults to `\lin`.
-   `lagTime` (float): The time in seconds to smooth the output signal. Defaults to `0.1`.

### Time-Domain Features

These are computationally inexpensive.

-   **`dPitch`/`sPitch`**: Tracks fundamental frequency (in MIDI note numbers for `dPitch`, Hz for `sPitch`).
-   **`dAmp`/`sAmp`**: Tracks peak amplitude (in decibels).
-   **`dRms`/`sRms`**: Tracks Root Mean Square power (in decibels).

### Frequency-Domain (FFT) Features

These are more CPU-intensive as they require an FFT calculation. They offer deep insight into the timbral quality of a sound.

-   **`dLoud`/`sLoud`**: **A measure of perceived volume.**
    Unlike `dAmp` which just measures the raw signal peak, `dLoud` models the non-linear sensitivity of human hearing (as described by equal-loudness contours). It more accurately represents how "loud" a sound feels to a listener, giving more weight to midrange frequencies our ears are most sensitive to.

-   **`dCent`/`sCent`**: **A measure of perceptual brightness.**
    The Spectral Centroid is the "center of mass" of the spectrum. A high centroid value means the sound's energy is concentrated in higher frequencies, making it sound bright, sharp, or "metallic." A low value means the energy is in the lower frequencies, making it sound dark, dull, or muffled.
   
-   **`dFlat`/`sFlat`**: **A measure of noisiness vs. tonality.**
    This feature describes how "flat" or "peaked" the spectrum is. A high flatness value (near 1.0) indicates the sound is noise-like, with energy spread evenly across the spectrum (like wind or static). A low value (near 0.0) indicates the sound is tonal, with energy concentrated in a few specific peaks (like a sine wave or a clear vocal note).
   
-   **`dSpread`/`sSpread`**: **A measure of spectral richness or complexity.**
    Related to the centroid, this describes how "spread out" the spectrum is. A sound with a low spread is "thin" and focused, with its frequencies clustered tightly around its center. A sound with a high spread is "rich" and "full-bodied," with significant energy far from its center. A complex chord, for instance, has a high spread.
   
-   **`dCrest`/`sCrest`**: **A measure of a spectrum's "peakiness."**
    Similar to flatness, this is another way to gauge tonality. It measures the ratio of the loudest frequency component to the overall average. A high crest factor indicates that one or a few frequencies are dramatically more prominent than the rest, which is characteristic of a strong, clear tone emerging from a quieter or noisier background.
    
-   **`dSlope`/`sSlope`**: **A measure of the spectrum's overall "tilt."**
    This describes whether a sound has more energy in the low or high frequencies. A negative slope means the sound is "dark" or bass-heavy, with energy decreasing as frequency increases (common in natural sounds). A positive slope means the sound is "bright" or treble-heavy. It's like a simple, one-number summary of the sound's EQ curve.
    
-   **`dPcile`/`sPcile`**: **A robust measure of brightness via energy distribution.**
    Spectral Percentile answers the question: "Below which frequency does 95% of the sound's energy lie?" It's a powerful alternative to the centroid for measuring brightness, as it's less affected by a few extreme high-frequency outliers. It gives a solid indication of where the main "body" of the sound's power is located.
    
### Onset Detectors

These return a trigger signal (`> 0`) when an attack is detected.

-   **`dOnsets`**: A general-purpose onset detector.
-   **`dOnsetsJA`**: The Jensen-Andersen phase-based onset detector.
-   **`dOnsetsHF`**: The Hainsworth-Foote spectral flux-based onset detector.

## Contributing

Contributions are welcome! If you find a bug, have an idea for a new feature, or want to improve the implementation, please feel free to open an issue or submit a pull request.
