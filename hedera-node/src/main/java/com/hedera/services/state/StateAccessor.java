package com.hedera.services.state;

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

import com.hedera.services.ServicesState;
import com.hedera.services.context.StateChildren;
import com.hedera.services.state.merkle.MerkleAccount;
import com.hedera.services.state.merkle.MerkleNetworkContext;
import com.hedera.services.state.merkle.MerkleOptionalBlob;
import com.hedera.services.state.merkle.MerkleSchedule;
import com.hedera.services.state.merkle.MerkleSpecialFiles;
import com.hedera.services.state.merkle.MerkleToken;
import com.hedera.services.state.merkle.MerkleTokenRelStatus;
import com.hedera.services.state.merkle.MerkleTopic;
import com.hedera.services.state.merkle.MerkleUniqueToken;
import com.hedera.services.stream.RecordsRunningHashLeaf;
import com.hedera.services.utils.EntityNum;
import com.hedera.services.utils.EntityNumPair;
import com.swirlds.common.AddressBook;
import com.swirlds.fchashmap.FCOneToManyRelation;
import com.swirlds.merkle.map.MerkleMap;

import java.time.Instant;

public class StateAccessor {
	private StateChildren children = new StateChildren();

	public StateAccessor() {
		/* No-op */
	}

	public StateAccessor(final ServicesState initialState) {
		updateChildrenFrom(initialState);
	}

	/**
	 * Updates this accessor's state children references from the given state (which in
	 * our usage will always be the latest working state).
	 *
	 * <b>NOTE:</b> This method is not thread-safe; that is, if another thread makes
	 * concurrent calls to getters on this accessor, that thread could get some references
	 * from the previous state and some references from the updated state.
	 *
	 * @param state the new working state to update children from
	 */
	public void updateChildrenFrom(final ServicesState state) {
		mapStateOnto(state, children);
	}

	/**
	 * Replaces this accessor's state children references with new references from given state
	 * (which in our usage will always be the latest signed state).
	 *
	 * @param state the latest signed state to replace children from
	 */
	public void replaceChildrenFrom(final ServicesState state, final Instant signedAt) {
		final var newChildren = new StateChildren(signedAt);
		mapStateOnto(state, newChildren);
		children = newChildren;
	}

	public MerkleMap<EntityNum, MerkleAccount> accounts() {
		return children.getAccounts();
	}

	public MerkleMap<EntityNum, MerkleTopic> topics() {
		return children.getTopics();
	}

	public MerkleMap<String, MerkleOptionalBlob> storage() {
		return children.getStorage();
	}

	public MerkleMap<EntityNum, MerkleToken> tokens() {
		return children.getTokens();
	}

	public MerkleMap<EntityNumPair, MerkleTokenRelStatus> tokenAssociations() {
		return children.getTokenAssociations();
	}

	public MerkleMap<EntityNum, MerkleSchedule> schedules() {
		return children.getSchedules();
	}

	public MerkleMap<EntityNumPair, MerkleUniqueToken> uniqueTokens() {
		return children.getUniqueTokens();
	}

	public FCOneToManyRelation<EntityNum, Long> uniqueTokenAssociations() {
		return children.getUniqueTokenAssociations();
	}

	public FCOneToManyRelation<EntityNum, Long> uniqueOwnershipAssociations() {
		return children.getUniqueOwnershipAssociations();
	}

	public FCOneToManyRelation<EntityNum, Long> uniqueOwnershipTreasuryAssociations() {
		return children.getUniqueOwnershipTreasuryAssociations();
	}

	public MerkleSpecialFiles specialFiles() {
		return children.getSpecialFiles();
	}

	public MerkleNetworkContext networkCtx() {
		return children.getNetworkCtx();
	}

	public AddressBook addressBook() {
		return children.getAddressBook();
	}

	public RecordsRunningHashLeaf runningHashLeaf() {
		return children.getRunningHashLeaf();
	}

	public StateChildren children() {
		return children;
	}

	private static void mapStateOnto(final ServicesState state, final StateChildren children) {
		children.setAccounts(state.accounts());
		children.setTopics(state.topics());
		children.setStorage(state.storage());
		children.setTokens(state.tokens());
		children.setTokenAssociations(state.tokenAssociations());
		children.setSchedules(state.scheduleTxs());
		children.setNetworkCtx(state.networkCtx());
		children.setAddressBook(state.addressBook());
		children.setSpecialFiles(state.specialFiles());
		children.setUniqueTokens(state.uniqueTokens());
		children.setUniqueTokenAssociations(state.uniqueTokenAssociations());
		children.setUniqueOwnershipAssociations(state.uniqueOwnershipAssociations());
		children.setUniqueOwnershipTreasuryAssociations(state.uniqueTreasuryOwnershipAssociations());
		children.setRunningHashLeaf(state.runningHashLeaf());
	}
}
