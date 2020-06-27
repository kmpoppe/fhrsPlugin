# fhrsPlugin - A JOSM Plugin
[![Twitter](https://img.shields.io/badge/Twitter-@kmpoppe-1DA1F2.svg?style=for-the-badge&logo=twitter)](https://twitter.com/kmpoppe)
[![Custom badge](https://img.shields.io/static/v1?label=TELEGRAM&message=%40kmpoppe&color=0088ff&logo=telegram&style=for-the-badge)](https://t.me/kmpoppe)
[![GitHub All Releases](https://img.shields.io/github/downloads/kmpoppe/fhrsPlugin/total?style=for-the-badge)](https://github.com/kmpoppe/fhrsPlugin/releases/latest)

This plugin for JOSM allows to use the FHRS API to merge data into OpenStreetMap.

## Contributing

If you want to contribute to this plugin, please read [CONTRIBUTING.md](/CONTRIBUTING.md).

## How to install this plugin manually

Download [fhrsPlugin.jar](https://github.com/kmpoppe/fhrsPlugin/releases/latest/download/fhrsPlugin.jar) and copy it to "plugins" inside the [user-data directory](https://josm.openstreetmap.de/wiki/Help/Preferences#JOSMpreferencedatacachedirectories).

Afterwards, restart JOSM,  go to settings, Plug-Ins and enable "fhrs". Now you have to restart JOSM again.

## How to use this plugin

The plugin adds a main menu entry called "FHRS".

**You need to select exactly one object on the map to use any of the menu entries!**

The menu "Get information" will get the data from FHRS that belongs to the fhrs:id, that's already set in the object. If there is none, the action is cancelled.

The menu "Search entry" looks up establishments in FHRS using this three options in order:

1. name and address
2. address only
3. name only

The search result is displayed in a list for you to pick the right establishment. After choosing one the merge dialog will show.
