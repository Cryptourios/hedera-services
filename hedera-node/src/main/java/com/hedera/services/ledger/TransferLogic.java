package com.hedera.services.ledger;

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

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.hedera.services.context.SideEffectsTracker;
import com.hedera.services.context.properties.GlobalDynamicProperties;
import com.hedera.services.exceptions.InvalidTransactionException;
import com.hedera.services.ledger.properties.AccountProperty;
import com.hedera.services.ledger.properties.NftProperty;
import com.hedera.services.ledger.properties.TokenRelProperty;
import com.hedera.services.legacy.core.jproto.JKey;
import com.hedera.services.state.merkle.MerkleAccount;
import com.hedera.services.state.merkle.MerkleTokenRelStatus;
import com.hedera.services.state.merkle.MerkleUniqueToken;
import com.hedera.services.store.models.NftId;
import com.hedera.services.store.tokens.TokenStore;
import com.hedera.services.store.tokens.views.UniqTokenViewsManager;
import com.hedera.services.txns.validation.OptionValidator;
import com.hederahashgraph.api.proto.java.AccountID;
import com.hederahashgraph.api.proto.java.Key;
import com.hederahashgraph.api.proto.java.ResponseCodeEnum;
import com.hederahashgraph.api.proto.java.TokenID;
import org.apache.commons.lang3.tuple.Pair;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;

import static com.hedera.services.ServicesState.getAutoAccountsInstance;
import static com.hedera.services.ledger.properties.AccountProperty.ALREADY_USED_AUTOMATIC_ASSOCIATIONS;
import static com.hedera.services.ledger.properties.AccountProperty.BALANCE;
import static com.hedera.services.ledger.properties.AccountProperty.NUM_NFTS_OWNED;
import static com.hedera.services.ledger.properties.AccountProperty.TOKENS;
import static com.hederahashgraph.api.proto.java.ResponseCodeEnum.INVALID_KEY_ENCODING;
import static com.hederahashgraph.api.proto.java.ResponseCodeEnum.OK;

@Singleton
public class TransferLogic {
    private static final List<AccountProperty> TOKEN_TRANSFER_SIDE_EFFECTS =
            List.of(TOKENS, NUM_NFTS_OWNED, ALREADY_USED_AUTOMATIC_ASSOCIATIONS);
    private static final List<AccountProperty> CREATION_SIDE_EFFECTS = List.of();
    private final TransactionalLedger<AccountID, AccountProperty, MerkleAccount> accountsLedger;
    private final TransactionalLedger<NftId, NftProperty, MerkleUniqueToken> nftsLedger;
    private final TransactionalLedger<Pair<AccountID, TokenID>, TokenRelProperty, MerkleTokenRelStatus> tokenRelsLedger;
    private final SideEffectsTracker sideEffectsTracker;
    private final TokenStore tokenStore;
    private final MerkleAccountScopedCheck scopedCheck;
    private final UniqTokenViewsManager tokenViewsManager;
    private TransferCreationsFactory transferCreations;

    @Inject
    public TransferLogic(TransactionalLedger<AccountID, AccountProperty, MerkleAccount> accountsLedger,
                         TransactionalLedger<NftId, NftProperty, MerkleUniqueToken> nftsLedger,
                         TransactionalLedger<Pair<AccountID, TokenID>, TokenRelProperty, MerkleTokenRelStatus> tokenRelsLedger,
                         TokenStore tokenStore,
                         SideEffectsTracker sideEffectsTracker,
                         UniqTokenViewsManager tokenViewsManager,
                         GlobalDynamicProperties dynamicProperties,
                         OptionValidator validator,
                         TransferCreationsFactory transferCreations) {
        this.accountsLedger = accountsLedger;
        this.nftsLedger = nftsLedger;
        this.tokenRelsLedger = tokenRelsLedger;
        this.sideEffectsTracker = sideEffectsTracker;
        this.tokenStore = tokenStore;
        this.tokenViewsManager = tokenViewsManager;
        this.transferCreations = transferCreations;

        scopedCheck = new MerkleAccountScopedCheck(dynamicProperties, validator);
    }

    public void transfer(List<BalanceChange> changes) {
        var validity = OK;
        List<ByteString> autoCreateAliases = new ArrayList<>();
        for (var change : changes) {
			if (change.isForHbar()) {
				if (change.createsAccount()) {
					autoCreateAliases.add(change.alias());
				}
				validity = accountsLedger.validate(change.accountId(), scopedCheck.setBalanceChange(change));
			} else {
				validity = tokenStore.tryTokenChange(change);
			}
			if (validity != OK) {
				break;
			}
		}

        validity = autoCreateForAliasTransfers(autoCreateAliases);

        if (validity == OK) {
            adjustHbarUnchecked(changes);
        } else {
            dropPendingTokenChanges();
            throw new InvalidTransactionException(validity);
        }
    }

	private ResponseCodeEnum autoCreateForAliasTransfers(final List<ByteString> autoCreateAliases) {
        try {
			List<Key> aliasKeys = new ArrayList<>();
			for (ByteString alias : autoCreateAliases) {
				final var key = Key.parseFrom(alias);
				aliasKeys.add(key);
				//Need to check if it is primitive key
			}
            transferCreations.createAutoAccounts(aliasKeys);
        } catch (InvalidProtocolBufferException ex) {
            return INVALID_KEY_ENCODING;
        }
        return OK;
    }

    private void adjustHbarUnchecked(List<BalanceChange> changes) {
        for (var change : changes) {
            if (change.isForHbar()) {
                final var accountId = change.accountId();
                final var newBalance = change.getNewBalance();
                accountsLedger.set(accountId, BALANCE, newBalance);
                sideEffectsTracker.trackHbarChange(accountId, change.units());
            }
        }
    }

    public void dropPendingTokenChanges() {
        if (tokenRelsLedger.isInTransaction()) {
            tokenRelsLedger.rollback();
        }
        if (nftsLedger.isInTransaction()) {
            nftsLedger.rollback();
        }
        if (tokenViewsManager.isInTransaction()) {
            tokenViewsManager.rollback();
        }
        accountsLedger.undoChangesOfType(TOKEN_TRANSFER_SIDE_EFFECTS);
        accountsLedger.undoCreations();
        sideEffectsTracker.resetTrackedTokenChanges();
    }
}
