package org.jfrog.wharf.ivy.resolver;

import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.resolve.ResolveData;
import org.apache.ivy.core.resolve.ResolvedModuleRevision;
import org.apache.ivy.plugins.repository.ArtifactResourceResolver;
import org.apache.ivy.plugins.repository.Resource;
import org.apache.ivy.plugins.repository.ResourceDownloader;
import org.apache.ivy.plugins.resolver.FileSystemResolver;
import org.apache.ivy.plugins.resolver.util.ResolvedResource;
import org.apache.ivy.util.Message;
import org.jfrog.wharf.ivy.cache.WharfCacheManager;
import org.jfrog.wharf.ivy.model.ModuleRevisionMetadata;
import org.jfrog.wharf.ivy.repository.WharfURLRepository;
import org.jfrog.wharf.ivy.util.WharfUtils;

import java.io.File;
import java.io.IOException;
import java.util.Date;

/**
 * @author Tomer Cohen
 */
public class FileSystemWharfResolver extends FileSystemResolver implements WharfResolver {

    private final WharfResourceDownloader downloader = new WharfResourceDownloader(this);
    private final ArtifactResourceResolver artifactResourceResolver = new ArtifactResourceResolver() {
        @Override
        public ResolvedResource resolve(Artifact artifact) {
            artifact = fromSystem(artifact);
            return getArtifactRef(artifact, null);
        }
    };

    public FileSystemWharfResolver() {
        //TODO: [by tc] support md5
        super.setChecksums(WharfUtils.SHA1_ALGORITHM);
        WharfUtils.hackIvyBasicResolver(this);
        setRepository(new WharfURLRepository());
    }

    @Override
    public void setChecksums(String checksums) {
        throw new UnsupportedOperationException("Wharf resolvers enforce the usage of SHA1 checksums only!");
    }

    @Override
    public ResourceDownloader getDownloader() {
        return downloader;
    }

    @Override
    public ArtifactResourceResolver getArtifactResourceResolver() {
        return artifactResourceResolver;
    }

    @Override
    protected ResolvedModuleRevision findModuleInCache(DependencyDescriptor dd, ResolveData data) {
        ResolvedModuleRevision moduleRevision = super.findModuleInCache(dd, data);
        if (moduleRevision == null) {
            return null;
        }
        ModuleRevisionMetadata metadata = getCacheProperties(dd, moduleRevision);
        if (metadata == null) {
            Message.debug("Dependency descriptor " + dd.getDependencyRevisionId() + " has no descriptor");
            metadata = new ModuleRevisionMetadata();
        }
        updateCachePropertiesToCurrentTime(metadata);
        WharfCacheManager cacheManager = (WharfCacheManager) getRepositoryCacheManager();
        cacheManager.getMetadataHandler().saveModuleRevisionMetadata(moduleRevision.getId(), metadata);
        return moduleRevision;
    }

    private ModuleRevisionMetadata getCacheProperties(DependencyDescriptor dd, ResolvedModuleRevision moduleRevision) {
        WharfCacheManager cacheManager = (WharfCacheManager) getRepositoryCacheManager();
        return cacheManager.getMetadataHandler().getModuleRevisionMetadata(moduleRevision.getId());
    }

    private void updateCachePropertiesToCurrentTime(ModuleRevisionMetadata cacheProperties) {
        cacheProperties.latestResolvedTime = String.valueOf(System.currentTimeMillis());
    }

    @Override
    protected ResolvedResource getArtifactRef(Artifact artifact, Date date) {
        ResolvedResource artifactRef = super.getArtifactRef(artifact, date);
        return WharfUtils.convertToWharfResource(artifactRef);
    }

    @Override
    public long getAndCheck(Resource resource, File dest) throws IOException {
        return WharfUtils.getAndCheck(this, resource, dest);
    }


    @Override
    public long get(Resource resource, File dest) throws IOException {
        return super.get(resource, dest);
    }
}