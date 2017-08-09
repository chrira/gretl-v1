package ch.so.agi.gretl.util;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;

import static org.junit.Assert.assertFalse;

/**
 * Tests for FileExtension-Class
 */
public class FileExtensionTest {
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void getFileExtension() throws Exception {
        File sqlFile =  folder.newFile("query.sql");

        Assert.assertEquals("getFileExtension causes Exception","sql",FileExtension.getFileExtension(sqlFile) );

    }

    @Test
    public void missingFileExtensionThrowsGretlException() throws Exception {
        File sqlFile = folder.newFile("file");
        try {
            FileExtension.getFileExtension(sqlFile);
        } catch (GretlException e) {
            Assert.assertEquals("no file extension: File must have a file extension", e.getMessage());
        }

    }

    @Test
    public void multipleFileExtension() throws Exception {
        File sqlFile = folder.newFile("file.ext1.ext2");

        Assert.assertEquals("multipleFileExtension causes Exception","ext2",FileExtension.getFileExtension(sqlFile));
    }

    @Test
    public void strangeFileNameExtensionThrowsGretlExtension() throws Exception {
        File sqlFile = folder.newFile("c:\\file");
        try {
            FileExtension.getFileExtension(sqlFile);
        } catch (GretlException e) {
            Assert.assertEquals("no file extension: File must have a file extension", e.getMessage());
        }

    }

}