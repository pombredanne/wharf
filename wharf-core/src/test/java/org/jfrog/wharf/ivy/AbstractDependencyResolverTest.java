/*
 *
 *  Copyright (C) 2010 JFrog Ltd.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 * /
 */
package org.jfrog.wharf.ivy;

import org.apache.ivy.core.event.EventManager;
import org.apache.ivy.core.resolve.DownloadOptions;
import org.apache.ivy.core.resolve.ResolveData;
import org.apache.ivy.core.resolve.ResolveEngine;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.core.sort.SortEngine;
import org.apache.ivy.util.FileUtil;
import org.apache.tools.ant.util.FileUtils;
import org.jfrog.wharf.ivy.cache.WharfCacheManager;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashSet;

import static org.junit.Assert.assertTrue;

public class AbstractDependencyResolverTest {

    public static final String SRC_TEST_REPOSITORIES = "src/test/repositories";
    protected static final String FS = System.getProperty("file.separator");
    protected static final String REL_IVY_PATTERN = "1" + FS
            + "[organisation]" + FS + "[module]" + FS + "ivys" + FS + "ivy-[revision].xml";

    protected IvySettingsTestHolder defaultSettings;

    protected class IvySettingsTestHolder {
        protected IvySettings settings;
        protected ResolveEngine engine;
        protected ResolveData data;
        protected WharfCacheManager cacheManager;

        protected void init(File baseDir) {
            settings = new IvySettings();
            settings.setBaseDir(baseDir);
            settings.setDefaultCache(cacheFolder);
            cacheManager = WharfCacheManager.newInstance(settings);
            settings.setDefaultRepositoryCacheManager(cacheManager);
            engine = new ResolveEngine(settings, new EventManager(), new SortEngine(settings));
            data = new ResolveData(engine, new ResolveOptions());
        }

    }

    protected File cacheFolder;
    protected File repoTestRoot;

    @Before
    public void setUp() throws Exception {
        // Find the baseDir based on where test repositories are located
        File baseDir = new File(".").getCanonicalFile();
        repoTestRoot = new File(baseDir, SRC_TEST_REPOSITORIES);
        if (!repoTestRoot.exists()) {
            baseDir = new File(baseDir, "wharf-core");
            repoTestRoot = new File(baseDir, SRC_TEST_REPOSITORIES);
        }
        assertTrue(repoTestRoot.exists());

        // Create empty test cache folder
        cacheFolder = new File(baseDir, "build/test/cache");
        deleteCacheFolder(cacheFolder);
        cacheFolder.mkdirs();

        // Configure the ivy settings
        defaultSettings = new IvySettingsTestHolder();
        defaultSettings.init(baseDir);
        setupLastModified();
    }

    protected IvySettingsTestHolder createNewSettings() {
        IvySettingsTestHolder holder = new IvySettingsTestHolder();
        holder.init(defaultSettings.settings.getBaseDir());
        return holder;
    }

    public String getIvyPattern() {
        return repoTestRoot + FS + REL_IVY_PATTERN;
    }

    public static void deleteCacheFolder(File toDelete) {
        if (toDelete.exists()) {
            // First delete the symlinks then the filestore
            File[] files = toDelete.listFiles();
            for (File file : files) {
                if (!"filestore".equals(file.getName())) {
                    assertTrue("Could not delete " + file.getAbsolutePath(), deleteFile(file));
                }
            }
            assertTrue("Could not delete " + toDelete.getAbsolutePath(), deleteFile(toDelete));
        }
    }

    private static boolean deleteFolder(File del) {
        File[] files = del.listFiles();
        if (files == null || files.length == 0) {
            return true;
        }
        for (File file : files) {
            if (!deleteFile(file)) {
                return false;
            }
        }
        return true;
    }

    private static boolean deleteFile(File del) {
        boolean result = true;
        if (del.isDirectory()) {
            result = deleteFolder(del);
        } else {
            // It may be a symlink delete and recheck
            FileUtils.getFileUtils().tryHardToDelete(del);
            result = !del.exists();
        }
        if (!result) {
            Assert.fail("Could not delete " + del.getAbsolutePath());
        }
        return result;
    }

    private void setupLastModified() {
        // change important last modified dates cause svn doesn't keep them
        long minute = 60 * 1000;
        long time = new GregorianCalendar().getTimeInMillis() - (4 * minute);
        new File(repoTestRoot, "1/org1/mod1.1/ivys/ivy-1.0.xml").setLastModified(time);
        time += minute;
        new File(repoTestRoot, "1/org1/mod1.1/ivys/ivy-1.0.1.xml").setLastModified(time);
        time += minute;
        new File(repoTestRoot, "1/org1/mod1.1/ivys/ivy-1.1.xml").setLastModified(time);
        time += minute;
        new File(repoTestRoot, "1/org1/mod1.1/ivys/ivy-2.0.xml").setLastModified(time);
    }

    @After()
    public void tearDown() throws Exception {
        deleteCacheFolder(cacheFolder);
    }

    protected DownloadOptions getDownloadOptions() {
        return new DownloadOptions();
    }

    protected Collection<File> getFilesInFileStore() {
        Collection<File> filestore = FileUtil.listAll(new File(cacheFolder, "filestore"), Collections.EMPTY_SET);
        Collection<File> filesInFileStore = new HashSet<File>();
        for (File file : filestore) {
            if (file.isFile()) {
                filesInFileStore.add(file);
            }
        }
        return filesInFileStore;
    }
}
