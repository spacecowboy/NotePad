#!/usr/bin/env python
'''
Removes translations which do not exceed
50% translated strings (roughly)
'''
from __future__ import print_function, division
import os, re, fileinput

stringfile = 'strings.xml'
resdir = './res/'
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

        if length < 0.6 * englishlength:
            print("Delete", lang)
            os.remove(fpath)
