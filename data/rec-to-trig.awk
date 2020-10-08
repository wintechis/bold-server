#!/usr/bin/gawk -f

$1 != "<https://w3id.org/rec/building/>" && $1 != "<https://w3id.org/rec/core/>" { # Filter out some unwanted triples
    if (seenctx[$1] != "true") {
      data[$1][0] = substr($0, 0, length($0) - 2);
    } else {
      newidx = length(data[$1]) + 1;
      data[$1][newidx] = substr($0, 0, length($0) - 2);
    }
    seenctx[$1] = "true";
}

END {
  for (ctx in data) {
    if (ctx ~ "^_:") # No blank nodes as graph name
      continue;
    c = ctx;
    gsub(/https:\/\/w3id.org\/rec\/building\//, "", c); # Make URIs relative
    gsub(/https:\/\/w3id.org\/rec\/core\//, "", c); # Make URIs relative
    print c, "{";
    for (triple in data[ctx]) {
       a = data[ctx][triple];
       gsub(/https:\/\/w3id.org\/rec\/building\//, "", a); # Make URIs relative
       gsub(/https:\/\/w3id.org\/rec\/core\//, "", a); # Make URIs relative
       print  a, ".";
    }
   print "}"
  }
}

