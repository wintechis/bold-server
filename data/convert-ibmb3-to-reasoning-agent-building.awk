#!/usr/bin/gawk -f
#
# Assigns some rooms to room classes from the Real Estate Core ontology and relativises Brick URIs.
#

BEGIN { rdf_type = "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>" }

{
  if ($0 ~ "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://buildsys.org/ontologies/Brick#Room> .$") {

    switch ($1) {
    case "<Room_ExpoSpace>":
      print $1, rdf_type, "<ExhibitionRoom>", ".";
      break;
    case "<Room_Aisle>":
    case "<Room_AisleWay>":
    case "<Room_BackAisle>":
    case "<Room_FrontAisle>":
    case "<Room_MiddleAisle>":
    case "<Room_TopAisle>":
      print $1, rdf_type, "<Hallway>", ".";
      break;
    case "<Room_StaircaseA>":
    case "<Room_StaircaseD>":
      print $1, rdf_type, "<Stairwell>", ".";
      break;
    case "<Room_Beckett>": # Assuming they name their meeting rooms after poets
    case "<Room_Kavanagh>":
    case "<Room_Vesda>":
    case "<Room_Wilde>":
    case "<Room_Yeats>": 
      print $1, rdf_type, "<ConferenceRoom>", ".";
      break;
    case "<Room_ExpoSpaceWC>":
    case "<Room_FemaleWC>":
    case "<Room_MenWC>":
    case "<Room_SOR42_G_MALE_WC>":
    case "<Room_SOR42_G_FEMALE_WC>":
    case "<Room_SOR46_G_Expo_WC>":
    case "<Room_SOR42_G_WC>":
    case "<Room_SOR42_F_WC>":
      print $1, rdf_type, "<Toilet>", ".";
      break;
    case "<Room_FemaleWC_Shower>":
    case "<Room_MenWC_Shower>":
    case "<Room_Shower>":
      print $1, rdf_type, "<ShowerRoom>", ".";
      break;
    case "<Room_FemaleWashroom>":
    case "<Room_MensWashroom>":
      print $1, rdf_type, "<PersonalHygiene>", ".";
      break;
    case "<Room_DisabledWC>":
      print $1, rdf_type, "<DisabledToilet>", ".";
      break;
    case "<Room_Coffeedesk>":
    case "<Room_CoffeeDesk>":
      print $1, rdf_type, "<CoffeeRoom>", ".";
      break;
    case "<Room_Reception>":
      print $1, rdf_type, "<Reception>", ".";
      break;
    default:
      print $0;
    }

  } else {
    gsub(/http:\/\/buildsys.org\/ontologies\//, "", $0); # Make URIs relative
    print $0
  }
}

