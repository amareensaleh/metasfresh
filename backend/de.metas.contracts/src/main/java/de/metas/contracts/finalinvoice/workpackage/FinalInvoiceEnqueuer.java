/*
 * #%L
 * de.metas.contracts
 * %%
 * Copyright (C) 2024 metas GmbH
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program. If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */

package de.metas.contracts.finalinvoice.workpackage;

import com.google.common.collect.ImmutableSet;
import de.metas.async.processor.IWorkPackageQueueFactory;
import de.metas.contracts.FlatrateTermId;
import de.metas.contracts.model.I_C_Flatrate_Term;
import de.metas.invoicecandidate.process.params.InvoicingParams;
import de.metas.process.ProcessInfoParameter;
import de.metas.user.UserId;
import de.metas.util.Services;
import lombok.NonNull;
import org.adempiere.ad.trx.api.ITrxManager;
import org.adempiere.util.lang.impl.TableRecordReference;
import org.springframework.stereotype.Service;

import static de.metas.ordercandidate.api.impl.OLCandUpdater.PARAM_C_BPARTNER_LOCATION_MAP;
import static org.compiere.util.Env.getCtx;

@Service
public class FinalInvoiceEnqueuer
{
	private final IWorkPackageQueueFactory workPackageQueueFactory = Services.get(IWorkPackageQueueFactory.class);
	private final ITrxManager trxManager = Services.get(ITrxManager.class);

	public void enqueueNow(
			@NonNull final ImmutableSet<FlatrateTermId> termIds,
			@NonNull final UserId userId,
			@NonNull final InvoicingParams invoicingParams)
	{
		trxManager.runInThreadInheritedTrx(() -> termIds.forEach(id -> enqueueNow(id, userId, invoicingParams)));
	}

	private void enqueueNow(
			@NonNull final FlatrateTermId termId,
			@NonNull final UserId userId,
			@NonNull final InvoicingParams invoicingParams)
	{
		trxManager.runInThreadInheritedTrx(() -> enqueueInTrx(termId, userId, invoicingParams));
	}

	private void enqueueInTrx(
			@NonNull final FlatrateTermId termId,
			@NonNull final UserId userId,
			@NonNull final InvoicingParams invoicingParams)
	{
		final TableRecordReference tableRecordReference = TableRecordReference.of(I_C_Flatrate_Term.Table_Name, termId);
		
		workPackageQueueFactory.getQueueForEnqueuing(getCtx(), FinalInvoiceWorkPackageProcessor.class)
				.newWorkPackage()
				// ensures we are only enqueueing after this trx is committed
				.bindToThreadInheritedTrx()
				.parameters(invoicingParams.toMap())
				.addElement(tableRecordReference)
				.setUserInChargeId(userId)
				.buildAndEnqueue();
	}
}
