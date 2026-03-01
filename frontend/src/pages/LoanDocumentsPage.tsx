import { useEffect, useMemo, useState } from "react";
import { useParams } from "react-router-dom";
import {
  deleteAttachment,
  downloadAttachment,
  getAttachmentList,
  getCovenantResults,
  uploadAttachment,
} from "../api/client";
import type { AttachmentMetadata, CovenantResult } from "../types/api";
import { Surface } from "../components/layout";
import { Button } from "../components/ui/button";
import { Input } from "../components/ui/input";
import { Select } from "../components/ui/select";
import { formatDateTime } from "../lib/format";

export function LoanDocumentsPage() {
  const { loanId } = useParams();
  const numericLoanId = Number(loanId);
  const [results, setResults] = useState<CovenantResult[]>([]);
  const [selectedStatementId, setSelectedStatementId] = useState<number | null>(null);
  const [attachments, setAttachments] = useState<AttachmentMetadata[]>([]);
  const [file, setFile] = useState<File | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    (async () => {
      try {
        const page = await getCovenantResults(numericLoanId, 0, 200);
        setResults(page.content);
      } catch (e) {
        setError((e as Error).message);
      }
    })();
  }, [numericLoanId]);

  const statementIds = useMemo(() => {
    const ids = new Set<number>();
    results.forEach((item) => ids.add(item.financialStatementId));
    return [...ids].sort((a, b) => b - a);
  }, [results]);

  useEffect(() => {
    if (!statementIds.length) return;
    if (!selectedStatementId || !statementIds.includes(selectedStatementId)) {
      setSelectedStatementId(statementIds[0]);
    }
  }, [statementIds, selectedStatementId]);

  const loadAttachments = async (statementId: number) => {
    try {
      const rows = await getAttachmentList(statementId);
      setAttachments(rows);
    } catch (e) {
      setError((e as Error).message);
    }
  };

  useEffect(() => {
    if (selectedStatementId) {
      void loadAttachments(selectedStatementId);
    }
  }, [selectedStatementId]);

  const onUpload = async () => {
    if (!selectedStatementId || !file) return;
    try {
      await uploadAttachment(selectedStatementId, file);
      setFile(null);
      await loadAttachments(selectedStatementId);
    } catch (e) {
      setError((e as Error).message);
    }
  };

  const onDownload = async (attachmentId: number, filename: string) => {
    try {
      const response = await downloadAttachment(attachmentId);
      if (!response.ok) throw new Error(`HTTP ${response.status}`);
      const blob = await response.blob();
      const url = URL.createObjectURL(blob);
      const anchor = document.createElement("a");
      anchor.href = url;
      anchor.download = filename;
      anchor.click();
      URL.revokeObjectURL(url);
    } catch (e) {
      setError((e as Error).message);
    }
  };

  const onDelete = async (attachmentId: number) => {
    if (!selectedStatementId) return;
    try {
      await deleteAttachment(attachmentId);
      await loadAttachments(selectedStatementId);
    } catch (e) {
      setError((e as Error).message);
    }
  };

  return (
    <Surface className="p-5">
      <h2 className="panel-title">Statement Attachments</h2>
      <div className="mt-3 grid gap-3 md:grid-cols-[1fr_auto_auto]">
        <Select
          value={selectedStatementId ?? ""}
          onChange={(event) => setSelectedStatementId(Number(event.target.value))}
        >
          {statementIds.map((id) => (
            <option key={id} value={id}>
              Statement #{id}
            </option>
          ))}
        </Select>
        <Input type="file" onChange={(event) => setFile(event.target.files?.[0] ?? null)} />
        <Button type="button" onClick={() => void onUpload()}>
          Upload
        </Button>
      </div>

      <table className="table-base mt-3">
        <thead>
          <tr>
            <th>Filename</th>
            <th>Size</th>
            <th>Uploaded By</th>
            <th>Time</th>
            <th />
          </tr>
        </thead>
        <tbody>
          {attachments.map((item) => (
            <tr key={item.id}>
              <td>{item.filename}</td>
              <td className="font-numeric">{item.fileSize.toLocaleString()}</td>
              <td>{item.uploadedBy}</td>
              <td className="text-xs text-[var(--text-secondary)]">{formatDateTime(item.uploadedAt)}</td>
              <td className="text-right">
                <div className="flex justify-end gap-2">
                  <Button variant="outline" onClick={() => void onDownload(item.id, item.filename)}>
                    Download
                  </Button>
                  <Button variant="outline" onClick={() => void onDelete(item.id)}>
                    Delete
                  </Button>
                </div>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      {error ? <p className="mt-3 text-sm text-[var(--risk-high)]">{error}</p> : null}
    </Surface>
  );
}
