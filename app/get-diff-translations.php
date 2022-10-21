<?php
$resourcesroot = 'translate-temp/res';

// Look in the directories for new translations
if ($open = @opendir($resourcesroot)) {

        while (false !== ($file = readdir($open))) {
                if (substr($file, 0, 7) == 'values-') {
                
                        $langcode = substr($file, 7, 6);
                        $langpath = $resourcesroot . '/values-' . $langcode;
                        echo 'Language found: ' . $langcode."\n";
                        
                        // Look all strings.[timestamp].xml files in this resource directory
                        unset($found);
                        if ($openv = opendir($langpath)) {
                        
                                while (false !== ($xml = readdir($openv))) {
                                        if (is_file($langpath . '/' . $xml) && ($xml != 'strings.xml') && substr($xml, 0, 7) == 'strings') {
                                        
                                                $found[] = $langpath . '/' . $xml;
                                                
                                        }
                                }
                        
                        }
                        
                        // If new translations exist
                        if (isset($found)) {
                        
                                // Sort all xml files
                                rsort($found);
                        
                                // Copy the newest translation to be the new 'strings.xml'
                                if (copy($found[0], $langpath . '/strings.xml')) {
                                        echo 'New translation file copied.'."\n";
                                } else {
                                        echo 'Error copying the new translation ' . $found[0] . ' to ' . $langpath . '/strings.xml!'."\n";
                                }

                                // Remove the temporary translation files
                                foreach ($found as $rm) {
                                        exec('rm ' . $rm);
                                }
                                
                        } else {
                                echo 'No new translations found for ' . $langcode . '.'."\n";
                        }
                }
        }
        
} else {
        echo 'Cannot find the resources directory \'' . $resourcesroot . '\'. Did the FTP session fail?'."\n";
}

?>
