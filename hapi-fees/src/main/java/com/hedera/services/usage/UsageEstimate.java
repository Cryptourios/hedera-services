package com.hedera.services.usage;

/*-
 * ‌
 * Hedera Services API Fees
 * ​
 * Copyright (C) 2018 - 2021 Hedera Hashgraph, LLC
 * ​
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
 * ‍
 */

import com.hederahashgraph.api.proto.java.FeeComponents;

import static com.hedera.services.usage.SingletonEstimatorUtils.ESTIMATOR_UTILS;
import static com.hederahashgraph.fee.FeeBuilder.HRS_DIVISOR;

public class UsageEstimate {
	static EstimatorUtils estimatorUtils = ESTIMATOR_UTILS;

	private long rbs;
	private long sbs;

	private final FeeComponents.Builder base;

	public UsageEstimate(FeeComponents.Builder base) {
		this.base = base;
	}

	public void addRbs(long amount) {
		rbs += amount;
	}

	public void addSbs(long amount) {
		sbs += amount;
	}

	public FeeComponents.Builder base() {
		return base;
	}

	public FeeComponents build() {
		return base
				.setSbh(estimatorUtils.nonDegenerateDiv(sbs, HRS_DIVISOR))
				.setRbh(estimatorUtils.nonDegenerateDiv(rbs, HRS_DIVISOR))
				.build();
	}

	public long getRbs() {
		return rbs;
	}
}
