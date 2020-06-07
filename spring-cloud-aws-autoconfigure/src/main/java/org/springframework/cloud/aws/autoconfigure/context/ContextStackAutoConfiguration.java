/*
 * Copyright 2013-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.aws.autoconfigure.context;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.ec2.AmazonEC2;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.aws.autoconfigure.context.properties.AwsStackProperties;
import org.springframework.cloud.aws.context.annotation.ConditionalOnMissingAmazonClient;
import org.springframework.cloud.aws.context.config.annotation.ContextDefaultConfigurationRegistrar;
import org.springframework.cloud.aws.core.config.AmazonWebserviceClientFactoryBean;
import org.springframework.cloud.aws.core.env.stack.StackResourceRegistry;
import org.springframework.cloud.aws.core.env.stack.config.AutoDetectingStackNameProvider;
import org.springframework.cloud.aws.core.env.stack.config.StackNameProvider;
import org.springframework.cloud.aws.core.env.stack.config.StackResourceRegistryFactoryBean;
import org.springframework.cloud.aws.core.env.stack.config.StaticStackNameProvider;
import org.springframework.cloud.aws.core.region.RegionProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * @author Agim Emruli
 * @author Maciej Walkowiak
 */
@Configuration(proxyBeanMethods = false)
@Import({ ContextCredentialsAutoConfiguration.class,
		ContextDefaultConfigurationRegistrar.class })
@ConditionalOnClass(name = "com.amazonaws.services.cloudformation.AmazonCloudFormation")
@EnableConfigurationProperties(AwsStackProperties.class)
public class ContextStackAutoConfiguration {

	@Autowired
	private AwsStackProperties properties;

	@Autowired(required = false)
	private AmazonEC2 amazonEC2;

	@Autowired(required = false)
	private RegionProvider regionProvider;

	@Autowired(required = false)
	private AWSCredentialsProvider credentialsProvider;

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty("cloud.aws.stack.name")
	public StackNameProvider staticStackNameProvider() {
		return new StaticStackNameProvider(properties.getName());
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty(name = "cloud.aws.stack.auto", havingValue = "true",
			matchIfMissing = true)
	public StackNameProvider autoDetectingStackNameProvider(
			AmazonCloudFormation amazonCloudFormation) {
		return new AutoDetectingStackNameProvider(amazonCloudFormation, this.amazonEC2);
	}

	@Bean
	@ConditionalOnMissingBean(StackResourceRegistry.class)
	@ConditionalOnBean(StackNameProvider.class)
	public StackResourceRegistryFactoryBean stackResourceRegistryFactoryBean(
			AmazonCloudFormation amazonCloudFormation,
			StackNameProvider stackNameProvider) {
		return new StackResourceRegistryFactoryBean(amazonCloudFormation,
				stackNameProvider);
	}

	@Bean
	@ConditionalOnMissingAmazonClient(AmazonCloudFormation.class)
	public AmazonWebserviceClientFactoryBean<AmazonCloudFormationClient> amazonCloudFormation() {
		return new AmazonWebserviceClientFactoryBean<>(AmazonCloudFormationClient.class,
				this.credentialsProvider, this.regionProvider);
	}

}
