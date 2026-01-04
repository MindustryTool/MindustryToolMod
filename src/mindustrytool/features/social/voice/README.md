# Voice Chat Feature (Social Domain)

## Overview
Provides Proximity and Global voice chat functionality using WebRTC/UDP.

## API (`api/`)
- `VoiceChatManager`: Main entry point for starting/stopping voice.
- `VoiceMicrophone`: Handles audio input.
- `VoiceSpeaker`: Handles audio output.

## Implementation (`internal/`)
- `VoiceConstants`: Protocol definitions.
- `JitterBuffer`: Networking optimization.
- `AudioMixer`: Audio processing.

## Events
- Listen for `VoiceConnectionEvent` on the central EventBus.
