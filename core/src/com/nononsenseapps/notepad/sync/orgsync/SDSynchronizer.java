package com.nononsenseapps.notepad.sync.orgsync;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;

import org.cowboyprogrammer.org.OrgFile;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.HashSet;

public class SDSynchronizer extends Synchronizer implements
		SynchronizerInterface {

	// Where files are kept. User changeable in preferences.
	public static final String DEFAULT_ORG_DIR = Environment
			.getExternalStorageDirectory().toString() + "/NoNonsenseNotes";
	public static final String PREF_ORG_DIR = "pref_org_dir";
	public static final String PREF_ORG_SD_ENABLED = "pref_org_sd_enabled";
    private final static String SERVICENAME = "SDORG";
    private final String ORG_DIR;
	private final boolean configured;

	public SDSynchronizer(Context context) {
		super(context);
		final SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(context);
		ORG_DIR = prefs.getString(PREF_ORG_DIR, DEFAULT_ORG_DIR);
		configured = prefs.getBoolean(PREF_ORG_SD_ENABLED, false);
	}
	
	@Override
	public String getAccountName() {
		return SERVICENAME;
	}

	@Override
	public String getServiceName() {
		return SERVICENAME;
	}

	@Override
	public boolean isConfigured() {
		// TODO testing
		if (true || this.configured) {
			File d = new File(ORG_DIR);
			if (!d.isDirectory()) {
				d.mkdir();
			}
			
			return this.configured && d.isDirectory();
		}
		
		return this.configured;
	}

    /**
     * Returns an OrgFile object with a filename set that is guaranteed to
     * not already exist. Use this method to avoid having multiple objects
     * pointing to the same file.
     *
     * @param desiredName The name you'd want. If it exists,
     *                    it will be used as the base in desiredName1,
     *                    desiredName2, etc. Limited to 99.
     * @return an OrgFile guaranteed not to exist.
     * @throws java.io.IOException
     */
    @Override
    public OrgFile getNewFile(final String desiredName) throws IOException,
            IllegalArgumentException {
        String filename;
        for (int i = 0; i < 100; i++) {
            if (i == 0) {
                filename = desiredName + ".org";
            } else {
                filename = desiredName + i + ".org";
            }
            File f = new File(ORG_DIR, filename);
            if (!f.exists()) {
                return new OrgFile(filename);
            }
        }
        throw new IllegalArgumentException("Filename not accessible");
    }

    @Override
    public void putRemoteFile(OrgFile orgFile) throws IOException {
		final File file = new File(ORG_DIR, orgFile.getFilename());
		final BufferedWriter bw = new BufferedWriter(new FileWriter(file));
		bw.write(orgFile.treeToString());
		bw.close();
	}

	@Override
	public void deleteRemoteFile(OrgFile orgFile) {
		final File file = new File(ORG_DIR, orgFile.getFilename());
		file.delete();
	}

	@Override
	public void renameRemoteFile(String oldName, OrgFile orgFile) {
		final File oldFile = new File(ORG_DIR, oldName);
		final File newFile = new File(ORG_DIR, orgFile.getFilename());
		oldFile.renameTo(newFile);
	}

	@Override
	public BufferedReader getRemoteFile(String filename) {
		final File file = new File(ORG_DIR, filename);
		BufferedReader br = null;
		if (file.exists()) {
			try {
				br = new BufferedReader(new FileReader(file));
			} catch (FileNotFoundException e) {
				br = null;
			}
		}

		return br;
	}

	@SuppressLint("DefaultLocale")
	@Override
	public HashSet<String> getRemoteFilenames() {
		final HashSet<String> filenames = new HashSet<String>();
		final File dir = new File(ORG_DIR);
		final File[] files = dir.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.toLowerCase().endsWith(".org");
			}
		});

		if (files != null) {
			for (File f : files) {
				filenames.add(f.getName());
			}
		}

		return filenames;
	}

	@Override
	public void postSynchronize() {
		// Nothing to do
	}
}
