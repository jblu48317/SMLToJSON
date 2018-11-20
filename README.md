# SMLToJSON

SMLToJSON is a simple Smart Meter data converter using the jSML version 1.1 library published by Fraunhofer-Institut fuer Solare Energiesysteme ISE. The library needed to run this module can be downloaded from:

https://www.openmuc.org/sml/

Please copy the downloaded jar to the 'libs' directory in the installation path of the software.

SMLToJSON is licensed under the Mozilla Public License v2.0.

SMLToJSON contains

- the source code (/src)
- additional libraries needed (/libs)
- sample bash script launching the java module (/bin)

SMLToJSON converts an SML data file read from the German INFO interface of a "Basiszaehler" or "moderne Messeinrichtung" into a simple JSON structure.

Please open the example start bash files in the 'bin' directory and adopt the configuration to your needs

In case og using RXTX-communication from the meterv data input device check out special serial device configurations that are neede for your operating system. Install the RXTX support, if necessary.
