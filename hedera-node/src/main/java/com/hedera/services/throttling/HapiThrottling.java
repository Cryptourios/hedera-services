package com.hedera.services.throttling;

/*-
 * ‌
 * Hedera Services Node
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

import com.hedera.services.sysfiles.domain.throttling.ThrottleDefinitions;
import com.hedera.services.throttles.DeterministicThrottle;
import com.hedera.services.throttles.GasLimitDeterministicThrottle;
import com.hedera.services.utils.TxnAccessor;
import com.hederahashgraph.api.proto.java.HederaFunctionality;
import com.hederahashgraph.api.proto.java.Query;

import java.time.Instant;
import java.util.List;

public class HapiThrottling implements FunctionalityThrottling {
	private final TimedFunctionalityThrottling delegate;
	private static final String STABLE_SOURCE_OF_THROTTLES_ERROR = "HAPI throttling should not be treated as a stable source of throttles";

	public HapiThrottling(TimedFunctionalityThrottling delegate) {
		this.delegate = delegate;
	}

	@Override
	public synchronized boolean shouldThrottleTxn(TxnAccessor accessor) {
		return delegate.shouldThrottleTxn(accessor, Instant.now());
	}

	@Override
	public synchronized boolean shouldThrottleQuery(HederaFunctionality queryFunction, Query query) {
		return delegate.shouldThrottleQuery(queryFunction, Instant.now(), query);
	}

	@Override
	public void leakUnusedGasPreviouslyReserved(long value) {
		delegate.leakUnusedGasPreviouslyReserved(value);
	}

	@Override
	public List<DeterministicThrottle> allActiveThrottles() {
		throw new UnsupportedOperationException(STABLE_SOURCE_OF_THROTTLES_ERROR);
	}

	@Override
	public GasLimitDeterministicThrottle gasLimitThrottle() {
		throw new UnsupportedOperationException(STABLE_SOURCE_OF_THROTTLES_ERROR);
	}

	@Override
	public List<DeterministicThrottle> activeThrottlesFor(HederaFunctionality function) {
		throw new UnsupportedOperationException(STABLE_SOURCE_OF_THROTTLES_ERROR);
	}

	@Override
	public void rebuildFor(ThrottleDefinitions defs) {
		delegate.rebuildFor(defs);
	}

	@Override
	public void applyGasConfig() {
		delegate.applyGasConfig();
	}
}
