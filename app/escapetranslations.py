#!/usr/bin/env python
'''
Fixes missing backslashes in translation files.
'''
from __future__ import print_function
import os, re, fileinput

def fixfile(filename):
    print(filename)
    for line in fileinput.FileInput(filename, inplace=1):
        # If ['] is not preceeded by [\], replace with [\']
        line = re.sub(r'(?<=[^\\])\'', r"\'", line)
        # replace ... with ellipsis &#8230;
        line = re.sub(r'\.\.\.', r'&#8230;', line)
        print(line, end='')

stringfile = 'strings.xml'
resdir = './src/main/res/'

# List dirs in res dir
dirs = os.listdir(resdir)

langs = []

for dir in dirs:
    if not dir.startswith('values-'):
        continue

    m = re.match('^values\-([a-z]{2})(-r([A-Z]{2}))?$', dir)

    if m is not None:
        if not os.path.exists(os.path.join(resdir, m.group(0),
                                           stringfile)):
            continue

        fixfile(os.path.join(resdir, m.group(0), stringfile))

        # Language iso code
        ##isocode = m.group(1)
        #if m.group(3) is not None:
            # Region code
        #    isocode += "_" +  m.group(3)

        #langs.append(isocode)

#for isocode in sorted(langs):
#    print(isocode)

#replacing = False
#for line in fileinput.FileInput(arrayfile, inplace=1):
#    if not replacing:
#        print(line, end='')
#
#    if 'name="translated_langs"' in line:
#        replacing = True
        #Print list of languages
        #Locale default is empty string
        #print('{}<item></item>'.format(' '*8))
#        for isocode in sorted(langs):
#            print('{}<item>{}</item>'.format(' '*8, isocode))

#    if replacing and '</string-array>' in line:
#        replacing = False
#        print(line, end='')
