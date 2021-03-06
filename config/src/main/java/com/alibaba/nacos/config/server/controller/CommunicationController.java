/*
 * Copyright 1999-2018 Alibaba Group Holding Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.nacos.config.server.controller;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.alibaba.nacos.config.server.constant.Constants;
import com.alibaba.nacos.config.server.model.SampleResult;
import com.alibaba.nacos.config.server.service.LongPullingService;
import com.alibaba.nacos.config.server.service.dump.DumpService;
import com.alibaba.nacos.config.server.service.notify.NotifyService;


/**
 * 用于其他节点通知的控制器
 * 
 * @author boyan
 * @date 2010-5-7
 */
@Controller
@RequestMapping(Constants.COMMUNICATION_CONTROLLER_PATH)
public class CommunicationController {

    @Autowired
    private DumpService dumpService;

    @Autowired
    protected LongPullingService longPullingService;
    
    private String trueStr = "true";
    
    /**
     * 通知配置信息改变
     *
     */
	@RequestMapping(value="/dataChange", method = RequestMethod.GET)
	@ResponseBody
	public Boolean notifyConfigInfo(HttpServletRequest request, HttpServletResponse response,
			@RequestParam("dataId") String dataId, @RequestParam("group") String group,
			@RequestParam(value = "tenant", required = false, defaultValue = StringUtils.EMPTY) String tenant,
			@RequestParam(value = "tag", required = false) String tag) {
		dataId = dataId.trim();
		group = group.trim();
		String lastModified = request.getHeader(NotifyService.NOTIFY_HEADER_LAST_MODIFIED);
		long lastModifiedTs = StringUtils.isEmpty(lastModified) ? -1 : Long.parseLong(lastModified);
		String handleIp = request.getHeader(NotifyService.NOTIFY_HEADER_OP_HANDLE_IP);
		String isBetaStr = request.getHeader("isBeta");
		if (StringUtils.isNotBlank(isBetaStr) && trueStr.equals(isBetaStr)) {
			dumpService.dump(dataId, group, tenant, lastModifiedTs, handleIp, true);
		} else {
			dumpService.dump(dataId, group, tenant, tag, lastModifiedTs, handleIp);
		}
		return true;
	}
    
    /**
	 * 在本台机器上获得订阅改配置的客户端信息
	 */
	@RequestMapping(value="/configWatchers", method = RequestMethod.GET)
	@ResponseBody
	public SampleResult getSubClientConfig(HttpServletRequest request,
			HttpServletResponse response,
			@RequestParam("dataId") String dataId,
			@RequestParam("group") String group,
			@RequestParam(value = "tenant", required = false) String tenant,
			 ModelMap modelMap)
			throws IOException, ServletException, Exception {
		group = StringUtils.isBlank(group) ? Constants.DEFAULT_GROUP : group;
		SampleResult sampleResult = longPullingService.getCollectSubscribleInfo(dataId, group, tenant);
		return sampleResult;
	}
	
	/**
	 * 在本台机器上获得客户端监听的配置列表
	 */
	@RequestMapping(value= "/watcherConfigs", method = RequestMethod.GET)
	@ResponseBody
	public SampleResult getSubClientConfigByIp(HttpServletRequest request,
			HttpServletResponse response,
			@RequestParam("ip") String ip,
			ModelMap modelMap)
					throws IOException, ServletException, Exception {
		SampleResult sampleResult = longPullingService.getCollectSubscribleInfoByIp(ip);
		return sampleResult;
	}
}
