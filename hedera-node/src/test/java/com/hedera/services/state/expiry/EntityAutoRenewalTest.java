package com.hedera.services.state.expiry;

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

import com.hedera.services.config.HederaNumbers;
import com.hedera.services.config.MockGlobalDynamicProps;
import com.hedera.services.config.MockHederaNumbers;
import com.hedera.services.state.expiry.renewal.RenewalProcess;
import com.hedera.services.state.logic.NetworkCtxManager;
import com.hedera.services.state.merkle.MerkleNetworkContext;
import com.hedera.services.state.submerkle.SequenceNumber;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class EntityAutoRenewalTest {
	private final Instant instantNow = Instant.ofEpochSecond(1_234_567L);
	private final HederaNumbers mockHederaNums = new MockHederaNumbers();
	private final MockGlobalDynamicProps properties = new MockGlobalDynamicProps();

	private final long aNum = 1002L, bNum = 1003L, cNum = 1004L;

	@Mock
	private SequenceNumber seqNo;
	@Mock
	private RenewalProcess renewalProcess;
	@Mock
	private NetworkCtxManager networkCtxManager;
	@Mock
	private MerkleNetworkContext networkCtx;

	private EntityAutoRenewal subject;

	@BeforeEach
	void setUp() {
		subject = new EntityAutoRenewal(
				mockHederaNums, renewalProcess, properties, networkCtxManager, () -> networkCtx, () -> seqNo);
	}

	@Test
	void abortsIfNotAutoRenewing() {
		// setup:
		properties.disableAutoRenew();

		// when:
		subject.execute(instantNow);

		// then:
		verifyNoInteractions(renewalProcess);

		// cleanup:
		properties.enableAutoRenew();
	}

	@Test
	void abortsIfNoNonSystemEntities() {
		// setup:
		givenWrapNum(mockHederaNums.numReservedSystemEntities() + 1);

		// when:
		subject.execute(instantNow);

		// then:
		verifyNoInteractions(renewalProcess);
	}

	@Test
	void resetsSummaryCountsIfNewConsensusSecond() {
		given(networkCtxManager.currentTxnIsFirstInConsensusSecond()).willReturn(true);
		givenWrapNum(aNum + 123);
		givenLastScanned(aNum - 1);

		// when:
		subject.execute(instantNow);

		// then:
		verify(networkCtx).clearAutoRenewSummaryCounts();
	}

	@Test
	void scansToExpectedNumWithNothingToTouch() {
		// setup:
		long numToScan = properties.autoRenewNumberOfEntitiesToScan();

		givenWrapNum(aNum + numToScan);
		givenLastScanned(aNum - 1);

		// when:
		subject.execute(instantNow);

		// then:
		verify(renewalProcess).beginRenewalCycle(instantNow);
		for (long i = aNum; i < aNum + numToScan; i++) {
			verify(renewalProcess).process(i);
		}
		// and:
		verify(renewalProcess).endRenewalCycle();
		verify(networkCtx).updateLastScannedEntity(aNum + numToScan - 1);
	}

	@Test
	void stopsEarlyWhenLotsToTouch() {
		// setup:
		long numToScan = properties.autoRenewNumberOfEntitiesToScan();

		givenWrapNum(aNum + numToScan);
		givenLastScanned(aNum - 1);
		given(renewalProcess.process(aNum)).willReturn(true);
		given(renewalProcess.process(bNum)).willReturn(true);

		// when:
		subject.execute(instantNow);

		// then:
		verify(renewalProcess).beginRenewalCycle(instantNow);
		for (long i = aNum; i < cNum; i++) {
			verify(renewalProcess).process(i);
		}
		// and:
		verify(renewalProcess, never()).process(cNum);
		verify(renewalProcess).endRenewalCycle();
		verify(networkCtx).updateLastScannedEntity(bNum);
	}

	@Test
	void understandsHowToWrap() {
		// setup:
		long numToScan = properties.autoRenewNumberOfEntitiesToScan();

		givenWrapNum(aNum + numToScan);
		givenLastScanned(aNum + numToScan - 2);
		given(renewalProcess.process(aNum + numToScan - 1)).willReturn(false);
		given(renewalProcess.process(aNum - 1)).willReturn(false);
		given(renewalProcess.process(aNum)).willReturn(true);
		given(renewalProcess.process(bNum)).willReturn(true);

		// when:
		subject.execute(instantNow);

		// then:
		verify(renewalProcess).beginRenewalCycle(instantNow);
		verify(renewalProcess).process(aNum + numToScan - 1);
		for (long i = aNum; i < cNum; i++) {
			verify(renewalProcess).process(i);
		}
		// and:
		verify(renewalProcess, never()).process(cNum);
		verify(renewalProcess).endRenewalCycle();
		verify(networkCtx).updateLastScannedEntity(bNum);
		// and:
		verify(networkCtx).updateAutoRenewSummaryCounts(4, 2);
	}


	private void givenWrapNum(long num) {
		given(seqNo.current()).willReturn(num);
	}

	private void givenLastScanned(long num) {
		given(networkCtx.lastScannedEntity()).willReturn(num);
	}
}
