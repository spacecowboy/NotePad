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
Removes translations which do not exceed
50% translated strings (roughly)
'''
from __future__ import print_function, division
import os, re, fileinput

stringfile = 'strings.xml'
resdir = './src/main/res/'
englishdir = 'values/'

def get_number_of_lines(fname):
    '''Returns the number of lines in file'''
    result = 1
    with open(fname) as f:
        for l in f:
            result += 1
    return result


def get_english_length():
    '''Read and return the number of lines
    of the english strings'''
    return get_number_of_lines(os.path.join(resdir,
                                            englishdir,
                                            stringfile))

# License text is 17 lines long, dont count that
liclength = 17

englishlength = get_english_length() - liclength
print("English: ", englishlength)

# List dirs in res dir
dirs = os.listdir(resdir)

langs = []

for dir in dirs:
    if not dir.startswith('values-'):
        continue

    m = re.match('^values\-([a-z]{2})(-r([A-Z]{2}))?$', dir)

    if m is not None:
        lang = m.group(0)
        fpath = os.path.join(resdir, lang, stringfile)

        if not os.path.exists(fpath):
            continue

        length = get_number_of_lines(fpath)
        length -= liclength

        print(lang, length)

        if length < 0.4 * englishlength:
            print("Delete", lang)
            os.remove(fpath)
