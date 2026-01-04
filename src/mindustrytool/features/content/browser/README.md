# Browser Feature (Content Domain)

## Overview
A complete in-game browser for Mindustry Maps and Schematics. Connects to `api.mindustry-tool.com`.

## API (`api/`)
- `BrowserFeature`: Main entry point.
- `ContentHandler`: Helper for downloading maps/schematics.
- `Api`: Raw HTTP request wrapper.

## Architecture
- `ui/*.java`: All dialogs (BrowserDialog, DetailDialog, ToolsMenuDialog).
- `data/*.java`: JSON data models (MapDetailData, ModData).
