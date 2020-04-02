#!/usr/bin/gawk -f

# converts nquads to trig

{
if ($(NF-1) != "<http://localhost:40200/ldbbc/>") { # Filter out some unwanted quads
  if (NF > 4) { # a quad!
    if (seenctx[$(NF-1)] != "true") {
      data[$(NF-1)][0] = substr($0, 0, length($0) - length($(NF-1)) - 3);
    } else {
      newidx = length(data[$(NF-1)]) + 1;
      data[$(NF-1)][newidx] = substr($0, 0, length($0) - length($(NF-1)) - 3);
    }
    seenctx[$(NF-1)] = "true"
  }
}
}
END {
  for (ctx in data) {
    c = ctx
    gsub(/http:\/\/localhost:40200\/ldbbc\//, "", c) # Make URIs relative 
    gsub(/http:\/\/localhost:40300\//, "property-", c) # Make URIs relative
    print c, "{"
    for (triple in data[ctx]) {
       a = data[ctx][triple];
       gsub(/http:\/\/localhost:40200\/ldbbc\//, "", a) # Make URIs relative
       gsub(/http:\/\/localhost:40300\//, "property-", a) # Make URIs relative
       print  a, "."
    }
   print "}"
  }
}
