package com.jnj.honeur.webapi.service;

import com.jnj.honeur.webapi.DataSourceLookup;
import org.ohdsi.webapi.service.SourceService;
import org.ohdsi.webapi.source.Source;
import org.ohdsi.webapi.source.SourceInfo;
import org.ohdsi.webapi.source.SourceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

@Component("honeurSourceService")
@ConditionalOnProperty(value = "datasource.honeur.enabled", havingValue = "true")
public class HoneurSourceService extends SourceService {

    @Autowired
    private SourceRepository sourceRepository;

    @Autowired
    private DataSourceLookup dataSourceLookup;

    @Override
    public Collection<SourceInfo> getSources() {
        Collection<SourceInfo> cachedSources = super.getSources();
        Iterable<Source> sourceIterable = sourceRepository.findAll();
        initDataSource(sourceIterable);
        return cachedSources;
    }

    private void initDataSource(Iterable<Source> sourceIterable) {
        if (dataSourceLookup != null) {
            dataSourceLookup.initDataSources(sourceIterable);
        }
    }
}
