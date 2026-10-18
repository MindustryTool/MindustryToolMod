## ADDED Requirements

### Requirement: Video Source Configuration
`SolimVideo` SHALL accept a `VideoSource` representing the source of the video stream, via constructor (`new SolimVideo(VideoSource)`), builder method (`src(VideoSource)`), and declarative property (`Values.src(VideoSource)` or `Values.src(Readable<VideoSource>)`), with convenience overloads for URL strings.

#### Scenario: Video initialized with VideoSource
- **WHEN** a `SolimVideo` is created with `VideoSource.of("http://example.com/test.mp4")`
- **THEN** it SHALL extract the URL and begin loading the video stream

#### Scenario: Video source configured via props
- **WHEN** a `SolimVideo` is configured via `.props(src(VideoSource.of(url)))`
- **THEN** the video component SHALL load and present the specified video source
