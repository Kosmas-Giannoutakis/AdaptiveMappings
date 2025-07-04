Of course. A great README is essential for any open-source project. Here is a comprehensive README file written in Markdown, suitable for your project's GitHub repository. It explains the concept, provides clear usage examples, and includes an API reference.

---

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

This library solves that. The **dynamic mapping** methods learn from the incoming audio in real-time. They find the quietest and loudest parts of the signal over a given time window and normalize the output accordingly, always giving you a responsive `0..1` signal to work with. It's like having a little engineer automatically riding the faders for you.

When you need precise, predictable control, the **static mapping** methods are there. Need a filter to open up only when a signal crosses a specific frequency? `sCent` (static centroid) is your tool.

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
    -   Finds the minimum and maximum values of the feature within that window.
    -   Scales the current feature value to a `0..1` range based on that dynamic min/max.
    -   **Use Case**: Expressive, adaptive control that "just works" regardless of the input signal's overall level. Perfect for modulating parameters based on performance dynamics.

-   **`s...` methods (Static)**:
    -   Compares the signal against a fixed, user-defined range (e.g., `-60` to `0` dB for amplitude).
    -   Scales the current feature value to a `0..1` range based on these absolute boundaries.
    -   **Use Case**: Technical, precise control. Useful for creating triggers, gates, or effects that should only activate when a signal crosses a specific, predetermined threshold.

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

These are more CPU-intensive as they require an FFT calculation.

-   **`dLoud`/`sLoud`**: Perceptual loudness.
-   **`dCent`/`sCent`**: Spectral Centroid, the "center of mass" of the spectrum. A measure of brightness.
-   **`dFlat`/`sFlat`**: Spectral Flatness, a measure of how noisy vs. tonal a sound is.
-   **`dSpread`/`sSpread`**: Spectral Spread, how the spectrum is spread around the centroid.
-   **`dCrest`/`sCrest`**: Spectral Crest, another measure of tonality.
-   **`dSlope`/`sSlope`**: Spectral Slope.
-   **`dPcile`/`sPcile`**: Spectral Percentile, the frequency below which a percentage of the spectral energy lies.

### Onset Detectors

These return a trigger signal (`> 0`) when an attack is detected.

-   **`dOnsets`**: A general-purpose onset detector.
-   **`dOnsetsJA`**: The Jensen-Andersen phase-based onset detector.
-   **`dOnsetsHF`**: The Hainsworth-Foote spectral flux-based onset detector.

## Contributing

Contributions are welcome! If you find a bug, have an idea for a new feature, or want to improve the implementation, please feel free to open an issue or submit a pull request.
