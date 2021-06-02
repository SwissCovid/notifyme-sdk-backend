/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.ubique.swisscovid.cn.sdk.backend.ws.config;

import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("cloud-test")
public class WSCloudTestConfig extends WSCloudBaseConfig {

	@Value("${vcap.services.ecdsa_test.credentials.privateKey}")
	private String privateKey;

	@Value("${vcap.services.ecdsa_test.credentials.publicKey}")
	public String publicKey;

	@Override
	String getSignaturePublicKey() {
		return new String(Base64.getDecoder().decode(publicKey));
	}

	@Override
	String getSignaturePrivateKey() {
		return new String(Base64.getDecoder().decode(privateKey));
	}
}
