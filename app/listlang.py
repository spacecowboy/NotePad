#    Copyright 2014 Jonas Kalderstam
#
#    This program is free software: you can redistribute it and/or modify
#    it under the terms of the GNU General Public License as published by
#    the Free Software Foundation, either version 3 of the License, or
#    (at your option) any later version.
#
#    This program is distributed in the hope that it will be useful,
#    but WITHOUT ANY WARRANTY; without even the implied warranty of
#    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#    GNU General Public License for more details.
#
#    You should have received a copy of the GNU General Public License
#    along with this program.  If not, see <http://www.gnu.org/licenses/>.
'''
Lists the values directories for languages. Then
inserts them in arrays.xml
'''
from __future__ import print_function
import os, re, fileinput

#Array file
arrayfile = './src/main/res/values/arrays.xml'

# List dirs in res dir
dirs = os.listdir('./src/main/res/')

langs = []

for dir in dirs:
    if not dir.startswith('values-'):
        continue

    m = re.match('^values\-([a-z]{2})(-r([A-Z]{2}))?$', dir)

    if m is not None:
        # If no file exists, then remove it
        hasfile = False
        for f in os.listdir('./src/main/res/' + dir):
            if f.endswith('.xml'):
                hasfile = True
                break
        if not hasfile:
            continue
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
