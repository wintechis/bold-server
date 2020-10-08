#!/bin/sh

./convert-ibmb3-to-reasoning-agent-building.awk IBM_B3.trig > reasoning-agent_IBM_B3.trig
./create-trig-for-brick.sh
./create-trig-for-real_estate_core.sh
