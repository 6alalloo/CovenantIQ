import { FormEvent, useEffect, useMemo, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { closeLoan, createLoan, getLoans } from "../api/client";
import type { Loan } from "../types/api";
import { PageSection, Surface } from "../components/layout";
import { Badge } from "../components/ui/badge";
import { Button } from "../components/ui/button";
import { ConfirmDialog } from "../components/ui/confirm-dialog";
import { Input } from "../components/ui/input";
import { Select } from "../components/ui/select";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "../components/ui/table";
import { formatEnumLabel } from "../lib/format";

export function LoansPage() {
  const navigate = useNavigate();
  const [loans, setLoans] = useState<Loan[]>([]);
  const [search, setSearch] = useState("");
  const [status, setStatus] = useState<"ALL" | "ACTIVE" | "CLOSED">("ALL");
  const [form, setForm] = useState({ borrowerName: "", principalAmount: "", startDate: "" });
  const [error, setError] = useState<string | null>(null);
  const [pendingClose, setPendingClose] = useState<Loan | null>(null);
  const [isClosing, setIsClosing] = useState(false);

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

  const handleClose = async () => {
    if (!pendingClose) return;
    setIsClosing(true);
    try {
      await closeLoan(pendingClose.id);
      setPendingClose(null);
      await loadLoans();
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setIsClosing(false);
    }
  };

  return (
    <PageSection title="Loan Directory" subtitle="Search, filter, and drill into individual loan operations.">
      <div className="grid gap-3 xl:grid-cols-[2fr_1fr]">
        <Surface className="p-5">
          <div className="mb-3 grid gap-2 md:grid-cols-3">
            <Input
              className="md:col-span-2"
              placeholder="Search borrower or loan id"
              value={search}
              onChange={(event) => setSearch(event.target.value)}
            />
            <Select value={status} onChange={(event) => setStatus(event.target.value as typeof status)}>
              <option value="ALL">All Statuses</option>
              <option value="ACTIVE">{formatEnumLabel("ACTIVE")}</option>
              <option value="CLOSED">{formatEnumLabel("CLOSED")}</option>
            </Select>
          </div>

          <Table data-testid="loans-table">
            <TableHeader>
              <TableRow>
                <TableHead>Loan</TableHead>
                <TableHead>Principal</TableHead>
                <TableHead>Start</TableHead>
                <TableHead>Status</TableHead>
                <TableHead />
              </TableRow>
            </TableHeader>
            <TableBody>
              {filtered.map((loan) => (
                <TableRow
                  key={loan.id}
                  className="cursor-pointer"
                  data-testid={`loan-row-${loan.id}`}
                  onClick={() => navigate(`/app/loans/${loan.id}/overview`)}
                >
                  <TableCell>
                    <p className="font-semibold">{loan.borrowerName}</p>
                    <p className="font-numeric text-xs text-[var(--text-secondary)]">#{loan.id}</p>
                  </TableCell>
                  <TableCell className="font-numeric">${Number(loan.principalAmount).toLocaleString()}</TableCell>
                  <TableCell>{loan.startDate}</TableCell>
                  <TableCell>
                    <Badge>{formatEnumLabel(loan.status)}</Badge>
                  </TableCell>
                  <TableCell className="text-right">
                    <div className="flex justify-end gap-2">
                      <Link
                        className="btn-secondary"
                        to={`/app/loans/${loan.id}/overview`}
                        data-testid={`loan-view-${loan.id}`}
                        onClick={(event) => event.stopPropagation()}
                      >
                        View
                      </Link>
                      {loan.status === "ACTIVE" ? (
                        <Button
                          variant="outline"
                          onClick={(event) => {
                            event.stopPropagation();
                            setPendingClose(loan);
                          }}
                          type="button"
                        >
                          Close
                        </Button>
                      ) : null}
                    </div>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </Surface>

        <Surface className="p-5">
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
      <ConfirmDialog
        open={!!pendingClose}
        title="Close loan?"
        description={
          pendingClose
            ? `You are about to close loan #${pendingClose.id} for ${pendingClose.borrowerName}. This action changes loan status immediately.`
            : ""
        }
        confirmLabel="Confirm Close"
        isLoading={isClosing}
        onCancel={() => setPendingClose(null)}
        onConfirm={() => void handleClose()}
      />
      {error ? <p className="mt-4 text-sm text-[var(--risk-high)]">{error}</p> : null}
    </PageSection>
  );
}
