package com.hedera.services.txns.file;

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

import com.hedera.services.context.TransactionContext;
import com.hedera.services.context.primitives.StateView;
import com.hedera.services.files.HFileMeta;
import com.hedera.services.files.HederaFs;
import com.hedera.services.legacy.core.jproto.JKey;
import com.hedera.services.txns.TransitionLogic;
import com.hedera.services.txns.validation.OptionValidator;
import com.hederahashgraph.api.proto.java.Duration;
import com.hederahashgraph.api.proto.java.FileCreateTransactionBody;
import com.hederahashgraph.api.proto.java.ResponseCodeEnum;
import com.hederahashgraph.api.proto.java.TransactionBody;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.function.Function;
import java.util.function.Predicate;

import static com.hedera.services.txns.file.FileUpdateTransitionLogic.mapToStatus;
import static com.hedera.services.txns.file.FileUpdateTransitionLogic.wrapped;
import static com.hedera.services.utils.MiscUtils.asFcKeyUnchecked;
import static com.hederahashgraph.api.proto.java.ResponseCodeEnum.AUTORENEW_DURATION_NOT_IN_RANGE;
import static com.hederahashgraph.api.proto.java.ResponseCodeEnum.FAIL_INVALID;
import static com.hederahashgraph.api.proto.java.ResponseCodeEnum.INVALID_EXPIRATION_TIME;
import static com.hederahashgraph.api.proto.java.ResponseCodeEnum.INVALID_FILE_WACL;
import static com.hederahashgraph.api.proto.java.ResponseCodeEnum.OK;
import static com.hederahashgraph.api.proto.java.ResponseCodeEnum.SUCCESS;

@Singleton
public class FileCreateTransitionLogic implements TransitionLogic {
	private static final Logger log = LogManager.getLogger(FileCreateTransitionLogic.class);

	private final HederaFs hfs;
	private final OptionValidator validator;
	private final TransactionContext txnCtx;

	private final Function<TransactionBody, ResponseCodeEnum> SEMANTIC_CHECK = this::validate;

	@Inject
	public FileCreateTransitionLogic(
			HederaFs hfs,
			OptionValidator validator,
			TransactionContext txnCtx
	) {
		this.hfs = hfs;
		this.validator = validator;
		this.txnCtx = txnCtx;
	}

	@Override
	public void doStateTransition() {
		var op = txnCtx.accessor().getTxn().getFileCreate();

		try {
			var validity = assessedValidity(op);
			if (validity != OK) {
				txnCtx.setStatus(validity);
				return;
			}

			var attr = asAttr(op);
			var sponsor = txnCtx.activePayer();
			var created = hfs.create(op.getContents().toByteArray(), attr, sponsor);

			txnCtx.setCreated(created);
			txnCtx.setStatus(SUCCESS);
		} catch (IllegalArgumentException iae) {
			mapToStatus(iae, txnCtx);
		} catch (Exception unknown) {
			log.warn("Unrecognized failure handling {}!", txnCtx.accessor().getSignedTxnWrapper(), unknown);
			txnCtx.setStatus(FAIL_INVALID);
		}
	}

	@Override
	public Predicate<TransactionBody> applicability() {
		return TransactionBody::hasFileCreate;
	}

	@Override
	public Function<TransactionBody, ResponseCodeEnum> semanticCheck() {
		return SEMANTIC_CHECK;
	}

	private ResponseCodeEnum assessedValidity(FileCreateTransactionBody op) {
		if (op.hasKeys() && !validator.hasGoodEncoding(wrapped(op.getKeys()))) {
			return INVALID_FILE_WACL;
		}

		return OK;
	}

	private HFileMeta asAttr(FileCreateTransactionBody op) {
		JKey wacl = op.hasKeys() ? asFcKeyUnchecked(wrapped(op.getKeys())) : StateView.EMPTY_WACL;

		return new HFileMeta(false, wacl, op.getExpirationTime().getSeconds(), op.getMemo());
	}

	private ResponseCodeEnum validate(TransactionBody fileCreateTxn) {
		var op = fileCreateTxn.getFileCreate();

		var memoValidity = validator.memoCheck(op.getMemo());
		if (memoValidity != OK) {
			return memoValidity;
		}

		if (!op.hasExpirationTime()) {
			return INVALID_EXPIRATION_TIME;
		}

		var effectiveDuration = Duration.newBuilder()
				.setSeconds(
						op.getExpirationTime().getSeconds() -
								fileCreateTxn.getTransactionID().getTransactionValidStart().getSeconds())
				.build();
		if (!validator.isValidAutoRenewPeriod(effectiveDuration)) {
			return AUTORENEW_DURATION_NOT_IN_RANGE;
		}

		return OK;
	}
}
