# WifiProbe

WifiProbe scans and displays Wifi access points in your environment.

## Description

The app registers a broadcast listener for the Wifi scanner (WifiManager.EXTRA_RESULTS_UPDATED).
Instead using the WifiManager.startScan(), it starts and stops immediately the wifi action panel, that trigger a wifi scan. So there is no limit for number of scan per minute, as with startScan().

Toolbar:
![Toolbar](./varia/md_images/toolbar.jpg)

## Getting Started

### Dependencies

* None

### Installing

* How/where to download your program
* Any modifications needed to be made to files/folders

### Executing program

* How to run the program
* Step-by-step bullets

```
#!/bin/bash
echo Hello world
```

## Help

Any advise for common problems or issues.
```
command to run if program contains helper info
```

## Authors

Contributors names and contact info

ex. Dominique Pizzie  
ex. [@DomPizzie](https://twitter.com/dompizzie)

## Special

### Determination of the manufacturer name of the router

An organizationally unique identifier (OUI) number uniquely identifies a vendor or manufacturer.
In MAC addresses, the OUI is combined with a 24-bit number (assigned by the assignee of the OUI) to form the address. The first three octets of the address are the OUI.
[Wikipedia about OUI](https://en.wikipedia.org/wiki/Organizationally_unique_identifier).
WifiProbe uses following lookup file: [The IEEE public OUI listings](http://standards-oui.ieee.org/oui/oui.csv).

## Version History

* 1.0
  * Initial version of the App

## License

This project is licensed under the [Apache License 2.0](https://github.com/NetVarg/WifiProbe/blob/main/LICENSE).

## Acknowledgments

Inspiration, code snippets, etc.

* [awesome-readme](https://github.com/matiassingers/awesome-readme)
