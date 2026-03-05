import { FormEvent, useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import {
  approveException,
  createCollateral,
  createCovenantException,
  expireException,
  getCollaterals,
  getCovenantExceptions,
  getCovenants,
} from "../api/client";
import { useRuntimeConfig } from "../runtime/RuntimeConfigContext";
import type { CollateralAsset, Covenant, CovenantException } from "../types/api";

export function LoanCollateralExceptionsPage() {
  const { sampleUxEnabled } = useRuntimeConfig();
  const loanId = Number(useParams().loanId);
  const [collaterals, setCollaterals] = useState<CollateralAsset[]>([]);
  const [exceptions, setExceptions] = useState<CovenantException[]>([]);
  const [covenants, setCovenants] = useState<Covenant[]>([]);
  const [error, setError] = useState<string | null>(null);

  const [assetType, setAssetType] = useState(sampleUxEnabled ? "ACCOUNTS_RECEIVABLE" : "");
  const [nominalValue, setNominalValue] = useState(sampleUxEnabled ? "1000000" : "");
  const [haircutPct, setHaircutPct] = useState(sampleUxEnabled ? "0.2" : "");
  const [lienRank, setLienRank] = useState(sampleUxEnabled ? "1" : "");
  const [currency, setCurrency] = useState(sampleUxEnabled ? "USD" : "");
  const [exceptionReason, setExceptionReason] = useState(sampleUxEnabled ? "Temporary covenant waiver request" : "");
  const [covenantId, setCovenantId] = useState<number | null>(null);

  useEffect(() => {
    if (sampleUxEnabled) return;
    setAssetType("");
    setNominalValue("");
    setHaircutPct("");
    setLienRank("");
    setCurrency("");
    setExceptionReason("");
  }, [sampleUxEnabled]);

  const load = async () => {
    try {
      const [c, e, cv] = await Promise.all([getCollaterals(loanId), getCovenantExceptions(loanId), getCovenants(loanId)]);
      setCollaterals(c);
      setExceptions(e);
      setCovenants(cv);
      if (!covenantId && cv.length) setCovenantId(cv[0].id);
    } catch (err) {
      setError((err as Error).message);
    }
  };

  useEffect(() => {
    void load();
  }, [loanId]);

  const onAddCollateral = async (event: FormEvent) => {
    event.preventDefault();
    await createCollateral(loanId, {
      assetType,
      nominalValue: Number(nominalValue),
      haircutPct: Number(haircutPct),
      lienRank: Number(lienRank),
      currency,
      effectiveDate: new Date().toISOString().slice(0, 10),
    });
    await load();
  };

  const onRequestException = async (event: FormEvent) => {
    event.preventDefault();
    if (!covenantId) return;
    await createCovenantException(loanId, {
      covenantId,
      exceptionType: "WAIVER",
      reason: exceptionReason,
      effectiveFrom: new Date().toISOString().slice(0, 10),
      effectiveTo: new Date(Date.now() + 14 * 24 * 60 * 60 * 1000).toISOString().slice(0, 10),
    });
    await load();
  };

  return (
    <div className="space-y-4">
      {error && <p className="text-sm text-[var(--accent-danger)]">{error}</p>}
      <div className="grid gap-4 lg:grid-cols-2">
        <form className="card space-y-2" onSubmit={onAddCollateral}>
          <h3 className="text-sm font-semibold">Add Collateral</h3>
          <input className="input" value={assetType} onChange={(e) => setAssetType(e.target.value)} placeholder="Asset type" required />
          <input className="input" value={nominalValue} onChange={(e) => setNominalValue(e.target.value)} placeholder="Nominal value" required />
          <input className="input" value={haircutPct} onChange={(e) => setHaircutPct(e.target.value)} placeholder="Haircut pct (0-1)" required />
          <div className="grid grid-cols-2 gap-2">
            <input className="input" value={lienRank} onChange={(e) => setLienRank(e.target.value)} placeholder="Lien rank" required />
            <input className="input" value={currency} onChange={(e) => setCurrency(e.target.value)} placeholder="Currency" required />
          </div>
          <button className="btn-primary" type="submit">Add Collateral</button>
        </form>
        <form className="card space-y-2" onSubmit={onRequestException}>
          <h3 className="text-sm font-semibold">Request Covenant Exception</h3>
          <select className="input" value={covenantId ?? ""} onChange={(e) => setCovenantId(Number(e.target.value))}>
            {covenants.map((c) => (
              <option key={c.id} value={c.id}>{c.type}</option>
            ))}
          </select>
          <input className="input" value={exceptionReason} onChange={(e) => setExceptionReason(e.target.value)} placeholder="Reason" required />
          <button className="btn-primary" type="submit">Request Exception</button>
        </form>
      </div>

      <div className="grid gap-4 lg:grid-cols-2">
        <div className="card">
          <h3 className="mb-2 text-sm font-semibold">Collateral Assets</h3>
          {collaterals.map((asset) => (
            <div key={asset.id} className="mb-2 rounded border border-[var(--border-default)] p-2 text-xs">
              {asset.assetType} | nominal {asset.nominalValue} | eligible {asset.netEligibleValue}
            </div>
          ))}
        </div>
        <div className="card">
          <h3 className="mb-2 text-sm font-semibold">Exceptions</h3>
          {exceptions.map((exception) => (
            <div key={exception.id} className="mb-2 rounded border border-[var(--border-default)] p-2 text-xs">
              #{exception.id} | covenant {exception.covenantId} | {exception.status}
              <div className="mt-2 flex gap-2">
                {exception.status === "REQUESTED" && (
                  <button className="btn-secondary" type="button" onClick={() => approveException(exception.id).then(load)}>Approve</button>
                )}
                {exception.status !== "EXPIRED" && (
                  <button className="btn-secondary" type="button" onClick={() => expireException(exception.id).then(load)}>Expire</button>
                )}
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
