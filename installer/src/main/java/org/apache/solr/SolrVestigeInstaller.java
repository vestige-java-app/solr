package org.apache.solr;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.IOUtils;

/**
 * @author gaellalire
 */
public class SolrVestigeInstaller {

    private File config;

    private File data;

    public SolrVestigeInstaller(final File config, final File data, final File cache) {
        this.config = config;
        this.data = data;
    }
    
    public void unzip(InputStream is, File dir) throws Exception {
        ZipInputStream zipFile = new ZipInputStream(is);
        ZipEntry entry = zipFile.getNextEntry();
        while (entry != null) {
            File entryDestination = new File(dir, entry.getName());
            if (entry.isDirectory()) {
                entryDestination.mkdirs();
            } else {
                entryDestination.getParentFile().mkdirs();
                OutputStream out = new FileOutputStream(entryDestination);
                IOUtils.copy(zipFile, out);
                IOUtils.closeQuietly(out);
            }
            zipFile.closeEntry();
            entry = zipFile.getNextEntry();
        }
    }

    public void install() throws Exception {
    	unzip(SolrVestigeInstaller.class.getResourceAsStream("/conf.zip"), config);
    	unzip(SolrVestigeInstaller.class.getResourceAsStream("/data.zip"), data);
    }

}
