#!/bin/sh
#
# Creates a Trig file for the core and the building file of the Real Estate Core ontology.
#

FILENAME=reasoning-agent_real-estate-core.trig

# AWK script to add the mapping from REC's Room to BRICK's Room
ROOM='$0 == "<Room> {" { print $0, "<Room> <http://www.w3.org/2000/01/rdf-schema#subClassOf> <Brick#Room> ." } $0 != "<Room> {" { print $0 }'

rapper -o ntriples https://doc.realestatecore.io/3.2/core.rdf | ./rec-to-trig.awk | awk "$ROOM" > $FILENAME

rapper -o ntriples https://doc.realestatecore.io/3.2/building.rdf | ./rec-to-trig.awk | awk "$ROOM" >> $FILENAME

