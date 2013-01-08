#!/usr/bin/env python
'''
Lists the values directories for languages. Then
inserts them in arrays.xml
'''
from __future__ import print_function
import os, re, fileinput


def print_play_texts(lang, filename):
    print(lang, filename)
    printing = False
    for line in fileinput.input(filename):
        if not printing and 'name="google_play' in line:
            print("\nLanguage: {}".format(lang))
            printing = True
            continue

        if printing:
            print(line, end='')

        if printing and '</string>' in line:
            printing = False
            print("\n", end='')


# List dirs in res dir
resdir = './res/'
dirs = os.listdir(resdir)
stringfile = 'strings.xml'

# First english
print_play_texts("en", os.path.join(resdir, 'values/', stringfile))

for dir in dirs:
    if not dir.startswith('values-'):
        continue

    m = re.match('^values\-([a-z]{2})(-r([A-Z]{2}))?$', dir)

    if m is not None:
        # Language iso code
        isocode = m.group(1)
        if m.group(3) is not None:
            # Region code
            isocode += "_" +  m.group(3)

        print_play_texts(isocode, os.path.join(resdir, m.group(0), stringfile))
