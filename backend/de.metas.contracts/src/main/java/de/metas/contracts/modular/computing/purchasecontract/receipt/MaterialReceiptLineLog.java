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

package de.metas.contracts.modular.computing.purchasecontract.receipt;

import de.metas.contracts.FlatrateTermId;
import de.metas.contracts.IFlatrateBL;
import de.metas.contracts.model.I_C_Flatrate_Term;
import de.metas.contracts.modular.ModularContractService;
import de.metas.contracts.modular.invgroup.interceptor.ModCntrInvoicingGroupRepository;
import de.metas.contracts.modular.workpackage.impl.AbstractMaterialReceiptLogHandler;
import de.metas.money.CurrencyId;
import de.metas.money.Money;
import de.metas.product.IProductBL;
import de.metas.product.ProductId;
import de.metas.product.ProductPrice;
import de.metas.uom.UomId;
import de.metas.util.Services;
import lombok.NonNull;
import org.adempiere.exceptions.AdempiereException;
import org.springframework.stereotype.Component;

@Component
class MaterialReceiptLineLog extends AbstractMaterialReceiptLogHandler
{
	private final IProductBL productBL = Services.get(IProductBL.class);
	private final IFlatrateBL flatrateBL = Services.get(IFlatrateBL.class);

	public MaterialReceiptLineLog(
			@NonNull final ModCntrInvoicingGroupRepository modCntrInvoicingGroupRepository,
			@NonNull final ReceiptComputingMethod computingMethod,
			@NonNull final ModularContractService modularContractService)
	{
		super(modCntrInvoicingGroupRepository, modularContractService, computingMethod);
	}

	@Override
	@NonNull
	protected ProductPrice getPriceActual(final @NonNull CreateLogRequest request)
	{
		final FlatrateTermId flatrateTermId = request.getContractId();
		final I_C_Flatrate_Term modularContract = flatrateBL.getById(flatrateTermId);
		final ProductId productId = request.getModularContractSettings().getRawProductId();
		final UomId stockUOMId = productBL.getStockUOMId(productId);

		final CurrencyId currencyId = CurrencyId.optionalOfRepoId(modularContract.getC_Currency_ID())
				.orElseThrow(() -> new AdempiereException("Currency must be set on the Modular Contract !")
						.appendParametersToMessage()
						.setParameter("ModularContractId", flatrateTermId.getRepoId()));
		return ProductPrice.builder()
				.productId(productId)
				.uomId(stockUOMId)
				.money(Money.zero(currencyId))
				.build();
	}
}
