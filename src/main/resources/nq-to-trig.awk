#!/usr/bin/gawk -f

# converts nquads to trig
# assuming literals without spaces

{
  if (NF < 6) {
  if (NF > 4) { # && substr($(NF-1),length($(NF-1)) == ">")  { # a quad! 
    if (seenctx[$(NF-1)] != "true") {
      data[$(NF-1)][0][0] = $1;
      data[$(NF-1)][0][1] = $2;
      data[$(NF-1)][0][2] = $3;
    } else {
      newidx = length(data[$(NF-1)]) + 1;
      data[$(NF-1)][newidx][0] = $1;
      data[$(NF-1)][newidx][1] = $2;
      data[$(NF-1)][newidx][2] = $3;
    }
    seenctx[$(NF-1)] = "true"
  }}
}
END {
  for (ctx in data) {
    print ctx, "{"
    for (triple in data[ctx]) {
       print data[ctx][triple][0], data[ctx][triple][1], data[ctx][triple][2], "."
    }
   print "}"
  }
}
