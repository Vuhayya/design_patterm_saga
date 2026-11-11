package com.sagademo.saga.service;

import com.sagademo.saga.dto.ServiceHealthDto;
import java.util.List;

/**
 * Feeds the dashboard "Services" panel by really probing each microservice:
 *
 * <p>Contract only. Controllers and other services depend on this interface, never on the
 * implementation class.
 *
 * @see com.sagademo.saga.service.impl.SystemHealthServiceImpl
 */
public interface SystemHealthService {

    List<ServiceHealthDto> checkAll();
}
