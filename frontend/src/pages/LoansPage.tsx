import { FormEvent, useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { closeLoan, createLoan, getLoans } from "../api/client";
import type { Loan } from "../types/api";
import { PageSection, Surface } from "../components/layout";
import { Badge } from "../components/ui/badge";
import { Button } from "../components/ui/button";
import { Input } from "../components/ui/input";

export function LoansPage() {
  const [loans, setLoans] = useState<Loan[]>([]);
  const [search, setSearch] = useState("");
  const [status, setStatus] = useState<"ALL" | "ACTIVE" | "CLOSED">("ALL");
  const [form, setForm] = useState({ borrowerName: "", principalAmount: "", startDate: "" });
  const [error, setError] = useState<string | null>(null);

  const loadLoans = async () => {
    try {
      const response = await getLoans();
      setLoans(response.content);
    } catch (e) {
      setError((e as Error).message);
    }
  };

  useEffect(() => {
    void loadLoans();
  }, []);

  const filtered = useMemo(() => {
    return loans.filter((loan) => {
      const matchesSearch =
        search.trim().length === 0 ||
        loan.borrowerName.toLowerCase().includes(search.toLowerCase()) ||
        String(loan.id).includes(search.trim());
      const matchesStatus = status === "ALL" || loan.status === status;
      return matchesSearch && matchesStatus;
    });
  }, [loans, search, status]);

  const handleCreate = async (event: FormEvent) => {
    event.preventDefault();
    try {
      await createLoan({
        borrowerName: form.borrowerName,
        principalAmount: Number(form.principalAmount),
        startDate: form.startDate,
      });
      setForm({ borrowerName: "", principalAmount: "", startDate: "" });
      await loadLoans();
    } catch (e) {
      setError((e as Error).message);
    }
  };

  const handleClose = async (loanId: number) => {
    try {
      await closeLoan(loanId);
      await loadLoans();
    } catch (e) {
      setError((e as Error).message);
    }
  };

  return (
    <PageSection title="Loan Directory" subtitle="Search, filter, and drill into individual loan operations.">
      <div className="grid gap-3 xl:grid-cols-[2fr_1fr]">
        <Surface className="p-4">
          <div className="mb-3 grid gap-2 md:grid-cols-3">
            <Input
              className="md:col-span-2"
              placeholder="Search borrower or loan id"
              value={search}
              onChange={(event) => setSearch(event.target.value)}
            />
            <select className="input" value={status} onChange={(event) => setStatus(event.target.value as typeof status)}>
              <option value="ALL">All Statuses</option>
              <option value="ACTIVE">Active</option>
              <option value="CLOSED">Closed</option>
            </select>
          </div>

          <table className="table-base">
            <thead>
              <tr>
                <th>Loan</th>
                <th>Principal</th>
                <th>Start</th>
                <th>Status</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {filtered.map((loan) => (
                <tr key={loan.id}>
                  <td>
                    <p className="font-semibold">{loan.borrowerName}</p>
                    <p className="font-numeric text-xs text-[var(--text-secondary)]">#{loan.id}</p>
                  </td>
                  <td className="font-numeric">${Number(loan.principalAmount).toLocaleString()}</td>
                  <td>{loan.startDate}</td>
                  <td>
                    <Badge>{loan.status}</Badge>
                  </td>
                  <td className="text-right">
                    <div className="flex justify-end gap-2">
                      <Link className="btn-secondary" to={`/app/loans/${loan.id}/overview`}>
                        Open
                      </Link>
                      {loan.status === "ACTIVE" ? (
                        <Button variant="outline" onClick={() => void handleClose(loan.id)} type="button">
                          Close
                        </Button>
                      ) : null}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </Surface>

        <Surface className="p-4">
          <h2 className="panel-title">Create Loan</h2>
          <form className="mt-3 space-y-3" onSubmit={handleCreate}>
            <Input
              placeholder="Borrower name"
              value={form.borrowerName}
              onChange={(event) => setForm({ ...form, borrowerName: event.target.value })}
              required
            />
            <Input
              className="font-numeric"
              type="number"
              placeholder="Principal amount"
              value={form.principalAmount}
              onChange={(event) => setForm({ ...form, principalAmount: event.target.value })}
              required
            />
            <Input
              type="date"
              value={form.startDate}
              onChange={(event) => setForm({ ...form, startDate: event.target.value })}
              required
            />
            <Button className="w-full" type="submit">
              Create
            </Button>
          </form>
        </Surface>
      </div>
      {error ? <p className="mt-4 text-sm text-[var(--risk-high)]">{error}</p> : null}
    </PageSection>
  );
}
