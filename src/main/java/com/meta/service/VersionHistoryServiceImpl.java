package com.meta.service;

import com.meta.mapper.VersionHistoryMapper;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Log4j2
@Service
public class VersionHistoryServiceImpl {

    @Autowired
    private VersionHistoryMapper versionHistoryMapper;



}