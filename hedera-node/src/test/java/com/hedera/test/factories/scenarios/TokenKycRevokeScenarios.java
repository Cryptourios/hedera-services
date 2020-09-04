package com.hedera.test.factories.scenarios;

/*-
 * ‌
 * Hedera Services Node
 * ​
 * Copyright (C) 2018 - 2020 Hedera Hashgraph, LLC
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

import com.hedera.services.utils.PlatformTxnAccessor;

import static com.hedera.test.factories.scenarios.TokenRevokeKycFactory.newSignedTokenRevokeKyc;
import static com.hedera.test.factories.txns.PlatformTxnFactory.from;
import static com.hedera.test.utils.IdUtils.asIdRef;

public enum TokenKycRevokeScenarios implements TxnHandlingScenario {
	VALID_REVOKE_WITH_EXTANT_TOKEN {
		@Override
		public PlatformTxnAccessor platformTxn() throws Throwable {
			return new PlatformTxnAccessor(from(
					newSignedTokenRevokeKyc()
							.revoking(asIdRef(KNOWN_TOKEN_WITH_KYC), MISC_ACCOUNT)
							.nonPayerKts(TOKEN_KYC_KT)
							.get()
			));
		}
	},
	REVOKE_WITH_MISSING_TOKEN {
		@Override
		public PlatformTxnAccessor platformTxn() throws Throwable {
			return new PlatformTxnAccessor(from(
					newSignedTokenRevokeKyc()
							.revoking(asIdRef(UNKNOWN_TOKEN), MISC_ACCOUNT)
							.nonPayerKts(TOKEN_KYC_KT)
							.get()
			));
		}
	},
	REVOKE_FOR_TOKEN_WITHOUT_KYC {
		@Override
		public PlatformTxnAccessor platformTxn() throws Throwable {
			return new PlatformTxnAccessor(from(
					newSignedTokenRevokeKyc()
							.revoking(asIdRef(KNOWN_TOKEN_WITH_FREEZE), MISC_ACCOUNT)
							.nonPayerKts(TOKEN_KYC_KT)
							.get()
			));
		}
	},
}
