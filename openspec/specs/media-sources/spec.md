# media-sources Specification

## Purpose

Behavior contract for the media source primitives shared by Solim media components: `ImageSource` for images (drawable, texture region, URL, or local file) and `VideoSource` for videos (URL or local file). Each source exposes a typed handle so components can resolve static and asynchronous media without overloading their APIs.

## Requirements

### Requirement: ImageSource Abstraction
Solim SHALL provide an `ImageSource` type representing an image data source. `ImageSource` SHALL provide factory methods:
- `ImageSource.of(Drawable)` for static in-memory drawables
- `ImageSource.of(TextureRegion)` for texture regions
- `ImageSource.of(String url)` for remote network URLs
- `ImageSource.of(Fi file)` for local files

#### Scenario: Resolve drawable source
- **WHEN** an `ImageSource` is created from a `Drawable`
- **THEN** it SHALL provide immediate access to that `Drawable` synchronously without asynchronous loading

#### Scenario: Resolve remote URL source
- **WHEN** an `ImageSource` is created from a URL string
- **THEN** it SHALL indicate that asynchronous loading is required and provide the target URL

### Requirement: VideoSource Abstraction
Solim SHALL provide a `VideoSource` type representing a video data source. `VideoSource` SHALL provide factory methods:
- `VideoSource.of(String url)` for remote network URLs
- `VideoSource.of(Fi file)` for local video files

#### Scenario: Resolve remote video source
- **WHEN** a `VideoSource` is created from a URL string
- **THEN** it SHALL provide the video URL for loading and caching

#### Scenario: Resolve local video file source
- **WHEN** a `VideoSource` is created from a local `Fi` file
- **THEN** it SHALL provide the file handle directly to bypass network fetching
