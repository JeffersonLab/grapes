This reopisotry is archival, read-only, moved to https://code.jlab.org/hallb/clas12/coatjava/grapes

# grapes
This is CLAS12 "analysis trains".  Everything below was imported from [here](https://userweb.jlab.org/~gavalian/docs/sphinx/hipo/html/chapters/analysis_train.html) and converted to markdown.
  
Data Analysis Trains
===========================================================================

CLAS12 Analysis trains are used for filtering reconstruction DST’s for specific reactions (requested by users) for further analysis.

```
io-services:
  reader:
    class: org.jlab.jnp.grapes.io.HipoFrameReader
    name: HipoFrameReader
  writer:
    class: org.jlab.jnp.grapes.io.HipoFrameWriter
    name: HipoFrameWriter
services:
  - class: org.jlab.jnp.grapes.services.GenericWagon
    name: PIONS
  - class: org.jlab.jnp.grapes.services.GenericWagon
    name: GAMMAS
  - class: org.jlab.jnp.grapes.services.GenericWagon
    name: PIONPID
configuration:
  services:
    PIONS:
      id: 1
      tagger: 11:X+:X-:Xn
      forward: 2-:1+
    GAMMAS:
      id: 2
      filter: 11:2212:22:22:X+:X-:Xn
    PIONPID:
      id: 3
      forward: 11:211:-211:Xn
mime-types:
  - binary/data-hipo-frame
```

This defines three generic Wagons that will skim data into separate files, the naming convention is that “\_id.hipo” fill be added to original file name. The filters are set to look for different event topologies depending on the detector system (central, forward and tagger), more about filters in the next section.

After setting all apropriate directory paths and making sure that the file described by variable fileList exists and contains names of the files to be processed, the train can be run:

`run-clara -y path/to/train.yml`

Event Filters
-------------------------------------------------------------

There are separate event topology filters for each part of CLAS12 detector. The detector is separated into the Forward Detector, the Central Detector and the Forward Tagger. And for each of three parts, separate event filters can be set. The configuration of a Wagon in the CLARA YAML file looks like this:
```
services:
 PIONS:
   id: 1
   tagger:  11:X+:X-:Xn
   forward: 211:-211:X+:X-:Xn
   central: X+:X-:Xn
```
This is the configuration for a Wagon called “PIONS” that will select only events where there is an identified electron in the Forward Tagger (X+:X-:Xn that follows just indicate that there can be other particles also in the Forward Tagger), there is at least one positive and at least one negative pion in the Forward Detector (X+:X-:Xn again means any other number of particles as long as two pions are there), and any number of particles in the Central Detector.

To specify an exclusive event topology, for example one negative pion and one proton, use the following syntax:

`forward: -211:2212`

The number of particle with given PID is not limited to one, the line can contain several particle ID’s of the same type, for example:

`forward: -211:2212:2212:2212`

will select events with 3 identified proton and one negative pion in final state. The X\[?\] flags can also be used to indicate a semi-exclusive topology, for example:

`forward: -211:2212:Xn`

will select events that have only two charged particle (namely negative pion and proton) and any number of neutral particles along with them. The filter also allows the user to select the particle by charge rather than by PID, so further improvements can be done on PID. For example:

`forward: 2-:1+:Xn`

When the number is followed by any of these symbol (“+”,”-”,”n”), it is assumed that the number is the number of that particular charge particle to be present in the event. The above example will select events with 2 (and only 2) negative particles, one (and only 1) positive particles and any number of neutral particles. To make this selection more inclusive, for example to require more than one positive particle, use following line:

`forward: 2-:1+:X+:Xn`

Adding X+ to the filter will force it to satisfy first the requirement “1+”, which means one positive particle and then will satisfy the “X+” requirement, which means any number of particles, so the selected final state will have 2 (and only 2) negative particles, 1 or more positive particles, and any number of neutral particles.

Inclusivity Syntax
-----------------------------------------------------------------------

The use of inclusivity operator (X\[?\]) can not be described in terms of OR or AND operation. It adds a condition to already existing particle selection list without explicitly imposing it’s rule. For example:
```
X+ // any number of positive particles
2+ // 2 positive particles
2+:X+ // 2 or more positive particles
X+:2+ // 2 or more positive particles
```
the order in which operators are written is not important, since these flags are applied independently to validate the event.
