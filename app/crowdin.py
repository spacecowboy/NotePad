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
Python script for automatically downloading/updating translations
from/to the Crowdin translation service. It is specifically aimed
at Android projects which needs files placed in a certain way.

First you should configure your project to export files in the
following format:

    /values-%android_code%/%original_file_name%

This is given as an example by Crowdin themselves and will make the
exported zipfile contain the correct structure: values-es-rES/strings.xml
etc.

Now, we want the structure to be named values-es/strings.xml instead.

Example yaml:

project_identifier: nononsensenotes
api_key: some-key-here
base_path: /home/jonas/workspace/NotePad/app

# Only files named here are renamed.
files:
  -
    source: '/src/main/res/values/strings.xml'
    translation: '/src/main/res/values-%android_code%/%original_file_name%'
    languages_mapping:
      android_code:
        # Directories with manual codes. Here I want them to stay the same.
        zh-rTW: zh-rTW
        zh-rCN: zh-rCN
        pt-rBR: pt-rBR

'''

# Maintain python2 compatibility
from __future__ import print_function, division
import requests
import shutil
import re
import os
from zipfile import ZipFile
import yaml


tmpdir = ".tmpcrowdin"
__valuespattern__ = re.compile(r"values-((\w+?)-r(\w+))")


# Base of all other URLs
_BASEURL = 'https://api.crowdin.com/api/project/{project_identifier}/'


def update_file(filepath, projectid, projectkey):
    '''
    Update a file

    Parameters:
    - filepath, path to file to upload
    '''
    filename = os.path.split(filepath)[1]
    # Read the file
    with open(filepath, 'rb') as F:
        # Upload
        url = _BASEURL + 'update-file?key={project_key}'

        r = requests.post(url.format(project_identifier=projectid,
                                     project_key=projectkey),
                          files=dict(filename=F))
        print(r.content)
        print(r.text)
        # Error out if shit hit fan
        r.raise_for_status()


def download_translations(config, path, lang='all'):
    '''
    Download the latest built translations

    Parameters:
    - path, the folder where the resulting zip file will be saved.
    - lang, the specific language code to download. Default 'all'.
    '''
    # Make sure dir exists
    #if not os.path.isdir(path):
    #    os.mkdir(path)

    url = _BASEURL + "download/{package}.zip?key={project_key}"

    r = requests.get(url.format(project_identifier=config['project_identifier'],
                                project_key=config['api_key'],
                                package=lang))

    # Error out if shit hit fan
    r.raise_for_status()

    # All is good! Save to file
    filename = "{}.zip".format(lang)
    filepath = os.path.join(path, filename)
    with open(filepath, 'wb') as F:
        F.write(r.content)

    # Return the path to the file
    return filepath


def unzip(filepath, path=None, delete=True):
    '''
    Unzips a file.

    Parameters:
    - filepath, path to file to extract
    - path, place to extract files to. Default same dir.
    - delete, True if the zip file should be deleted after extraction
    '''
    if path is None:
        path = os.path.split(filepath)[0]
    zf = ZipFile(filepath)
    zf.extractall(path)
    zf.close()
    if delete:
        os.remove(filepath)


def listdirs(path):
    '''List only directories'''
    for f in os.listdir(path):
        if os.path.isdir(os.path.join(path, f)):
            yield f


def rename_value_dirs(config, prepath=""):
    '''
    Rename the dirs to follow android convention of
      values-AA
    instead of
      values-AA-rBB

    Default behavior is to remove the '-rBB' part of the name.
    If you want a different behavior, specify that in your
    config.
    '''
    for df in config['files']:
        if 'languages_mapping' not in df:
            lang_map = {}
        else:
            lang_map = df['languages_mapping']['android_code']
        fp = df['source']
        basedir, fname = os.path.split(fp)
        basedir, _ = os.path.split(basedir)
        # Just to make sure
        if basedir[0] == '/':
            basedir = basedir[1:]

        for d in listdirs(os.path.join(prepath, basedir)):
            m = __valuespattern__.match(d)
            if m:
                langregion, lang, region = m.groups()
                if langregion in lang_map:
                    # Use desired code
                    code = lang_map[langregion]
                else:
                    # Defalt, use just lang
                    code = lang
                # Do renaming
                src = os.path.join(prepath, basedir, d)
                dst = os.path.join(prepath, basedir, "values-" + code)
                os.rename(src, dst)


def read_config(path):
    with open(path, 'r') as FILE:
        try:
            config = yaml.load(FILE)
            return config
        except yaml.YAMLError as e:
            sys.exit(str(e) +
                     "\nCould not parse config file. Try validating it?\n" +
                     "http://yamllint.com/validator")


def mkdir_p(path):
    if not os.path.isdir(path):
        a, b = os.path.split(path)
        if a:
            mkdir_p(a)
        os.mkdir(path)


def copy_files(src, dst):
    '''
    Copy directory tree from src to dst. Behaves like

        mv src dst

    Destination does not have to be empty, but files will be
    overwritten if they exist with the same name.
    '''
    for srcdir, dirs, files in os.walk(src):
        if len(files) > 0:
            dstdir = srcdir.replace(src, dst)
            mkdir_p(dstdir)
            for f in files:
                srcfile = os.path.join(srcdir, f)
                dstfile = os.path.join(dstdir, f)
                shutil.copy(srcfile, dstfile)



if __name__ == '__main__':
    import sys

    if '-c' in sys.argv:
        i = sys.argv.index('-c')
        sys.argv.pop(i)
        config_path = sys.argv.pop(i)
    else:
        config_path = 'crowdin.yaml'
    #homeconfig = os.path.expanduser("~/.crowdin.yaml")

    config = read_config(config_path)

    if len(sys.argv) < 2:
        sys.exit(__doc__)
    action = sys.argv[1]
    if action == 'download':
        if len(sys.argv) > 2:
            base_path = sys.argv[2]
        else:
            base_path = config['base_path']
        # Make sure to clean first
        if os.path.isdir(tmpdir):
            sys.exit("Temp directory already exists: " + tmpdir)
        else:
            mkdir_p(tmpdir)

        fp = download_translations(config,
                                   tmpdir)
        unzip(fp)
        rename_value_dirs(config, tmpdir)

        # Copy files to real directories
        copy_files(tmpdir, base_path)

        # Always clean up
        shutil.rmtree(tmpdir)
    else:
        sys.exit("Unknown action")
