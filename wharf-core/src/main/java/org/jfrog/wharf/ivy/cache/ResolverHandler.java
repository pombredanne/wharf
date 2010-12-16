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

package org.jfrog.wharf.ivy.cache;

import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.plugins.IvySettingsAware;
import org.apache.ivy.plugins.resolver.DependencyResolver;
import org.apache.ivy.util.Message;
import org.jfrog.wharf.ivy.marshall.api.MarshallerFactory;
import org.jfrog.wharf.ivy.marshall.api.WharfResolverMarshaller;
import org.jfrog.wharf.ivy.model.WharfResolverMetadata;

import java.io.File;
import java.util.*;

/**
 * @author Tomer Cohen
 */
public class ResolverHandler implements IvySettingsAware {

    /**
     * This is the directory where you have the right to put all of the needed files for the handler. Default is
     * cacheDir/.wharf
     */
    private final File baseDir;

    private final Map<String, WharfResolverMetadata> resolvers = new HashMap<String, WharfResolverMetadata>();
    private final Map<Integer, WharfResolverMetadata> resolverFromDependencyResolverHash =
            new HashMap<Integer, WharfResolverMetadata>();
    private WharfResolverMarshaller wharfResolverMarshaller = MarshallerFactory.createWharfResolverMarshaller();
    private IvySettings settings;

    public ResolverHandler(File baseDir) {
        this.baseDir = baseDir;
        // populate the set of resolvers from the baseDir/resolvers.json file
        Set<WharfResolverMetadata> resolverMetadataIds = wharfResolverMarshaller.getWharfMetadatas(baseDir);
        for (WharfResolverMetadata wharfResolverMetadata : resolverMetadataIds) {
            resolvers.put(wharfResolverMetadata.getId(), wharfResolverMetadata);
        }
    }

    /**
     * @return Get a resolver ID according
     */
    public WharfResolverMetadata getResolver(DependencyResolver resolver) {
        // find in shortcut
        if (resolver == null) {
            throw new IllegalArgumentException("Cannot find null resolver");
        }
        int hash = resolver.hashCode();
        if (resolverFromDependencyResolverHash.containsKey(hash)) {
            return resolverFromDependencyResolverHash.get(hash);
        }
        // Need to find if in my cache then save to json file and shortcut
        WharfResolverMetadata wharfResolverMetadata = new WharfResolverMetadata(resolver);
        resolverFromDependencyResolverHash.put(hash, wharfResolverMetadata);
        resolvers.put(wharfResolverMetadata.getId(), wharfResolverMetadata);
        saveCacheResolverFile();
        return wharfResolverMetadata;
    }

    public void cleanResolvers() {
        resolvers.clear();
        saveCacheResolverFile();
    }

    /**
     * @return set of all resolvers Ids ever used in this cache
     */
    public Collection<WharfResolverMetadata> getAllResolvers() {
        return resolvers.values();
    }

    public WharfResolverMetadata getResolver(String resolverId) {
        return resolvers.get(resolverId);
    }

    public void saveCacheResolverFile() {
        wharfResolverMarshaller.save(baseDir, new HashSet<WharfResolverMetadata>(getAllResolvers()));
    }

    @Override
    public void setSettings(IvySettings settings) {
        this.settings = settings;
    }

    public boolean isActiveResolver(String resolverId) {
        WharfResolverMetadata resolverMetadata = getResolver(resolverId);
        if (resolverMetadata == null) {
            Message.error("No resolver for " + resolverId + " This cannot happen, please check cache corruption");
            return false;
        }
        if (settings.getResolverNames().contains(resolverMetadata.name)) {
            String currentResolverId = new WharfResolverMetadata(settings.getResolver(resolverMetadata.name)).getId();
            return resolverId.equals(currentResolverId);
        }
        return false;
    }

    public boolean contains(String resolverId) {
        return resolvers.containsKey(resolverId);
    }
}
