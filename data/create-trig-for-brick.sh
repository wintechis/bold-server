#!/bin/sh
#
# Creates a Trig file with the three parts of the Brick ontology, with relativised URIs.
#

FILENAME=reasoning-agent_brick.trig

if [ -f $FILENAME ] ; then
  rm $FILENAME
fi

for i in Brick BrickFrame BrickTag ; do

  echo "Processing $i..." >&2

  echo "<$i> { " >> $FILENAME

  rapper -i turtle -o turtle -I 'http://buildsys.org/ontologies/' -f'relativeURIs' `# downloading RDF and relativising URIs to a base URI` \
     https://raw.githubusercontent.com/BuildSysUniformMetadata/GroundTruth/789ff13adc7713bb7cbfd374f370d9dc72ac07ac/Brick/$i.ttl \
  | tail -n+2 `# removing the line with @base` \
  >> $FILENAME

  echo "}" >> $FILENAME

done

