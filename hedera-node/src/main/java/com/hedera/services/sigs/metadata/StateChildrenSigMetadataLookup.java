package com.hedera.services.sigs.metadata;

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

import com.hedera.services.config.FileNumbers;
import com.hedera.services.context.StateChildren;
import com.hedera.services.files.HFileMeta;
import com.hedera.services.files.MetadataMapFactory;
import com.hedera.services.files.store.FcBlobsBytesStore;
import com.hedera.services.ledger.accounts.AliasManager;
import com.hedera.services.legacy.core.jproto.JContractIDKey;
import com.hedera.services.legacy.core.jproto.JKey;
import com.hedera.services.state.merkle.MerkleOptionalBlob;
import com.hedera.services.state.merkle.MerkleToken;
import com.hedera.services.utils.EntityNum;
import com.hederahashgraph.api.proto.java.AccountID;
import com.hederahashgraph.api.proto.java.ContractID;
import com.hederahashgraph.api.proto.java.FileID;
import com.hederahashgraph.api.proto.java.ScheduleID;
import com.hederahashgraph.api.proto.java.TokenID;
import com.hederahashgraph.api.proto.java.TopicID;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static com.hedera.services.context.primitives.StateView.EMPTY_WACL;
import static com.hedera.services.sigs.order.KeyOrderingFailure.IMMUTABLE_CONTRACT;
import static com.hedera.services.sigs.order.KeyOrderingFailure.INVALID_CONTRACT;
import static com.hedera.services.sigs.order.KeyOrderingFailure.INVALID_TOPIC;
import static com.hedera.services.sigs.order.KeyOrderingFailure.MISSING_ACCOUNT;
import static com.hedera.services.sigs.order.KeyOrderingFailure.MISSING_FILE;
import static com.hedera.services.sigs.order.KeyOrderingFailure.MISSING_SCHEDULE;
import static com.hedera.services.sigs.order.KeyOrderingFailure.MISSING_TOKEN;
import static com.hedera.services.utils.EntityIdUtils.isAlias;
import static com.hedera.services.utils.EntityNum.MISSING_NUM;
import static com.hedera.services.utils.EntityNum.fromAccountId;
import static com.hedera.services.utils.EntityNum.fromContractId;
import static com.hedera.services.utils.EntityNum.fromTokenId;
import static com.hedera.services.utils.EntityNum.fromTopicId;

public class StateChildrenSigMetadataLookup implements SigMetadataLookup {
	private final FileNumbers fileNumbers;
	private final AliasManager aliasManager;
	private final StateChildren stateChildren;
	private final Map<FileID, HFileMeta> metaMap;
	private final Function<MerkleToken, TokenSigningMetadata> tokenMetaTransform;

	public StateChildrenSigMetadataLookup(
			final FileNumbers fileNumbers,
			final AliasManager aliasManager,
			final StateChildren stateChildren,
			final Function<MerkleToken, TokenSigningMetadata> tokenMetaTransform
	) {
		this.fileNumbers = fileNumbers;
		this.aliasManager = aliasManager;
		this.stateChildren = stateChildren;
		this.tokenMetaTransform = tokenMetaTransform;

		final var blobStore = new FcBlobsBytesStore(MerkleOptionalBlob::new, stateChildren::getStorage);
		this.metaMap = MetadataMapFactory.metaMapFrom(blobStore);
	}

	@Override
	public SafeLookupResult<FileSigningMetadata> fileSigningMetaFor(final FileID id) {
		if (fileNumbers.isSoftwareUpdateFile(id.getFileNum())) {
			return SPECIAL_FILE_RESULT;
		}
		final var meta = metaMap.get(id);
		return (meta == null)
				? SafeLookupResult.failure(MISSING_FILE)
				: new SafeLookupResult<>(new FileSigningMetadata(meta.getWacl()));
	}

	@Override
	public SafeLookupResult<TopicSigningMetadata> topicSigningMetaFor(final TopicID id) {
		final var topic = stateChildren.getTopics().get(fromTopicId(id));
		if (topic == null || topic.isDeleted()) {
			return SafeLookupResult.failure(INVALID_TOPIC);
		} else {
			final var effAdminKey = topic.hasAdminKey() ? topic.getAdminKey() : null;
			final var effSubmitKey = topic.hasSubmitKey() ? topic.getSubmitKey() : null;
			return new SafeLookupResult<>(new TopicSigningMetadata(effAdminKey, effSubmitKey));
		}
	}

	@Override
	public SafeLookupResult<TokenSigningMetadata> tokenSigningMetaFor(final TokenID id) {
		final var token = stateChildren.getTokens().get(fromTokenId(id));
		return (token == null)
				? SafeLookupResult.failure(MISSING_TOKEN)
				: new SafeLookupResult<>(tokenMetaTransform.apply(token));
	}

	@Override
	public SafeLookupResult<AccountSigningMetadata> accountSigningMetaFor(final AccountID id) {
		return lookupByNumber(fromAccountId(id));
	}

	@Override
	public SafeLookupResult<AccountSigningMetadata> aliasableAccountSigningMetaFor(final AccountID idOrAlias) {
		if (isAlias(idOrAlias)) {
			final var explicitId = aliasManager.lookupIdBy(idOrAlias.getAlias());
			return (explicitId == MISSING_NUM)
					? SafeLookupResult.failure(MISSING_ACCOUNT)
					: lookupByNumber(explicitId);
		} else {
			return lookupByNumber(fromAccountId(idOrAlias));
		}
	}

	@Override
	public SafeLookupResult<ScheduleSigningMetadata> scheduleSigningMetaFor(final ScheduleID id) {
		final var schedule = stateChildren.getSchedules().get(EntityNum.fromScheduleId(id));
		if (schedule == null) {
			return SafeLookupResult.failure(MISSING_SCHEDULE);
		} else {
			final var scheduleMeta = new ScheduleSigningMetadata(
					schedule.adminKey(),
					schedule.ordinaryViewOfScheduledTxn(),
					schedule.hasExplicitPayer() ? Optional.of(schedule.payer().toGrpcAccountId()) : Optional.empty());
			return new SafeLookupResult<>(scheduleMeta);
		}
	}

	@Override
	public SafeLookupResult<ContractSigningMetadata> contractSigningMetaFor(final ContractID id) {
		final var contract = stateChildren.getAccounts().get(fromContractId(id));
		if (contract == null || contract.isDeleted() || !contract.isSmartContract()) {
			return SafeLookupResult.failure(INVALID_CONTRACT);
		} else {
			JKey key;
			if ((key = contract.getAccountKey()) == null || key instanceof JContractIDKey) {
				return SafeLookupResult.failure(IMMUTABLE_CONTRACT);
			} else {
				return new SafeLookupResult<>(new ContractSigningMetadata(key, contract.isReceiverSigRequired()));
			}
		}
	}

	private SafeLookupResult<AccountSigningMetadata> lookupByNumber(final EntityNum id) {
		final var account = stateChildren.getAccounts().get(id);
		if (account == null) {
			return SafeLookupResult.failure(MISSING_ACCOUNT);
		} else {
			return new SafeLookupResult<>(
					new AccountSigningMetadata(
							account.getAccountKey(), account.isReceiverSigRequired()));
		}
	}

	private static final FileSigningMetadata SPECIAL_FILE_META =
			new FileSigningMetadata(EMPTY_WACL);
	private static final SafeLookupResult<FileSigningMetadata> SPECIAL_FILE_RESULT =
			new SafeLookupResult<>(SPECIAL_FILE_META);
}
