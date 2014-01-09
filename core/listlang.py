#!/usr/bin/env python
'''
Lists the values directories for languages. Then
inserts them in arrays.xml
'''
from __future__ import print_function
import os, re, fileinput

#Array file
arrayfile = './res/values/arrays.xml'

# List dirs in res dir
dirs = os.listdir('./res/')

langs = []

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

        langs.append(isocode)

if 'en' not in langs:
    langs.append('en')

#for isocode in sorted(langs):
#    print(isocode)

replacing = False
for line in fileinput.FileInput(arrayfile, inplace=1):
    if not replacing:
        print(line, end='')

    if 'name="translated_langs"' in line:
        replacing = True
        #Print list of languages
        #Locale default is empty string
        #print('{}<item></item>'.format(' '*8))
        for isocode in sorted(langs):
            print('{}<item>{}</item>'.format(' '*8, isocode))

    if replacing and '</string-array>' in line:
        replacing = False
        print(line, end='')
