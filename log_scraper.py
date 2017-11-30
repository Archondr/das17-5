#!/usr/bin/env python2

import sys

out = "parsed.txt"
values = []

for fname in sys.argv[1:]:
    with open(fname, "r") as fp:
        config = fname.split('.')[0]
        config = "\n" + config + "\n"
        values.append(config)
        for line in fp:
            if "Results" in line:
                values.append(line)

with open(out, "a") as fout:
    for val in values:
        fout.write(str(val))
