package com.hedera.services.context.properties;

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
import com.hedera.services.fees.calculation.CongestionMultipliers;
import com.hedera.services.sysfiles.domain.throttling.ThrottleReqOpsScaleFactor;
import com.hederahashgraph.api.proto.java.AccountID;
import com.hederahashgraph.api.proto.java.HederaFunctionality;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class GlobalDynamicPropertiesTest {
	private static final String[] balanceExportPaths = new String[] {
			"/opt/hgcapp/accountBalances",
			"data/saved/accountBalances"
	};

	private static final String[] upgradeArtifactLocs = new String[] {
			"/opt/hgcapp/HapiApp2.0/data/upgrade",
			"data/upgrade"
	};

	private PropertySource properties;

	private HederaNumbers numbers;
	private CongestionMultipliers oddCongestion = CongestionMultipliers.from("90,11x,95,27x,99,103x");
	private CongestionMultipliers evenCongestion = CongestionMultipliers.from("90,10x,95,25x,99,100x");
	private ThrottleReqOpsScaleFactor oddFactor = ThrottleReqOpsScaleFactor.from("5:2");
	private ThrottleReqOpsScaleFactor evenFactor = ThrottleReqOpsScaleFactor.from("7:2");
	private GlobalDynamicProperties subject;

	@BeforeEach
	void setup() {
		numbers = mock(HederaNumbers.class);
		given(numbers.shard()).willReturn(1L);
		given(numbers.realm()).willReturn(2L);
		properties = mock(PropertySource.class);
	}

	@Test
	void constructsFlagsAsExpected() {
		givenPropsWithSeed(1);

		// when:
		subject = new GlobalDynamicProperties(numbers, properties);

		// then:
		assertTrue(subject.shouldExportBalances());
		assertTrue(subject.shouldExportTokenBalances());
		assertFalse(subject.autoRenewEnabled());
		assertTrue(subject.areNftsEnabled());
	}

	@Test
	void nftPropertiesTest() {
		givenPropsWithSeed(1);
		subject = new GlobalDynamicProperties(numbers, properties);

		assertEquals(37, subject.maxNftTransfersLen());
		assertEquals(38, subject.maxBatchSizeBurn());
		assertEquals(39, subject.maxBatchSizeWipe());
		assertEquals(40, subject.maxBatchSizeMint());
		assertEquals(41, subject.maxNftQueryRange());
		assertEquals(42, subject.maxNftMetadataBytes());
		assertEquals(43, subject.maxTokenNameUtf8Bytes());
		assertEquals(oddFactor, subject.nftMintScaleFactor());
	}

	@Test
	void constructsIntsAsExpected() {
		givenPropsWithSeed(1);

		// when:
		subject = new GlobalDynamicProperties(numbers, properties);

		// then:
		assertEquals(1, subject.maxTokensPerAccount());
		assertEquals(2, subject.maxTokenSymbolUtf8Bytes());
		assertEquals(6, subject.maxFileSizeKb());
		assertEquals(8, subject.cacheRecordsTtl());
		assertEquals(9, subject.maxContractStorageKb());
		assertEquals(10, subject.ratesIntradayChangeLimitPercent());
		assertEquals(11, subject.balancesExportPeriodSecs());
		assertEquals(15, subject.maxTransferListSize());
		assertEquals(16, subject.maxTokenTransferListSize());
		assertEquals(17, subject.maxMemoUtf8Bytes());
		assertEquals(20, subject.minValidityBuffer());
		assertEquals(21, subject.maxGas());
		assertEquals(22, subject.getChainId());
		assertEquals(24, subject.feesTokenTransferUsageMultiplier());
		assertEquals(25, subject.maxAutoRenewDuration());
		assertEquals(26, subject.minAutoRenewDuration());
		assertEquals(27, subject.localCallEstRetBytes());
		assertEquals(28, subject.scheduledTxExpiryTimeSecs());
		assertEquals(29, subject.messageMaxBytesAllowed());
		assertEquals(30, subject.feesMinCongestionPeriod());
		assertEquals(32, subject.autoRenewNumberOfEntitiesToScan());
		assertEquals(33, subject.autoRenewMaxNumberOfEntitiesToRenewOrDelete());
		assertEquals(36, subject.maxCustomFeesAllowed());
		assertEquals(46, subject.maxXferBalanceChanges());
		assertEquals(47, subject.maxCustomFeeDepth());
	}

	@Test
	void constructsLongsAsExpected() {
		givenPropsWithSeed(1);

		// when:
		subject = new GlobalDynamicProperties(numbers, properties);

		// then:
		assertEquals(3L, subject.maxAccountNum());
		assertEquals(13L, subject.nodeBalanceWarningThreshold());
		assertEquals(18L, subject.maxTxnDuration());
		assertEquals(19L, subject.minTxnDuration());
		assertEquals(23L, subject.defaultContractLifetime());
		assertEquals(34L, subject.autoRenewGracePeriod());
		assertEquals(35L, subject.ratesMidnightCheckInterval());
		assertEquals(44L, subject.maxNftMints());
		assertEquals(48L, subject.triggerTxnWindBackNanos());
	}

	@Test
	void constructsMiscAsExpected() {
		givenPropsWithSeed(1);

		// when:
		subject = new GlobalDynamicProperties(numbers, properties);

		// expect:
		assertEquals(accountWith(1L, 2L, 7L), subject.fundingAccount());
		assertEquals(balanceExportPaths[1], subject.pathToBalancesExportDir());
		assertEquals(Set.of(HederaFunctionality.CryptoTransfer), subject.schedulingWhitelist());
		assertEquals(oddCongestion, subject.congestionMultipliers());
		assertEquals(upgradeArtifactLocs[1], subject.upgradeArtifactsLoc());
	}

	@Test
	void reloadsFlagsAsExpected() {
		givenPropsWithSeed(2);

		// when:
		subject = new GlobalDynamicProperties(numbers, properties);

		// then:
		assertFalse(subject.shouldExportBalances());
		assertFalse(subject.shouldExportTokenBalances());
		assertTrue(subject.autoRenewEnabled());
		assertFalse(subject.areNftsEnabled());
	}

	@Test
	void reloadsIntsAsExpected() {
		givenPropsWithSeed(2);

		// when:
		subject = new GlobalDynamicProperties(numbers, properties);

		// then:
		assertEquals(2, subject.maxTokensPerAccount());
		assertEquals(3, subject.maxTokenSymbolUtf8Bytes());
		assertEquals(7, subject.maxFileSizeKb());
		assertEquals(9, subject.cacheRecordsTtl());
		assertEquals(10, subject.maxContractStorageKb());
		assertEquals(11, subject.ratesIntradayChangeLimitPercent());
		assertEquals(12, subject.balancesExportPeriodSecs());
		assertEquals(16, subject.maxTransferListSize());
		assertEquals(17, subject.maxTokenTransferListSize());
		assertEquals(18, subject.maxMemoUtf8Bytes());
		assertEquals(21, subject.minValidityBuffer());
		assertEquals(22, subject.maxGas());
		assertEquals(25, subject.feesTokenTransferUsageMultiplier());
		assertEquals(26, subject.maxAutoRenewDuration());
		assertEquals(27, subject.minAutoRenewDuration());
		assertEquals(28, subject.localCallEstRetBytes());
		assertEquals(29, subject.scheduledTxExpiryTimeSecs());
		assertEquals(30, subject.messageMaxBytesAllowed());
		assertEquals(31, subject.feesMinCongestionPeriod());
		assertEquals(33, subject.autoRenewNumberOfEntitiesToScan());
		assertEquals(34, subject.autoRenewMaxNumberOfEntitiesToRenewOrDelete());
		assertEquals(37, subject.maxCustomFeesAllowed());
		assertEquals(47, subject.maxXferBalanceChanges());
		assertEquals(48, subject.maxCustomFeeDepth());
	}

	@Test
	void reloadsLongsAsExpected() {
		givenPropsWithSeed(2);

		// when:
		subject = new GlobalDynamicProperties(numbers, properties);

		// then:
		assertEquals(4L, subject.maxAccountNum());
		assertEquals(14L, subject.nodeBalanceWarningThreshold());
		assertEquals(19L, subject.maxTxnDuration());
		assertEquals(20L, subject.minTxnDuration());
		assertEquals(24L, subject.defaultContractLifetime());
		assertEquals(35L, subject.autoRenewGracePeriod());
		assertEquals(36L, subject.ratesMidnightCheckInterval());
		assertEquals(45L, subject.maxNftMints());
		assertEquals(49L, subject.triggerTxnWindBackNanos());
	}

	@Test
	void reloadsMiscAsExpected() {
		givenPropsWithSeed(2);

		// when:
		subject = new GlobalDynamicProperties(numbers, properties);

		// expect:
		assertEquals(accountWith(1L, 2L, 8L), subject.fundingAccount());
		assertEquals(balanceExportPaths[0], subject.pathToBalancesExportDir());
		assertEquals(Set.of(HederaFunctionality.CryptoCreate), subject.schedulingWhitelist());
		assertEquals(evenCongestion, subject.congestionMultipliers());
		assertEquals(evenFactor, subject.nftMintScaleFactor());
		assertEquals(upgradeArtifactLocs[0], subject.upgradeArtifactsLoc());
	}

	private void givenPropsWithSeed(int i) {
		given(properties.getIntProperty("tokens.maxPerAccount")).willReturn(i);
		given(properties.getIntProperty("tokens.maxSymbolUtf8Bytes")).willReturn(i + 1);
		given(properties.getBooleanProperty("ledger.keepRecordsInState")).willReturn((i % 2) == 0);
		given(properties.getLongProperty("ledger.maxAccountNum")).willReturn((long) i + 2);
		given(properties.getIntProperty("files.maxSizeKb")).willReturn(i + 5);
		given(properties.getLongProperty("ledger.fundingAccount")).willReturn((long) i + 6);
		given(properties.getIntProperty("cache.records.ttl")).willReturn(i + 7);
		given(properties.getIntProperty("contracts.maxStorageKb")).willReturn(i + 8);
		given(properties.getIntProperty("rates.intradayChangeLimitPercent")).willReturn(i + 9);
		given(properties.getIntProperty("balances.exportPeriodSecs")).willReturn(i + 10);
		given(properties.getBooleanProperty("balances.exportEnabled")).willReturn((i + 11) % 2 == 0);
		given(properties.getLongProperty("balances.nodeBalanceWarningThreshold")).willReturn(i + 12L);
		given(properties.getStringProperty("balances.exportDir.path")).willReturn(balanceExportPaths[i % 2]);
		given(properties.getBooleanProperty("balances.exportTokenBalances")).willReturn((i + 13) % 2 == 0);
		given(properties.getIntProperty("ledger.transfers.maxLen")).willReturn(i + 14);
		given(properties.getIntProperty("ledger.tokenTransfers.maxLen")).willReturn(i + 15);
		given(properties.getIntProperty("hedera.transaction.maxMemoUtf8Bytes")).willReturn(i + 16);
		given(properties.getLongProperty("hedera.transaction.maxValidDuration")).willReturn(i + 17L);
		given(properties.getLongProperty("hedera.transaction.minValidDuration")).willReturn(i + 18L);
		given(properties.getIntProperty("hedera.transaction.minValidityBufferSecs")).willReturn(i + 19);
		given(properties.getIntProperty("contracts.maxGas")).willReturn(i + 20);
		given(properties.getIntProperty("contracts.chainId")).willReturn(i + 21);
		given(properties.getLongProperty("contracts.defaultLifetime")).willReturn(i + 22L);
		given(properties.getIntProperty("fees.tokenTransferUsageMultiplier")).willReturn(i + 23);
		given(properties.getLongProperty("ledger.autoRenewPeriod.maxDuration")).willReturn(i + 24L);
		given(properties.getLongProperty("ledger.autoRenewPeriod.minDuration")).willReturn(i + 25L);
		given(properties.getIntProperty("contracts.localCall.estRetBytes")).willReturn(i + 26);
		given(properties.getIntProperty("ledger.schedule.txExpiryTimeSecs")).willReturn(i + 27);
		given(properties.getIntProperty("consensus.message.maxBytesAllowed")).willReturn(i + 28);
		given(properties.getFunctionsProperty("scheduling.whitelist")).willReturn(i % 2 == 0
				? Set.of(HederaFunctionality.CryptoCreate)
				: Set.of(HederaFunctionality.CryptoTransfer));
		given(properties.getCongestionMultiplierProperty("fees.percentCongestionMultipliers"))
				.willReturn(i % 2 == 0 ? evenCongestion : oddCongestion);
		given(properties.getIntProperty("fees.minCongestionPeriod")).willReturn(i + 29);
		given(properties.getBooleanProperty("autorenew.isEnabled")).willReturn(i % 2 == 0);
		given(properties.getIntProperty("autorenew.numberOfEntitiesToScan")).willReturn(i + 31);
		given(properties.getIntProperty("autorenew.maxNumberOfEntitiesToRenewOrDelete")).willReturn(i + 32);
		given(properties.getLongProperty("autorenew.gracePeriod")).willReturn(i + 33L);
		given(properties.getLongProperty("rates.midnightCheckInterval")).willReturn(i + 34L);
		given(properties.getIntProperty("tokens.maxCustomFeesAllowed")).willReturn(i + 35);
		given(properties.getIntProperty("ledger.nftTransfers.maxLen")).willReturn(i + 36);
		given(properties.getIntProperty("tokens.nfts.maxBatchSizeBurn")).willReturn(i + 37);
		given(properties.getIntProperty("tokens.nfts.maxBatchSizeWipe")).willReturn(i + 38);
		given(properties.getIntProperty("tokens.nfts.maxBatchSizeMint")).willReturn(i + 39);
		given(properties.getLongProperty("tokens.nfts.maxQueryRange")).willReturn(i + 40L);
		given(properties.getIntProperty("tokens.nfts.maxMetadataBytes")).willReturn(i + 41);
		given(properties.getIntProperty("tokens.maxTokenNameUtf8Bytes")).willReturn(i + 42);
		given(properties.getBooleanProperty("tokens.nfts.areEnabled")).willReturn((i + 43) % 2 == 0);
		given(properties.getLongProperty("tokens.nfts.maxAllowedMints")).willReturn(i + 43L);
		given(properties.getIntProperty("tokens.nfts.mintThrottleScaleFactor")).willReturn(i + 44);
		given(properties.getIntProperty("ledger.xferBalanceChanges.maxLen")).willReturn(i + 45);
		given(properties.getIntProperty("tokens.maxCustomFeeDepth")).willReturn(i + 46);
		given(properties.getThrottleScaleFactor("tokens.nfts.mintThrottleScaleFactor"))
				.willReturn(i % 2 == 0 ? evenFactor : oddFactor);
		given(properties.getStringProperty("upgrade.artifacts.path")).willReturn(upgradeArtifactLocs[i % 2]);
		given(properties.getLongProperty("scheduling.triggerTxn.windBackNanos")).willReturn(i + 47L);
	}

	private AccountID accountWith(long shard, long realm, long num) {
		return AccountID.newBuilder()
				.setShardNum(shard)
				.setRealmNum(realm)
				.setAccountNum(num)
				.build();
	}
}
